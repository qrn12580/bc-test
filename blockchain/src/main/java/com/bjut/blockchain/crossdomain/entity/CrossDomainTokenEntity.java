package com.bjut.blockchain.crossdomain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 跨域认证令牌实体类
 * 用于存储和管理跨域认证过程中的令牌信息
 */
@Entity
@Table(name = "cross_domain_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrossDomainTokenEntity {

    @Id
    @Column(name = "token_id", length = 128)
    private String tokenId;

    @Column(name = "user_id", nullable = false, length = 200)
    private String userId;

    @Column(name = "source_domain_id", nullable = false, length = 100)
    private String sourceDomainId;

    @Column(name = "target_domain_id", nullable = false, length = 100)
    private String targetDomainId;

    @Column(name = "token_type", length = 50)
    private String tokenType; // FEDERATION, DELEGATION, ASSERTION

    @Column(name = "token_data", columnDefinition = "TEXT")
    private String tokenData;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Column(name = "nonce", length = 128)
    private String nonce;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "scope", length = 500)
    private String scope; // 权限范围

    @Column(name = "additional_claims", columnDefinition = "TEXT")
    private String additionalClaims; // JSON格式的额外声明

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    // 检查令牌是否有效
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !isUsed && 
               (issuedAt == null || !now.isBefore(issuedAt)) &&
               (expiresAt == null || !now.isAfter(expiresAt));
    }

    // 标记令牌为已使用
    public void markAsUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }
} 