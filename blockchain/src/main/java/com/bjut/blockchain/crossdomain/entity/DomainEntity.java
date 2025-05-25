package com.bjut.blockchain.crossdomain.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 认证域实体类
 * 用于表示区块链网络中的不同认证域
 */
@Entity
@Table(name = "domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainEntity {

    @Id
    @Column(name = "domain_id", length = 100)
    private String domainId;

    @Column(name = "domain_name", nullable = false, length = 200)
    private String domainName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "endpoint_url", nullable = false, length = 500)
    private String endpointUrl;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "certificate", columnDefinition = "TEXT")
    private String certificate;

    @Column(name = "domain_type", length = 50)
    private String domainType; // BLOCKCHAIN, TRADITIONAL, HYBRID

    @Column(name = "network_id", length = 100)
    private String networkId;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 