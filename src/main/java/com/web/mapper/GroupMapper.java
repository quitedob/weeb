package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Group;
import org.apache.ibatis.annotations.Mapper;
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
    List<Group> findGroupsByUserId(Integer userId); // Added
}
