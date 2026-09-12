package com.web.mapper;

import com.web.model.User;
import org.apache.ibatis.annotations.*;

import java.util.Date;

/** All mutations first lock the user row, then change generation or exact-token revocation state. */
@Mapper
public interface AuthTokenStateMapper {
    @Select("SELECT id, username, password, status FROM `user` WHERE id=#{userId} FOR UPDATE")
    User lockUser(@Param("userId") Long userId);

    @Select("SELECT generation FROM auth_user_session WHERE user_id=#{userId}")
    String selectGeneration(@Param("userId") Long userId);

    @Select("SELECT generation FROM auth_user_session WHERE user_id=#{userId} FOR UPDATE")
    String lockGeneration(@Param("userId") Long userId);

    @Insert("INSERT IGNORE INTO auth_user_session (user_id,generation) VALUES (#{userId},#{generation})")
    int createGeneration(@Param("userId") Long userId, @Param("generation") String generation);

    @Insert("INSERT INTO auth_user_session (user_id,generation) VALUES (#{userId},#{generation}) "
            + "ON DUPLICATE KEY UPDATE generation=VALUES(generation)")
    int replaceGeneration(@Param("userId") Long userId, @Param("generation") String generation);

    @Select("SELECT EXISTS(SELECT 1 FROM auth_token_revocation WHERE jti=#{jti})")
    boolean isRevoked(@Param("jti") String jti);

    @Insert("INSERT IGNORE INTO auth_token_revocation (jti,user_id,expires_at) VALUES (#{jti},#{userId},#{expiresAt})")
    int revoke(@Param("jti") String jti, @Param("userId") Long userId, @Param("expiresAt") Date expiresAt);
}
