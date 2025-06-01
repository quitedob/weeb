package com.web.service;

import com.web.vo.file.AcceptVo;
import com.web.vo.file.AnswerVo;
import com.web.vo.file.CancelVo;
import com.web.vo.file.CandidateVo;
import com.web.vo.file.InviteVo;
import com.web.vo.file.OfferVo;

public interface FileService {

    /**
     * 发起offer
     */
    boolean offer(String userId, OfferVo offerVo);

    /**
     * 发起answer
     */
    boolean answer(String userId, AnswerVo answerVo);

    /**
     * 发送candidate
     */
    boolean candidate(String userId, CandidateVo candidateVo);

    /**
     * 取消或挂断
     */
    boolean cancel(String userId, CancelVo cancelVo);

    /**
     * 邀请
     */
    boolean invite(String userId, InviteVo inviteVo);

    /**
     * 同意
     */
    boolean accept(String userId, AcceptVo acceptVo);
}
