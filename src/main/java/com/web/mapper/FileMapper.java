package com.web.mapper;

import com.web.model.FileTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface FileMapper {

    /**
     * 插入一条新的 file_transfer 记录
     */
    int insertFileTransfer(FileTransfer record);

    /**
     * 更新offer信息
     */
    int updateOffer(@Param("id") Long id,
                    @Param("offerSdp") String offerSdp,
                    @Param("now") Date now);

    /**
     * 更新answer信息
     */
    int updateAnswer(@Param("id") Long id,
                     @Param("answerSdp") String answerSdp,
                     @Param("now") Date now);

    /**
     * 更新candidate信息
     */
    int updateCandidate(@Param("id") Long id,
                        @Param("candidate") String candidate,
                        @Param("now") Date now);

    /**
     * 邀请后对方接受
     */
    int acceptTransfer(@Param("id") Long id, @Param("now") Date now);

    /**
     * 取消(或挂断)传输
     */
    int cancelTransfer(@Param("id") Long id, @Param("now") Date now);

    /**
     * 根据 id 查询记录
     */
    FileTransfer findById(@Param("id") Long id);
}
