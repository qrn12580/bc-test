package com.bjut.blockchain.web.controller;

import com.bjut.blockchain.web.entity.NodeTrustEntity;
import com.bjut.blockchain.web.service.NodeTrustService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 节点信任管理控制器
 * 提供节点信任评估和管理相关的REST API
 */
@RestController
@RequestMapping("/api/trust")
public class NodeTrustController {

    private static final Logger logger = LoggerFactory.getLogger(NodeTrustController.class);

    @Autowired
    private NodeTrustService nodeTrustService;

    /**
     * 注册新节点
     */
    @PostMapping("/nodes/register")
    public ResponseEntity<Map<String, Object>> registerNode(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String nodeId = request.get("nodeId");
            String publicKey = request.get("publicKey");
            
            if (nodeId == null || publicKey == null) {
                response.put("success", false);
                response.put("message", "节点ID和公钥不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            NodeTrustEntity nodeTrust = nodeTrustService.registerNode(nodeId, publicKey);
            
            response.put("success", true);
            response.put("nodeTrust", nodeTrust);
            response.put("message", "节点注册成功");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("节点注册失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("节点注册时发生错误: ", e);
            response.put("success", false);
            response.put("message", "注册节点时发生错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取节点信任信息
     */
    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<Map<String, Object>> getNodeTrust(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<NodeTrustEntity> nodeOpt = nodeTrustService.getNodeTrust(nodeId);
            
            if (nodeOpt.isPresent()) {
                NodeTrustEntity node = nodeOpt.get();
                response.put("success", true);
                response.put("nodeTrust", node);
                response.put("overallTrust", node.calculateOverallTrust());
                response.put("isTrusted", node.isTrusted());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "节点不存在: " + nodeId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("获取节点信任信息失败: ", e);
            response.put("success", false);
            response.put("message", "获取节点信任信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取所有可信节点
     */
    @GetMapping("/nodes/trusted")
    public ResponseEntity<Map<String, Object>> getTrustedNodes() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NodeTrustEntity> trustedNodes = nodeTrustService.getTrustedNodes();
            
            response.put("success", true);
            response.put("trustedNodes", trustedNodes);
            response.put("count", trustedNodes.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取可信节点失败: ", e);
            response.put("success", false);
            response.put("message", "获取可信节点失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取活跃节点
     */
    @GetMapping("/nodes/active")
    public ResponseEntity<Map<String, Object>> getActiveNodes() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NodeTrustEntity> activeNodes = nodeTrustService.getActiveNodes();
            
            response.put("success", true);
            response.put("activeNodes", activeNodes);
            response.put("count", activeNodes.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取活跃节点失败: ", e);
            response.put("success", false);
            response.put("message", "获取活跃节点失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取高风险节点
     */
    @GetMapping("/nodes/risk")
    public ResponseEntity<Map<String, Object>> getRiskNodes() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NodeTrustEntity> riskNodes = nodeTrustService.getRiskNodes();
            
            response.put("success", true);
            response.put("riskNodes", riskNodes);
            response.put("count", riskNodes.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取高风险节点失败: ", e);
            response.put("success", false);
            response.put("message", "获取高风险节点失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取网络信任统计
     */
    @GetMapping("/network/statistics")
    public ResponseEntity<Map<String, Object>> getNetworkTrustStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            NodeTrustService.NetworkTrustStatistics stats = nodeTrustService.getNetworkTrustStatistics();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalNodes", stats.getTotalNodes());
            statistics.put("trustedNodes", stats.getTrustedNodes());
            statistics.put("blacklistedNodes", stats.getBlacklistedNodes());
            statistics.put("averageTrustScore", stats.getAverageTrustScore());
            statistics.put("trustRate", stats.getTrustRate());
            
            response.put("success", true);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取网络信任统计失败: ", e);
            response.put("success", false);
            response.put("message", "获取网络信任统计失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新节点活跃状态
     */
    @PostMapping("/nodes/{nodeId}/activity")
    public ResponseEntity<Map<String, Object>> updateNodeActivity(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            nodeTrustService.updateNodeActivity(nodeId);
            
            response.put("success", true);
            response.put("message", "节点活跃状态更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新节点活跃状态失败: ", e);
            response.put("success", false);
            response.put("message", "更新节点活跃状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 记录节点成功挖矿
     */
    @PostMapping("/nodes/{nodeId}/mining")
    public ResponseEntity<Map<String, Object>> recordSuccessfulMining(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            nodeTrustService.recordSuccessfulMining(nodeId);
            
            response.put("success", true);
            response.put("message", "挖矿记录更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("记录挖矿失败: ", e);
            response.put("success", false);
            response.put("message", "记录挖矿失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 记录有效交易
     */
    @PostMapping("/nodes/{nodeId}/transactions/valid")
    public ResponseEntity<Map<String, Object>> recordValidTransaction(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            nodeTrustService.recordValidTransaction(nodeId);
            
            response.put("success", true);
            response.put("message", "有效交易记录更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("记录有效交易失败: ", e);
            response.put("success", false);
            response.put("message", "记录有效交易失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 记录无效交易
     */
    @PostMapping("/nodes/{nodeId}/transactions/invalid")
    public ResponseEntity<Map<String, Object>> recordInvalidTransaction(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            nodeTrustService.recordInvalidTransaction(nodeId);
            
            response.put("success", true);
            response.put("message", "无效交易记录更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("记录无效交易失败: ", e);
            response.put("success", false);
            response.put("message", "记录无效交易失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 更新共识参与率
     */
    @PostMapping("/nodes/{nodeId}/consensus")
    public ResponseEntity<Map<String, Object>> updateConsensusParticipation(
            @PathVariable String nodeId, 
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String participationRateStr = request.get("participationRate");
            if (participationRateStr == null) {
                response.put("success", false);
                response.put("message", "参与率不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            BigDecimal participationRate = new BigDecimal(participationRateStr);
            nodeTrustService.updateConsensusParticipation(nodeId, participationRate);
            
            response.put("success", true);
            response.put("message", "共识参与率更新成功");
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "参与率格式不正确");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("更新共识参与率失败: ", e);
            response.put("success", false);
            response.put("message", "更新共识参与率失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 将节点加入黑名单
     */
    @PostMapping("/nodes/{nodeId}/blacklist")
    public ResponseEntity<Map<String, Object>> blacklistNode(
            @PathVariable String nodeId, 
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "黑名单原因不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            nodeTrustService.blacklistNode(nodeId, reason);
            
            response.put("success", true);
            response.put("message", "节点已加入黑名单");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("加入黑名单失败: ", e);
            response.put("success", false);
            response.put("message", "加入黑名单失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 将节点移出黑名单
     */
    @DeleteMapping("/nodes/{nodeId}/blacklist")
    public ResponseEntity<Map<String, Object>> removeFromBlacklist(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            nodeTrustService.removeFromBlacklist(nodeId);
            
            response.put("success", true);
            response.put("message", "节点已移出黑名单");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("移出黑名单失败: ", e);
            response.put("success", false);
            response.put("message", "移出黑名单失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 检查节点是否可信
     */
    @GetMapping("/nodes/{nodeId}/trusted")
    public ResponseEntity<Map<String, Object>> isNodeTrusted(@PathVariable String nodeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isTrusted = nodeTrustService.isNodeTrusted(nodeId);
            
            response.put("success", true);
            response.put("nodeId", nodeId);
            response.put("isTrusted", isTrusted);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查节点信任状态失败: ", e);
            response.put("success", false);
            response.put("message", "检查节点信任状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
} 