-- ============================================================================
-- seed-e2e.sql — fixtures SOLO para la base de pruebas manuales "pulsepass_e2e".
-- NO es una migración de Flyway y NO debe ejecutarse en desarrollo, QA ni producción.
--
-- Uso (después de que la app haya arrancado una vez y Flyway haya creado las tablas):
--   psql -U postgres -d pulsepass_e2e -v "staff_pw=<clave STAFF>" -v "admin_pw=<clave ADMIN>" `
--        -v "inactive_pw=<clave INACTIVE>" -f tests/e2e/fixtures/seed-e2e.sql
-- Las contraseñas se pasan por línea de comandos y se guardan con BCrypt (pgcrypto): no están en el repositorio.
-- ============================================================================
\set ON_ERROR_STOP on

-- Protección: aborta si no es la base E2E.
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Seed bloqueado: la base actual es "%" y no "pulsepass_e2e"', current_database();
    END IF;
END $$;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

BEGIN;

-- Venues: uno normal, uno de capacidad 1 (prueba de aforo) y uno inactivo.
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
    ('E2E-VEN-01',  'Arena E2E',            'Santa Marta', 'Calle 1 # 1-1', 500, TRUE),
    ('E2E-VEN-S1',  'Sala Pequeña E2E',     'Santa Marta', 'Calle 2 # 2-2',   1, TRUE),
    ('E2E-VEN-OFF', 'Recinto Inactivo E2E', 'Santa Marta', 'Calle 3 # 3-3', 100, FALSE);

-- Eventos con fechas relativas a hoy (siempre futuras al momento de sembrar).
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
SELECT e.code, e.name, e.descr, e.cat, e.status, LOCALTIMESTAMP + e.days * INTERVAL '1 day', e.min_age, v.id
FROM (VALUES
    ('E2E-DRAFT-01',  'Borrador E2E',        'Evento en borrador: invisible para el público', 'MUSIC', 'DRAFT',      120,   0, 'E2E-VEN-01'),
    ('E2E-PUB-01',    'Concierto E2E',       'Evento publicado con cupo',                     'MUSIC', 'PUBLISHED',  120,   0, 'E2E-VEN-01'),
    ('E2E-TOPUB-01',  'Por publicar E2E',    'Evento en borrador listo para publicar',        'MUSIC', 'DRAFT',      120,   0, 'E2E-VEN-01'),
    ('E2E-SMALL-01',  'Aforo uno E2E',       'Evento publicado en un venue de capacidad 1',   'MUSIC', 'PUBLISHED',  120,   0, 'E2E-VEN-S1'),
    ('E2E-ADULT-01',  'Edad mínima 120 E2E', 'Evento publicado con edad mínima inalcanzable','MUSIC', 'PUBLISHED',  120, 120, 'E2E-VEN-01')
) AS e(code, name, descr, cat, status, days, min_age, venue_code)
JOIN venues v ON v.code = e.venue_code;

-- Solar Beat (catálogo inicial de V2) participa en el borrador y en el publicado:
-- así se comprueba que la búsqueda por artista oculta el borrador al público.
INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id
FROM events e, artists a
WHERE a.stage_name = 'Solar Beat' AND e.event_code IN ('E2E-DRAFT-01', 'E2E-PUB-01');

-- Usuarios privilegiados e inactivable (no se crean por HTTP).
INSERT INTO users (username, email, active) VALUES
    ('e2e_staff',    'e2e.staff@example.test',    TRUE),
    ('e2e_admin',    'e2e.admin@example.test',    TRUE),
    ('e2e_inactive', 'e2e.inactive@example.test', TRUE);

INSERT INTO user_profiles (user_id, first_name, last_name, phone, city, birth_date)
SELECT id, initcap(split_part(username, '_', 2)), 'E2E', '3000000000', 'Santa Marta', DATE '1990-01-01'
FROM users WHERE email LIKE 'e2e.%@example.test';

INSERT INTO user_credentials (user_id, password_hash)
SELECT id, crypt(:'staff_pw',    gen_salt('bf', 10)) FROM users WHERE email = 'e2e.staff@example.test'
UNION ALL
SELECT id, crypt(:'admin_pw',    gen_salt('bf', 10)) FROM users WHERE email = 'e2e.admin@example.test'
UNION ALL
SELECT id, crypt(:'inactive_pw', gen_salt('bf', 10)) FROM users WHERE email = 'e2e.inactive@example.test';

INSERT INTO user_roles (user_id, role)
SELECT id, CASE username WHEN 'e2e_staff' THEN 'STAFF' WHEN 'e2e_admin' THEN 'ADMIN' ELSE 'USER' END
FROM users WHERE email LIKE 'e2e.%@example.test';

COMMIT;

-- Resumen para comprobar a ojo.
SELECT 'venues' AS tabla, count(*) FROM venues
UNION ALL SELECT 'events', count(*) FROM events
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'user_roles', count(*) FROM user_roles;
