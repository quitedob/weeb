package com.web.campus;

import java.time.Instant;

/** Explicit projections prevent authentication fields and verification PII leaking into directory/member responses. */
public final class CampusSchoolDtos {
    private CampusSchoolDtos() { }

    public record CreateSchool(String name, String description, Boolean preModeration) { }
    public record UpdateSchool(String name, String description, Boolean preModeration, Boolean active, Long version) { }
    public record Apply(String realName, String studentNumber, String department, Integer enrollmentYear, String statement) { }
    public record Review(String decision, String reason, Long version) { }
    public record UpdateMember(String status, String role, String reason, Long version) { }

    public record Membership(String status, String role, long version) { }
    public record LatestApplication(long id, String status, String reason, long version) { }
    public record Capabilities(boolean canRead, boolean canPost, boolean canModerate,
                               boolean canManageMembers, boolean canManageSchool, boolean isSiteAdmin) { }
    public record School(long id, String name, String description, boolean active, boolean preModeration,
                         long version, Instant createdAt, long memberCount, Membership membership,
                         LatestApplication latestApplication, Capabilities capabilities) { }
    public record Application(long id, long schoolId, long userId, String username, String realName,
                              String studentNumber, String department, int enrollmentYear, String statement,
                              String status, String reason, long version, Instant createdAt,
                              Instant reviewedAt) { }
    public record Member(long userId, String username, String nickname, String avatar, String status,
                         String role, long version, Instant joinedAt) { }
    public record Audit(long id, long actorId, String action, String targetType, String targetId,
                        String details, Instant createdAt) { }
}
