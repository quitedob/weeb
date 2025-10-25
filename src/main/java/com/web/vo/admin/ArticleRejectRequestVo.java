package com.web.vo.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章拒绝请求VO
 */
@Data
public class ArticleRejectRequestVo {

    /**
     * 拒绝原因
     */
    @Size(max = 500, message = "拒绝原因长度不能超过500个字符")
    private String reason = "内容不符合社区规范";

    /**
     * 拒绝类型：spam(垃圾信息), inappropriate(不当内容), copyright(版权问题), quality(质量不佳), other(其他)
     */
    private String rejectType = "other";

    /**
     * 是否通知作者
     */
    private Boolean notifyAuthor = true;

    /**
     * 是否允许修改后重新提交
     */
    private Boolean allowResubmission = true;

    /**
     * 修改建议（可选）
     */
    @Size(max = 1000, message = "修改建议长度不能超过1000个字符")
    private String suggestions;
}