-- Координаты заведения необязательны: карта может опираться на адрес (геокодирование в приложении).
-- Выполнить в Supabase SQL Editor после предыдущих миграций.

ALTER TABLE public.locations
  ALTER COLUMN latitude DROP NOT NULL,
  ALTER COLUMN longitude DROP NOT NULL;

COMMENT ON COLUMN public.locations.latitude IS 'WGS84; может быть NULL, если задан только адрес.';
COMMENT ON COLUMN public.locations.longitude IS 'WGS84; может быть NULL, если задан только адрес.';

-- RPC register_place_as_owner: параметры p_latitude/p_longitude могут быть NULL после этой миграции
-- (сигнатуру менять не требуется).
