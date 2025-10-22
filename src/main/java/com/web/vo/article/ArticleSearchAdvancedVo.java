package com.web.vo.article;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 高级文章搜索的视图对象 (VO)
 * 用于封装从前端接收的复杂搜索参数
 */
@Data
public class ArticleSearchAdvancedVo {
    // 搜索关键词
    @Size(max = 100, message = "关键词长度不能超过100个字符")
    private String query;

    // 页码
    @Min(value = 1, message = "页码不能小于1")
    private int page = 1;

    // 每页大小
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private int pageSize = 10;

    // 开始日期
    private String startDate;

    // 结束日期
    private String endDate;

    // 分类ID列表
    private List<Long> categoryIds;

    // 作者ID
    private Long authorId;

    // 文章状态
    private Integer status;

    // 最小点赞数
    private Long minLikes;

    // 最大点赞数
    private Long maxLikes;

    // 最小曝光数
    private Long minExposure;

    // 最大曝光数
    private Long maxExposure;

    // 排序字段
    @Size(max = 50, message = "排序字段过长")
    private String sortBy = "created_at";

    // 排序方向 (asc/desc)
    @Size(max = 10, message = "排序方向过长")
    private String sortOrder = "desc";
}
