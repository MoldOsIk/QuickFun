-- Подсчёт броней по заведению за всё время (для сортировки каталога по популярности).
-- Выполнить в Supabase SQL Editor после предыдущих миграций.

CREATE OR REPLACE FUNCTION public.catalog_place_booking_counts()
RETURNS TABLE (place_id uuid, booking_count bigint)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT p.id AS place_id,
         COUNT(b.id)::bigint AS booking_count
  FROM public.places p
  LEFT JOIN public.time_slots ts ON ts.place_id = p.id
  LEFT JOIN public.bookings b ON b.time_slot_id = ts.id
  GROUP BY p.id;
$$;

REVOKE ALL ON FUNCTION public.catalog_place_booking_counts() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.catalog_place_booking_counts() TO anon, authenticated;
