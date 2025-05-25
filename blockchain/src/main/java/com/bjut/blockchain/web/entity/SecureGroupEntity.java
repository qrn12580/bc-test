package com.bjut.blockchain.web.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 安全群组通信实体类
 * 用于管理加密的群组通信和消息传递
 */
@Entity
@Table(name = "secure_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SecureGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, unique = true, length = 100)
    private String groupId;

    @Column(name = "group_name", nullable = false, length = 200)
    private String groupName;

    @Column(name = "group_type", nullable = false, length = 50)
    private String groupType; // CONSENSUS, VOTING, TRANSACTION, GENERAL

    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm = "AES256"; // 加密算法

    @Column(name = "group_key", nullable = false, columnDefinition = "TEXT")
    private String groupKey; // 群组加密密钥

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion = 1; // 密钥版本

    @Column(name = "members", nullable = false, columnDefinition = "TEXT")
    private String members; // JSON格式的成员列表

    @Column(name = "admins", columnDefinition = "TEXT")
    private String admins; // JSON格式的管理员列表

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers = 100; // 最大成员数

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy; // 创建者DID

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ARCHIVED

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "message_retention_days", nullable = false)
    private Integer messageRetentionDays = 30; // 消息保留天数

    @Column(name = "require_approval", nullable = false)
    private Boolean requireApproval = false; // 是否需要批准加入

    @Column(name = "allow_anonymous", nullable = false)
    private Boolean allowAnonymous = false; // 是否允许匿名消息

    @Column(name = "enable_forward_secrecy", nullable = false)
    private Boolean enableForwardSecrecy = true; // 是否启用前向保密

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查群组是否活跃
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * 激活群组
     */
    public void activate() {
        this.status = "ACTIVE";
    }

    /**
     * 停用群组
     */
    public void deactivate() {
        this.status = "INACTIVE";
    }

    /**
     * 归档群组
     */
    public void archive() {
        this.status = "ARCHIVED";
    }

    /**
     * 更新密钥版本
     */
    public void rotateKey(String newKey) {
        this.groupKey = newKey;
        this.keyVersion++;
    }

    /**
     * 检查是否需要密钥轮换
     */
    public boolean needsKeyRotation() {
        // 如果群组创建超过30天或密钥版本较旧，需要轮换
        return createdAt.plusDays(30).isBefore(LocalDateTime.now()) || 
               (lastActivity != null && lastActivity.plusDays(7).isBefore(LocalDateTime.now()));
    }
} 