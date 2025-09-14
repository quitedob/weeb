package com.web.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param; // For named parameters if using XML or @Select
// import org.apache.ibatis.annotations.Select; // Example if using @Select for custom queries

import java.util.List;
import java.util.stream.Collectors;

/**
 * 群组成员 Mapper 接口
 * 简化注释：群成员Mapper
 */
@Mapper
public interface GroupMemberMapper extends BaseMapper<GroupMember> {

    /**
     * 检查用户是否为群成员
     * @param groupId 群ID
     * @param userId 用户ID
     * @return true if member, false otherwise
     */
    default boolean isMember(Long groupId, Long userId) {
        return selectCount(new QueryWrapper<GroupMember>()
            .eq("group_id", groupId)
            .eq("user_id", userId)) > 0;
    }

    /**
     * 根据群ID和用户ID查找群成员记录
     * @param groupId 群ID
     * @param userId 用户ID
     * @return GroupMember object or null if not found
     */
    default GroupMember findByGroupAndUser(Long groupId, Long userId) {
        return selectOne(new QueryWrapper<GroupMember>()
            .eq("group_id", groupId)
            .eq("user_id", userId));
    }

    /**
     * 根据群ID查找所有用户ID
     * @param groupId 群ID
     * @return List of User IDs
     */
    default List<Long> findUserIdsByGroupId(Long groupId) {
        List<GroupMember> members = selectList(new QueryWrapper<GroupMember>()
            .eq("group_id", groupId)
            .select("user_id")); // Optimize to select only user_id column
        return members.stream().map(GroupMember::getUserId).collect(Collectors.toList());
    }

    // Example of a more complex query if needed (e.g., using @Select or XML):
    /*
    @Select("SELECT * FROM group_member WHERE group_id = #{groupId} AND role = #{role}")
    List<GroupMember> findMembersByRole(@Param("groupId") Long groupId, @Param("role") Integer role);
    */
}
