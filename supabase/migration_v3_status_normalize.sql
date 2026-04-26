-- Опционально, если в каталоге «пропадают» одобренные места:
-- в данных могли оказаться статусы не в нижнем регистре (Approved, APPROVED) или с пробелами.
-- RLS в migration_v2 сравнивает строго с 'approved'.

UPDATE public.places
SET status = lower(trim(status))
WHERE status IS NOT NULL;

-- Пересоздать политику SELECT с устойчивым сравнением (после UPDATE выше можно не менять RLS;
-- этот блок — если не хотите трогать данные, а только ослабить сравнение):

drop policy if exists "places_select" on public.places;

create policy "places_select"
  on public.places for select
  using (
    lower(trim(coalesce(status, ''))) = 'approved'
    or owner_id = auth.uid()
    or public.is_admin()
  );
