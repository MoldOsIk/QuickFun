-- Отзывы о заведениях: оценка 1–10 + текст. Один отзыв на пользователя на заведение (обновление через UPDATE).
-- Выполнить в Supabase SQL Editor после предыдущих миграций.

CREATE TABLE IF NOT EXISTS public.place_reviews (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  place_id uuid NOT NULL REFERENCES public.places (id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  rating smallint NOT NULL CHECK (rating >= 1 AND rating <= 10),
  body text NOT NULL CHECK (char_length(trim(body)) >= 3),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT place_reviews_place_user_unique UNIQUE (place_id, user_id)
);

CREATE INDEX IF NOT EXISTS place_reviews_place_id_idx
  ON public.place_reviews (place_id);

CREATE INDEX IF NOT EXISTS place_reviews_place_created_idx
  ON public.place_reviews (place_id, created_at DESC);

ALTER TABLE public.place_reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "place_reviews_select" ON public.place_reviews;
CREATE POLICY "place_reviews_select"
  ON public.place_reviews FOR SELECT
  USING (
    EXISTS (
      SELECT 1
      FROM public.places p
      WHERE p.id = place_reviews.place_id
        AND (
          coalesce(p.status, '') = 'approved'
          OR (auth.uid() IS NOT NULL AND p.owner_id = auth.uid())
          OR (auth.uid() IS NOT NULL AND public.is_admin())
        )
    )
  );

DROP POLICY IF EXISTS "place_reviews_insert" ON public.place_reviews;
CREATE POLICY "place_reviews_insert"
  ON public.place_reviews FOR INSERT
  TO authenticated
  WITH CHECK (
    user_id = auth.uid()
    AND EXISTS (
      SELECT 1
      FROM public.places p
      WHERE p.id = place_id
        AND coalesce(p.status, '') = 'approved'
        AND p.owner_id IS DISTINCT FROM auth.uid()
    )
  );

DROP POLICY IF EXISTS "place_reviews_update_own" ON public.place_reviews;
CREATE POLICY "place_reviews_update_own"
  ON public.place_reviews FOR UPDATE
  TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (
    user_id = auth.uid()
    AND EXISTS (
      SELECT 1
      FROM public.places p
      WHERE p.id = place_id
        AND coalesce(p.status, '') = 'approved'
        AND p.owner_id IS DISTINCT FROM auth.uid()
    )
  );

DROP POLICY IF EXISTS "place_reviews_delete_own" ON public.place_reviews;
CREATE POLICY "place_reviews_delete_own"
  ON public.place_reviews FOR DELETE
  TO authenticated
  USING (user_id = auth.uid());

GRANT SELECT ON public.place_reviews TO anon, authenticated;
GRANT INSERT, UPDATE, DELETE ON public.place_reviews TO authenticated;
