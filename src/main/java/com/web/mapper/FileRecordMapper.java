package com.web.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.web.model.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文件记录Mapper接口
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
    
    /**
     * 根据文件哈希查找文件
     * @param fileHash 文件哈希值
     * @return 文件记录
     */
    FileRecord findByHash(@Param("fileHash") String fileHash);
    
    /**
     * 获取用户文件列表
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 文件列表
     */
    List<Map<String, Object>> getUserFiles(@Param("userId") Long userId, 
                                          @Param("offset") int offset, 
                                          @Param("limit") int limit);
    
    /**
     * 获取用户文件总数
     * @param userId 用户ID
     * @return 文件总数
     */
    int getUserFileCount(@Param("userId") Long userId);
}