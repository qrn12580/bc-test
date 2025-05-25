package com.bjut.blockchain.web.repository;

import com.bjut.blockchain.web.entity.NodeTrustEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 节点信任评估数据访问接口
 */
@Repository
public interface NodeTrustRepository extends JpaRepository<NodeTrustEntity, Long> {

    /**
     * 根据节点ID查找节点信任信息
     */
    Optional<NodeTrustEntity> findByNodeId(String nodeId);

    /**
     * 根据公钥查找节点信任信息
     */
    Optional<NodeTrustEntity> findByPublicKey(String publicKey);

    /**
     * 查找所有可信节点
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.isBlacklisted = false AND " +
           "nt.trustScore >= :minTrustScore")
    List<NodeTrustEntity> findTrustedNodes(@Param("minTrustScore") BigDecimal minTrustScore);

    /**
     * 查找活跃节点（最近活跃时间在指定时间之后）
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.lastActive >= :since AND nt.isBlacklisted = false")
    List<NodeTrustEntity> findActiveNodes(@Param("since") LocalDateTime since);

    /**
     * 查找黑名单节点
     */
    List<NodeTrustEntity> findByIsBlacklistedTrue();

    /**
     * 根据信任度排序查找前N个可信节点
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.isBlacklisted = false " +
           "ORDER BY nt.trustScore DESC, nt.reputationScore DESC")
    List<NodeTrustEntity> findTopTrustedNodes();

    /**
     * 查找指定时间范围内的活跃节点
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.lastActive BETWEEN :startTime AND :endTime " +
           "AND nt.isBlacklisted = false")
    List<NodeTrustEntity> findActiveNodesBetween(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计网络中可信节点数量
     */
    @Query("SELECT COUNT(nt) FROM NodeTrustEntity nt WHERE " +
           "nt.isBlacklisted = false AND nt.trustScore >= :minTrustScore")
    long countTrustedNodes(@Param("minTrustScore") BigDecimal minTrustScore);

    /**
     * 计算网络平均信任度
     */
    @Query("SELECT AVG(nt.trustScore) FROM NodeTrustEntity nt WHERE " +
           "nt.isBlacklisted = false")
    BigDecimal getAverageTrustScore();

    /**
     * 查找需要更新信任度的节点（长时间未活跃）
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.lastActive < :threshold AND nt.isBlacklisted = false")
    List<NodeTrustEntity> findInactiveNodes(@Param("threshold") LocalDateTime threshold);

    /**
     * 查找高风险节点（信任度低但仍在活跃）
     */
    @Query("SELECT nt FROM NodeTrustEntity nt WHERE " +
           "nt.trustScore < :riskThreshold AND " +
           "nt.lastActive >= :recentTime AND " +
           "nt.isBlacklisted = false")
    List<NodeTrustEntity> findRiskNodes(@Param("riskThreshold") BigDecimal riskThreshold,
                                        @Param("recentTime") LocalDateTime recentTime);

    /**
     * 检查节点是否存在
     */
    boolean existsByNodeId(String nodeId);

    /**
     * 检查公钥是否已注册
     */
    boolean existsByPublicKey(String publicKey);
} 