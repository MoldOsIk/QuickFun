-- Владелец: удаление сеанса (слота) с бронями; удаление одного места с бронями; очистка всех мест зала.
-- Выполнить в Supabase SQL Editor после предыдущих миграций.

CREATE OR REPLACE FUNCTION public.owner_delete_seat(p_seat_id integer)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  pid uuid;
BEGIN
  SELECT s.place_id INTO pid FROM public.seats s WHERE s.id = p_seat_id;
  IF pid IS NULL THEN
    RETURN;
  END IF;
  IF NOT (public.is_place_owner(pid) OR public.is_admin()) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  DELETE FROM public.bookings b WHERE b.seat_id = p_seat_id;
  DELETE FROM public.seats WHERE id = p_seat_id;
END;
$$;

REVOKE ALL ON FUNCTION public.owner_delete_seat(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.owner_delete_seat(integer) TO authenticated;

CREATE OR REPLACE FUNCTION public.clear_place_seats(p_place_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT (public.is_place_owner(p_place_id) OR public.is_admin()) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  DELETE FROM public.bookings b
  USING public.seats s
  WHERE b.seat_id = s.id AND s.place_id = p_place_id;
  DELETE FROM public.seats WHERE place_id = p_place_id;
END;
$$;

REVOKE ALL ON FUNCTION public.clear_place_seats(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.clear_place_seats(uuid) TO authenticated;

CREATE OR REPLACE FUNCTION public.owner_delete_time_slot(p_slot_id integer)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  pid uuid;
BEGIN
  SELECT ts.place_id INTO pid FROM public.time_slots ts WHERE ts.id = p_slot_id;
  IF pid IS NULL THEN
    RETURN;
  END IF;
  IF NOT (public.is_place_owner(pid) OR public.is_admin()) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  DELETE FROM public.bookings b WHERE b.time_slot_id = p_slot_id;
  DELETE FROM public.time_slots WHERE id = p_slot_id;
END;
$$;

REVOKE ALL ON FUNCTION public.owner_delete_time_slot(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.owner_delete_time_slot(integer) TO authenticated;
