ALTER TABLE public.gastos_operativos ADD COLUMN IF NOT EXISTS es_recurrente BOOLEAN DEFAULT false;
ALTER TABLE public.gastos_operativos ADD COLUMN IF NOT EXISTS frecuencia TEXT DEFAULT 'mensual';;
