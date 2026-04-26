-- Исправление RLS: подзапросы к public.places из политик storage.objects и place_gallery
-- выполняются с RLS на places — строка может быть «невидима», EXISTS ложен, INSERT падает.
-- Решение: SECURITY DEFINER — чтение places без RLS, auth.uid() остаётся от сессии пользователя.
--
-- Выполнить в Supabase SQL Editor один раз (если уже применяли migration_v7 / v8).

CREATE OR REPLACE FUNCTION public.is_place_owner_or_admin(p_place_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.places p
    WHERE p.id = p_place_id
      AND (p.owner_id = auth.uid() OR public.is_admin())
  );
$$;

CREATE OR REPLACE FUNCTION public.user_can_select_place_gallery(p_place_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.places p
    WHERE p.id = p_place_id
      AND (
        coalesce(p.status, '') = 'approved'
        OR p.owner_id = auth.uid()
        OR public.is_admin()
      )
  );
$$;

CREATE OR REPLACE FUNCTION public.can_manage_venue_storage_path(path_name text)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT public.is_place_owner_or_admin(
    (split_part(trim(path_name), '/', 1))::uuid
  );
$$;

REVOKE ALL ON FUNCTION public.is_place_owner_or_admin(uuid) FROM public;
REVOKE ALL ON FUNCTION public.user_can_select_place_gallery(uuid) FROM public;
REVOKE ALL ON FUNCTION public.can_manage_venue_storage_path(text) FROM public;
GRANT EXECUTE ON FUNCTION public.is_place_owner_or_admin(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.user_can_select_place_gallery(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.can_manage_venue_storage_path(text) TO authenticated;

DROP POLICY IF EXISTS "place_gallery_select" ON public.place_gallery;
CREATE POLICY "place_gallery_select"
  ON public.place_gallery FOR SELECT
  USING (public.user_can_select_place_gallery(place_id));

DROP POLICY IF EXISTS "place_gallery_insert" ON public.place_gallery;
CREATE POLICY "place_gallery_insert"
  ON public.place_gallery FOR INSERT
  WITH CHECK (public.is_place_owner_or_admin(place_id));

DROP POLICY IF EXISTS "place_gallery_delete" ON public.place_gallery;
CREATE POLICY "place_gallery_delete"
  ON public.place_gallery FOR DELETE
  USING (public.is_place_owner_or_admin(place_id));

DROP POLICY IF EXISTS "venue_images_insert_owner" ON storage.objects;
CREATE POLICY "venue_images_insert_owner"
  ON storage.objects FOR INSERT
  TO authenticated
  WITH CHECK (
    bucket_id = 'venue-images'
    AND public.can_manage_venue_storage_path(name)
  );

DROP POLICY IF EXISTS "venue_images_delete_owner" ON storage.objects;
CREATE POLICY "venue_images_delete_owner"
  ON storage.objects FOR DELETE
  TO authenticated
  USING (
    bucket_id = 'venue-images'
    AND public.can_manage_venue_storage_path(name)
  );
