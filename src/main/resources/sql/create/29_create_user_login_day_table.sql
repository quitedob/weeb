-- Successful logins are counted once per database calendar day.
-- Existing accounts start accruing days on deployment; registration age is not login activity.
CREATE TABLE IF NOT EXISTS user_login_day (
    user_id BIGINT NOT NULL,
    login_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, login_date),
    KEY idx_user_login_day_date (login_date),
    CONSTRAINT fk_user_login_day_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
