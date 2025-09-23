package com.web.service.impl;

import com.web.mapper.FileMapper;
import com.web.model.FileTransfer;
import com.web.service.FileService;
import com.web.vo.file.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;

@Service
public class FileServiceImpl implements FileService {

    @Resource
    private FileMapper fileMapper;

    @Override
    public boolean offer(String userId, OfferVo offerVo) {
        // 假设这里拿到 fileTransferId 或者 targetId
        Long transferId = offerVo.getTransferId();
        String offerSdp = offerVo.getSdp();
        // 更新DB
        int updated = fileMapper.updateOffer(transferId, offerSdp, new Date());
        return updated > 0;
    }

    @Override
    public boolean answer(String userId, AnswerVo answerVo) {
        Long transferId = answerVo.getTransferId();
        String answerSdp = answerVo.getSdp();
        int updated = fileMapper.updateAnswer(transferId, answerSdp, new Date());
        return updated > 0;
    }

    @Override
    public boolean candidate(String userId, CandidateVo candidateVo) {
        Long transferId = candidateVo.getTransferId();
        String candidate_ = candidateVo.getCandidate_();
        int updated = fileMapper.updateCandidate(transferId, candidate_, new Date());
        return updated > 0;
    }

    @Override
    public boolean cancel(String userId, CancelVo cancelVo) {
        // 简单处理：将记录删除或更新状态
        Long transferId = cancelVo.getTransferId();
        int updated = fileMapper.cancelTransfer(transferId, new Date());
        return updated > 0;
    }

    @Override
    public boolean invite(String userId, InviteVo inviteVo) {
        // 新增一条 record
        FileTransfer record = new FileTransfer();
        record.setInitiatorId(Long.valueOf(userId));       // 发起者
        record.setTargetId(inviteVo.getTargetId());        // 接收者
        record.setStatus(0);                               // 0=INVITE
        fileMapper.insertFileTransfer(record);
        return record.getId() != null && record.getId() > 0;
    }

    @Override
    public boolean accept(String userId, AcceptVo acceptVo) {
        // 更新状态
        Long transferId = acceptVo.getTransferId();
        int updated = fileMapper.acceptTransfer(transferId, new Date());
        return updated > 0;
    }
}
