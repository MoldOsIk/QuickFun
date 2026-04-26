-- Выполнить в Supabase SQL Editor после базовых скриптов (handle_new_user, roles user/admin, assign_default_user_role).
-- Добавляет роль place_admin, RPC регистрации заведения и модерации, обновляет RLS.

-- Роли
insert into public.roles (name) values ('place_admin')
on conflict (name) do nothing;

-- is_admin() = главный администратор (модерация) — уже есть; оставляем имя 'admin'

create or replace function public.is_place_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.user_roles ur
    join public.roles r on r.id = ur.role_id
    where ur.user_id = auth.uid() and r.name = 'place_admin'
  );
$$;

grant execute on function public.is_place_admin() to authenticated, anon;

-- RPC: регистрация заведения владельцем (статус pending + роль place_admin)
create or replace function public.register_place_as_owner(
  p_name text,
  p_description text,
  p_city text,
  p_address text,
  p_category_id integer,
  p_latitude double precision,
  p_longitude double precision
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_loc_id int;
  v_place_id uuid;
  v_role_id int;
begin
  if v_uid is null then
    raise exception 'Not authenticated';
  end if;
  if p_name is null or trim(p_name) = '' then
    raise exception 'Name required';
  end if;

  insert into public.locations (latitude, longitude, address, city)
  values (p_latitude, p_longitude, nullif(trim(p_address), ''), nullif(trim(p_city), ''))
  returning id into v_loc_id;

  insert into public.places (name, description, category_id, location_id, owner_id, status)
  values (
    trim(p_name),
    nullif(trim(coalesce(p_description, '')), ''),
    p_category_id,
    v_loc_id,
    v_uid,
    'pending'
  )
  returning id into v_place_id;

  select id into v_role_id from public.roles where name = 'place_admin' limit 1;
  if v_role_id is not null then
    insert into public.user_roles (user_id, role_id)
    values (v_uid, v_role_id)
    on conflict (user_id, role_id) do nothing;
  end if;

  return v_place_id;
end;
$$;

grant execute on function public.register_place_as_owner(
  text, text, text, text, integer, double precision, double precision
) to authenticated;

-- RPC: одобрение / отклонение (только главный admin)
create or replace function public.approve_place(p_place_id uuid, p_approved boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Not allowed';
  end if;
  update public.places
  set status = case when p_approved then 'approved' else 'rejected' end
  where id = p_place_id;
end;
$$;

grant execute on function public.approve_place(uuid, boolean) to authenticated;

-- Пересоздание политик places
drop policy if exists "places_select" on public.places;
drop policy if exists "places_insert_admin" on public.places;
drop policy if exists "places_insert" on public.places;
drop policy if exists "places_update_admin" on public.places;
drop policy if exists "places_update" on public.places;

create policy "places_select"
  on public.places for select
  using (
    coalesce(status, '') = 'approved'
    or owner_id = auth.uid()
    or public.is_admin()
  );

create policy "places_insert"
  on public.places for insert
  with check (
    public.is_admin()
    or (
      auth.uid() is not null
      and owner_id = auth.uid()
      and coalesce(status, '') = 'pending'
    )
  );

create policy "places_update"
  on public.places for update
  to authenticated
  using (public.is_admin() or owner_id = auth.uid())
  with check (public.is_admin() or owner_id = auth.uid());

-- locations: разрешить вставку владельцу через прямой API (если не используете только RPC) — оставим как было для admin;
-- обновим insert для совместимости с createPlace от admin
drop policy if exists "locations_insert_admin" on public.locations;
drop policy if exists "locations_insert" on public.locations;

create policy "locations_insert"
  on public.locations for insert
  with check (public.is_admin());

drop policy if exists "locations_update_admin" on public.locations;
drop policy if exists "locations_update" on public.locations;

create policy "locations_update"
  on public.locations for update
  to authenticated
  using (public.is_admin() or exists (
    select 1 from public.places p
    where p.location_id = locations.id and p.owner_id = auth.uid()
  ))
  with check (public.is_admin() or exists (
    select 1 from public.places p
    where p.location_id = locations.id and p.owner_id = auth.uid()
  ));
