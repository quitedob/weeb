-- Preserve literal search semantics while moving JSON extraction/lowercasing to writes.
-- The migrator rejects an existing column with a different type, collation or expression.
SET @message_search_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'message' AND COLUMN_NAME = 'search_text'),
    'SELECT 1',
    'ALTER TABLE message ADD COLUMN search_text LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin GENERATED ALWAYS AS (LOWER(JSON_UNQUOTE(JSON_EXTRACT(content, ''$.content'')))) STORED');
PREPARE message_search_stmt FROM @message_search_ddl;
EXECUTE message_search_stmt;
DEALLOCATE PREPARE message_search_stmt;
