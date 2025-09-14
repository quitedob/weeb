package com.web.vo.message;

import lombok.Data;

import jakarta.validation.constraints.Max;

@Data
public class RecordVo {
    //目标id
    private String targetId;
    //起始
    private int index;
    //查询条数
    @Max(100)
    private int num;
}
