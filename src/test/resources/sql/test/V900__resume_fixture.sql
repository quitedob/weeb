CREATE TABLE IF NOT EXISTS migration_resume_fixture (id INT PRIMARY KEY, value VARCHAR(30));
INSERT INTO migration_resume_fixture(id,value) SELECT id,value FROM migration_resume_dependency
ON DUPLICATE KEY UPDATE value=VALUES(value);
