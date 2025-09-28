package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.FileShare;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文件分享Mapper接口
 */
public interface FileShareMapper extends BaseMapper<FileShare> {

    /**
     * 根据分享令牌查找分享记录
     * @param shareToken 分享令牌
     * @return 分享记录
     */
    @Select("SELECT * FROM file_share WHERE share_token = #{shareToken} AND status = 'ACTIVE'")
    FileShare selectByShareToken(@Param("shareToken") String shareToken);

    /**
     * 获取用户的分享记录
     * @param userId 用户ID
     * @return 分享记录列表
     */
    @Select("SELECT fs.*, fr.file_name, fr.file_size, fr.mime_type " +
            "FROM file_share fs " +
            "LEFT JOIN file_record fr ON fs.file_id = fr.id " +
            "WHERE fs.sharer_id = #{userId} " +
            "ORDER BY fs.created_at DESC")
    List<FileShare> selectUserShares(@Param("userId") Long userId);

    /**
     * 获取分享给特定用户的文件
     * @param userId 用户ID
     * @return 分享记录列表
     */
    @Select("SELECT fs.*, fr.file_name, fr.file_size, fr.mime_type " +
            "FROM file_share fs " +
            "LEFT JOIN file_record fr ON fs.file_id = fr.id " +
            "WHERE fs.shared_to_user_id = #{userId} AND fs.status = 'ACTIVE' " +
            "ORDER BY fs.created_at DESC")
    List<FileShare> selectSharedToUser(@Param("userId") Long userId);

    /**
     * 更新分享记录的访问次数
     * @param shareId 分享ID
     */
    @Select("UPDATE file_share SET access_count = access_count + 1 WHERE id = #{shareId}")
    void incrementAccessCount(@Param("shareId") Long shareId);
}
