-- Account preferences; rows are created by the first settings save.
CREATE TABLE IF NOT EXISTS `user_preferences` (
    `user_id` BIGINT NOT NULL,
    `online_visible` BOOLEAN NOT NULL DEFAULT TRUE,
    `allow_messages` BOOLEAN NOT NULL DEFAULT TRUE,
    `show_follows` BOOLEAN NOT NULL DEFAULT TRUE,
    `new_messages` BOOLEAN NOT NULL DEFAULT TRUE,
    `follows` BOOLEAN NOT NULL DEFAULT TRUE,
    `likes` BOOLEAN NOT NULL DEFAULT TRUE,
    `comments` BOOLEAN NOT NULL DEFAULT TRUE,
    `group_invites` BOOLEAN NOT NULL DEFAULT TRUE,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_preferences_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
