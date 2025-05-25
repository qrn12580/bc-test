package com.bjut.blockchain.web.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 节点信任评估实体类
 * 用于管理区块链网络中节点的信任度评估
 */
@Entity
@Table(name = "node_trust_evaluation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NodeTrustEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_id", nullable = false, unique = true, length = 100)
    private String nodeId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "trust_score", precision = 10, scale = 4)
    private BigDecimal trustScore = BigDecimal.valueOf(0.5000); // 初始信任度50%

    @Column(name = "reputation_score", precision = 10, scale = 4)
    private BigDecimal reputationScore = BigDecimal.valueOf(0.5000); // 初始声誉50%

    @Column(name = "blocks_mined", nullable = false)
    private Long blocksMined = 0L;

    @Column(name = "valid_transactions", nullable = false)
    private Long validTransactions = 0L;

    @Column(name = "invalid_transactions", nullable = false)
    private Long invalidTransactions = 0L;

    @Column(name = "uptime_hours", nullable = false)
    private Long uptimeHours = 0L;

    @Column(name = "last_active", nullable = false)
    private LocalDateTime lastActive;

    @Column(name = "certificates_issued", nullable = false)
    private Long certificatesIssued = 0L;

    @Column(name = "certificate_revocations", nullable = false)
    private Long certificateRevocations = 0L;

    @Column(name = "network_latency_avg", precision = 10, scale = 2)
    private BigDecimal networkLatencyAvg = BigDecimal.ZERO;

    @Column(name = "consensus_participation_rate", precision = 5, scale = 4)
    private BigDecimal consensusParticipationRate = BigDecimal.valueOf(0.0000);

    @Column(name = "is_blacklisted", nullable = false)
    private Boolean isBlacklisted = false;

    @Column(name = "blacklist_reason", length = 500)
    private String blacklistReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lastActive == null) {
            lastActive = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 计算节点整体信任度
     * 基于多个因素的加权平均
     */
    public BigDecimal calculateOverallTrust() {
        if (isBlacklisted) {
            return BigDecimal.ZERO;
        }

        // 计算交易准确率
        BigDecimal transactionAccuracy = BigDecimal.ZERO;
        long totalTransactions = validTransactions + invalidTransactions;
        if (totalTransactions > 0) {
            transactionAccuracy = BigDecimal.valueOf(validTransactions)
                    .divide(BigDecimal.valueOf(totalTransactions), 4, BigDecimal.ROUND_HALF_UP);
        }

        // 计算证书管理信誉
        BigDecimal certificateReliability = BigDecimal.ONE;
        long totalCertificateOperations = certificatesIssued + certificateRevocations;
        if (totalCertificateOperations > 0) {
            certificateReliability = BigDecimal.valueOf(certificatesIssued)
                    .divide(BigDecimal.valueOf(totalCertificateOperations), 4, BigDecimal.ROUND_HALF_UP);
        }

        // 在线时长权重
        BigDecimal uptimeWeight = BigDecimal.valueOf(Math.min(uptimeHours / 24.0, 30.0) / 30.0);

        // 共识参与度权重
        BigDecimal consensusWeight = consensusParticipationRate;

        // 加权计算最终信任度
        BigDecimal overallTrust = trustScore
                .multiply(BigDecimal.valueOf(0.3))
                .add(reputationScore.multiply(BigDecimal.valueOf(0.2)))
                .add(transactionAccuracy.multiply(BigDecimal.valueOf(0.2)))
                .add(certificateReliability.multiply(BigDecimal.valueOf(0.15)))
                .add(uptimeWeight.multiply(BigDecimal.valueOf(0.1)))
                .add(consensusWeight.multiply(BigDecimal.valueOf(0.05)));

        return overallTrust.min(BigDecimal.ONE).max(BigDecimal.ZERO);
    }

    /**
     * 检查节点是否可信
     */
    public boolean isTrusted() {
        return !isBlacklisted && calculateOverallTrust().compareTo(BigDecimal.valueOf(0.6)) >= 0;
    }

    /**
     * 更新节点活跃时间
     */
    public void updateLastActive() {
        this.lastActive = LocalDateTime.now();
    }

    /**
     * 增加有效交易数量
     */
    public void incrementValidTransactions() {
        this.validTransactions++;
    }

    /**
     * 增加无效交易数量
     */
    public void incrementInvalidTransactions() {
        this.invalidTransactions++;
    }

    /**
     * 增加挖矿区块数量
     */
    public void incrementBlocksMined() {
        this.blocksMined++;
    }

    /**
     * 加入黑名单
     */
    public void blacklist(String reason) {
        this.isBlacklisted = true;
        this.blacklistReason = reason;
        this.trustScore = BigDecimal.ZERO;
        this.reputationScore = BigDecimal.ZERO;
    }

    /**
     * 移出黑名单
     */
    public void removeFromBlacklist() {
        this.isBlacklisted = false;
        this.blacklistReason = null;
        this.trustScore = BigDecimal.valueOf(0.3); // 恢复较低的初始信任度
        this.reputationScore = BigDecimal.valueOf(0.3);
    }
} 