package com.web.Controller;

import cn.hutool.json.JSONObject;
import com.web.annotation.UrlLimit;
import com.web.annotation.Userid;
import com.web.service.FileService;
import com.web.util.ResultUtil;
import com.web.vo.file.AcceptVo;
import com.web.vo.file.AnswerVo;
import com.web.vo.file.CancelVo;
import com.web.vo.file.CandidateVo;
import com.web.vo.file.InviteVo;
import com.web.vo.file.OfferVo;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    @Resource
    private FileService fileService;

    /**
     * 发送offer
     */
    @UrlLimit
    @PostMapping("/offer")
    public JSONObject offer(@Userid String userId, @RequestBody OfferVo offerVo) {
        boolean result = fileService.offer(userId, offerVo);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 发送answer
     */
    @UrlLimit
    @PostMapping("/answer")
    public JSONObject answer(@Userid String userId, @RequestBody AnswerVo answerVo) {
        boolean result = fileService.answer(userId, answerVo);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 发送candidate
     */
    @UrlLimit
    @PostMapping("/candidate")
    public JSONObject candidate(@Userid String userId, @RequestBody CandidateVo candidateVo) {
        boolean result = fileService.candidate(userId, candidateVo);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 取消
     */
    @UrlLimit
    @PostMapping("/cancel")
    public JSONObject cancel(@Userid String userId, @RequestBody CancelVo cancelVo) {
        boolean result = fileService.cancel(userId, cancelVo);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 邀请
     */
    @UrlLimit
    @PostMapping("/invite")
    public JSONObject invite(@Userid String userId, @RequestBody InviteVo inviteVo) {
        boolean result = fileService.invite(userId, inviteVo);
        return ResultUtil.ResultByFlag(result);
    }

    /**
     * 同意
     */
    @UrlLimit
    @PostMapping("/accept")
    public JSONObject accept(@Userid String userId, @RequestBody AcceptVo acceptVo) {
        boolean result = fileService.accept(userId, acceptVo);
        return ResultUtil.ResultByFlag(result);
    }
}
