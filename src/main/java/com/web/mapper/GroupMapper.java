package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Group;
import com.web.dto.GroupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List; // Added

/**
 * 群组 Mapper 接口
 * 简化注释：群组Mapper
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
    // Custom methods can be added here if needed later.
    // For now, BaseMapper provides common CRUD operations.

    /**
     * 根据用户ID查找其所在的所有群组
     * @param userId 用户ID
     * @return 群组列表
     */
    List<Group> findGroupsByUserId(Long userId); // 修改为Long类型以匹配数据库BIGINT

    /**
     * 根据群主ID查找其拥有的所有群组
     * @param ownerId 群主ID
     * @return 群组列表
     */
    List<Group> findGroupsByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * 搜索群组（按群组名称模糊搜索）
     * @param keyword 关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 群组列表
     */
    List<Group> searchGroups(String keyword, int offset, int limit);

    /**
     * 统计搜索群组数量
     * @param keyword 关键词
     * @return 总数
     */
    long countSearchGroups(String keyword);

    /**
     * 带过滤条件的搜索群组
     * @param keyword 关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param sortClause 排序子句
     * @return 群组列表
     */
    List<Group> searchGroupsWithFilters(@Param("keyword") String keyword,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit,
                                       @Param("startDate") String startDate,
                                       @Param("endDate") String endDate,
                                       @Param("sortClause") String sortClause);

    /**
     * 统计带过滤条件的搜索群组数量
     * @param keyword 关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 总数
     */
    long countSearchGroupsWithFilters(@Param("keyword") String keyword,
                                     @Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    /**
     * 高级搜索群组（支持多维度过滤）
     * @param keyword 搜索关键词
     * @param offset 偏移量
     * @param limit 限制数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param groupTypes 群组类型列表
     * @param isPublic 是否公开群组
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @return 群组列表
     */
    List<Group> searchGroupsAdvanced(@Param("keyword") String keyword,
                                    @Param("offset") int offset,
                                    @Param("limit") int limit,
                                    @Param("startDate") String startDate,
                                    @Param("endDate") String endDate,
                                    @Param("groupTypes") List<Integer> groupTypes,
                                    @Param("isPublic") Boolean isPublic,
                                    @Param("sortBy") String sortBy,
                                    @Param("sortOrder") String sortOrder);

    /**
     * 统计高级搜索群组数量
     * @param keyword 搜索关键词
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param groupTypes 群组类型列表
     * @param isPublic 是否公开群组
     * @return 总数
     */
    long countSearchGroupsAdvanced(@Param("keyword") String keyword,
                                  @Param("startDate") String startDate,
                                  @Param("endDate") String endDate,
                                  @Param("groupTypes") List<Integer> groupTypes,
                                  @Param("isPublic") Boolean isPublic);

    /**
     * 查询用户所在的所有群组详细信息
     * @param userId 用户ID
     * @return 群组详细信息列表
     */
    List<GroupDto> selectUserGroupsWithDetails(@Param("userId") Long userId);

    /**
     * 查询用户创建的所有群组详细信息
     * @param userId 用户ID
     * @return 群组详细信息列表
     */
    List<GroupDto> selectUserCreatedGroupsWithDetails(@Param("userId") Long userId);

    /**
     * 查询单个群组详细信息（包含用户角色）
     * @param groupId 群组ID
     * @param userId 用户ID
     * @return 群组详细信息
     */
    GroupDto selectGroupWithDetails(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
