-- Existing bearer tokens lack durable session state and require a fresh login after this migration.
CREATE TABLE IF NOT EXISTS auth_user_session (
    user_id BIGINT NOT NULL,
    generation CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS auth_token_revocation (
    jti CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (jti),
    KEY idx_auth_revocation_expiry (expires_at),
    KEY idx_auth_revocation_user (user_id),
    CONSTRAINT fk_auth_revocation_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
