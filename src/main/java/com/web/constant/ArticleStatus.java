package com.web.constant;

/**
 * 文章状态枚举
 * 统一管理文章的各种状态
 */
public enum ArticleStatus {
    /**
     * 草稿状态 - 文章尚未提交审核
     */
    DRAFT("草稿", 0),
    
    /**
     * 待审核状态 - 文章已提交，等待审核
     */
    PENDING_REVIEW("待审核", 1),
    
    /**
     * 已发布状态 - 文章审核通过并已发布
     */
    PUBLISHED("已发布", 2),
    
    /**
     * 已拒绝状态 - 文章审核未通过
     */
    REJECTED("已拒绝", 3);

    private final String description;
    private final int code;

    ArticleStatus(String description, int code) {
        this.description = description;
        this.code = code;
    }

    /**
     * 获取状态描述
     * @return 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取状态代码
     * @return 状态代码
     */
    public int getCode() {
        return code;
    }

    /**
     * 根据代码获取状态枚举
     * @param code 状态代码
     * @return 对应的状态枚举，如果不存在则返回null
     */
    public static ArticleStatus fromCode(int code) {
        for (ArticleStatus status : ArticleStatus.values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据描述获取状态枚举
     * @param description 状态描述
     * @return 对应的状态枚举，如果不存在则返回null
     */
    public static ArticleStatus fromDescription(String description) {
        for (ArticleStatus status : ArticleStatus.values()) {
            if (status.description.equals(description)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 检查状态代码是否有效
     * @param code 状态代码
     * @return 是否有效
     */
    public static boolean isValidCode(int code) {
        return fromCode(code) != null;
    }

    /**
     * 检查是否可以从当前状态转换到目标状态
     * @param from 当前状态
     * @param to 目标状态
     * @return 是否可以转换
     */
    public static boolean canTransition(ArticleStatus from, ArticleStatus to) {
        if (from == null || to == null) {
            return false;
        }

        // 定义状态转换规则
        switch (from) {
            case DRAFT:
                // 草稿可以转换为待审核
                return to == PENDING_REVIEW;
            case PENDING_REVIEW:
                // 待审核可以转换为已发布或已拒绝
                return to == PUBLISHED || to == REJECTED;
            case PUBLISHED:
                // 已发布的文章不能转换状态（除非撤回到草稿，但这需要特殊权限）
                return false;
            case REJECTED:
                // 已拒绝的文章可以修改后重新提交审核
                return to == PENDING_REVIEW;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return description + "(" + code + ")";
    }
}
