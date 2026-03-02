#!/bin/bash
set -e
cd /home/mharko/Escritorio/Tingeso/Tingeso_3/K6-Tests

PSQL="kubectl exec -it toolrent-db-0 -- psql -U mharko -d toolrent_db"

seed() {
    local n=$1
    echo "[seed] Insertando $n loans + kardex..."
    $PSQL -c "
WITH new_loans AS (
  INSERT INTO loan (status, init_date, return_date, id_user)
  SELECT 'FINALIZADO',
    CURRENT_DATE - (random()*365)::int,
    CURRENT_DATE - (random()*365)::int + 7,
    (SELECT id FROM users WHERE rol='CLIENT' ORDER BY random() LIMIT 1)
  FROM generate_series(1, $n)
  RETURNING id, init_date, id_user
)
INSERT INTO kardex (type, date, cant, cost, id_tool, id_user, id_employee)
SELECT 'PRESTAMO', nl.init_date, 1,
  (SELECT price_rent FROM tool ORDER BY random() LIMIT 1),
  (SELECT id FROM tool ORDER BY random() LIMIT 1),
  nl.id_user,
  (SELECT id FROM users WHERE rol IN ('EMPLOYEE','ADMIN') ORDER BY random() LIMIT 1)
FROM new_loans nl;
"
    echo "[seed] Conteo actual:"
    $PSQL -c "SELECT (SELECT COUNT(*) FROM loan) AS loans, (SELECT COUNT(*) FROM kardex) AS kardex;"
}

echo "=== PHASE 0: DB limpia ==="
k6 run --insecure-skip-tls-verify epica2-volume.js 2>&1 | tee resultados/epica2-volume-phase0-inicial.txt
k6 run --insecure-skip-tls-verify epica6-volume.js 2>&1 | tee resultados/epica6-volume-phase0-inicial.txt

echo "=== Seed 1k ==="
seed 1000

echo "=== PHASE 1: 1k registros ==="
k6 run --insecure-skip-tls-verify epica2-volume.js 2>&1 | tee resultados/epica2-volume-phase1-1k.txt
k6 run --insecure-skip-tls-verify epica6-volume.js 2>&1 | tee resultados/epica6-volume-phase1-1k.txt

echo "=== Seed 10k ==="
seed 10000

echo "=== PHASE 2: 10k registros ==="
k6 run --insecure-skip-tls-verify epica2-volume.js 2>&1 | tee resultados/epica2-volume-phase2-10k.txt
k6 run --insecure-skip-tls-verify epica6-volume.js 2>&1 | tee resultados/epica6-volume-phase2-10k.txt

echo "=== Seed 50k ==="
seed 50000

echo "=== PHASE 3: 50k registros ==="
k6 run --insecure-skip-tls-verify epica2-volume.js 2>&1 | tee resultados/epica2-volume-phase3-50k.txt
k6 run --insecure-skip-tls-verify epica6-volume.js 2>&1 | tee resultados/epica6-volume-phase3-50k.txt

echo "=== DONE ==="
