package com.web.vo.chat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 聊天消息列表视图对象
 * 用于分页获取消息历史的请求数据
 */
public class ChatMessagesVo {

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1; // 页码

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 20; // 每页大小

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}

