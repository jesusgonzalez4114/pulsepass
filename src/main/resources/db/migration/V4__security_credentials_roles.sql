-- Credenciales y roles.

CREATE TABLE user_credentials (
                                  user_id             BIGINT PRIMARY KEY,
                                  password_hash       VARCHAR(100) NOT NULL,
                                  password_changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_credentials_user
                                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
                            user_id BIGINT      NOT NULL,
                            role    VARCHAR(20) NOT NULL,
                            PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT chk_user_role
                                CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- Usuarios legados (anteriores a la Fase 4): reciben el rol USER pero NO tienen credencial.
-- No se inventa ninguna contraseña por defecto.
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;
