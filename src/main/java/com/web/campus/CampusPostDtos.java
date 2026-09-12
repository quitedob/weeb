package com.web.campus;

import java.util.List;

/** Campus requests intentionally contain no actor, school ownership, or moderation status fields. */
public final class CampusPostDtos {
    private CampusPostDtos() {}
    public record PostWrite(String title, String content, String category, List<String> mediaIds,
                            Boolean submit, Long version) {}
    public record Review(String decision, String reason, Long version) {}
    public record Pin(Boolean pinned, Long version) {}
    public record CommentWrite(String content, Long replyToCommentId) {}
    public record ReportWrite(String reason) {}
}
