-- Причина отклонения заявки, повторная подача владельцем, обновлённый approve_place.
-- Выполнить в Supabase SQL Editor после предыдущих миграций.

ALTER TABLE public.places
  ADD COLUMN IF NOT EXISTS rejection_reason text;

COMMENT ON COLUMN public.places.rejection_reason IS
  'Текст от главного админа при отклонении; очищается при одобрении или повторной подаче.';

-- Старая сигнатура (2 аргумента) — убрать, чтобы PostgREST вызывал одну функцию.
DROP FUNCTION IF EXISTS public.approve_place(uuid, boolean);

CREATE OR REPLACE FUNCTION public.approve_place(
  p_place_id uuid,
  p_approved boolean,
  p_rejection_reason text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF NOT public.is_admin() THEN
    RAISE EXCEPTION 'Not allowed';
  END IF;
  UPDATE public.places
  SET
    status = CASE WHEN p_approved THEN 'approved' ELSE 'rejected' END,
    rejection_reason = CASE
      WHEN p_approved THEN NULL
      ELSE NULLIF(trim(coalesce(p_rejection_reason, '')), '')
    END
  WHERE id = p_place_id;
END;
$$;

REVOKE ALL ON FUNCTION public.approve_place(uuid, boolean, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.approve_place(uuid, boolean, text) TO authenticated;

-- Владелец: снова на модерацию после отклонения (только rejected → pending).
CREATE OR REPLACE FUNCTION public.resubmit_place_after_rejection(p_place_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  IF auth.uid() IS NULL THEN
    RAISE EXCEPTION 'Not authenticated';
  END IF;
  IF NOT public.is_place_owner(p_place_id) THEN
    RAISE EXCEPTION 'forbidden';
  END IF;
  UPDATE public.places
  SET status = 'pending', rejection_reason = NULL
  WHERE id = p_place_id
    AND owner_id = auth.uid()
    AND lower(trim(coalesce(status, ''))) = 'rejected';
  IF NOT FOUND THEN
    RAISE EXCEPTION 'not_rejected_or_not_owner';
  END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.resubmit_place_after_rejection(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.resubmit_place_after_rejection(uuid) TO authenticated;
