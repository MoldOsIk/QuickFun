-- Подпись сеанса для кино (фильм) + отдача в RPC свободных слотов.
-- Выполнить в Supabase SQL Editor после migration_v5.

ALTER TABLE public.time_slots
  ADD COLUMN IF NOT EXISTS label text;

COMMENT ON COLUMN public.time_slots.label IS 'Для кино: название фильма / сеанса; для остальных — опционально.';

DROP FUNCTION IF EXISTS public.available_booking_slots(uuid);

CREATE FUNCTION public.available_booking_slots(p_place_id uuid)
RETURNS TABLE (
  time_slot_id integer,
  seat_id integer,
  start_time timestamp without time zone,
  end_time timestamp without time zone,
  row_number integer,
  seat_number integer,
  seat_label text,
  session_label text
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
    NULLIF(trim(coalesce(s.label, '')), '') AS seat_label,
    NULLIF(trim(coalesce(ts.label, '')), '') AS session_label
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
