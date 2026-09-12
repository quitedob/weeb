-- Private campus content intentionally does not share the public article tables.
CREATE TABLE IF NOT EXISTS campus_school (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 description VARCHAR(2000) NOT NULL DEFAULT '',
 active BOOLEAN NOT NULL DEFAULT TRUE,
 pre_moderation BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 1,
 created_by BIGINT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_campus_school_name (name),
 KEY idx_campus_school_active (active,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_membership (
 school_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 role VARCHAR(16) NOT NULL DEFAULT 'MEMBER',
 version BIGINT NOT NULL DEFAULT 1,
 joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (school_id,user_id),
 KEY idx_campus_membership_user (user_id,status,school_id),
 KEY idx_campus_membership_school (school_id,status,role,user_id),
 CONSTRAINT fk_campus_member_school FOREIGN KEY (school_id) REFERENCES campus_school(id),
 CONSTRAINT chk_campus_member_status CHECK (status IN ('VERIFIED','LEFT','SUSPENDED')),
 CONSTRAINT chk_campus_member_role CHECK (role IN ('MEMBER','ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_verification_application (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 real_name VARCHAR(80) NOT NULL,
 student_number VARCHAR(40) NOT NULL,
 department VARCHAR(100) NOT NULL,
 enrollment_year SMALLINT NOT NULL,
 statement VARCHAR(1000) NOT NULL DEFAULT '',
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 reason VARCHAR(500) NOT NULL DEFAULT '',
 version BIGINT NOT NULL DEFAULT 1,
 reviewed_by BIGINT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 reviewed_at DATETIME(3) NULL,
 pending_user_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN user_id ELSE NULL END) STORED,
 UNIQUE KEY uk_campus_application_pending (school_id,pending_user_id),
 KEY idx_campus_application_queue (school_id,status,id),
 KEY idx_campus_application_user (school_id,user_id,id),
 CONSTRAINT fk_campus_application_school FOREIGN KEY (school_id) REFERENCES campus_school(id),
 CONSTRAINT chk_campus_application_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_post (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL,
 author_id BIGINT NOT NULL,
 title VARCHAR(120) NOT NULL,
 content TEXT NOT NULL,
 category VARCHAR(24) NOT NULL DEFAULT 'GENERAL',
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
 version BIGINT NOT NULL DEFAULT 1,
 pinned BOOLEAN NOT NULL DEFAULT FALSE,
 review_reason VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_by BIGINT NULL,
 reviewed_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_campus_post_feed (school_id,status,pinned,id),
 KEY idx_campus_post_category (school_id,status,category,id),
 KEY idx_campus_post_author (school_id,author_id,status,id),
 CONSTRAINT fk_campus_post_school FOREIGN KEY (school_id) REFERENCES campus_school(id),
 CONSTRAINT chk_campus_post_status CHECK (status IN ('DRAFT','PENDING','PUBLISHED','REJECTED','REMOVED')),
 CONSTRAINT chk_campus_post_category CHECK (category IN ('GENERAL','STUDY','LIFE','LOST_FOUND','ANNOUNCEMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_post_like (
 post_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (post_id,user_id),
 CONSTRAINT fk_campus_like_post FOREIGN KEY (post_id) REFERENCES campus_post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_post_bookmark (
 post_id BIGINT NOT NULL,
 user_id BIGINT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 PRIMARY KEY (post_id,user_id),
 KEY idx_campus_bookmark_user (user_id,post_id),
 CONSTRAINT fk_campus_bookmark_post FOREIGN KEY (post_id) REFERENCES campus_post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_comment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 post_id BIGINT NOT NULL,
 author_id BIGINT NOT NULL,
 content VARCHAR(1000) NOT NULL,
 reply_to_comment_id BIGINT NULL,
 deleted BOOLEAN NOT NULL DEFAULT FALSE,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_campus_comment_post (post_id,id),
 KEY idx_campus_comment_count (post_id,deleted),
 CONSTRAINT fk_campus_comment_post FOREIGN KEY (post_id) REFERENCES campus_post(id),
 CONSTRAINT fk_campus_comment_reply FOREIGN KEY (reply_to_comment_id) REFERENCES campus_comment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_report (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL,
 post_id BIGINT NOT NULL,
 reporter_id BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 decision_reason VARCHAR(500) NOT NULL DEFAULT '',
 version BIGINT NOT NULL DEFAULT 1,
 reviewed_by BIGINT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 reviewed_at DATETIME(3) NULL,
 pending_reporter_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='PENDING' THEN reporter_id ELSE NULL END) STORED,
 UNIQUE KEY uk_campus_report_pending (post_id,pending_reporter_id),
 KEY idx_campus_report_queue (school_id,status,id),
 CONSTRAINT fk_campus_report_school FOREIGN KEY (school_id) REFERENCES campus_school(id),
 CONSTRAINT fk_campus_report_post FOREIGN KEY (post_id) REFERENCES campus_post(id),
 CONSTRAINT chk_campus_report_status CHECK (status IN ('PENDING','REMOVED','DISMISSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 school_id BIGINT NOT NULL,
 actor_id BIGINT NOT NULL,
 action VARCHAR(60) NOT NULL,
 target_type VARCHAR(32) NOT NULL,
 target_id VARCHAR(64) NOT NULL,
 details VARCHAR(1000) NOT NULL DEFAULT '',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_campus_audit_school (school_id,id),
 CONSTRAINT fk_campus_audit_school FOREIGN KEY (school_id) REFERENCES campus_school(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_media (
 id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
 school_id BIGINT NOT NULL,
 owner_id BIGINT NOT NULL,
 post_id BIGINT NULL,
 width INT NOT NULL,
 height INT NOT NULL,
 size BIGINT NOT NULL,
 position INT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 detached_at DATETIME(3) NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_campus_media_post (post_id,position,id),
 KEY idx_campus_media_owner (school_id,owner_id,post_id),
 KEY idx_campus_media_cleanup (post_id,detached_at),
 CONSTRAINT fk_campus_media_school FOREIGN KEY (school_id) REFERENCES campus_school(id),
 CONSTRAINT fk_campus_media_post FOREIGN KEY (post_id) REFERENCES campus_post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
