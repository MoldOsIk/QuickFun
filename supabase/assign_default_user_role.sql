-- Run once in Supabase SQL Editor.
-- Assigns default role "user" to every newly created profile row in public.users.

create or replace function public.assign_default_user_role()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_role_id integer;
begin
  select id into v_role_id
  from public.roles
  where name = 'user'
  limit 1;

  if v_role_id is null then
    insert into public.roles(name)
    values ('user')
    on conflict (name) do nothing;

    select id into v_role_id
    from public.roles
    where name = 'user'
    limit 1;
  end if;

  insert into public.user_roles(user_id, role_id)
  values (new.id, v_role_id)
  on conflict (user_id, role_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_public_user_created_assign_role on public.users;

create trigger on_public_user_created_assign_role
after insert on public.users
for each row
execute function public.assign_default_user_role();
