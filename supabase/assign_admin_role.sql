-- Выполни после того, как пользователь-админ уже есть в Authentication.
--
-- Рекомендуемый способ:
-- 1) Supabase Dashboard → Authentication → Users → Add user
--    Email: admin@admin.com
--    Password: admin
--    Auto Confirm User: включи (чтобы не ждать письмо)
-- 2) Убедись, что сработал триггер handle_new_user: в public.users есть строка с тем же id.
-- 3) Запусти этот скрипт — привяжет роль admin к этому пользователю.

insert into public.user_roles (user_id, role_id)
select au.id, r.id
from auth.users au
cross join public.roles r
where au.email = 'admin@admin.com'
  and r.name = 'admin'
on conflict (user_id, role_id) do nothing;

-- Если нужно снять только роль user у этого же аккаунта (необязательно):
-- delete from public.user_roles
-- where user_id = (select id from auth.users where email = 'admin@admin.com')
--   and role_id = (select id from public.roles where name = 'user');
