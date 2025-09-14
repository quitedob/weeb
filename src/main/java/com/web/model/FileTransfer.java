package com.web.model;

import java.util.Date;

public class FileTransfer {
    private Long id;
    private Long initiatorId;
    private Long targetId;
    private String offerSdp;
    private String answerSdp;
    private String candidate;
    private Integer status;     // 0=INVITE,1=OFFERED,2=ANSWERED等
    private Date createdAt;
    private Date updatedAt;

    // getter/setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInitiatorId() { return initiatorId; }
    public void setInitiatorId(Long initiatorId) { this.initiatorId = initiatorId; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getOfferSdp() { return offerSdp; }
    public void setOfferSdp(String offerSdp) { this.offerSdp = offerSdp; }

    public String getAnswerSdp() { return answerSdp; }
    public void setAnswerSdp(String answerSdp) { this.answerSdp = answerSdp; }

    public String getCandidate() { return candidate; }
    public void setCandidate(String candidate) { this.candidate = candidate; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
