package com.web.vo.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员删除文章请求VO
 */
@Data
public class ArticleAdminDeleteRequestVo {

    /**
     * 删除原因
     */
    @Size(max = 500, message = "删除原因长度不能超过500个字符")
    private String reason = "管理员删除";

    /**
     * 删除类型：violation(违规), spam(垃圾信息), duplicate(重复), quality(质量不佳), other(其他)
     */
    private String deleteType = "other";

    /**
     * 是否通知作者
     */
    private Boolean notifyAuthor = true;

    /**
     * 是否永久删除（false则移到回收站）
     */
    private Boolean permanent = false;

    /**
     * 是否封禁作者
     */
    private Boolean banAuthor = false;

    /**
     * 封禁时长（天）
     */
    private Integer banDuration;
}