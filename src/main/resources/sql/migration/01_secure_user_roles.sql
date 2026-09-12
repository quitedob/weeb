-- Existing installations: run after backing up the user table.
-- Only the persisted type grants roles. Never infer a role from a username.
UPDATE `user`
SET `type` = CASE UPPER(COALESCE(`type`, 'USER'))
    WHEN 'ADMIN' THEN 'ADMIN'
    WHEN 'BOT' THEN 'BOT'
    ELSE 'USER'
END;

ALTER TABLE `user` MODIFY COLUMN `type` VARCHAR(50) NOT NULL DEFAULT 'USER';

-- Review previously created privileged accounts before enabling public access.
-- A registered account can be promoted deliberately using its verified numeric ID:
-- UPDATE `user` SET `type` = 'ADMIN' WHERE `id` = <verified_user_id>;
