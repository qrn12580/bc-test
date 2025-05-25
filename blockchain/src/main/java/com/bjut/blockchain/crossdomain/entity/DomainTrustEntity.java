package com.bjut.blockchain.crossdomain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 域间信任关系实体类
 * 用于管理不同认证域之间的信任关系
 */
@Entity
@Table(name = "domain_trust_relationships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainTrustEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_domain_id", nullable = false, length = 100)
    private String sourceDomainId;

    @Column(name = "target_domain_id", nullable = false, length = 100)
    private String targetDomainId;

    @Column(name = "trust_level", nullable = false)
    private Integer trustLevel; // 1-10, 10为最高信任

    @Column(name = "trust_type", length = 50)
    private String trustType; // BIDIRECTIONAL, UNIDIRECTIONAL

    @Column(name = "shared_secret", columnDefinition = "TEXT")
    private String sharedSecret;

    @Column(name = "trust_certificate", columnDefinition = "TEXT")
    private String trustCertificate;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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

    // 检查信任关系是否有效
    public boolean isValidTrust() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               (validFrom == null || !now.isBefore(validFrom)) &&
               (validUntil == null || !now.isAfter(validUntil));
    }
} 