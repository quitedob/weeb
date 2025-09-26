package com.web.service.impl;

import com.web.mapper.FileMapper;
import com.web.model.FileTransfer;
import com.web.service.FileService;
import com.web.vo.file.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 文件服务实现类
 * 处理WebRTC文件传输相关的业务逻辑
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Override
    public boolean invite(Long userId, InviteVo inviteVo) {
        try {
            // 创建文件传输邀请记录
            FileTransfer fileTransfer = new FileTransfer();
            fileTransfer.setInitiatorId(userId);
            fileTransfer.setTargetId(inviteVo.getTargetId());
            // 注意：根据FileTransfer实际字段设置文件信息
            // fileTransfer.setFileName(inviteVo.getFileName());
            // fileTransfer.setFileSize(inviteVo.getFileSize());
            fileTransfer.setStatus(1); // 使用Integer状态码
            fileTransfer.setCreatedAt(new Date());
            fileTransfer.setUpdatedAt(new Date());
            
            int result = fileMapper.insertFileTransfer(fileTransfer);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean accept(Long userId, AcceptVo acceptVo) {
        try {
            // 接受文件传输邀请
            Date now = new Date();
            int result = fileMapper.acceptTransfer(acceptVo.getTransferId(), now);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean offer(Long userId, OfferVo offerVo) {
        try {
            // 更新offer信息
            Date now = new Date();
            int result = fileMapper.updateOffer(offerVo.getTransferId(), offerVo.getSdp(), now);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean answer(Long userId, AnswerVo answerVo) {
        try {
            // 更新answer信息
            Date now = new Date();
            int result = fileMapper.updateAnswer(answerVo.getTransferId(), answerVo.getSdp(), now);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean candidate(Long userId, CandidateVo candidateVo) {
        try {
            // 更新candidate信息
            Date now = new Date();
            int result = fileMapper.updateCandidate(candidateVo.getTransferId(), candidateVo.getCandidate_(), now);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean cancel(Long userId, CancelVo cancelVo) {
        try {
            // 取消文件传输
            Date now = new Date();
            int result = fileMapper.cancelTransfer(cancelVo.getTransferId(), now);
            return result > 0;
        } catch (Exception e) {
            return false;
        }
    }
}