package com.web.vo.file;

import lombok.Data;

@Data
public class OfferVo {
    private Long transferId;  // 用于标识文件传输记录的ID
    private Long userId;    // 当前用户ID
    private String desc;      // SDP描述信息（原Object类型改为String）
    private String Sdp;}

    // 通过 Lombok 的 @Data 自动生成 getter 和 setter 方法
