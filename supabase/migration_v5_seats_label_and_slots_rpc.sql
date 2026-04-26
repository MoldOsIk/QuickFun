-- Метки мест для UI + расширение RPC свободных слотов (ряд/место/подпись).
-- Выполнить в Supabase SQL Editor после migration_v4_bookings_rls.sql.

ALTER TABLE public.seats
  ADD COLUMN IF NOT EXISTS label text;

COMMENT ON COLUMN public.seats.label IS 'Человекочитаемая подпись: дорожка, стол, ряд/место, комната';

-- Старый available_booking_slots(uuid) возвращал 4 колонки — REPLACE нельзя при смене OUT-типа (42P13).
DROP FUNCTION IF EXISTS public.available_booking_slots(uuid);

-- Свободные слоты: те же правила, плюс данные места для клиента (без сырого SQL в приложении — только rpc()).
CREATE FUNCTION public.available_booking_slots(p_place_id uuid)
RETURNS TABLE (
  time_slot_id integer,
  seat_id integer,
  start_time timestamp without time zone,
  end_time timestamp without time zone,
  row_number integer,
  seat_number integer,
  seat_label text
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT
    ts.id,
    s.id,
    ts.start_time,
    ts.end_time,
    s.row_number,
    s.seat_number,
    NULLIF(trim(coalesce(s.label, '')), '') AS seat_label
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
