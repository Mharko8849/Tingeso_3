-- Volume Testing — Script de inserción masiva de datos para Épica 2 y Épica 6.
-- Inserta préstamos finalizados, sus herramientas y movimientos de kardex en la BD.
--
-- ACUÉRDATE QUE SE EJECUTA ASÍ:
--
-- 1) Conectarse al pod de PostgreSQL:
--    kubectl exec -it deployment/toolrent-db -- psql -U mharko -d toolrent_db
--
-- 2) Ejecutar este archivo dentro de psql:
--    \i /ruta/local/data-seed.sql
--
-- 3) Correr el script de K6 correspondiente y registrar los tiempos p50/p95.
--
-- 4) Repetir cambiando el número en generate_series(1, 1000):
--    (1000 → seed pequeño, 10000 → seed mediano, 50000 → seed grande)
--
-- 5) Para limpiar los datos insertados y volver al estado original:
--    DELETE FROM kardex    WHERE id > <id_máximo_original>;
--    DELETE FROM loan_xtools WHERE id > <id_máximo_original>;
--    DELETE FROM loan        WHERE id > <id_máximo_original>;
--    (El id_máximo_original se obtiene con SELECT MAX(id) FROM loans; antes de correr el seed)


-- Muestra cuántos registros hay antes del seed.
DO $$
BEGIN
    RAISE NOTICE 'Antes del seed — loans: %, kardex: %',
        (SELECT COUNT(*) FROM loan),
        (SELECT COUNT(*) FROM kardex);
END $$;

-- Inserta préstamos en estado FINALIZED para no afectar el stock activo.
INSERT INTO loan (state, init_date, return_date, id_client, id_employee)
SELECT
    'FINALIZADO',
    CURRENT_DATE - (random() * 365)::int,
    CURRENT_DATE - (random() * 365)::int + 7,
    (SELECT id FROM users WHERE rol = 'CLIENT'               ORDER BY random() LIMIT 1),
    (SELECT id FROM users WHERE rol IN ('EMPLOYEE', 'ADMIN') ORDER BY random() LIMIT 1)
FROM generate_series(1, 1000); -- Cambiar este número según la fase del test

-- Asocia una herramienta a cada préstamo insertado.
INSERT INTO loan_xtools (state, id_loan, id_tool, fine)
SELECT
    'DEVUELTA',
    l.id,
    (SELECT id FROM tool ORDER BY random() LIMIT 1),
    0
FROM loan l
WHERE l.state = 'FINALIZADO'
  AND l.id NOT IN (SELECT DISTINCT id_loan FROM loan_xtools);

-- Registra el movimiento de PRESTAMO en kardex por cada préstamo.
INSERT INTO kardex (type, date, quantity, amount, id_tool, id_user, id_employee)
SELECT
    'PRESTAMO',
    l.init_date,
    1,
    (SELECT price_rent FROM tool ORDER BY random() LIMIT 1),
    (SELECT id FROM tool ORDER BY random() LIMIT 1),
    l.id_client,
    l.id_employee
FROM loan l
WHERE l.state = 'FINALIZADO';

-- Registra el movimiento de DEVOLUCION en kardex por cada préstamo.
INSERT INTO kardex (type, date, quantity, amount, id_tool, id_user, id_employee)
SELECT
    'DEVOLUCION',
    l.return_date,
    1,
    0,
    (SELECT id FROM tool ORDER BY random() LIMIT 1),
    l.id_client,
    l.id_employee
FROM loan l
WHERE l.state = 'FINALIZADO';

-- Muestra cuántos registros quedaron después del seed.
DO $$
BEGIN
    RAISE NOTICE 'Después del seed — loans: %, kardex: %',
        (SELECT COUNT(*) FROM loans),
        (SELECT COUNT(*) FROM kardex);
END $$;
