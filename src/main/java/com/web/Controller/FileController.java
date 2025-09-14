package com.web.Controller;

import com.web.annotation.UrlLimit;
import com.web.annotation.Userid;
import com.web.common.ApiResponse;
import com.web.service.FileService;
import com.web.vo.file.AcceptVo;
import com.web.vo.file.AnswerVo;
import com.web.vo.file.CancelVo;
import com.web.vo.file.CandidateVo;
import com.web.vo.file.InviteVo;
import com.web.vo.file.OfferVo;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<String>> offer(@Userid String userId, @RequestBody OfferVo offerVo) {
        boolean result = fileService.offer(userId, offerVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }

    /**
     * 发送answer
     */
    @UrlLimit
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<String>> answer(@Userid String userId, @RequestBody AnswerVo answerVo) {
        boolean result = fileService.answer(userId, answerVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }

    /**
     * 发送candidate
     */
    @UrlLimit
    @PostMapping("/candidate")
    public ResponseEntity<ApiResponse<String>> candidate(@Userid String userId, @RequestBody CandidateVo candidateVo) {
        boolean result = fileService.candidate(userId, candidateVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }

    /**
     * 取消
     */
    @UrlLimit
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<String>> cancel(@Userid String userId, @RequestBody CancelVo cancelVo) {
        boolean result = fileService.cancel(userId, cancelVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }

    /**
     * 邀请
     */
    @UrlLimit
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<String>> invite(@Userid String userId, @RequestBody InviteVo inviteVo) {
        boolean result = fileService.invite(userId, inviteVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }

    /**
     * 同意
     */
    @UrlLimit
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<String>> accept(@Userid String userId, @RequestBody AcceptVo acceptVo) {
        boolean result = fileService.accept(userId, acceptVo);
        if (result) {
            return ResponseEntity.ok(ApiResponse.success("操作成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(1, "操作失败"));
        }
    }
}
