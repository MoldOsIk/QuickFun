-- Учёт отзывов без броней в catalog_sort_score (после v16).
-- Раньше ln(1+0)=0 давало score=0 при любых отзывах, если броней не было.

CREATE OR REPLACE FUNCTION public.catalog_place_catalog_scores()
RETURNS TABLE (
  place_id uuid,
  booking_count bigint,
  review_count bigint,
  avg_rating double precision,
  bayes_rating double precision,
  catalog_sort_score double precision
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  WITH booking_counts AS (
    SELECT p.id AS pid,
           COUNT(b.id)::bigint AS cnt
    FROM public.places p
    LEFT JOIN public.time_slots ts ON ts.place_id = p.id
    LEFT JOIN public.bookings b ON b.time_slot_id = ts.id
    GROUP BY p.id
  ),
  review_agg AS (
    SELECT r.place_id AS pid,
           COUNT(*)::bigint AS rcnt,
           AVG(r.rating::double precision) AS avg_r,
           SUM(r.rating::double precision) AS sum_r
    FROM public.place_reviews r
    GROUP BY r.place_id
  )
  SELECT
    p.id AS place_id,
    COALESCE(bc.cnt, 0)::bigint AS booking_count,
    COALESCE(ra.rcnt, 0)::bigint AS review_count,
    CASE WHEN COALESCE(ra.rcnt, 0) > 0 THEN ra.avg_r ELSE NULL END AS avg_rating,
    ((8.0::double precision * 5.5::double precision + COALESCE(ra.sum_r, 0.0::double precision))
      / (8.0::double precision + COALESCE(ra.rcnt, 0)::double precision)) AS bayes_rating,
    (
      LN(1.0::double precision + COALESCE(bc.cnt, 0)::double precision)
      + 0.22::double precision
        * LN(1.0::double precision + COALESCE(ra.rcnt, 0)::double precision)
    )
    * ((8.0::double precision * 5.5::double precision + COALESCE(ra.sum_r, 0.0::double precision))
        / (8.0::double precision + COALESCE(ra.rcnt, 0)::double precision)) AS catalog_sort_score
  FROM public.places p
  LEFT JOIN booking_counts bc ON bc.pid = p.id
  LEFT JOIN review_agg ra ON ra.pid = p.id;
$$;

REVOKE ALL ON FUNCTION public.catalog_place_catalog_scores() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.catalog_place_catalog_scores() TO anon, authenticated;
