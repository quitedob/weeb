-- Upgrade the known repository baseline without deriving privileges from account names.
UPDATE `user` SET `type` = CASE UPPER(COALESCE(`type`, 'USER'))
    WHEN 'ADMIN' THEN 'ADMIN' WHEN 'BOT' THEN 'BOT' ELSE 'USER' END;
ALTER TABLE `user` MODIFY COLUMN `type` VARCHAR(50) NOT NULL DEFAULT 'USER';

-- The older optional reaction migration used created_at; the maintained mapper uses create_time.
-- Keep the original values, and make a same-checksum retry safe after MySQL commits the DDL.
SET @weeb_has_old_reaction_time = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='message_reaction' AND column_name='created_at');
SET @weeb_has_reaction_time = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='message_reaction' AND column_name='create_time');
SET @weeb_migration_sql = IF(@weeb_has_old_reaction_time=1 AND @weeb_has_reaction_time=0,
    'ALTER TABLE message_reaction CHANGE COLUMN created_at create_time DATETIME DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE weeb_migration_statement FROM @weeb_migration_sql;
EXECUTE weeb_migration_statement;
DEALLOCATE PREPARE weeb_migration_statement;

-- Older optional DDL lacked these constraints. Orphans fail visibly; never silently delete them.
SET @weeb_migration_sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema=DATABASE() AND table_name='message_reaction' AND constraint_name='fk_reaction_message')=0,
    'ALTER TABLE message_reaction ADD CONSTRAINT fk_reaction_message FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE weeb_migration_statement FROM @weeb_migration_sql;
EXECUTE weeb_migration_statement;
DEALLOCATE PREPARE weeb_migration_statement;
SET @weeb_migration_sql = IF((SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE constraint_schema=DATABASE() AND table_name='message_reaction' AND constraint_name='fk_reaction_user')=0,
    'ALTER TABLE message_reaction ADD CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE', 'SELECT 1');
PREPARE weeb_migration_statement FROM @weeb_migration_sql;
EXECUTE weeb_migration_statement;
DEALLOCATE PREPARE weeb_migration_statement;
SET @weeb_migration_sql = IF((SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='message_reaction' AND index_name='idx_reaction_type')=0,
    'CREATE INDEX idx_reaction_type ON message_reaction(reaction_type)', 'SELECT 1');
PREPARE weeb_migration_statement FROM @weeb_migration_sql;
EXECUTE weeb_migration_statement;
DEALLOCATE PREPARE weeb_migration_statement;
