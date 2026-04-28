-- Телефон пользователя (E.164, например +79001234567) + RLS: владелец/админ видят гостей со бронью на своё место.

ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS phone_e164 text;

COMMENT ON COLUMN public.users.phone_e164 IS 'Международный формат E.164 (+ и цифры), опционально';

-- Триггер: копировать phone_e164 из raw_user_meta_data при регистрации
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.users (id, name, phone_e164)
  VALUES (
    new.id,
    COALESCE(
      new.raw_user_meta_data->>'full_name',
      new.raw_user_meta_data->>'name',
      NULLIF(split_part(COALESCE(new.email, ''), '@', 1), '')
    ),
    NULLIF(trim(COALESCE(new.raw_user_meta_data->>'phone_e164', '')), '')
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN new;
END;
$$;

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_select_policy" ON public.users;
CREATE POLICY "users_select_policy"
  ON public.users FOR SELECT
  TO authenticated
  USING (
    auth.uid() = id
    OR public.is_admin()
    OR EXISTS (
      SELECT 1
      FROM public.bookings b
      JOIN public.time_slots ts ON ts.id = b.time_slot_id
      JOIN public.places pl ON pl.id = ts.place_id
      WHERE b.user_id = users.id
        AND pl.owner_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS "users_update_own" ON public.users;
CREATE POLICY "users_update_own"
  ON public.users FOR UPDATE
  TO authenticated
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);
