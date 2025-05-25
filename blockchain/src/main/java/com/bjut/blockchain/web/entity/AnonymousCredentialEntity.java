package com.bjut.blockchain.web.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 匿名凭证实体类
 * 用于管理区块链网络中的匿名认证凭证
 */
@Entity
@Table(name = "anonymous_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnonymousCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "credential_id", nullable = false, unique = true, length = 100)
    private String credentialId;

    @Column(name = "issuer_did", nullable = false, length = 200)
    private String issuerDid;

    @Column(name = "subject_pseudonym", nullable = false, length = 200)
    private String subjectPseudonym; // 用户假名，不暴露真实身份

    @Column(name = "credential_schema", nullable = false, length = 100)
    private String credentialSchema; // 凭证模式类型

    @Column(name = "commitment_value", nullable = false, columnDefinition = "TEXT")
    private String commitmentValue; // 承诺值，用于零知识证明

    @Column(name = "proof_data", nullable = false, columnDefinition = "TEXT")
    private String proofData; // 零知识证明数据

    @Column(name = "blinding_factor", columnDefinition = "TEXT")
    private String blindingFactor; // 盲化因子

    @Column(name = "revocation_handle", length = 200)
    private String revocationHandle; // 撤销句柄

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    @Column(name = "max_usage", nullable = false)
    private Long maxUsage = Long.MAX_VALUE; // 最大使用次数

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (validFrom == null) {
            validFrom = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查凭证是否有效
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !isRevoked && 
               !now.isBefore(validFrom) && 
               !now.isAfter(validUntil) &&
               usageCount < maxUsage;
    }

    /**
     * 检查凭证是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(validUntil);
    }

    /**
     * 撤销凭证
     */
    public void revoke(String reason) {
        this.isRevoked = true;
        this.revocationReason = reason;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        this.usageCount++;
    }

    /**
     * 检查是否可以使用
     */
    public boolean canUse() {
        return isValid() && usageCount < maxUsage;
    }

    /**
     * 获取剩余使用次数
     */
    public long getRemainingUsage() {
        if (maxUsage == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(0, maxUsage - usageCount);
    }
} 