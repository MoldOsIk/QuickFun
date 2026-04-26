-- Выполни в Supabase → SQL Editor (один раз).
-- 1) Роль "admin" в public.roles
-- 2) Функция public.is_admin() для RLS
-- 3) Политики: чтение одобренных мест всем с сессией; вставка мест/локаций/категорий — только admin
--
-- Вход в приложение: email admin@admin.com, пароль admin (логин "admin" в смысле email/пароль — см. assign_admin_role.sql).

-- Роль в справочнике
insert into public.roles (name)
values ('admin')
on conflict (name) do nothing;

-- Проверка роли текущего пользователя (SECURITY DEFINER обходит RLS на user_roles при чтении)
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.user_roles ur
    join public.roles r on r.id = ur.role_id
    where ur.user_id = auth.uid()
      and r.name = 'admin'
  );
$$;

grant execute on function public.is_admin() to authenticated, anon;

-- RLS
alter table public.places enable row level security;
alter table public.locations enable row level security;
alter table public.categories enable row level security;

drop policy if exists "places_select" on public.places;
drop policy if exists "places_insert_admin" on public.places;

create policy "places_select"
  on public.places
  for select
  using (
    coalesce(status, '') = 'approved'
    or public.is_admin()
  );

create policy "places_insert_admin"
  on public.places
  for insert
  with check (public.is_admin());

drop policy if exists "locations_select" on public.locations;
drop policy if exists "locations_insert_admin" on public.locations;

create policy "locations_select"
  on public.locations
  for select
  using (true);

create policy "locations_insert_admin"
  on public.locations
  for insert
  with check (public.is_admin());

drop policy if exists "categories_select" on public.categories;
drop policy if exists "categories_insert_admin" on public.categories;

create policy "categories_select"
  on public.categories
  for select
  using (true);

create policy "categories_insert_admin"
  on public.categories
  for insert
  with check (public.is_admin());

-- Обновление мест и локаций (редактирование в приложении)
drop policy if exists "places_update_admin" on public.places;
create policy "places_update_admin"
  on public.places
  for update
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists "locations_update_admin" on public.locations;
create policy "locations_update_admin"
  on public.locations
  for update
  to authenticated
  using (public.is_admin())
  with check (public.is_admin());
