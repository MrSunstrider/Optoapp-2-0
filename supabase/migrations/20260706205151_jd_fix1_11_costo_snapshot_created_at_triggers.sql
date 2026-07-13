-- Fix #1 + #11: Add costo_unitario_snapshot and created_at to venta triggers

CREATE OR REPLACE FUNCTION public.fn_upsert_venta_from_dispensacion()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.ventas (
        id, optica_id, origen, origen_id, paciente_id,
        fecha, fecha_entrega, monto_total, costo_unitario_snapshot, estado,
        created_at
    ) VALUES (
        'v_disp_' || NEW.id,
        NEW.optica_id,
        'dispensacion',
        NEW.id,
        COALESCE(NEW.paciente_id, ''),
        NEW.fecha,
        NEW.fecha_entrega,
        NEW.monto_total,
        NULL, -- costo_unitario_snapshot comes from Android sync, not computed here
        NEW.estado_entrega,
        timezone('utc', now())
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

CREATE OR REPLACE FUNCTION public.fn_upsert_venta_from_servicio_extra()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.ventas (
        id, optica_id, origen, origen_id, paciente_id,
        fecha, fecha_entrega, monto_total, costo_unitario_snapshot, estado,
        created_at
    ) VALUES (
        'v_serv_' || NEW.id,
        NEW.optica_id,
        'servicio_extra',
        NEW.id,
        COALESCE(NEW.paciente_id, ''),
        NEW.fecha,
        NEW.fecha_entrega,
        NEW.monto_total,
        NULL, -- servicios have no product cost
        NEW.estado,
        timezone('utc', now())
    )
    ON CONFLICT (id) DO UPDATE SET
        monto_total = EXCLUDED.monto_total,
        estado = EXCLUDED.estado,
        fecha_entrega = EXCLUDED.fecha_entrega,
        paciente_id = EXCLUDED.paciente_id,
        updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$;;
