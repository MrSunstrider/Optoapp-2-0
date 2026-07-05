-- Migration: Triggers to keep ventas in sync with dispensaciones and servicios_extra
-- These are the single authority for ventas writes on the server side.
-- Android does NOT upload ventas — these triggers handle server-side creation.

-- Trigger function for dispensaciones
CREATE OR REPLACE FUNCTION public.fn_upsert_venta_from_dispensacion()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.ventas (
        id, optica_id, origen, origen_id, paciente_id,
        fecha, fecha_entrega, monto_total, estado
    ) VALUES (
        'v_disp_' || NEW.id,
        NEW.optica_id,
        'dispensacion',
        NEW.id,
        COALESCE(NEW.paciente_id, ''),
        NEW.fecha,
        NEW.fecha_entrega,
        NEW.monto_total,
        NEW.estado_entrega
    )
    ON CONFLICT (id) DO UPDATE SET
        monto_total = EXCLUDED.monto_total,
        estado = EXCLUDED.estado,
        fecha_entrega = EXCLUDED.fecha_entrega,
        paciente_id = EXCLUDED.paciente_id,
        updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_dispensacion_to_venta ON public.dispensaciones;

CREATE TRIGGER trg_dispensacion_to_venta
    AFTER INSERT OR UPDATE OF monto_total, estado_entrega, fecha_entrega, paciente_id
    ON public.dispensaciones
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_upsert_venta_from_dispensacion();

-- Trigger function for servicios_extra
CREATE OR REPLACE FUNCTION public.fn_upsert_venta_from_servicio_extra()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.ventas (
        id, optica_id, origen, origen_id, paciente_id,
        fecha, fecha_entrega, monto_total, estado
    ) VALUES (
        'v_serv_' || NEW.id,
        NEW.optica_id,
        'servicio_extra',
        NEW.id,
        COALESCE(NEW.paciente_id, ''),
        NEW.fecha,
        NEW.fecha_entrega,
        NEW.monto_total,
        NEW.estado
    )
    ON CONFLICT (id) DO UPDATE SET
        monto_total = EXCLUDED.monto_total,
        estado = EXCLUDED.estado,
        fecha_entrega = EXCLUDED.fecha_entrega,
        paciente_id = EXCLUDED.paciente_id,
        updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_servicio_to_venta ON public.servicios_extra;

CREATE TRIGGER trg_servicio_to_venta
    AFTER INSERT OR UPDATE OF monto_total, estado, fecha_entrega, paciente_id
    ON public.servicios_extra
    FOR EACH ROW
    EXECUTE FUNCTION public.fn_upsert_venta_from_servicio_extra();
