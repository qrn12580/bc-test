package com.bjut.blockchain.crossdomain.controller;

import com.bjut.blockchain.crossdomain.dto.DomainRegistrationRequest;
import com.bjut.blockchain.crossdomain.entity.DomainEntity;
import com.bjut.blockchain.crossdomain.service.DomainService;
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
import java.util.Optional;

/**
 * 域管理控制器
 * 提供域注册、查询、管理等REST API
 */
@RestController
@RequestMapping("/api/crossdomain/domains")
public class DomainController {

    private static final Logger logger = LoggerFactory.getLogger(DomainController.class);

    @Autowired
    private DomainService domainService;

    /**
     * 注册新域
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDomain(@Valid @RequestBody DomainRegistrationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DomainEntity domain = domainService.registerDomain(request);
            response.put("success", true);
            response.put("message", "域注册成功");
            response.put("domain", domain);
            
            logger.info("域注册成功: {}", domain.getDomainId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("域注册失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "域注册失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取域信息
     */
    @GetMapping("/{domainId}")
    public ResponseEntity<Map<String, Object>> getDomain(@PathVariable String domainId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<DomainEntity> domainOpt = domainService.getDomain(domainId);
            
            if (domainOpt.isPresent()) {
                response.put("success", true);
                response.put("domain", domainOpt.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "域不存在: " + domainId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("获取域信息失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取域信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有活跃域
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveDomains() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DomainEntity> domains = domainService.getAllActiveDomains();
            response.put("success", true);
            response.put("domains", domains);
            response.put("count", domains.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取活跃域列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取活跃域列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 根据域类型获取域
     */
    @GetMapping("/type/{domainType}")
    public ResponseEntity<Map<String, Object>> getDomainsByType(@PathVariable String domainType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DomainEntity> domains = domainService.getDomainsByType(domainType);
            response.put("success", true);
            response.put("domains", domains);
            response.put("domainType", domainType);
            response.put("count", domains.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("根据类型获取域列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取域列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新域信息
     */
    @PutMapping("/{domainId}")
    public ResponseEntity<Map<String, Object>> updateDomain(@PathVariable String domainId, 
                                                           @Valid @RequestBody DomainRegistrationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DomainEntity updatedDomain = domainService.updateDomain(domainId, request);
            response.put("success", true);
            response.put("message", "域更新成功");
            response.put("domain", updatedDomain);
            
            logger.info("域更新成功: {}", domainId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("域更新失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "域更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 停用域
     */
    @PostMapping("/{domainId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateDomain(@PathVariable String domainId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            domainService.deactivateDomain(domainId);
            response.put("success", true);
            response.put("message", "域已停用");
            
            logger.info("域已停用: {}", domainId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("域停用失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "域停用失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 激活域
     */
    @PostMapping("/{domainId}/activate")
    public ResponseEntity<Map<String, Object>> activateDomain(@PathVariable String domainId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            domainService.activateDomain(domainId);
            response.put("success", true);
            response.put("message", "域已激活");
            
            logger.info("域已激活: {}", domainId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("域激活失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "域激活失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证域证书
     */
    @GetMapping("/{domainId}/validate-certificate")
    public ResponseEntity<Map<String, Object>> validateDomainCertificate(@PathVariable String domainId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = domainService.validateDomainCertificate(domainId);
            response.put("success", true);
            response.put("domainId", domainId);
            response.put("certificateValid", isValid);
            response.put("message", isValid ? "证书验证通过" : "证书验证失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("域证书验证失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "证书验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 