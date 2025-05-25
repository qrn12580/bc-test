package com.bjut.blockchain.crossdomain.repository;

import com.bjut.blockchain.crossdomain.entity.CrossDomainTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 跨域认证令牌数据访问接口
 */
@Repository
public interface CrossDomainTokenRepository extends JpaRepository<CrossDomainTokenEntity, String> {

    /**
     * 查找用户在特定域间的有效令牌
     */
    @Query("SELECT cdt FROM CrossDomainTokenEntity cdt WHERE " +
           "cdt.userId = :userId AND cdt.sourceDomainId = :sourceDomainId " +
           "AND cdt.targetDomainId = :targetDomainId " +
           "AND cdt.isUsed = false " +
           "AND cdt.expiresAt > :now")
    List<CrossDomainTokenEntity> findValidTokens(@Param("userId") String userId,
                                                  @Param("sourceDomainId") String sourceDomainId,
                                                  @Param("targetDomainId") String targetDomainId,
                                                  @Param("now") LocalDateTime now);

    /**
     * 根据令牌ID查找有效令牌
     */
    @Query("SELECT cdt FROM CrossDomainTokenEntity cdt WHERE " +
           "cdt.tokenId = :tokenId AND cdt.isUsed = false " +
           "AND cdt.expiresAt > :now")
    Optional<CrossDomainTokenEntity> findValidTokenById(@Param("tokenId") String tokenId,
                                                         @Param("now") LocalDateTime now);

    /**
     * 查找用户的所有令牌
     */
    List<CrossDomainTokenEntity> findByUserIdOrderByIssuedAtDesc(String userId);

    /**
     * 查找特定类型的令牌
     */
    List<CrossDomainTokenEntity> findByTokenTypeAndIsUsedFalse(String tokenType);

    /**
     * 查找源域的所有令牌
     */
    List<CrossDomainTokenEntity> findBySourceDomainIdOrderByIssuedAtDesc(String sourceDomainId);

    /**
     * 查找目标域的所有令牌
     */
    List<CrossDomainTokenEntity> findByTargetDomainIdOrderByIssuedAtDesc(String targetDomainId);

    /**
     * 根据随机数查找令牌（防重放攻击）
     */
    Optional<CrossDomainTokenEntity> findByNonce(String nonce);

    /**
     * 删除过期的令牌
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM CrossDomainTokenEntity cdt WHERE cdt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * 标记令牌为已使用
     */
    @Modifying
    @Transactional
    @Query("UPDATE CrossDomainTokenEntity cdt SET cdt.isUsed = true, cdt.usedAt = :usedAt " +
           "WHERE cdt.tokenId = :tokenId")
    int markTokenAsUsed(@Param("tokenId") String tokenId, @Param("usedAt") LocalDateTime usedAt);

    /**
     * 查找即将过期的令牌（用于提醒刷新）
     */
    @Query("SELECT cdt FROM CrossDomainTokenEntity cdt WHERE " +
           "cdt.isUsed = false AND cdt.expiresAt BETWEEN :now AND :thresholdTime")
    List<CrossDomainTokenEntity> findTokensAboutToExpire(@Param("now") LocalDateTime now,
                                                          @Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * 检查令牌是否已存在（防重复）
     */
    boolean existsByTokenId(String tokenId);

    /**
     * 检查随机数是否已存在（防重放）
     */
    boolean existsByNonce(String nonce);
} 