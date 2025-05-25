package com.bjut.blockchain.crossdomain.repository;

import com.bjut.blockchain.crossdomain.entity.DomainEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 域管理数据访问接口
 */
@Repository
public interface DomainRepository extends JpaRepository<DomainEntity, String> {

    /**
     * 根据域名查找域
     */
    Optional<DomainEntity> findByDomainName(String domainName);

    /**
     * 查找所有活跃的域
     */
    List<DomainEntity> findByIsActiveTrue();

    /**
     * 根据域类型查找域
     */
    List<DomainEntity> findByDomainTypeAndIsActiveTrue(String domainType);

    /**
     * 根据网络ID查找域
     */
    List<DomainEntity> findByNetworkIdAndIsActiveTrue(String networkId);

    /**
     * 根据端点URL查找域
     */
    Optional<DomainEntity> findByEndpointUrl(String endpointUrl);

    /**
     * 检查域名是否已存在
     */
    boolean existsByDomainName(String domainName);

    /**
     * 检查端点URL是否已存在
     */
    boolean existsByEndpointUrl(String endpointUrl);

    /**
     * 根据公钥查找域
     */
    @Query("SELECT d FROM DomainEntity d WHERE d.publicKey = :publicKey AND d.isActive = true")
    Optional<DomainEntity> findByPublicKey(@Param("publicKey") String publicKey);
} 