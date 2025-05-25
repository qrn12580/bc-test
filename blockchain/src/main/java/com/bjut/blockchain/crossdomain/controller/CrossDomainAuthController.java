package com.bjut.blockchain.crossdomain.controller;

import com.bjut.blockchain.crossdomain.dto.CrossDomainAuthRequest;
import com.bjut.blockchain.crossdomain.entity.CrossDomainTokenEntity;
import com.bjut.blockchain.crossdomain.service.CrossDomainAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨域认证控制器
 * 提供跨域身份认证相关的REST API
 */
@RestController
@RequestMapping("/api/crossdomain/auth")
public class CrossDomainAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CrossDomainAuthController.class);

    @Autowired
    private CrossDomainAuthService crossDomainAuthService;

    /**
     * 生成跨域认证挑战码
     */
    @PostMapping("/challenge")
    public ResponseEntity<Map<String, Object>> generateChallenge(
            @RequestParam String sourceDomainId,
            @RequestParam String targetDomainId,
            @RequestParam String userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> challengeData = crossDomainAuthService.generateCrossDomainChallenge(
                    sourceDomainId, targetDomainId, userId);
            
            response.put("success", true);
            response.put("message", "挑战码生成成功");
            response.putAll(challengeData);
            
            logger.info("为用户 {} 生成跨域认证挑战码成功", userId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("生成跨域认证挑战码失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "生成挑战码失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证跨域认证请求
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCrossDomainAuth(
            @Valid @RequestBody CrossDomainAuthRequest request) {
        
        try {
            Map<String, Object> result = crossDomainAuthService.verifyCrossDomainAuth(request);
            
            if ((Boolean) result.get("success")) {
                logger.info("用户 {} 跨域认证验证成功", request.getUserId());
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }
            
        } catch (Exception e) {
            logger.error("跨域认证验证失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "认证验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证跨域认证令牌
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestParam String tokenId,
            @RequestParam String targetDomainId) {
        
        try {
            Map<String, Object> result = crossDomainAuthService.validateCrossDomainToken(tokenId, targetDomainId);
            
            if ((Boolean) result.get("success")) {
                logger.info("跨域认证令牌验证成功: {}", tokenId);
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
            }
            
        } catch (Exception e) {
            logger.error("跨域认证令牌验证失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "令牌验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取用户的跨域认证令牌列表
     */
    @GetMapping("/tokens/{userId}")
    public ResponseEntity<Map<String, Object>> getUserTokens(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<CrossDomainTokenEntity> tokens = crossDomainAuthService.getUserTokens(userId);
            
            response.put("success", true);
            response.put("userId", userId);
            response.put("tokens", tokens);
            response.put("count", tokens.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取用户令牌列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取令牌列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 清理过期的令牌
     */
    @PostMapping("/cleanup-expired")
    public ResponseEntity<Map<String, Object>> cleanupExpiredTokens() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int deletedCount = crossDomainAuthService.cleanupExpiredTokens();
            
            response.put("success", true);
            response.put("message", "过期令牌清理完成");
            response.put("deletedCount", deletedCount);
            
            logger.info("过期令牌清理完成，删除了 {} 个令牌", deletedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("清理过期令牌失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "清理过期令牌失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 