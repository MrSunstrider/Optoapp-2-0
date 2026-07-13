
-- Direct test: update first pago
UPDATE public.pagos 
SET venta_id = 'v_disp_e028926e-a615-4420-a3cb-ddc726c36769'
WHERE id = '77977aab-78ea-4285-96c6-32548536bee6'
AND venta_id IS NULL;
;
