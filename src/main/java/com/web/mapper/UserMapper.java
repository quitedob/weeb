package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.User; // Assuming User.java is in com.web.model
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户Mapper接口
 * 简化注释：用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 更新用户的在线状态
     * @param userId 用户ID (should be Long to match User.id)
     * @param onlineStatus 在线状态码
     * 简化注释：更新在线状态
     */
    @Update("UPDATE `user` SET online_status = #{onlineStatus} WHERE id = #{userId}")
    void updateOnlineStatus(@Param("userId") Long userId, @Param("onlineStatus") Integer onlineStatus);
    // Self-correction: Changed userId parameter to Long to match User.id type.
    // The user's example had Integer, but User.id is Long.
}
