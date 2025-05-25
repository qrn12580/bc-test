package com.bjut.blockchain.web.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 门限认证实体类
 * 用于管理门限签名和多方认证配置
 */
@Entity
@Table(name = "threshold_auth_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdAuthEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_group_id", nullable = false, unique = true, length = 100)
    private String authGroupId;

    @Column(name = "group_name", nullable = false, length = 200)
    private String groupName;

    @Column(name = "threshold", nullable = false)
    private Integer threshold; // 门限值，需要多少个参与者

    @Column(name = "total_participants", nullable = false)
    private Integer totalParticipants; // 总参与者数量

    @Column(name = "participants", nullable = false, columnDefinition = "TEXT")
    private String participants; // JSON格式的参与者列表

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey; // 群组公钥

    @Column(name = "verification_key", columnDefinition = "TEXT")
    private String verificationKey; // 验证密钥

    @Column(name = "auth_type", nullable = false, length = 50)
    private String authType; // SIGNATURE, KEY_RECOVERY, CONSENSUS

    @Column(name = "created_by", nullable = false, length = 200)
    private String createdBy; // 创建者DID

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
     * 检查门限配置是否有效
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return "ACTIVE".equals(status) &&
               !now.isBefore(validFrom) &&
               (validUntil == null || !now.isAfter(validUntil)) &&
               threshold > 0 &&
               threshold <= totalParticipants;
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return validUntil != null && LocalDateTime.now().isAfter(validUntil);
    }

    /**
     * 激活认证组
     */
    public void activate() {
        this.status = "ACTIVE";
    }

    /**
     * 停用认证组
     */
    public void deactivate() {
        this.status = "INACTIVE";
    }

    /**
     * 设置过期
     */
    public void expire() {
        this.status = "EXPIRED";
    }

    /**
     * 检查门限值是否合理
     */
    public boolean isThresholdValid() {
        return threshold > 0 && 
               threshold <= totalParticipants && 
               threshold <= totalParticipants * 0.75; // 防止门限过高
    }
} 