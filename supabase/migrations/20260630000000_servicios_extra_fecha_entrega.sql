ALTER TABLE public.servicios_extra ADD COLUMN fecha_entrega DATE;
COMMENT ON COLUMN public.servicios_extra.fecha_entrega IS 'Fecha de entrega efectiva del servicio extra';
