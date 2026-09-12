-- Frozen baseline tables are upgraded here. Each DDL step can resume after interruption.
SET @message_reliability_ddl = IF(
    (SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'message' AND COLUMN_NAME = 'client_message_id') = 'utf8mb4_bin',
    'SELECT 1', 'ALTER TABLE message MODIFY client_message_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL');
PREPARE message_reliability_stmt FROM @message_reliability_ddl;
EXECUTE message_reliability_stmt;
DEALLOCATE PREPARE message_reliability_stmt;

SET @message_reliability_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'message' AND INDEX_NAME = 'uk_sender_client_message'),
    'SELECT 1', 'ALTER TABLE message ADD UNIQUE KEY uk_sender_client_message (sender_id, client_message_id)');
PREPARE message_reliability_stmt FROM @message_reliability_ddl;
EXECUTE message_reliability_stmt;
DEALLOCATE PREPARE message_reliability_stmt;

SET @message_reliability_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'message' AND INDEX_NAME = 'uk_client_message_id'),
    'ALTER TABLE message DROP INDEX uk_client_message_id', 'SELECT 1');
PREPARE message_reliability_stmt FROM @message_reliability_ddl;
EXECUTE message_reliability_stmt;
DEALLOCATE PREPARE message_reliability_stmt;

SET @message_reliability_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'message' AND COLUMN_NAME = 'reaction_version'),
    'SELECT 1', 'ALTER TABLE message ADD reaction_version BIGINT NOT NULL DEFAULT 0');
PREPARE message_reliability_stmt FROM @message_reliability_ddl;
EXECUTE message_reliability_stmt;
DEALLOCATE PREPARE message_reliability_stmt;

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
