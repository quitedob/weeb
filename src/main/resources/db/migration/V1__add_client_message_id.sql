-- 添加客户端消息ID字段用于幂等性控制
ALTER TABLE `message` 
ADD COLUMN `client_message_id` VARCHAR(100) NULL COMMENT '客户端消息ID（用于幂等性）' AFTER `id`,
ADD UNIQUE INDEX `uk_client_message_id` (`client_message_id`);

-- 添加索引以提高查询性能
CREATE INDEX `idx_sender_client_msg` ON `message` (`sender_id`, `client_message_id`);
