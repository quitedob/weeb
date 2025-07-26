package com.web.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.*; // Using jakarta
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType; // For TableId type if needed

import java.io.Serializable;
import java.util.Date;

/**
 * 群组实体类
 * 简化注释：群组实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "`group`") // Using backticks because 'group' can be a reserved keyword in SQL
@TableName(value = "`group`") // For MyBatis Plus, also using backticks
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @TableId(type = IdType.AUTO) // MyBatis Plus: Use database auto-increment
    @GeneratedValue(strategy = GenerationType.IDENTITY) // JPA: Use database auto-increment
    private Long id; // 群组ID

    @Column(nullable = false, length = 100)
    private String groupName; // 群名称

    @Column(nullable = false)
    private Long ownerId; // 群主的用户ID

    @Column(length = 255)
    private String groupAvatarUrl; // 群头像URL (optional)

    @Column(nullable = false)
    private Date createTime; // 创建时间

    // Consider adding an updateTime field as well for good practice
    // private Date updateTime;

    // Default constructor
    public Group() {
        this.createTime = new Date();
    }

    // Constructor for creation (without id as it's auto-generated)
    public Group(String groupName, Long ownerId, String groupAvatarUrl) {
        this.groupName = groupName;
        this.ownerId = ownerId;
        this.groupAvatarUrl = groupAvatarUrl;
        this.createTime = new Date();
    }
}
