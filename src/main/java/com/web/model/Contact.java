package com.web.model;

// import com.web.constant.ContactStatus; // This will be needed by services, not directly in entity if status is Integer
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*; // Using jakarta
import com.baomidou.mybatisplus.annotation.TableId; // For potential MyBatis Plus usage
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;


import java.io.Serializable;
import java.util.Date;

/**
 * 联系人（好友）关系实体
 * 简化注释：联系人实体
 */
@Data
@EqualsAndHashCode(callSuper = false) // Consistent with other new entities
@Entity
@Table(name = "contact")
@TableName(value = "contact") // For MyBatis Plus
public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.AUTO) // MyBatis Plus
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA
    private Long id; // 关系ID

    @Column(nullable = false)
    private Long userId; // 用户自身のID (Changed to Long)

    @Column(nullable = false)
    private Long friendId; // 好友的ID (Changed to Long)

    /**
     * 关系状态
     * Storing as Integer, corresponding to ContactStatus.code
     * @see com.web.constant.ContactStatus
     * 简化注释：关系状态
     */
    @Column(nullable = false)
    private Integer status;

    /**
     * 备注/附言
     * 简化注释：申请附言
     */
    @Column(length = 255) // Max length for remarks
    private String remarks;

    @Column(nullable = false)
    private Date createTime; // 创建时间

    @Column(nullable = false)
    private Date updateTime; // 更新时间

    // Default constructor
    public Contact() {
        Date now = new Date();
        this.createTime = now;
        this.updateTime = now;
    }

    // Constructor for new application
    public Contact(Long userId, Long friendId, String remarks) {
        this(); // Call default constructor to set times
        this.userId = userId;
        this.friendId = friendId;
        this.remarks = remarks;
        this.status = com.web.constant.ContactStatus.PENDING.getCode(); // Default to PENDING
    }
}
