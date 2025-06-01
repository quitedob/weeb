package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息映射器接口，负责消息的数据库操作
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 获取上一条需要显示时间的消息
     *
     * @param userId    用户ID
     * @param targetId  目标聊天对象ID
     * @return 上一条显示时间的消息
     */
    Message getPreviousShowTimeMsg(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 获取聊天记录
     *
     * @param userId   用户ID
     * @param targetId 目标聊天对象ID
     * @param index    起始索引
     * @param num      查询条数
     * @return 消息列表
     */
    List<Message> record(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("index") int index, @Param("num") int num);
}
