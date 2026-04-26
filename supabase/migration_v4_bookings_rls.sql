-- Бронирования: RLS, уникальность активной брони, RPC списка свободных слотов.
-- Выполнить в Supabase SQL Editor после migration_v2 (и при необходимости v3).

-- Связь user_id с auth. Сначала убрать «осиротевшие» строки: иначе ALTER ADD CONSTRAINT падает
-- (uuid в bookings нет в auth.users — удалённый аккаунт, тесты, импорт).
DELETE FROM public.bookings b
WHERE b.user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = b.user_id);

-- Если прошлый запуск упал после части шагов — снять обломки и создать заново
ALTER TABLE public.bookings DROP CONSTRAINT IF EXISTS bookings_user_id_fkey;

ALTER TABLE public.bookings
  ADD CONSTRAINT bookings_user_id_fkey
  FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

-- Одна активная бронь на пару (слот, место)
CREATE UNIQUE INDEX IF NOT EXISTS bookings_one_active_per_slot_seat
  ON public.bookings (time_slot_id, seat_id)
  WHERE status = 'active';

-- Вспомогательная функция: владелец заведения
CREATE OR REPLACE FUNCTION public.is_place_owner(p_place_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.places p
    WHERE p.id = p_place_id AND p.owner_id = auth.uid()
  );
$$;

GRANT EXECUTE ON FUNCTION public.is_place_owner(uuid) TO authenticated, anon;

-- Свободные варианты брони (без user_id) — для каталога; SECURITY DEFINER обходит RLS на bookings
CREATE OR REPLACE FUNCTION public.available_booking_slots(p_place_id uuid)
RETURNS TABLE (
  time_slot_id integer,
  seat_id integer,
  start_time timestamp without time zone,
  end_time timestamp without time zone
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT ts.id, s.id, ts.start_time, ts.end_time
  FROM public.time_slots ts
  CROSS JOIN public.seats s
  WHERE ts.place_id = p_place_id
    AND s.place_id = p_place_id
    AND ts.start_time >= now()
    AND EXISTS (
      SELECT 1 FROM public.places p
      WHERE p.id = p_place_id
        AND lower(trim(coalesce(p.status, ''))) = 'approved'
    )
    AND NOT EXISTS (
      SELECT 1 FROM public.bookings b
      WHERE b.time_slot_id = ts.id
        AND b.seat_id = s.id
        AND b.status = 'active'
    );
$$;

GRANT EXECUTE ON FUNCTION public.available_booking_slots(uuid) TO authenticated;

ALTER TABLE public.time_slots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.seats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "time_slots_select" ON public.time_slots;
DROP POLICY IF EXISTS "time_slots_insert" ON public.time_slots;
DROP POLICY IF EXISTS "time_slots_update" ON public.time_slots;
DROP POLICY IF EXISTS "time_slots_delete" ON public.time_slots;

CREATE POLICY "time_slots_select"
  ON public.time_slots FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.places p
      WHERE p.id = time_slots.place_id
        AND (
          lower(trim(coalesce(p.status, ''))) = 'approved'
          OR p.owner_id = auth.uid()
          OR public.is_admin()
        )
    )
  );

CREATE POLICY "time_slots_insert"
  ON public.time_slots FOR INSERT
  WITH CHECK (public.is_place_owner(place_id));

CREATE POLICY "time_slots_update"
  ON public.time_slots FOR UPDATE
  USING (public.is_place_owner(place_id))
  WITH CHECK (public.is_place_owner(place_id));

CREATE POLICY "time_slots_delete"
  ON public.time_slots FOR DELETE
  USING (public.is_place_owner(place_id));

DROP POLICY IF EXISTS "seats_select" ON public.seats;
DROP POLICY IF EXISTS "seats_insert" ON public.seats;
DROP POLICY IF EXISTS "seats_update" ON public.seats;
DROP POLICY IF EXISTS "seats_delete" ON public.seats;

CREATE POLICY "seats_select"
  ON public.seats FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM public.places p
      WHERE p.id = seats.place_id
        AND (
          lower(trim(coalesce(p.status, ''))) = 'approved'
          OR p.owner_id = auth.uid()
          OR public.is_admin()
        )
    )
  );

CREATE POLICY "seats_insert"
  ON public.seats FOR INSERT
  WITH CHECK (public.is_place_owner(place_id));

CREATE POLICY "seats_update"
  ON public.seats FOR UPDATE
  USING (public.is_place_owner(place_id))
  WITH CHECK (public.is_place_owner(place_id));

CREATE POLICY "seats_delete"
  ON public.seats FOR DELETE
  USING (public.is_place_owner(place_id));

DROP POLICY IF EXISTS "bookings_select" ON public.bookings;
DROP POLICY IF EXISTS "bookings_insert" ON public.bookings;
DROP POLICY IF EXISTS "bookings_update" ON public.bookings;

CREATE POLICY "bookings_select"
  ON public.bookings FOR SELECT
  USING (
    user_id = auth.uid()
    OR public.is_admin()
    OR EXISTS (
      SELECT 1
      FROM public.time_slots ts
      JOIN public.places p ON p.id = ts.place_id
      WHERE ts.id = time_slot_id
        AND p.owner_id = auth.uid()
    )
  );

CREATE POLICY "bookings_insert"
  ON public.bookings FOR INSERT
  WITH CHECK (
    user_id = auth.uid()
    AND EXISTS (
      SELECT 1
      FROM public.time_slots ts
      JOIN public.places p ON p.id = ts.place_id
      JOIN public.seats s ON s.id = seat_id AND s.place_id = p.id
      WHERE ts.id = time_slot_id
        AND ts.place_id = p.id
        AND lower(trim(coalesce(p.status, ''))) = 'approved'
    )
  );

CREATE POLICY "bookings_update"
  ON public.bookings FOR UPDATE
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid() AND status IN ('active', 'cancelled'));
