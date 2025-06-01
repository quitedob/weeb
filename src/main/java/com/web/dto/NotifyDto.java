package com.web.dto;

import lombok.Data;

import java.util.Date;

/**
 * 通知数据传输对象 (NotifyDto)。
 * 用于封装通知相关的数据，便于在系统中传递和操作。
 */
@Data // Lombok注解，自动生成Getter、Setter、toString等方法
public class NotifyDto {

    /**
     * 通知类型。
     * 用于标识通知的类别，例如系统通知、用户消息等。
     */
    private String type;

    /**
     * 通知内容。
     * 包含通知的主要信息，例如提示内容、消息正文等。
     */
    private String content;

    /**
     * 通知时间。
     * 表示通知的创建时间或发生时间，用于记录时间戳。
     */
    private Date time;

    /**
     * 扩展字段。
     * 用于存储额外的信息，可根据需求自定义，例如用户ID、额外数据等。
     */
    private String ext;
}
