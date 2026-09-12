package com.web.campus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Private images: the database grants access; filenames are never an authorization token. */
@Service
public class CampusMediaService {
    private static final Logger log = LoggerFactory.getLogger(CampusMediaService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final JdbcTemplate jdbc;
    private final CampusAccessService access;
    private final TransactionTemplate transactions;
    private final Path directory;
    private String orphanScanAfter = "";

    public CampusMediaService(JdbcTemplate jdbc, CampusAccessService access, PlatformTransactionManager manager,
                              @Value("${campus.media-directory:.local/campus-media}") String directory) {
        this.jdbc = jdbc;
        this.access = access;
        this.transactions = new TransactionTemplate(manager);
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        for (String publicPath : List.of("uploads", "src/main/resources/static", "src/main/resources/public")) {
            if (this.directory.startsWith(Path.of(publicPath).toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("Campus media must be stored outside public static directories");
            }
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Map<String, Object> upload(long actor, long school, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new CampusException(400, "图片大小必须在5MB以内");
        }
        // Decode before taking the school write lock; untrusted MIME and filenames are ignored.
        BufferedImage pixels = decode(file);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(pixels, "png", bytes)) throw new IOException("PNG encoder unavailable");
        if (bytes.size() > MAX_BYTES) throw new CampusException(400, "重新编码后的图片超过5MB，请压缩后重试");
        access.lockSchool(actor, school);
        access.requireRead(actor, school);
        long pending = jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE school_id=? AND owner_id=? AND post_id IS NULL", Long.class, school, actor);
        if (pending >= 12) throw new CampusException(409, "未使用图片最多12张，请先删除不用的图片");
        long used = jdbc.queryForObject("SELECT COALESCE(SUM(size),0) FROM campus_media WHERE school_id=? AND owner_id=?", Long.class, school, actor);
        if (used + bytes.size() > 100L * 1024 * 1024) throw new CampusException(409, "本校图片存储已达100MB，请清理旧动态后重试");
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new IOException("Media directory must not be a symbolic link");
        if (Files.getFileStore(directory).getUsableSpace() < 100L * 1024 * 1024 + bytes.size()) {
            throw new CampusException(503, "图片存储空间暂时不足，请稍后再试");
        }
        String id = UUID.randomUUID().toString();
        Path path = filePath(id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) removeFileQuietly(path);
            }
        });
        Files.write(path, bytes.toByteArray(), StandardOpenOption.CREATE_NEW);
        jdbc.update("INSERT INTO campus_media(id,school_id,owner_id,width,height,size) VALUES (?,?,?,?,?,?)",
                id, school, actor, pixels.getWidth(), pixels.getHeight(), bytes.size());
        return media(id, pixels.getWidth(), pixels.getHeight(), bytes.size());
    }

    private static BufferedImage decode(MultipartFile file) throws IOException {
        try (var input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw new CampusException(400, "请上传有效的PNG、JPEG或GIF图片");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new CampusException(400, "请上传有效的PNG、JPEG或GIF图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                if (!reader.getFormatName().matches("(?i)png|jpeg|jpg|gif")) throw new CampusException(400, "不支持的图片格式");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 16_000_000) {
                    throw new CampusException(400, "图片长宽最多4096像素，总像素最多1600万");
                }
                BufferedImage result = reader.read(0);
                if (result == null) throw new CampusException(400, "无法读取图片");
                return result;
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException invalid) {
            throw new CampusException(400, "图片损坏或格式不正确");
        }
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public byte[] read(long actor, String id) throws IOException {
        filePath(id);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT school_id,owner_id,post_id,detached_at FROM campus_media WHERE id=?", id);
        if (rows.isEmpty()) throw new CampusException(404, "图片不存在");
        Map<String, Object> row = rows.get(0);
        access.requireRead(actor, number(row, "school_id"));
        if (row.get("post_id") != null) access.requirePostRead(actor, number(row, "post_id"));
        else {
            if (number(row, "owner_id") != actor) throw new CampusException(403, "无权访问该图片");
            if (jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE id=? AND detached_at > CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR", Long.class, id) == 0) {
                throw new CampusException(404, "未使用图片已过期，请重新上传");
            }
        }
        Path path = filePath(id);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new CampusException(404, "图片文件不存在");
        return Files.readAllBytes(path);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(long actor, String id) {
        filePath(id);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT school_id FROM campus_media WHERE id=?", id);
        if (rows.isEmpty()) throw new CampusException(404, "图片不存在");
        long school = number(rows.get(0), "school_id");
        access.lockSchool(actor, school);
        access.requireRead(actor, school);
        int removed = jdbc.update("DELETE FROM campus_media WHERE id=? AND owner_id=? AND post_id IS NULL", id, actor);
        if (removed == 0) throw new CampusException(409, "只能删除本人未关联动态的图片");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { removeFileQuietly(filePath(id)); }
        });
    }

    /** Invoked inside the post transaction after taking the school lock. */
    public void replaceAttachments(long actor, long school, long post, List<String> ids) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Attachments require a transaction");
        if (ids == null || ids.size() > 6 || new HashSet<>(ids).size() != ids.size()) throw new CampusException(400, "每条动态最多6张不重复图片");
        access.requireRead(actor, school);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM campus_post WHERE id=? AND school_id=? AND author_id=? AND status <> 'REMOVED'", Long.class, post, school, actor) != 1) {
            throw new CampusException(403, "只能关联本人同校动态的图片");
        }
        for (String id : ids) {
            filePath(id);
            long allowed = jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE id=? AND owner_id=? AND school_id=? "
                            + "AND (post_id=? OR (post_id IS NULL AND detached_at > CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR))",
                    Long.class, id, actor, school, post);
            if (allowed != 1 || !Files.isRegularFile(filePath(id), LinkOption.NOFOLLOW_LINKS)) {
                throw new CampusException(400, "图片已失效、属于其他动态或没有使用权限");
            }
        }
        jdbc.update("UPDATE campus_media SET post_id=NULL,detached_at=CURRENT_TIMESTAMP(3),position=0 WHERE post_id=?", post);
        for (int position = 0; position < ids.size(); position++) {
            jdbc.update("UPDATE campus_media SET post_id=?,detached_at=NULL,position=? WHERE id=?", post, position, ids.get(position));
        }
    }

    /** Internal only: callers must first enforce the current post read policy. */
    public List<Map<String, Object>> listForPost(long post) {
        return jdbc.query("SELECT id,width,height,size FROM campus_media WHERE post_id=? ORDER BY position,id",
                (rs, n) -> media(rs.getString("id"), rs.getInt("width"), rs.getInt("height"), rs.getLong("size")), post);
    }

    @Scheduled(fixedDelayString = "${campus.media-cleanup-interval-ms:3600000}", initialDelayString = "${campus.media-cleanup-initial-delay-ms:60000}")
    public synchronized void cleanup() {
        try {
            // Bounded batch; deleted posts have a 24h retention window and cannot be restored.
            List<Map<String, Object>> expired = jdbc.queryForList("SELECT m.id,m.school_id FROM campus_media m LEFT JOIN campus_post p ON p.id=m.post_id "
                    + "WHERE (m.post_id IS NULL AND m.detached_at < CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR) "
                    + "OR (p.status='REMOVED' AND p.updated_at < CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR) ORDER BY m.id LIMIT 200");
            for (Map<String, Object> row : expired) {
                transactions.executeWithoutResult(status -> {
                    jdbc.queryForList("SELECT id FROM campus_school WHERE id=? FOR UPDATE", number(row, "school_id"));
                    String id = row.get("id").toString();
                    long eligible = jdbc.queryForObject("SELECT COUNT(*) FROM campus_media m LEFT JOIN campus_post p ON p.id=m.post_id WHERE m.id=? "
                            + "AND ((m.post_id IS NULL AND m.detached_at < CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR) "
                            + "OR (p.status='REMOVED' AND p.updated_at < CURRENT_TIMESTAMP(3) - INTERVAL 24 HOUR))", Long.class, id);
                    if (eligible == 1 && removeFileQuietly(filePath(id))) jdbc.update("DELETE FROM campus_media WHERE id=?", id);
                });
            }
            // Recover files left by a process crash before the upload transaction committed.
            if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                for (Path path : nextOrphanBatch()) {
                    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                            || !Files.getLastModifiedTime(path).toInstant().isBefore(Instant.now().minus(24, ChronoUnit.HOURS))) continue;
                    String id = path.getFileName().toString().substring(0, 36);
                    if (jdbc.queryForObject("SELECT COUNT(*) FROM campus_media WHERE id=?", Long.class, id) == 0) removeFileQuietly(path);
                }
            }
        } catch (Exception failure) {
            log.warn("Campus media cleanup failed: {}", failure.getClass().getSimpleName());
        }
    }

    /** A bounded retained batch advances through the directory instead of revisiting its first 1000 files forever. */
    private List<Path> nextOrphanBatch() throws IOException {
        var batch = new TreeMap<String, Path>();
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path path : entries) {
                String name = path.getFileName().toString();
                if (name.compareTo(orphanScanAfter) <= 0 || !name.matches("[0-9a-f-]{36}\\.png")) continue;
                batch.put(name, path);
                if (batch.size() > 1000) batch.pollLastEntry();
            }
        }
        orphanScanAfter = batch.isEmpty() ? "" : batch.lastKey();
        return List.copyOf(batch.values());
    }

    private Path filePath(String id) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new CampusException(400, "无效的图片标识");
        }
        return directory.resolve(id + ".png");
    }
    private static Map<String, Object> media(String id, int width, int height, long size) {
        return Map.of("id", id, "url", "/api/campus/media/" + id, "width", width, "height", height, "size", size);
    }
    private static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    private boolean removeFileQuietly(Path path) {
        try { Files.deleteIfExists(path); return true; }
        catch (IOException failure) { log.warn("Campus media file cleanup deferred: {}", failure.getClass().getSimpleName()); return false; }
    }
}
