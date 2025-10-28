package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.ContactGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 联系人分组Mapper接口
 * 处理联系人分组相关的数据库操作
 */
@Mapper
public interface ContactGroupMapper extends BaseMapper<ContactGroup> {

    /**
     * 根据用户ID获取所有分组
     * @param userId 用户ID
     * @return 分组列表
     */
    @Select("SELECT * FROM contact_group WHERE user_id = #{userId} ORDER BY group_order ASC, created_at ASC")
    List<ContactGroup> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和分组名称查找分组
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 分组对象
     */
    @Select("SELECT * FROM contact_group WHERE user_id = #{userId} AND group_name = #{groupName} LIMIT 1")
    ContactGroup findByUserIdAndName(@Param("userId") Long userId, @Param("groupName") String groupName);

    /**
     * 获取用户的默认分组
     * @param userId 用户ID
     * @return 默认分组
     */
    @Select("SELECT * FROM contact_group WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    ContactGroup findDefaultByUserId(@Param("userId") Long userId);

    /**
     * 创建默认分组
     * @param userId 用户ID
     * @param groupName 分组名称
     * @return 影响行数
     */
    @Insert("INSERT INTO contact_group (user_id, group_name, group_order, is_default, created_at, updated_at) " +
            "VALUES (#{userId}, #{groupName}, 0, 1, NOW(), NOW())")
    int createDefaultGroup(@Param("userId") Long userId, @Param("groupName") String groupName);

    /**
     * 更新分组排序
     * @param id 分组ID
     * @param groupOrder 新排序
     * @return 影响行数
     */
    @Update("UPDATE contact_group SET group_order = #{groupOrder}, updated_at = NOW() WHERE id = #{id}")
    int updateGroupOrder(@Param("id") Long id, @Param("groupOrder") Integer groupOrder);

    /**
     * 删除分组
     * @param id 分组ID
     * @param userId 用户ID（用于权限检查）
     * @return 影响行数
     */
    @Delete("DELETE FROM contact_group WHERE id = #{id} AND user_id = #{userId} AND is_default = 0")
    int deleteGroup(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 统计用户的分组数量
     * @param userId 用户ID
     * @return 分组数量
     */
    @Select("SELECT COUNT(*) FROM contact_group WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);
}
