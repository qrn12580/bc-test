package com.bjut.blockchain.crossdomain.controller;

import com.bjut.blockchain.crossdomain.dto.DomainTrustRequest;
import com.bjut.blockchain.crossdomain.entity.DomainTrustEntity;
import com.bjut.blockchain.crossdomain.service.DomainTrustService;
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
 * 域间信任管理控制器
 * 提供域间信任关系管理的REST API
 */
@RestController
@RequestMapping("/api/crossdomain/trust")
public class DomainTrustController {

    private static final Logger logger = LoggerFactory.getLogger(DomainTrustController.class);

    @Autowired
    private DomainTrustService domainTrustService;

    /**
     * 建立域间信任关系
     */
    @PostMapping("/establish")
    public ResponseEntity<Map<String, Object>> establishTrust(@Valid @RequestBody DomainTrustRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            DomainTrustEntity trust = domainTrustService.establishTrust(request);
            
            response.put("success", true);
            response.put("message", "域间信任关系建立成功");
            response.put("trust", trust);
            
            logger.info("域间信任关系建立成功: {} -> {}", 
                       request.getSourceDomainId(), request.getTargetDomainId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("建立域间信任关系失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "建立信任关系失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查域间信任关系
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkTrustRelation(
            @RequestParam String sourceDomainId,
            @RequestParam String targetDomainId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean hasTrust = domainTrustService.hasTrustRelation(sourceDomainId, targetDomainId);
            
            response.put("success", true);
            response.put("sourceDomainId", sourceDomainId);
            response.put("targetDomainId", targetDomainId);
            response.put("hasTrust", hasTrust);
            response.put("message", hasTrust ? "存在信任关系" : "不存在信任关系");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查域间信任关系失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "检查信任关系失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取有效的信任关系
     */
    @GetMapping("/valid")
    public ResponseEntity<Map<String, Object>> getValidTrustRelation(
            @RequestParam String sourceDomainId,
            @RequestParam String targetDomainId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<DomainTrustEntity> trustOpt = domainTrustService.getValidTrustRelation(
                    sourceDomainId, targetDomainId);
            
            if (trustOpt.isPresent()) {
                response.put("success", true);
                response.put("trust", trustOpt.get());
                response.put("message", "找到有效的信任关系");
            } else {
                response.put("success", false);
                response.put("message", "未找到有效的信任关系");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取有效信任关系失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取信任关系失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取域的所有信任关系
     */
    @GetMapping("/domain/{domainId}")
    public ResponseEntity<Map<String, Object>> getDomainTrustRelations(@PathVariable String domainId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<DomainTrustEntity> trustRelations = domainTrustService.getDomainTrustRelations(domainId);
            
            response.put("success", true);
            response.put("domainId", domainId);
            response.put("trustRelations", trustRelations);
            response.put("count", trustRelations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取域信任关系失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取域信任关系失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新信任级别
     */
    @PutMapping("/trust-level")
    public ResponseEntity<Map<String, Object>> updateTrustLevel(
            @RequestParam String sourceDomainId,
            @RequestParam String targetDomainId,
            @RequestParam Integer newTrustLevel) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            DomainTrustEntity updatedTrust = domainTrustService.updateTrustLevel(
                    sourceDomainId, targetDomainId, newTrustLevel);
            
            response.put("success", true);
            response.put("message", "信任级别更新成功");
            response.put("trust", updatedTrust);
            
            logger.info("域间信任级别更新成功: {} -> {} = {}", 
                       sourceDomainId, targetDomainId, newTrustLevel);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("更新信任级别失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "更新信任级别失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 撤销信任关系
     */
    @PostMapping("/revoke")
    public ResponseEntity<Map<String, Object>> revokeTrust(
            @RequestParam String sourceDomainId,
            @RequestParam String targetDomainId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            domainTrustService.revokeTrust(sourceDomainId, targetDomainId);
            
            response.put("success", true);
            response.put("message", "域间信任关系已撤销");
            response.put("sourceDomainId", sourceDomainId);
            response.put("targetDomainId", targetDomainId);
            
            logger.info("域间信任关系已撤销: {} -> {}", sourceDomainId, targetDomainId);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("撤销信任关系失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "撤销信任关系失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 