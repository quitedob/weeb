package com.web.campus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Audit rows participate in the action transaction. Never include verification PII. */
@Service
public class CampusAuditService {
    private final JdbcTemplate jdbc;
    public CampusAuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(long actor, long school, String action, String targetType, String targetId, String details) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Campus audit requires a business transaction");
        }
        jdbc.update("INSERT INTO campus_audit(school_id,actor_id,action,target_type,target_id,details) VALUES (?,?,?,?,?,?)",
                school, actor, action, targetType, targetId, details == null ? "" : details);
    }
}
