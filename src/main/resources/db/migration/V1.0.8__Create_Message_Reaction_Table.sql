-- 创建消息反应表
CREATE TABLE IF NOT EXISTS message_reaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反应ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    reaction_type VARCHAR(50) NOT NULL COMMENT '反应类型（如👍、❤️等）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_message_user_reaction (message_id, user_id, reaction_type) COMMENT '同一用户对同一消息的同一反应类型唯一',
    KEY idx_message_id (message_id) COMMENT '消息ID索引',
    KEY idx_user_id (user_id) COMMENT '用户ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息反应表';
