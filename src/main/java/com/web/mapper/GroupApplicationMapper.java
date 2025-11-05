package com.web.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.GroupApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 群组申请 Mapper 接口
 */
@Mapper
public interface GroupApplicationMapper extends BaseMapper<GroupApplication> {

    /**
     * 根据群组ID查找待审批的申请列表
     * @param groupId 群组ID
     * @return 待审批申请列表
     */
    default List<GroupApplication> findPendingApplicationsByGroupId(Long groupId) {
        return selectList(new QueryWrapper<GroupApplication>()
            .eq("group_id", groupId)
            .eq("status", "PENDING")
            .orderByDesc("created_at"));
    }

    /**
     * 根据用户ID和群组ID查找申请记录
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 申请记录
     */
    default GroupApplication findByGroupAndUser(Long groupId, Long userId) {
        return selectOne(new QueryWrapper<GroupApplication>()
            .eq("group_id", groupId)
            .eq("user_id", userId)
            .orderByDesc("created_at")
            .last("LIMIT 1"));
    }

    /**
     * 检查用户是否已有待审批的申请
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 是否存在待审批申请
     */
    default boolean hasPendingApplication(Long groupId, Long userId) {
        return selectCount(new QueryWrapper<GroupApplication>()
            .eq("group_id", groupId)
            .eq("user_id", userId)
            .eq("status", "PENDING")) > 0;
    }

    /**
     * 根据群组ID查找所有申请（包括已处理的）
     * @param groupId 群组ID
     * @return 申请列表
     */
    default List<GroupApplication> findAllApplicationsByGroupId(Long groupId) {
        return selectList(new QueryWrapper<GroupApplication>()
            .eq("group_id", groupId)
            .orderByDesc("created_at"));
    }
}
