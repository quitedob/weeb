package com.web.campus;

import com.web.constant.UserType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Current, persisted campus authority shared by HTTP, media and notification reads. */
@Service
public class CampusAccessService {
    public record Access(long schoolId, boolean active, boolean siteAdmin, boolean member,
                         boolean manager, boolean preModeration) { }
    private record School(long id, boolean active, boolean preModeration) { }
    private record Account(int status, String type) { }
    private record Membership(String status, String role) { }

    private final JdbcTemplate jdbc;

    public CampusAccessService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void requireActiveUser(long actor) { account(actor, false); }

    public boolean isSiteAdmin(long actor) {
        if (actor <= 0) return false;
        var users = jdbc.query("SELECT status,type FROM `user` WHERE id=?",
                (rs, row) -> new Account(rs.getInt("status"), rs.getString("type")), actor);
        return !users.isEmpty() && users.get(0).status() == 1
                && UserType.ADMIN.equals(UserType.normalize(users.get(0).type()));
    }

    public Access inspect(long actor, long school) {
        Account user = account(actor, false);
        return access(actor, school(school, false), user, false);
    }

    /** All campus writes serialize here, before locking application/member/content rows. */
    public Access lockSchool(long actor, long school) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Campus school lock requires a business transaction");
        }
        School current = school(school, true);
        // Locking reads deliberately bypass an older REPEATABLE_READ snapshot.
        return access(actor, current, account(actor, true), true);
    }

    public Access requireRead(long actor, long school) {
        Access current = inspect(actor, school);
        if (!current.active() || !(current.member() || current.siteAdmin())) {
            throw new CampusException(403, "需要有效的本校认证成员身份");
        }
        return current;
    }

    public Access requireManage(long actor, long school) {
        Access current = inspect(actor, school);
        if (!current.active() || !current.manager()) throw new CampusException(403, "没有本校管理权限");
        return current;
    }

    public Access requirePostRead(long actor, long post) {
        positive(post);
        record Post(long school, long author, String status) { }
        var posts = jdbc.query("SELECT school_id,author_id,status FROM campus_post WHERE id=?",
                (rs, row) -> new Post(rs.getLong("school_id"), rs.getLong("author_id"), rs.getString("status")), post);
        if (posts.isEmpty() || "REMOVED".equals(posts.get(0).status())) {
            throw new CampusException(404, "校园动态不存在");
        }
        Post item = posts.get(0);
        Access current = requireRead(actor, item.school());
        if (!"PUBLISHED".equals(item.status()) && item.author() != actor && !current.manager()) {
            throw new CampusException(403, "没有查看此校园动态的权限");
        }
        return current;
    }

    public boolean canReadPost(long actor, long post) {
        try {
            requirePostRead(actor, post);
            return true;
        } catch (CampusException denied) {
            if (denied.getStatus() == 401 || denied.getStatus() == 403 || denied.getStatus() == 404) return false;
            throw denied;
        }
    }

    private Account account(long actor, boolean lock) {
        if (actor <= 0) throw new CampusException(401, "请先登录");
        var users = jdbc.query("SELECT status,type FROM `user` WHERE id=?" + (lock ? " FOR SHARE" : ""),
                (rs, row) -> new Account(rs.getInt("status"), rs.getString("type")), actor);
        if (users.isEmpty() || users.get(0).status() != 1) throw new CampusException(401, "账号不可用，请重新登录");
        return users.get(0);
    }

    private School school(long school, boolean lock) {
        positive(school);
        var rows = jdbc.query("SELECT id,active,pre_moderation FROM campus_school WHERE id=?"
                        + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new School(rs.getLong("id"), rs.getBoolean("active"), rs.getBoolean("pre_moderation")), school);
        if (rows.isEmpty()) throw new CampusException(404, "学校不存在");
        return rows.get(0);
    }

    private Access access(long actor, School school, Account user, boolean lock) {
        var memberships = jdbc.query("SELECT status,role FROM campus_membership WHERE school_id=? AND user_id=?"
                        + (lock ? " FOR UPDATE" : ""),
                (rs, row) -> new Membership(rs.getString("status"), rs.getString("role")), school.id(), actor);
        Membership membership = memberships.isEmpty() ? null : memberships.get(0);
        boolean member = membership != null && "VERIFIED".equals(membership.status())
                && ("MEMBER".equals(membership.role()) || "ADMIN".equals(membership.role()));
        boolean siteAdmin = UserType.ADMIN.equals(UserType.normalize(user.type()));
        return new Access(school.id(), school.active(), siteAdmin, member,
                siteAdmin || (member && "ADMIN".equals(membership.role())), school.preModeration());
    }

    static void positive(long id) {
        if (id <= 0) throw new CampusException(400, "ID必须为正整数");
    }
}
