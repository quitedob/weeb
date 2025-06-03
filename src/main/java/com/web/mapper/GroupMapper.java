package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.Group;
import org.apache.ibatis.annotations.Mapper;

/**
 * 群组 Mapper 接口
 * 简化注释：群组Mapper
 */
@Mapper
public interface GroupMapper extends BaseMapper<Group> {
    // Custom methods can be added here if needed later.
    // For now, BaseMapper provides common CRUD operations.
}
