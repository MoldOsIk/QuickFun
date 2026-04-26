-- Исправление RLS для bucket venue-images (если уже применили migration_v7 со storage.foldername).
-- Путь объекта: {place_id}/{uuid}.jpg — split_part(name, '/', 1).
-- Если после этого загрузка всё ещё падает — выполните migration_v9 (обход RLS на places внутри политики Storage).
-- Выполнить в Supabase SQL Editor один раз.

DROP POLICY IF EXISTS "venue_images_insert_owner" ON storage.objects;
CREATE POLICY "venue_images_insert_owner"
  ON storage.objects FOR INSERT
  TO authenticated
  WITH CHECK (
    bucket_id = 'venue-images'
    AND EXISTS (
      SELECT 1 FROM public.places p
      WHERE p.id::text = split_part(name, '/', 1)
        AND (p.owner_id = auth.uid() OR public.is_admin())
    )
  );

DROP POLICY IF EXISTS "venue_images_delete_owner" ON storage.objects;
CREATE POLICY "venue_images_delete_owner"
  ON storage.objects FOR DELETE
  TO authenticated
  USING (
    bucket_id = 'venue-images'
    AND EXISTS (
      SELECT 1 FROM public.places p
      WHERE p.id::text = split_part(name, '/', 1)
        AND (p.owner_id = auth.uid() OR public.is_admin())
    )
  );
