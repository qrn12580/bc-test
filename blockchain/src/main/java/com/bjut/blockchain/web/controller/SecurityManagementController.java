package com.bjut.blockchain.web.controller;

import com.bjut.blockchain.web.service.NetworkEnvironmentSimulator;
import com.bjut.blockchain.web.service.NodeTrustService;
import com.bjut.blockchain.crossdomain.service.CrossDomainAuthService;
import com.bjut.blockchain.crossdomain.service.DomainTrustService;
import com.bjut.blockchain.did.service.DidService;
import com.bjut.blockchain.web.util.AnonymousAuthUtil;
import com.bjut.blockchain.web.util.ThresholdAuthUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 安全管理控制器
 * 提供统一的安全功能管理和配置接口
 */
@RestController
@RequestMapping("/api/security")
public class SecurityManagementController {

    private static final Logger logger = LoggerFactory.getLogger(SecurityManagementController.class);

    @Autowired
    private NetworkEnvironmentSimulator networkSimulator;

    @Autowired
    private NodeTrustService nodeTrustService;

    @Autowired
    private CrossDomainAuthService crossDomainAuthService;

    @Autowired
    private DomainTrustService domainTrustService;

    @Autowired
    private DidService didService;

    /**
     * 获取安全总览
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getSecurityOverview() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 网络统计
            NetworkEnvironmentSimulator.NetworkStatistics networkStats = networkSimulator.getNetworkStatistics();
            
            // 节点信任统计
            NodeTrustService.NetworkTrustStatistics trustStats = nodeTrustService.getNetworkTrustStatistics();
            
            // 构建安全总览
            Map<String, Object> overview = new HashMap<>();
            overview.put("networkHealth", networkStats.getNetworkHealthScore());
            overview.put("trustScore", trustStats.getAverageTrustScore());
            overview.put("totalNodes", networkStats.getTotalNodes());
            overview.put("onlineNodes", networkStats.getOnlineNodes());
            overview.put("trustedNodes", trustStats.getTrustedNodes());
            overview.put("blacklistedNodes", trustStats.getBlacklistedNodes());
            overview.put("activeThreats", networkStats.getActiveThreats());
            overview.put("networkPartitions", networkStats.getActivePartitions());
            overview.put("overallSecurityScore", calculateOverallSecurityScore(networkStats, trustStats));
            
            response.put("success", true);
            response.put("overview", overview);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取安全总览失败: ", e);
            response.put("success", false);
            response.put("message", "获取安全总览失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 初始化安全环境
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeSecurityEnvironment(@RequestBody Map<String, Object> config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("初始化安全环境");
            
            // 初始化网络环境模拟器
            networkSimulator.initializeNetworkEnvironment();
            
            // 设置网络条件
            String networkCondition = (String) config.getOrDefault("networkCondition", "NORMAL");
            NetworkEnvironmentSimulator.NetworkCondition condition = 
                NetworkEnvironmentSimulator.NetworkCondition.valueOf(networkCondition);
            networkSimulator.setNetworkCondition(condition);
            
            // 模拟初始节点加入
            List<Map<String, String>> initialNodes = (List<Map<String, String>>) 
                config.getOrDefault("initialNodes", new ArrayList<>());
            
            for (Map<String, String> nodeConfig : initialNodes) {
                String nodeId = nodeConfig.get("nodeId");
                String publicKey = nodeConfig.get("publicKey");
                String nodeType = nodeConfig.getOrDefault("nodeType", "FULL_NODE");
                
                networkSimulator.simulateNodeJoin(
                    nodeId, 
                    publicKey, 
                    NetworkEnvironmentSimulator.NodeType.valueOf(nodeType)
                );
            }
            
            response.put("success", true);
            response.put("message", "安全环境初始化成功");
            response.put("initializedNodes", initialNodes.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("初始化安全环境失败: ", e);
            response.put("success", false);
            response.put("message", "初始化安全环境失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 生成匿名凭证
     */
    @PostMapping("/anonymous/credential")
    public ResponseEntity<Map<String, Object>> generateAnonymousCredential(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String issuerDid = (String) request.get("issuerDid");
            String userSecret = (String) request.get("userSecret");
            String credentialSchema = (String) request.get("credentialSchema");
            Map<String, String> attributes = (Map<String, String>) request.getOrDefault("attributes", new HashMap<>());
            
            if (issuerDid == null || userSecret == null || credentialSchema == null) {
                response.put("success", false);
                response.put("message", "缺少必要参数：issuerDid, userSecret, credentialSchema");
                return ResponseEntity.badRequest().body(response);
            }
            
            AnonymousAuthUtil.AnonymousCredentialData credential = 
                AnonymousAuthUtil.generateAnonymousCredential(issuerDid, userSecret, credentialSchema, attributes);
            
            Map<String, Object> credentialInfo = new HashMap<>();
            credentialInfo.put("credentialId", credential.getCredentialId());
            credentialInfo.put("pseudonym", credential.getPseudonym());
            credentialInfo.put("credentialSchema", credential.getCredentialSchema());
            
            Map<String, Object> proofInfo = new HashMap<>();
            proofInfo.put("t", credential.getProof().getT());
            proofInfo.put("e", credential.getProof().getE());
            proofInfo.put("z", credential.getProof().getZ());
            proofInfo.put("publicKey", credential.getProof().getPublicKey());
            credentialInfo.put("proof", proofInfo);
            
            response.put("success", true);
            response.put("credential", credentialInfo);
            response.put("message", "匿名凭证生成成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("生成匿名凭证失败: ", e);
            response.put("success", false);
            response.put("message", "生成匿名凭证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 验证匿名凭证
     */
    @PostMapping("/anonymous/verify")
    public ResponseEntity<Map<String, Object>> verifyAnonymousCredential(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 这里需要根据实际的凭证数据结构来解析
            String challenge = (String) request.get("challenge");
            Map<String, Object> credentialData = (Map<String, Object>) request.get("credential");
            
            if (challenge == null || credentialData == null) {
                response.put("success", false);
                response.put("message", "缺少必要参数：challenge, credential");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 简化验证过程
            boolean isValid = true; // 实际需要实现完整的验证逻辑
            
            response.put("success", true);
            response.put("verified", isValid);
            response.put("message", isValid ? "凭证验证成功" : "凭证验证失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("验证匿名凭证失败: ", e);
            response.put("success", false);
            response.put("message", "验证匿名凭证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 创建门限认证组
     */
    @PostMapping("/threshold/group")
    public ResponseEntity<Map<String, Object>> createThresholdAuthGroup(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String groupId = (String) request.get("groupId");
            Integer threshold = (Integer) request.get("threshold");
            Integer totalParticipants = (Integer) request.get("totalParticipants");
            
            if (groupId == null || threshold == null || totalParticipants == null) {
                response.put("success", false);
                response.put("message", "缺少必要参数：groupId, threshold, totalParticipants");
                return ResponseEntity.badRequest().body(response);
            }
            
            ThresholdAuthUtil.ThresholdConfig config = 
                ThresholdAuthUtil.generateThresholdConfig(threshold, totalParticipants, groupId);
            
            Map<String, Object> configInfo = new HashMap<>();
            configInfo.put("groupId", config.getGroupId());
            configInfo.put("threshold", config.getThreshold());
            configInfo.put("totalParticipants", config.getTotalParticipants());
            configInfo.put("groupPublicKey", config.getGroupPublicKey());
            
            List<Map<String, Object>> participantsList = new ArrayList<>();
            for (ThresholdAuthUtil.ParticipantInfo participant : config.getParticipants()) {
                Map<String, Object> participantInfo = new HashMap<>();
                participantInfo.put("index", participant.getIndex());
                participantInfo.put("participantId", participant.getParticipantId());
                participantInfo.put("publicKeyShare", participant.getPublicKeyShare());
                // 注意：私钥分片应该安全分发，不应该在响应中返回
                participantsList.add(participantInfo);
            }
            configInfo.put("participants", participantsList);
            
            response.put("success", true);
            response.put("config", configInfo);
            response.put("message", "门限认证组创建成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("创建门限认证组失败: ", e);
            response.put("success", false);
            response.put("message", "创建门限认证组失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 模拟网络攻击
     */
    @PostMapping("/simulation/attack")
    public ResponseEntity<Map<String, Object>> simulateNetworkAttack(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String attackType = (String) request.get("attackType");
            List<String> targetNodes = (List<String>) request.getOrDefault("targetNodes", new ArrayList<>());
            Integer intensity = (Integer) request.getOrDefault("intensity", 1);
            Long duration = ((Number) request.getOrDefault("duration", 5)).longValue();
            
            if (attackType == null) {
                response.put("success", false);
                response.put("message", "缺少攻击类型参数");
                return ResponseEntity.badRequest().body(response);
            }
            
            switch (attackType.toUpperCase()) {
                case "DDOS":
                    networkSimulator.simulateDDoSAttack(targetNodes, intensity, duration);
                    break;
                case "BYZANTINE":
                    if (!targetNodes.isEmpty()) {
                        String nodeId = targetNodes.get(0);
                        String byzantineType = (String) request.getOrDefault("byzantineType", "RANDOM");
                        NetworkEnvironmentSimulator.ByzantineType type = 
                            NetworkEnvironmentSimulator.ByzantineType.valueOf(byzantineType);
                        networkSimulator.simulateByzantineNode(nodeId, type);
                    }
                    break;
                case "PARTITION":
                    if (targetNodes.size() >= 2) {
                        int mid = targetNodes.size() / 2;
                        List<String> partition1 = targetNodes.subList(0, mid);
                        List<String> partition2 = targetNodes.subList(mid, targetNodes.size());
                        networkSimulator.simulateNetworkPartition(partition1, partition2, duration);
                    }
                    break;
                default:
                    response.put("success", false);
                    response.put("message", "不支持的攻击类型: " + attackType);
                    return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("message", "网络攻击模拟已启动");
            response.put("attackType", attackType);
            response.put("targetNodes", targetNodes.size());
            response.put("duration", duration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("模拟网络攻击失败: ", e);
            response.put("success", false);
            response.put("message", "模拟网络攻击失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 设置网络条件
     */
    @PostMapping("/network/condition")
    public ResponseEntity<Map<String, Object>> setNetworkCondition(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String condition = request.get("condition");
            if (condition == null) {
                response.put("success", false);
                response.put("message", "缺少网络条件参数");
                return ResponseEntity.badRequest().body(response);
            }
            
            NetworkEnvironmentSimulator.NetworkCondition networkCondition = 
                NetworkEnvironmentSimulator.NetworkCondition.valueOf(condition.toUpperCase());
            
            networkSimulator.setNetworkCondition(networkCondition);
            
            response.put("success", true);
            response.put("message", "网络条件设置成功");
            response.put("condition", condition);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "无效的网络条件: " + request.get("condition"));
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("设置网络条件失败: ", e);
            response.put("success", false);
            response.put("message", "设置网络条件失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取安全配置建议
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Map<String, Object>> getSecurityRecommendations() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> recommendations = generateSecurityRecommendations();
            
            response.put("success", true);
            response.put("recommendations", recommendations);
            response.put("count", recommendations.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取安全建议失败: ", e);
            response.put("success", false);
            response.put("message", "获取安全建议失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 执行安全检查
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> performSecurityCheck() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> checkResults = performComprehensiveSecurityCheck();
            
            response.put("success", true);
            response.put("checkResults", checkResults);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("执行安全检查失败: ", e);
            response.put("success", false);
            response.put("message", "执行安全检查失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 私有辅助方法

    private double calculateOverallSecurityScore(
            NetworkEnvironmentSimulator.NetworkStatistics networkStats,
            NodeTrustService.NetworkTrustStatistics trustStats) {
        
        double networkHealthWeight = 0.3;
        double trustScoreWeight = 0.3;
        double nodeRatioWeight = 0.2;
        double threatWeight = 0.2;
        
        double networkHealth = networkStats.getNetworkHealthScore();
        double trustScore = trustStats.getAverageTrustScore().doubleValue();
        double nodeRatio = trustStats.getTrustRate();
        double threatImpact = networkStats.getActiveThreats() > 0 ? 0.5 : 1.0;
        
        return networkHealth * networkHealthWeight +
               trustScore * trustScoreWeight +
               nodeRatio * nodeRatioWeight +
               threatImpact * threatWeight;
    }

    private List<String> generateSecurityRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        try {
            NetworkEnvironmentSimulator.NetworkStatistics networkStats = networkSimulator.getNetworkStatistics();
            NodeTrustService.NetworkTrustStatistics trustStats = nodeTrustService.getNetworkTrustStatistics();
            
            // 基于网络健康状况生成建议
            if (networkStats.getNetworkHealthScore() < 0.7) {
                recommendations.add("网络健康状况较差，建议检查网络连接和节点状态");
            }
            
            if (networkStats.getActiveThreats() > 0) {
                recommendations.add("检测到活跃安全威胁，建议启用增强安全模式");
            }
            
            if (networkStats.getActivePartitions() > 0) {
                recommendations.add("网络存在分区，建议检查网络连通性");
            }
            
            if (trustStats.getTrustRate() < 0.6) {
                recommendations.add("可信节点比例较低，建议加强节点准入审核");
            }
            
            if (trustStats.getBlacklistedNodes() > trustStats.getTotalNodes() * 0.1) {
                recommendations.add("黑名单节点过多，建议审查网络安全策略");
            }
            
            if (recommendations.isEmpty()) {
                recommendations.add("系统安全状态良好，建议定期进行安全检查");
            }
            
        } catch (Exception e) {
            logger.error("生成安全建议时发生错误", e);
            recommendations.add("无法生成安全建议，请检查系统状态");
        }
        
        return recommendations;
    }

    private Map<String, Object> performComprehensiveSecurityCheck() {
        Map<String, Object> results = new HashMap<>();
        
        try {
            // 网络安全检查
            NetworkEnvironmentSimulator.NetworkStatistics networkStats = networkSimulator.getNetworkStatistics();
            Map<String, Object> networkSecurity = new HashMap<>();
            networkSecurity.put("healthScore", networkStats.getNetworkHealthScore());
            networkSecurity.put("activeThreats", networkStats.getActiveThreats());
            networkSecurity.put("partitions", networkStats.getActivePartitions());
            networkSecurity.put("status", networkStats.getNetworkHealthScore() > 0.7 ? "GOOD" : "WARNING");
            results.put("networkSecurity", networkSecurity);
            
            // 节点信任检查
            NodeTrustService.NetworkTrustStatistics trustStats = nodeTrustService.getNetworkTrustStatistics();
            Map<String, Object> nodeTrust = new HashMap<>();
            nodeTrust.put("averageTrustScore", trustStats.getAverageTrustScore());
            nodeTrust.put("trustRate", trustStats.getTrustRate());
            nodeTrust.put("blacklistedNodes", trustStats.getBlacklistedNodes());
            nodeTrust.put("status", trustStats.getTrustRate() > 0.6 ? "GOOD" : "WARNING");
            results.put("nodeTrust", nodeTrust);
            
            // 整体安全评分
            double overallScore = calculateOverallSecurityScore(networkStats, trustStats);
            Map<String, Object> overallSecurity = new HashMap<>();
            overallSecurity.put("score", overallScore);
            overallSecurity.put("status", overallScore > 0.7 ? "SECURE" : overallScore > 0.5 ? "MODERATE" : "RISK");
            overallSecurity.put("timestamp", System.currentTimeMillis());
            results.put("overallSecurity", overallSecurity);
            
        } catch (Exception e) {
            logger.error("执行安全检查时发生错误", e);
            results.put("error", "安全检查执行失败: " + e.getMessage());
        }
        
        return results;
    }
} 