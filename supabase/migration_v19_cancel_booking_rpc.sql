-- Отмена брони: гость (user_id), владелец площадки или админ.
-- Прямой UPDATE чужой брони владельцем блокируется RLS (bookings_update только user_id = auth.uid());
-- RPC SECURITY DEFINER обходит RLS при проверке прав внутри функции.

CREATE OR REPLACE FUNCTION public.cancel_booking(p_booking_id integer)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_user_id uuid;
  v_ts_id integer;
  v_status text;
BEGIN
  SELECT b.user_id, b.time_slot_id, lower(trim(coalesce(b.status, '')))
  INTO v_user_id, v_ts_id, v_status
  FROM public.bookings b
  WHERE b.id = p_booking_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'booking not found';
  END IF;

  IF v_status <> 'active' THEN
    RAISE EXCEPTION 'booking not active';
  END IF;

  IF v_user_id = auth.uid() THEN
    UPDATE public.bookings SET status = 'cancelled' WHERE id = p_booking_id;
    RETURN;
  END IF;

  IF EXISTS (
    SELECT 1
    FROM public.time_slots ts
    JOIN public.places p ON p.id = ts.place_id
    WHERE ts.id = v_ts_id
      AND (p.owner_id = auth.uid() OR public.is_admin())
  ) THEN
    UPDATE public.bookings SET status = 'cancelled' WHERE id = p_booking_id;
    RETURN;
  END IF;

  RAISE EXCEPTION 'not allowed';
END;
$$;

REVOKE ALL ON FUNCTION public.cancel_booking(integer) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.cancel_booking(integer) TO authenticated;
