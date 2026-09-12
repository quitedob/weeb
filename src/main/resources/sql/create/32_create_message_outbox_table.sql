CREATE TABLE IF NOT EXISTS message_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_key VARCHAR(160) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    recipient_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    payload JSON NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    lease_until DATETIME(3) NULL,
    lease_token VARCHAR(36) NULL,
    delivered_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_error VARCHAR(256) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_outbox_event_recipient (event_key, recipient_id),
    KEY idx_message_outbox_due (delivered_at, available_at, lease_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
