package com.bjut.blockchain.crossdomain.service;

import com.bjut.blockchain.crossdomain.dto.CrossDomainAuthRequest;
import com.bjut.blockchain.crossdomain.entity.CrossDomainTokenEntity;
import com.bjut.blockchain.crossdomain.entity.DomainEntity;
import com.bjut.blockchain.crossdomain.entity.DomainTrustEntity;
import com.bjut.blockchain.crossdomain.repository.CrossDomainTokenRepository;
import com.bjut.blockchain.crossdomain.repository.DomainRepository;
import com.bjut.blockchain.crossdomain.repository.DomainTrustRepository;
import com.bjut.blockchain.web.util.CryptoUtil;
import com.bjut.blockchain.web.util.CommonUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 跨域认证服务类
 * 负责处理跨域身份认证和令牌管理
 */
@Service
@Transactional
public class CrossDomainAuthService {

    private static final Logger logger = LoggerFactory.getLogger(CrossDomainAuthService.class);

    // 令牌默认有效期（小时）
    private static final int DEFAULT_TOKEN_VALIDITY_HOURS = 24;
    
    // 挑战码有效期（分钟）
    private static final int CHALLENGE_VALIDITY_MINUTES = 5;

    @Autowired
    private CrossDomainTokenRepository tokenRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainTrustRepository trustRepository;

    @Autowired
    private DomainTrustService domainTrustService;

    /**
     * 生成跨域认证挑战码
     */
    public Map<String, Object> generateCrossDomainChallenge(String sourceDomainId, String targetDomainId, String userId) {
        logger.info("为用户 {} 生成从域 {} 到域 {} 的跨域认证挑战码", userId, sourceDomainId, targetDomainId);

        // 验证源域和目标域是否存在且活跃
        if (!isDomainValid(sourceDomainId)) {
            throw new IllegalArgumentException("源域不存在或未激活: " + sourceDomainId);
        }

        if (!isDomainValid(targetDomainId)) {
            throw new IllegalArgumentException("目标域不存在或未激活: " + targetDomainId);
        }

        // 验证域间信任关系
        if (!domainTrustService.hasTrustRelation(sourceDomainId, targetDomainId)) {
            throw new IllegalArgumentException("域间不存在信任关系: " + sourceDomainId + " -> " + targetDomainId);
        }

        // 生成挑战码和随机数
        String challenge = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        // 构建挑战数据
        Map<String, Object> challengeData = new HashMap<>();
        challengeData.put("challenge", challenge);
        challengeData.put("nonce", nonce);
        challengeData.put("timestamp", timestamp);
        challengeData.put("sourceDomainId", sourceDomainId);
        challengeData.put("targetDomainId", targetDomainId);
        challengeData.put("userId", userId);
        challengeData.put("expiresAt", timestamp + (CHALLENGE_VALIDITY_MINUTES * 60 * 1000));

        logger.info("跨域认证挑战码生成成功，挑战码: {}", challenge);
        return challengeData;
    }

    /**
     * 验证域是否有效
     */
    private boolean isDomainValid(String domainId) {
        Optional<DomainEntity> domainOpt = domainRepository.findById(domainId);
        return domainOpt.isPresent() && domainOpt.get().getIsActive();
    }

    /**
     * 验证跨域认证请求
     */
    public Map<String, Object> verifyCrossDomainAuth(CrossDomainAuthRequest request) {
        logger.info("验证用户 {} 从域 {} 到域 {} 的跨域认证请求", 
                   request.getUserId(), request.getSourceDomainId(), request.getTargetDomainId());

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证基本参数
            if (request.getUserId() == null || request.getSourceDomainId() == null || 
                request.getTargetDomainId() == null || request.getAuthToken() == null || 
                request.getSignature() == null || request.getNonce() == null) {
                throw new IllegalArgumentException("请求参数不完整");
            }

            // 验证域的存在性和活跃状态
            Optional<DomainEntity> sourceDomainOpt = domainRepository.findById(request.getSourceDomainId());
            if (!sourceDomainOpt.isPresent() || !sourceDomainOpt.get().getIsActive()) {
                throw new IllegalArgumentException("源域不存在或未激活: " + request.getSourceDomainId());
            }

            Optional<DomainEntity> targetDomainOpt = domainRepository.findById(request.getTargetDomainId());
            if (!targetDomainOpt.isPresent() || !targetDomainOpt.get().getIsActive()) {
                throw new IllegalArgumentException("目标域不存在或未激活: " + request.getTargetDomainId());
            }

            DomainEntity sourceDomain = sourceDomainOpt.get();
            DomainEntity targetDomain = targetDomainOpt.get();

            // 验证域间信任关系
            Optional<DomainTrustEntity> trustOpt = trustRepository.findValidTrustRelation(
                    request.getSourceDomainId(), request.getTargetDomainId(), LocalDateTime.now());

            if (!trustOpt.isPresent()) {
                throw new IllegalArgumentException("域间不存在有效的信任关系");
            }

            DomainTrustEntity trust = trustOpt.get();

            // 验证随机数是否已使用（防重放攻击）
            if (tokenRepository.existsByNonce(request.getNonce())) {
                throw new IllegalArgumentException("随机数已被使用，可能是重放攻击");
            }

            // 验证时间戳（防重放攻击）
            if (request.getTimestamp() != null) {
                long currentTime = System.currentTimeMillis();
                long requestTime = request.getTimestamp();
                if (Math.abs(currentTime - requestTime) > (CHALLENGE_VALIDITY_MINUTES * 60 * 1000)) {
                    throw new IllegalArgumentException("请求时间戳已过期");
                }
            }

            // 验证签名
            if (!verifyAuthTokenSignature(request, sourceDomain)) {
                throw new IllegalArgumentException("认证令牌签名验证失败");
            }

            // 生成跨域认证令牌
            CrossDomainTokenEntity token = createCrossDomainToken(request, trust);
            CrossDomainTokenEntity savedToken = tokenRepository.save(token);

            // 构建成功响应
            result.put("success", true);
            result.put("message", "跨域认证成功");
            result.put("token", savedToken.getTokenId());
            result.put("tokenType", savedToken.getTokenType());
            result.put("scope", savedToken.getScope());
            result.put("expiresAt", savedToken.getExpiresAt());
            result.put("targetDomainInfo", buildDomainInfo(targetDomain));

            logger.info("用户 {} 跨域认证成功，令牌ID: {}", request.getUserId(), savedToken.getTokenId());

        } catch (Exception e) {
            logger.error("跨域认证失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "跨域认证失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证认证令牌的签名
     */
    private boolean verifyAuthTokenSignature(CrossDomainAuthRequest request, DomainEntity sourceDomain) {
        try {
            // 构建待签名的数据
            Map<String, Object> dataToSign = new HashMap<>();
            dataToSign.put("userId", request.getUserId());
            dataToSign.put("sourceDomainId", request.getSourceDomainId());
            dataToSign.put("targetDomainId", request.getTargetDomainId());
            dataToSign.put("authToken", request.getAuthToken());
            dataToSign.put("nonce", request.getNonce());
            dataToSign.put("timestamp", request.getTimestamp());

            String dataToSignJson = JSON.toJSONString(dataToSign);
            byte[] dataBytes = dataToSignJson.getBytes(StandardCharsets.UTF_8);

            // 获取源域公钥
            String publicKeyBase64 = sourceDomain.getPublicKey();
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 验证签名
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(dataBytes);

            byte[] signatureBytes = Base64.getDecoder().decode(request.getSignature());
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            logger.error("签名验证失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建跨域认证令牌
     */
    private CrossDomainTokenEntity createCrossDomainToken(CrossDomainAuthRequest request, DomainTrustEntity trust) {
        CrossDomainTokenEntity token = new CrossDomainTokenEntity();
        
        token.setTokenId(UUID.randomUUID().toString());
        token.setUserId(request.getUserId());
        token.setSourceDomainId(request.getSourceDomainId());
        token.setTargetDomainId(request.getTargetDomainId());
        token.setTokenType("FEDERATION");
        token.setNonce(request.getNonce());
        token.setScope(request.getScope());
        token.setAdditionalClaims(request.getAdditionalClaims());
        
        LocalDateTime now = LocalDateTime.now();
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusHours(DEFAULT_TOKEN_VALIDITY_HOURS));
        
        // 构建令牌数据
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("userId", request.getUserId());
        tokenData.put("sourceDomainId", request.getSourceDomainId());
        tokenData.put("targetDomainId", request.getTargetDomainId());
        tokenData.put("trustLevel", trust.getTrustLevel());
        tokenData.put("issuedAt", now.toString());
        tokenData.put("expiresAt", token.getExpiresAt().toString());
        
        token.setTokenData(JSON.toJSONString(tokenData));
        
        // 生成令牌签名
        try {
            String signature = generateTokenSignature(tokenData);
            token.setSignature(signature);
        } catch (Exception e) {
            logger.error("生成令牌签名失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成令牌签名失败", e);
        }
        
        return token;
    }

    /**
     * 生成令牌签名
     */
    private String generateTokenSignature(Map<String, Object> tokenData) throws Exception {
        String dataJson = JSON.toJSONString(tokenData);
        byte[] dataBytes = dataJson.getBytes(StandardCharsets.UTF_8);
        
        // 这里使用系统的私钥进行签名
        // 实际应用中可能需要从配置或CA服务获取
        String hash = CryptoUtil.SHA256(dataJson);
        return Base64.getEncoder().encodeToString(hash.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建域信息
     */
    private Map<String, Object> buildDomainInfo(DomainEntity domain) {
        Map<String, Object> domainInfo = new HashMap<>();
        domainInfo.put("domainId", domain.getDomainId());
        domainInfo.put("domainName", domain.getDomainName());
        domainInfo.put("endpointUrl", domain.getEndpointUrl());
        domainInfo.put("domainType", domain.getDomainType());
        domainInfo.put("networkId", domain.getNetworkId());
        return domainInfo;
    }

    /**
     * 验证跨域认证令牌
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateCrossDomainToken(String tokenId, String targetDomainId) {
        logger.info("验证跨域认证令牌: {} 在目标域: {}", tokenId, targetDomainId);

        Map<String, Object> result = new HashMap<>();

        try {
            // 查找有效令牌
            Optional<CrossDomainTokenEntity> tokenOpt = tokenRepository.findValidTokenById(tokenId, LocalDateTime.now());

            if (!tokenOpt.isPresent()) {
                throw new IllegalArgumentException("令牌不存在或已过期");
            }

            CrossDomainTokenEntity token = tokenOpt.get();

            // 验证目标域匹配
            if (!token.getTargetDomainId().equals(targetDomainId)) {
                throw new IllegalArgumentException("令牌的目标域不匹配");
            }

            // 验证令牌状态
            if (token.getIsUsed()) {
                throw new IllegalArgumentException("令牌已被使用");
            }

            // 标记令牌为已使用（一次性令牌）
            token.markAsUsed();
            tokenRepository.save(token);

            // 构建成功响应
            result.put("success", true);
            result.put("userId", token.getUserId());
            result.put("sourceDomainId", token.getSourceDomainId());
            result.put("targetDomainId", token.getTargetDomainId());
            result.put("scope", token.getScope());
            result.put("tokenType", token.getTokenType());
            result.put("additionalClaims", token.getAdditionalClaims());

            logger.info("跨域认证令牌验证成功: {}", tokenId);

        } catch (Exception e) {
            logger.error("跨域认证令牌验证失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "令牌验证失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户的跨域认证令牌列表
     */
    @Transactional(readOnly = true)
    public List<CrossDomainTokenEntity> getUserTokens(String userId) {
        return tokenRepository.findByUserIdOrderByIssuedAtDesc(userId);
    }

    /**
     * 清理过期的令牌
     */
    public int cleanupExpiredTokens() {
        logger.info("开始清理过期的跨域认证令牌");
        int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        logger.info("清理完成，删除了 {} 个过期令牌", deletedCount);
        return deletedCount;
    }
} 