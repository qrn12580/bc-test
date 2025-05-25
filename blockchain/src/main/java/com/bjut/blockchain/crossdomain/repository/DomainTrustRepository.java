package com.bjut.blockchain.crossdomain.repository;

import com.bjut.blockchain.crossdomain.entity.DomainTrustEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 域间信任关系数据访问接口
 */
@Repository
public interface DomainTrustRepository extends JpaRepository<DomainTrustEntity, Long> {

    /**
     * 查找两个域之间的信任关系
     */
    @Query("SELECT dt FROM DomainTrustEntity dt WHERE " +
           "dt.sourceDomainId = :sourceDomainId AND dt.targetDomainId = :targetDomainId " +
           "AND dt.isActive = true")
    Optional<DomainTrustEntity> findTrustRelation(@Param("sourceDomainId") String sourceDomainId,
                                                  @Param("targetDomainId") String targetDomainId);

    /**
     * 查找双向信任关系
     */
    @Query("SELECT dt FROM DomainTrustEntity dt WHERE " +
           "((dt.sourceDomainId = :domain1 AND dt.targetDomainId = :domain2) OR " +
           " (dt.sourceDomainId = :domain2 AND dt.targetDomainId = :domain1)) " +
           "AND dt.isActive = true AND dt.trustType = 'BIDIRECTIONAL'")
    Optional<DomainTrustEntity> findBidirectionalTrust(@Param("domain1") String domain1,
                                                       @Param("domain2") String domain2);

    /**
     * 查找源域的所有信任关系
     */
    List<DomainTrustEntity> findBySourceDomainIdAndIsActiveTrue(String sourceDomainId);

    /**
     * 查找目标域的所有信任关系
     */
    List<DomainTrustEntity> findByTargetDomainIdAndIsActiveTrue(String targetDomainId);

    /**
     * 查找某个域的所有信任关系（包括作为源域和目标域）
     */
    @Query("SELECT dt FROM DomainTrustEntity dt WHERE " +
           "(dt.sourceDomainId = :domainId OR dt.targetDomainId = :domainId) " +
           "AND dt.isActive = true")
    List<DomainTrustEntity> findAllTrustRelationsByDomain(@Param("domainId") String domainId);

    /**
     * 查找有效的信任关系
     */
    @Query("SELECT dt FROM DomainTrustEntity dt WHERE " +
           "dt.sourceDomainId = :sourceDomainId AND dt.targetDomainId = :targetDomainId " +
           "AND dt.isActive = true " +
           "AND (dt.validFrom IS NULL OR dt.validFrom <= :now) " +
           "AND (dt.validUntil IS NULL OR dt.validUntil > :now)")
    Optional<DomainTrustEntity> findValidTrustRelation(@Param("sourceDomainId") String sourceDomainId,
                                                       @Param("targetDomainId") String targetDomainId,
                                                       @Param("now") LocalDateTime now);

    /**
     * 根据信任级别查找信任关系
     */
    @Query("SELECT dt FROM DomainTrustEntity dt WHERE " +
           "dt.trustLevel >= :minTrustLevel AND dt.isActive = true")
    List<DomainTrustEntity> findByMinTrustLevel(@Param("minTrustLevel") Integer minTrustLevel);

    /**
     * 检查两个域之间是否存在任何信任关系
     */
    @Query("SELECT COUNT(dt) > 0 FROM DomainTrustEntity dt WHERE " +
           "((dt.sourceDomainId = :domain1 AND dt.targetDomainId = :domain2) OR " +
           " (dt.sourceDomainId = :domain2 AND dt.targetDomainId = :domain1 AND dt.trustType = 'BIDIRECTIONAL')) " +
           "AND dt.isActive = true")
    boolean existsTrustRelation(@Param("domain1") String domain1, @Param("domain2") String domain2);
} 