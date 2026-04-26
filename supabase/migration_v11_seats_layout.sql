-- Позиции мест на схеме зала (клетки условной сетки). Бронь по-прежнему по seat.id.
-- row_number / seat_number — «логические» ряд и номер на билете; layout_* — где рисовать в UI.

ALTER TABLE public.seats
  ADD COLUMN IF NOT EXISTS layout_x integer,
  ADD COLUMN IF NOT EXISTS layout_y integer;

COMMENT ON COLUMN public.seats.layout_x IS 'Колонка на схеме зала (0-based), null = брать из seat_number для отображения.';
COMMENT ON COLUMN public.seats.layout_y IS 'Строка на схеме зала (0-based), null = брать из row_number для отображения.';
