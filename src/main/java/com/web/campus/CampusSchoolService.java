package com.web.campus;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.web.campus.CampusSchoolDtos.*;

/** School membership and verification are separate from global account authority. */
@Service
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class CampusSchoolService {
    private static final Set<String> APPLICATION_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "CANCELLED");
    private static final Set<String> MEMBER_STATUSES = Set.of("VERIFIED", "LEFT", "SUSPENDED");
    private static final String SCHOOL_JOIN = " FROM campus_school s LEFT JOIN campus_membership m"
            + " ON m.school_id=s.id AND m.user_id=?";
    private static final String SCHOOL_VISIBLE = " (s.active=TRUE OR ?=TRUE OR (m.status='VERIFIED' AND m.role='ADMIN'))";
    private static final String SCHOOL_SELECT = "SELECT s.id,s.name,s.description,s.active,s.pre_moderation,s.version,s.created_at,"
            + CampusTimestamps.select("s", "created_at") + ","
            + "m.status membership_status,m.role membership_role,m.version membership_version,"
            + "a.id application_id,a.status application_status,a.reason application_reason,a.version application_version,"
            + "(SELECT COUNT(*) FROM campus_membership mc JOIN `user` uc ON uc.id=mc.user_id AND uc.status=1"
            + " WHERE mc.school_id=s.id AND mc.status='VERIFIED' AND mc.role IN ('MEMBER','ADMIN')) member_count";
    private static final String APPLICATION_SELECT = "SELECT a.id,a.school_id,a.user_id,u.username,a.real_name,"
            + "a.student_number,a.department,a.enrollment_year,a.statement,a.status,a.reason,a.version,a.created_at,a.reviewed_at,"
            + CampusTimestamps.select("a", "created_at", "reviewed_at")
            + " FROM campus_verification_application a LEFT JOIN `user` u ON u.id=a.user_id";
    private static final String MEMBER_SELECT = "SELECT m.user_id,u.username,u.nickname,u.avatar,m.status,m.role,m.version,m.joined_at,"
            + CampusTimestamps.select("m", "joined_at")
            + " FROM campus_membership m LEFT JOIN `user` u ON u.id=m.user_id";
    // Lock only the business row, never the joined user projection. Two different schools
    // may involve the same users and must not upgrade their shared account locks here.
    private static final String APPLICATION_LOCK_SELECT = APPLICATION_SELECT.replace("u.username", "NULL AS username")
            .replace(" LEFT JOIN `user` u ON u.id=a.user_id", "");
    private static final String MEMBER_LOCK_SELECT = MEMBER_SELECT
            .replace("u.username,u.nickname,u.avatar", "NULL AS username,NULL AS nickname,NULL AS avatar")
            .replace(" LEFT JOIN `user` u ON u.id=m.user_id", "");
    private static final RowMapper<Application> APPLICATION_ROW = (rs, row) -> new Application(
            rs.getLong("id"), rs.getLong("school_id"), rs.getLong("user_id"), rs.getString("username"),
            rs.getString("real_name"), rs.getString("student_number"), rs.getString("department"),
            rs.getInt("enrollment_year"), rs.getString("statement"), rs.getString("status"), rs.getString("reason"),
            rs.getLong("version"), CampusTimestamps.read(rs, "created_at"), CampusTimestamps.read(rs, "reviewed_at"));
    private static final RowMapper<Member> MEMBER_ROW = (rs, row) -> new Member(rs.getLong("user_id"),
            rs.getString("username"), rs.getString("nickname"), rs.getString("avatar"), rs.getString("status"),
            rs.getString("role"), rs.getLong("version"), CampusTimestamps.read(rs, "joined_at"));

    private final JdbcTemplate jdbc;
    private final CampusAccessService access;
    private final CampusAuditService audit;
    private final CampusNotificationService notifications;

    public CampusSchoolService(JdbcTemplate jdbc, CampusAccessService access, CampusAuditService audit,
                               CampusNotificationService notifications) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.notifications = notifications;
    }

    public CampusPage<School> listSchools(long actor, String q, boolean mine, int page, int size) {
        int offset = CampusPage.offset(page, size);
        String query = CampusPage.query(q);
        access.requireActiveUser(actor);
        boolean siteAdmin = access.isSiteAdmin(actor);
        String where = " WHERE" + SCHOOL_VISIBLE
                + " AND (?='' OR LOCATE(LOWER(?),LOWER(s.name))>0)"
                + (mine ? " AND m.status='VERIFIED' AND m.role IN ('MEMBER','ADMIN')" : "");
        long total = count("SELECT COUNT(*)" + SCHOOL_JOIN + where, actor, siteAdmin, query, query);
        String latest = " LEFT JOIN campus_verification_application a ON a.id="
                + "(SELECT ax.id FROM campus_verification_application ax WHERE ax.school_id=s.id AND ax.user_id=? ORDER BY ax.id DESC LIMIT 1)";
        List<School> schools = jdbc.query(SCHOOL_SELECT + SCHOOL_JOIN + latest + where + " ORDER BY s.id DESC LIMIT ? OFFSET ?",
                (rs, row) -> schoolRow(rs, siteAdmin), actor, actor, siteAdmin, query, query, size, offset);
        return new CampusPage<>(schools, total, page, size);
    }

    public School getSchool(long actor, long school) {
        CampusAccessService.positive(school);
        access.requireActiveUser(actor);
        boolean siteAdmin = access.isSiteAdmin(actor);
        String latest = " LEFT JOIN campus_verification_application a ON a.id="
                + "(SELECT ax.id FROM campus_verification_application ax WHERE ax.school_id=s.id AND ax.user_id=? ORDER BY ax.id DESC LIMIT 1)";
        var schools = jdbc.query(SCHOOL_SELECT + SCHOOL_JOIN + latest + " WHERE s.id=? AND" + SCHOOL_VISIBLE,
                (rs, row) -> schoolRow(rs, siteAdmin), actor, actor, school, siteAdmin);
        if (schools.isEmpty()) {
            if (count("SELECT COUNT(*) FROM campus_school WHERE id=?", school) == 0) throw new CampusException(404, "学校不存在");
            throw new CampusException(403, "学校已停用");
        }
        return schools.get(0);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public School createSchool(long actor, CreateSchool request) {
        required(request);
        String name = text(request.name(), 120, true, "学校名称");
        String description = text(request.description(), 2000, false, "学校简介");
        access.requireActiveUser(actor);
        if (!access.isSiteAdmin(actor)) throw new CampusException(403, "只有站点管理员可以创建学校");
        long school;
        try {
            school = insert("INSERT INTO campus_school(name,description,pre_moderation,created_by) VALUES (?,?,?,?)",
                    name, description, request.preModeration() == null || request.preModeration(), actor);
        } catch (DuplicateKeyException duplicate) {
            throw new CampusException(409, "学校名称已存在");
        }
        // The new school row now exists; acquire the same lock order as every subsequent mutation.
        var current = access.lockSchool(actor, school);
        if (!current.siteAdmin()) throw new CampusException(403, "站点管理员权限已变更");
        jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED','ADMIN')", school, actor);
        audit.record(actor, school, "SCHOOL_CREATED", "campus_school", Long.toString(school), "创建学校并设置初始管理员");
        return getSchool(actor, school);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public School updateSchool(long actor, long school, UpdateSchool request) {
        required(request);
        version(request.version());
        String name = request.name() == null ? null : text(request.name(), 120, true, "学校名称");
        String description = request.description() == null ? null : text(request.description(), 2000, false, "学校简介");
        var current = access.lockSchool(actor, school);
        if (!current.manager()) throw new CampusException(403, "没有本校管理权限");
        var row = jdbc.queryForMap("SELECT name,description,pre_moderation,active,version FROM campus_school WHERE id=? FOR UPDATE", school);
        checkVersion(((Number) row.get("version")).longValue(), request.version());
        String nextName = name == null ? (String) row.get("name") : name;
        boolean nextActive = request.active() == null ? current.active() : request.active();
        if (!current.siteAdmin() && (!nextName.equals(row.get("name")) || nextActive != current.active())) {
            throw new CampusException(403, "只有站点管理员可以修改学校名称或启停学校");
        }
        try {
            changed(jdbc.update("UPDATE campus_school SET name=?,description=?,pre_moderation=?,active=?,version=version+1,updated_at=CURRENT_TIMESTAMP(3)"
                            + " WHERE id=? AND version=?", nextName,
                    description == null ? row.get("description") : description,
                    request.preModeration() == null ? current.preModeration() : request.preModeration(),
                    nextActive, school, request.version()));
        } catch (DuplicateKeyException duplicate) {
            throw new CampusException(409, "学校名称已存在");
        }
        audit.record(actor, school, "SCHOOL_UPDATED", "campus_school", Long.toString(school),
                "更新学校设置；active=" + nextActive + ";preModeration="
                        + (request.preModeration() == null ? current.preModeration() : request.preModeration()));
        return getSchool(actor, school);
    }

    public CampusPage<Application> myApplications(long actor, long school, int page, int size) {
        int offset = CampusPage.offset(page, size);
        access.inspect(actor, school); // Own history remains accessible after leaving or school suspension.
        long total = count("SELECT COUNT(*) FROM campus_verification_application WHERE school_id=? AND user_id=?", school, actor);
        var rows = jdbc.query(APPLICATION_SELECT + " WHERE a.school_id=? AND a.user_id=? ORDER BY a.id DESC LIMIT ? OFFSET ?",
                APPLICATION_ROW, school, actor, size, offset);
        return new CampusPage<>(rows, total, page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Application apply(long actor, long school, Apply request) {
        required(request);
        String realName = text(request.realName(), 80, true, "姓名");
        String studentNumber = text(request.studentNumber(), 40, true, "学号");
        String department = text(request.department(), 100, true, "院系");
        String statement = text(request.statement(), 1000, false, "申请说明");
        if (request.enrollmentYear() == null || request.enrollmentYear() < 1900 || request.enrollmentYear() > Year.now().getValue() + 1) {
            throw new CampusException(400, "入学年份无效");
        }
        var current = access.lockSchool(actor, school);
        active(current);
        Member existing = lockedMember(school, actor);
        if (existing != null && "SUSPENDED".equals(existing.status())) throw new CampusException(409, "成员身份已暂停，请联系学校管理员");
        if (existing != null && "VERIFIED".equals(existing.status())) throw new CampusException(409, "已经是本校认证成员");
        if (count("SELECT COUNT(*) FROM campus_verification_application WHERE school_id=? AND user_id=? AND status='PENDING'", school, actor) != 0) {
            throw new CampusException(409, "已有待审核申请");
        }
        long id;
        try {
            id = insert("INSERT INTO campus_verification_application(school_id,user_id,real_name,student_number,department,enrollment_year,statement) VALUES (?,?,?,?,?,?,?)",
                    school, actor, realName, studentNumber, department, request.enrollmentYear(), statement);
        } catch (DuplicateKeyException duplicate) {
            throw new CampusException(409, "已有待审核申请");
        }
        audit.record(actor, school, "APPLICATION_SUBMITTED", "campus_application", Long.toString(id), "提交认证申请");
        for (Long manager : jdbc.query("SELECT DISTINCT u.id FROM `user` u LEFT JOIN campus_membership m ON m.user_id=u.id AND m.school_id=?"
                        + " WHERE u.status=1 AND u.id<>? AND (UPPER(u.type)='ADMIN' OR (m.status='VERIFIED' AND m.role='ADMIN'))",
                (rs, row) -> rs.getLong("id"), school, actor)) {
            notifications.send(actor, manager, "CAMPUS_VERIFICATION", "campus_school", school);
        }
        return application(school, id, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelApplication(long actor, long school, long id) {
        access.lockSchool(actor, school);
        Application application = application(school, id, true);
        if (application.userId() != actor) throw new CampusException(403, "只能撤回本人的申请");
        pending(application);
        changed(jdbc.update("UPDATE campus_verification_application SET status='CANCELLED',version=version+1,reviewed_at=CURRENT_TIMESTAMP(3)"
                + " WHERE id=? AND school_id=? AND user_id=? AND status='PENDING' AND version=?", id, school, actor, application.version()));
        audit.record(actor, school, "APPLICATION_CANCELLED", "campus_application", Long.toString(id), "申请人撤回申请");
    }

    public CampusPage<Application> applications(long actor, long school, String status, int page, int size) {
        int offset = CampusPage.offset(page, size);
        String state = enumValue(status == null ? "PENDING" : status, APPLICATION_STATUSES, "申请状态");
        access.requireManage(actor, school);
        long total = count("SELECT COUNT(*) FROM campus_verification_application WHERE school_id=? AND status=?", school, state);
        var rows = jdbc.query(APPLICATION_SELECT + " WHERE a.school_id=? AND a.status=? ORDER BY a.id DESC LIMIT ? OFFSET ?",
                APPLICATION_ROW, school, state, size, offset);
        return new CampusPage<>(rows, total, page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Application reviewApplication(long actor, long school, long id, Review request) {
        required(request);
        version(request.version());
        String decision = enumValue(request.decision(), Set.of("APPROVE", "REJECT"), "审核操作");
        String reason = text(request.reason(), 500, "REJECT".equals(decision), "审核理由");
        manage(access.lockSchool(actor, school));
        Application application = application(school, id, true);
        if (application.userId() == actor) throw new CampusException(403, "不能审核自己的申请");
        checkVersion(application.version(), request.version());
        pending(application);
        String status = "APPROVE".equals(decision) ? "APPROVED" : "REJECTED";
        if ("APPROVE".equals(decision)) {
            var enabled = jdbc.query("SELECT status FROM `user` WHERE id=? FOR SHARE", (rs, row) -> rs.getInt("status"), application.userId());
            if (enabled.isEmpty() || enabled.get(0) != 1) throw new CampusException(409, "申请人账号不可用");
            Member member = lockedMember(school, application.userId());
            if (member != null && !"LEFT".equals(member.status())) throw new CampusException(409, "当前成员状态不能通过申请变更");
            if (member == null) {
                jdbc.update("INSERT INTO campus_membership(school_id,user_id,status,role) VALUES (?,?,'VERIFIED','MEMBER')", school, application.userId());
            } else {
                changed(jdbc.update("UPDATE campus_membership SET status='VERIFIED',role='MEMBER',version=version+1,joined_at=CURRENT_TIMESTAMP(3),updated_at=CURRENT_TIMESTAMP(3)"
                        + " WHERE school_id=? AND user_id=? AND status='LEFT' AND version=?", school, application.userId(), member.version()));
            }
        }
        changed(jdbc.update("UPDATE campus_verification_application SET status=?,reason=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP(3),version=version+1"
                + " WHERE id=? AND school_id=? AND status='PENDING' AND version=?", status, reason, actor, id, school, request.version()));
        audit.record(actor, school, "APPLICATION_" + status, "campus_application", Long.toString(id), reason);
        notifications.send(actor, application.userId(), "CAMPUS_VERIFICATION", "campus_school", school);
        return application(school, id, false);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leave(long actor, long school) {
        var current = access.lockSchool(actor, school);
        active(current);
        Member member = lockedMember(school, actor);
        if (member == null || "LEFT".equals(member.status())) return;
        if ("SUSPENDED".equals(member.status())) throw new CampusException(409, "暂停的成员身份不能自行变更，请联系学校管理员");
        protectLastAdministrator(school, member, "LEFT", "MEMBER");
        changed(jdbc.update("UPDATE campus_membership SET status='LEFT',role='MEMBER',version=version+1,updated_at=CURRENT_TIMESTAMP(3)"
                + " WHERE school_id=? AND user_id=? AND status='VERIFIED' AND version=?", school, actor, member.version()));
        audit.record(actor, school, "MEMBER_LEFT", "campus_member", Long.toString(actor), "成员主动退出");
    }

    public CampusPage<Member> members(long actor, long school, String q, String status, int page, int size) {
        int offset = CampusPage.offset(page, size);
        String query = CampusPage.query(q);
        String state = status == null ? null : enumValue(status, MEMBER_STATUSES, "成员状态");
        access.requireManage(actor, school);
        String where = " WHERE m.school_id=? AND (?='' OR LOCATE(LOWER(?),LOWER(u.username))>0 OR LOCATE(LOWER(?),LOWER(COALESCE(u.nickname,'')))>0)"
                + (state == null ? "" : " AND m.status=?");
        var params = new ArrayList<Object>(List.of(school, query, query, query));
        if (state != null) params.add(state);
        long total = count("SELECT COUNT(*) FROM campus_membership m LEFT JOIN `user` u ON u.id=m.user_id" + where, params.toArray());
        params.add(size);
        params.add(offset);
        return new CampusPage<>(jdbc.query(MEMBER_SELECT + where + " ORDER BY m.user_id ASC LIMIT ? OFFSET ?", MEMBER_ROW, params.toArray()), total, page, size);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member updateMember(long actor, long school, long userId, UpdateMember request) {
        CampusAccessService.positive(userId);
        required(request);
        version(request.version());
        String status = enumValue(request.status(), Set.of("VERIFIED", "SUSPENDED"), "成员状态");
        String role = enumValue(request.role(), Set.of("MEMBER", "ADMIN"), "成员角色");
        String reason = text(request.reason(), 500, "SUSPENDED".equals(status), "处理理由");
        var current = access.lockSchool(actor, school);
        manage(current);
        Member member = lockedMember(school, userId);
        if (member == null) throw new CampusException(404, "校园成员不存在");
        if (!current.siteAdmin() && (actor == userId || "ADMIN".equals(member.role()) || !role.equals(member.role()))) {
            throw new CampusException(403, "校园管理员不能修改自己、另一管理员或成员角色");
        }
        checkVersion(member.version(), request.version());
        if ("VERIFIED".equals(status)) {
            var enabled = jdbc.query("SELECT status FROM `user` WHERE id=? FOR SHARE", (rs, row) -> rs.getInt("status"), userId);
            if (enabled.isEmpty() || enabled.get(0) != 1) throw new CampusException(409, "目标账号不可用");
        }
        protectLastAdministrator(school, member, status, role);
        changed(jdbc.update("UPDATE campus_membership SET status=?,role=?,version=version+1,updated_at=CURRENT_TIMESTAMP(3)"
                + " WHERE school_id=? AND user_id=? AND version=?", status, role, school, userId, request.version()));
        audit.record(actor, school, "MEMBER_UPDATED", "campus_member", Long.toString(userId), "status=" + status + ";role=" + role + ";" + reason);
        notifications.send(actor, userId, "CAMPUS_MEMBERSHIP", "campus_school", school);
        return member(school, userId, false);
    }

    public CampusPage<Audit> audit(long actor, long school, int page, int size) {
        int offset = CampusPage.offset(page, size);
        access.requireManage(actor, school);
        long total = count("SELECT COUNT(*) FROM campus_audit WHERE school_id=?", school);
        var rows = jdbc.query("SELECT id,actor_id,action,target_type,target_id,details,created_at,"
                        + CampusTimestamps.select("a", "created_at") + " FROM campus_audit a WHERE school_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new Audit(rs.getLong("id"), rs.getLong("actor_id"), rs.getString("action"),
                        rs.getString("target_type"), rs.getString("target_id"), rs.getString("details"), CampusTimestamps.read(rs, "created_at")), school, size, offset);
        return new CampusPage<>(rows, total, page, size);
    }

    private Application application(long school, long id, boolean lock) {
        CampusAccessService.positive(id);
        var rows = jdbc.query((lock ? APPLICATION_LOCK_SELECT : APPLICATION_SELECT)
                + " WHERE a.school_id=? AND a.id=?" + (lock ? " FOR UPDATE" : ""), APPLICATION_ROW, school, id);
        if (rows.isEmpty()) throw new CampusException(404, "认证申请不存在");
        return rows.get(0);
    }

    private Member lockedMember(long school, long user) { return member(school, user, true); }

    private Member member(long school, long user, boolean lock) {
        var rows = jdbc.query((lock ? MEMBER_LOCK_SELECT : MEMBER_SELECT)
                + " WHERE m.school_id=? AND m.user_id=?" + (lock ? " FOR UPDATE" : ""), MEMBER_ROW, school, user);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void protectLastAdministrator(long school, Member member, String nextStatus, String nextRole) {
        if ("VERIFIED".equals(member.status()) && "ADMIN".equals(member.role())
                && !("VERIFIED".equals(nextStatus) && "ADMIN".equals(nextRole))
                && count("SELECT COUNT(*) FROM campus_membership m JOIN `user` u ON u.id=m.user_id AND u.status=1"
                        + " WHERE m.school_id=? AND m.user_id<>? AND m.status='VERIFIED' AND m.role='ADMIN'", school, member.userId()) == 0) {
            throw new CampusException(409, "最后一名有效校园管理员不能退出、暂停或降级");
        }
    }

    private School schoolRow(ResultSet rs, boolean siteAdmin) throws SQLException {
        String memberStatus = rs.getString("membership_status");
        String role = rs.getString("membership_role");
        Membership membership = memberStatus == null ? null : new Membership(memberStatus, role, rs.getLong("membership_version"));
        LatestApplication application = rs.getObject("application_id") == null ? null : new LatestApplication(rs.getLong("application_id"),
                rs.getString("application_status"), rs.getString("application_reason"), rs.getLong("application_version"));
        boolean member = "VERIFIED".equals(memberStatus) && ("MEMBER".equals(role) || "ADMIN".equals(role));
        boolean manager = siteAdmin || (member && "ADMIN".equals(role));
        boolean active = rs.getBoolean("active");
        boolean read = active && (siteAdmin || member);
        return new School(rs.getLong("id"), rs.getString("name"), rs.getString("description"), active,
                rs.getBoolean("pre_moderation"), rs.getLong("version"), CampusTimestamps.read(rs, "created_at"), rs.getLong("member_count"), membership,
                application, new Capabilities(read, read, active && manager, active && manager, manager, siteAdmin));
    }

    private long insert(String sql, Object... params) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) statement.setObject(i + 1, params[i]);
            return statement;
        }, key);
        Number result = key.getKey();
        if (result == null) throw new IllegalStateException("Campus insert did not return an identity");
        return result.longValue();
    }

    private long count(String sql, Object... params) {
        Long count = jdbc.queryForObject(sql, Long.class, params);
        return count == null ? 0 : count;
    }

    private static void required(Object request) {
        if (request == null) throw new CampusException(400, "请求内容不能为空");
    }

    private static String text(String value, int maximum, boolean nonempty, String label) {
        String result = value == null ? "" : value.trim();
        if ((nonempty && result.isEmpty()) || result.length() > maximum) throw new CampusException(400, label + "长度无效，最多" + maximum + "个字符");
        return result;
    }

    private static String enumValue(String value, Set<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) throw new CampusException(400, label + "无效");
        return value;
    }

    private static void version(Long version) {
        if (version == null || version < 1) throw new CampusException(400, "必须提供有效版本号");
    }

    private static void checkVersion(long actual, Long expected) {
        if (actual != expected) throw new CampusException(409, "内容已变更，请刷新后重试");
    }

    private static void changed(int rows) {
        if (rows != 1) throw new CampusException(409, "状态已变更，请刷新后重试");
    }

    private static void pending(Application application) {
        if (!"PENDING".equals(application.status())) throw new CampusException(409, "申请已经处理或撤回");
    }

    private static void active(CampusAccessService.Access current) {
        if (!current.active()) throw new CampusException(403, "学校已停用");
    }

    private static void manage(CampusAccessService.Access current) {
        active(current);
        if (!current.manager()) throw new CampusException(403, "没有本校管理权限");
    }
}
