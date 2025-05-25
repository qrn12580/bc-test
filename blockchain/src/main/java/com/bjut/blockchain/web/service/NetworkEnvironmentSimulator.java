package com.bjut.blockchain.web.service;

import com.bjut.blockchain.web.entity.NodeTrustEntity;
import com.bjut.blockchain.web.entity.SecureGroupEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 网络环境模拟器
 * 模拟真实的区块链网络环境，包括节点动态、网络状况、安全威胁等
 */
@Service
public class NetworkEnvironmentSimulator {

    private static final Logger logger = LoggerFactory.getLogger(NetworkEnvironmentSimulator.class);

    @Autowired
    private NodeTrustService nodeTrustService;

    // 网络状态缓存
    private final Map<String, NodeNetworkStatus> nodeNetworkStatus = new ConcurrentHashMap<>();
    private final Map<String, NetworkPartition> networkPartitions = new ConcurrentHashMap<>();
    private final List<SecurityThreat> activeThreets = new ArrayList<>();

    // 网络参数
    private volatile NetworkCondition currentNetworkCondition = NetworkCondition.NORMAL;
    private volatile double baseNetworkLatency = 50.0; // 毫秒
    private volatile double packetLossRate = 0.01; // 1%
    private volatile double byzantineNodeRatio = 0.1; // 10%恶意节点

    /**
     * 初始化网络环境
     */
    public void initializeNetworkEnvironment() {
        logger.info("初始化网络环境模拟器");
        
        // 设置默认网络条件
        setNetworkCondition(NetworkCondition.NORMAL);
        
        // 启动网络监控
        startNetworkMonitoring();
        
        logger.info("网络环境模拟器初始化完成");
    }

    /**
     * 模拟节点加入网络
     */
    public void simulateNodeJoin(String nodeId, String publicKey, NodeType nodeType) {
        logger.info("模拟节点 {} 加入网络，类型: {}", nodeId, nodeType);
        
        try {
            // 注册节点信任信息
            nodeTrustService.registerNode(nodeId, publicKey);
            
            // 设置节点网络状态
            NodeNetworkStatus status = new NodeNetworkStatus(
                nodeId,
                nodeType,
                NetworkStatus.ONLINE,
                calculateInitialLatency(),
                0.0,
                LocalDateTime.now()
            );
            
            nodeNetworkStatus.put(nodeId, status);
            
            // 模拟网络发现过程
            simulateNetworkDiscovery(nodeId);
            
            logger.info("节点 {} 成功加入网络", nodeId);
            
        } catch (Exception e) {
            logger.error("节点 {} 加入网络失败: {}", nodeId, e.getMessage());
        }
    }

    /**
     * 模拟节点退出网络
     */
    public void simulateNodeLeave(String nodeId, LeaveReason reason) {
        logger.info("模拟节点 {} 退出网络，原因: {}", nodeId, reason);
        
        NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
        if (status != null) {
            status.setStatus(NetworkStatus.OFFLINE);
            status.setLastSeen(LocalDateTime.now());
            
            // 根据退出原因更新信任度
            if (reason == LeaveReason.MALICIOUS) {
                nodeTrustService.blacklistNode(nodeId, "恶意行为导致的强制退出");
            } else if (reason == LeaveReason.NETWORK_ISSUE) {
                // 网络问题不影响信任度
            } else {
                // 正常退出
                nodeTrustService.updateNodeActivity(nodeId);
            }
        }
        
        logger.info("节点 {} 已退出网络", nodeId);
    }

    /**
     * 模拟网络分区
     */
    public void simulateNetworkPartition(List<String> partition1, List<String> partition2, long durationMinutes) {
        String partitionId = UUID.randomUUID().toString();
        logger.warn("模拟网络分区 {}: 分区1={}, 分区2={}, 持续时间={}分钟", 
                   partitionId, partition1.size(), partition2.size(), durationMinutes);
        
        NetworkPartition partition = new NetworkPartition(
            partitionId,
            partition1,
            partition2,
            System.currentTimeMillis(),
            durationMinutes * 60 * 1000
        );
        
        networkPartitions.put(partitionId, partition);
        
        // 更新节点状态
        updatePartitionedNodeStatus(partition1, partition2);
        
        // 安排分区恢复
        schedulePartitionRecovery(partitionId, durationMinutes);
    }

    /**
     * 模拟DDoS攻击
     */
    public void simulateDDoSAttack(List<String> targetNodes, int intensityLevel, long durationMinutes) {
        logger.warn("模拟DDoS攻击: 目标节点数={}, 强度级别={}, 持续时间={}分钟", 
                   targetNodes.size(), intensityLevel, durationMinutes);
        
        SecurityThreat threat = new SecurityThreat(
            UUID.randomUUID().toString(),
            ThreatType.DDOS,
            targetNodes,
            intensityLevel,
            System.currentTimeMillis(),
            durationMinutes * 60 * 1000
        );
        
        activeThreets.add(threat);
        
        // 影响目标节点的网络性能
        for (String nodeId : targetNodes) {
            NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
            if (status != null) {
                // 增加延迟和丢包率
                status.setLatency(status.getLatency() * (1 + intensityLevel));
                status.setPacketLoss(Math.min(0.5, status.getPacketLoss() + intensityLevel * 0.1));
            }
        }
        
        // 安排攻击结束
        scheduleAttackEnd(threat.getThreatId(), durationMinutes);
    }

    /**
     * 模拟拜占庭节点行为
     */
    public void simulateByzantineNode(String nodeId, ByzantineType byzantineType) {
        logger.warn("模拟拜占庭节点行为: 节点={}, 类型={}", nodeId, byzantineType);
        
        NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
        if (status != null) {
            status.setByzantineType(byzantineType);
            
            // 根据拜占庭类型执行不同的恶意行为
            switch (byzantineType) {
                case SILENT:
                    // 静默节点 - 不响应请求
                    status.setStatus(NetworkStatus.UNRESPONSIVE);
                    break;
                case RANDOM:
                    // 随机响应
                    status.setReliabilityScore(0.3);
                    break;
                case ADVERSARIAL:
                    // 恶意响应
                    status.setReliabilityScore(0.1);
                    nodeTrustService.recordInvalidTransaction(nodeId);
                    break;
                case SELFISH:
                    // 自私节点 - 只为自己利益
                    status.setReliabilityScore(0.6);
                    break;
            }
        }
    }

    /**
     * 获取网络统计信息
     */
    public NetworkStatistics getNetworkStatistics() {
        int totalNodes = nodeNetworkStatus.size();
        long onlineNodes = nodeNetworkStatus.values().stream()
            .mapToLong(status -> status.getStatus() == NetworkStatus.ONLINE ? 1 : 0)
            .sum();
        
        double avgLatency = nodeNetworkStatus.values().stream()
            .filter(status -> status.getStatus() == NetworkStatus.ONLINE)
            .mapToDouble(NodeNetworkStatus::getLatency)
            .average()
            .orElse(0.0);
        
        double avgPacketLoss = nodeNetworkStatus.values().stream()
            .filter(status -> status.getStatus() == NetworkStatus.ONLINE)
            .mapToDouble(NodeNetworkStatus::getPacketLoss)
            .average()
            .orElse(0.0);
        
        int activePartitions = networkPartitions.size();
        int activeThreats = activeThreets.size();
        
        return new NetworkStatistics(
            totalNodes,
            (int) onlineNodes,
            avgLatency,
            avgPacketLoss,
            activePartitions,
            activeThreats,
            currentNetworkCondition
        );
    }

    /**
     * 设置网络条件
     */
    public void setNetworkCondition(NetworkCondition condition) {
        logger.info("设置网络条件: {}", condition);
        this.currentNetworkCondition = condition;
        
        switch (condition) {
            case EXCELLENT:
                baseNetworkLatency = 10.0;
                packetLossRate = 0.001;
                break;
            case GOOD:
                baseNetworkLatency = 30.0;
                packetLossRate = 0.005;
                break;
            case NORMAL:
                baseNetworkLatency = 50.0;
                packetLossRate = 0.01;
                break;
            case POOR:
                baseNetworkLatency = 200.0;
                packetLossRate = 0.05;
                break;
            case TERRIBLE:
                baseNetworkLatency = 1000.0;
                packetLossRate = 0.2;
                break;
        }
        
        // 更新所有在线节点的网络状态
        updateAllNodeNetworkStatus();
    }

    /**
     * 定时网络状态更新
     */
    @Scheduled(fixedRate = 30000) // 每30秒执行一次
    public void updateNetworkStatus() {
        logger.debug("更新网络状态");
        
        // 清理过期的网络分区
        cleanupExpiredPartitions();
        
        // 清理过期的安全威胁
        cleanupExpiredThreats();
        
        // 模拟网络波动
        simulateNetworkFluctuation();
        
        // 更新节点网络状态
        updateNodeReachability();
    }

    /**
     * 检查两个节点之间的连通性
     */
    public boolean checkNodeConnectivity(String node1, String node2) {
        // 检查是否在不同的网络分区中
        for (NetworkPartition partition : networkPartitions.values()) {
            if (isInDifferentPartitions(node1, node2, partition)) {
                return false;
            }
        }
        
        // 检查节点状态
        NodeNetworkStatus status1 = nodeNetworkStatus.get(node1);
        NodeNetworkStatus status2 = nodeNetworkStatus.get(node2);
        
        if (status1 == null || status2 == null) {
            return false;
        }
        
        return status1.getStatus() == NetworkStatus.ONLINE && 
               status2.getStatus() == NetworkStatus.ONLINE;
    }

    /**
     * 计算两个节点之间的网络延迟
     */
    public double calculateNetworkLatency(String node1, String node2) {
        if (!checkNodeConnectivity(node1, node2)) {
            return Double.MAX_VALUE; // 不连通
        }
        
        NodeNetworkStatus status1 = nodeNetworkStatus.get(node1);
        NodeNetworkStatus status2 = nodeNetworkStatus.get(node2);
        
        if (status1 == null || status2 == null) {
            return baseNetworkLatency;
        }
        
        // 计算平均延迟并加入随机波动
        double avgLatency = (status1.getLatency() + status2.getLatency()) / 2.0;
        double fluctuation = ThreadLocalRandom.current().nextGaussian() * 10.0;
        
        return Math.max(0, avgLatency + fluctuation);
    }

    // 私有方法实现
    
    private void simulateNetworkDiscovery(String nodeId) {
        // 模拟节点发现其他节点的过程
        List<String> discoveredNodes = new ArrayList<>();
        for (String existingNodeId : nodeNetworkStatus.keySet()) {
            if (!existingNodeId.equals(nodeId)) {
                discoveredNodes.add(existingNodeId);
            }
        }
        logger.debug("节点 {} 发现了 {} 个其他节点", nodeId, discoveredNodes.size());
    }
    
    private double calculateInitialLatency() {
        // 基于当前网络条件计算初始延迟
        double randomFactor = ThreadLocalRandom.current().nextGaussian() * 0.2 + 1.0;
        return baseNetworkLatency * Math.max(0.1, randomFactor);
    }
    
    private void updatePartitionedNodeStatus(List<String> partition1, List<String> partition2) {
        // 更新分区中节点的状态
        for (String nodeId : partition1) {
            NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
            if (status != null) {
                status.setPartitioned(true);
            }
        }
        for (String nodeId : partition2) {
            NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
            if (status != null) {
                status.setPartitioned(true);
            }
        }
    }
    
    private void schedulePartitionRecovery(String partitionId, long delayMinutes) {
        // 简化实现：实际应该使用ScheduledExecutorService
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                recoverFromPartition(partitionId);
            }
        }, delayMinutes * 60 * 1000);
    }
    
    private void scheduleAttackEnd(String threatId, long delayMinutes) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endSecurityThreat(threatId);
            }
        }, delayMinutes * 60 * 1000);
    }
    
    private void recoverFromPartition(String partitionId) {
        NetworkPartition partition = networkPartitions.remove(partitionId);
        if (partition != null) {
            logger.info("网络分区 {} 已恢复", partitionId);
            
            // 恢复节点状态
            List<String> allNodes = new ArrayList<>(partition.getPartition1());
            allNodes.addAll(partition.getPartition2());
            
            for (String nodeId : allNodes) {
                NodeNetworkStatus status = nodeNetworkStatus.get(nodeId);
                if (status != null) {
                    status.setPartitioned(false);
                }
            }
        }
    }
    
    private void endSecurityThreat(String threatId) {
        activeThreets.removeIf(threat -> threat.getThreatId().equals(threatId));
        logger.info("安全威胁 {} 已结束", threatId);
    }
    
    private boolean isInDifferentPartitions(String node1, String node2, NetworkPartition partition) {
        boolean node1InPartition1 = partition.getPartition1().contains(node1);
        boolean node1InPartition2 = partition.getPartition2().contains(node1);
        boolean node2InPartition1 = partition.getPartition1().contains(node2);
        boolean node2InPartition2 = partition.getPartition2().contains(node2);
        
        return (node1InPartition1 && node2InPartition2) || 
               (node1InPartition2 && node2InPartition1);
    }
    
    private void startNetworkMonitoring() {
        logger.info("启动网络监控");
        // 实现网络监控逻辑
    }
    
    private void updateAllNodeNetworkStatus() {
        for (NodeNetworkStatus status : nodeNetworkStatus.values()) {
            if (status.getStatus() == NetworkStatus.ONLINE) {
                status.setLatency(calculateInitialLatency());
                status.setPacketLoss(packetLossRate + ThreadLocalRandom.current().nextGaussian() * 0.01);
            }
        }
    }
    
    private void cleanupExpiredPartitions() {
        long currentTime = System.currentTimeMillis();
        networkPartitions.entrySet().removeIf(entry -> {
            NetworkPartition partition = entry.getValue();
            if (currentTime > partition.getStartTime() + partition.getDuration()) {
                recoverFromPartition(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    private void cleanupExpiredThreats() {
        long currentTime = System.currentTimeMillis();
        activeThreets.removeIf(threat -> 
            currentTime > threat.getStartTime() + threat.getDuration());
    }
    
    private void simulateNetworkFluctuation() {
        // 模拟网络波动
        if (ThreadLocalRandom.current().nextDouble() < 0.1) {
            // 10%概率发生网络波动
            double fluctuation = ThreadLocalRandom.current().nextGaussian() * 0.2;
            baseNetworkLatency *= (1 + fluctuation);
            baseNetworkLatency = Math.max(10, Math.min(2000, baseNetworkLatency));
        }
    }
    
    private void updateNodeReachability() {
        for (NodeNetworkStatus status : nodeNetworkStatus.values()) {
            if (status.getStatus() == NetworkStatus.ONLINE) {
                // 随机模拟节点可达性变化
                if (ThreadLocalRandom.current().nextDouble() < 0.02) {
                    status.setStatus(NetworkStatus.UNRESPONSIVE);
                }
            } else if (status.getStatus() == NetworkStatus.UNRESPONSIVE) {
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    status.setStatus(NetworkStatus.ONLINE);
                }
            }
        }
    }

    // 枚举和内部类定义
    public enum NetworkCondition {
        EXCELLENT, GOOD, NORMAL, POOR, TERRIBLE
    }
    
    public enum NetworkStatus {
        ONLINE, OFFLINE, UNRESPONSIVE, PARTITIONED
    }
    
    public enum NodeType {
        FULL_NODE, LIGHT_NODE, VALIDATOR, OBSERVER, MALICIOUS
    }
    
    public enum LeaveReason {
        NORMAL, NETWORK_ISSUE, MALICIOUS, MAINTENANCE
    }
    
    public enum ThreatType {
        DDOS, SYBIL, ECLIPSE, ROUTING_ATTACK
    }
    
    public enum ByzantineType {
        SILENT, RANDOM, ADVERSARIAL, SELFISH
    }
    
    // 内部数据类
    public static class NodeNetworkStatus {
        private String nodeId;
        private NodeType nodeType;
        private NetworkStatus status;
        private double latency;
        private double packetLoss;
        private LocalDateTime lastSeen;
        private boolean partitioned;
        private ByzantineType byzantineType;
        private double reliabilityScore = 1.0;
        
        public NodeNetworkStatus(String nodeId, NodeType nodeType, NetworkStatus status, 
                               double latency, double packetLoss, LocalDateTime lastSeen) {
            this.nodeId = nodeId;
            this.nodeType = nodeType;
            this.status = status;
            this.latency = latency;
            this.packetLoss = packetLoss;
            this.lastSeen = lastSeen;
        }
        
        // Getters and Setters
        public String getNodeId() { return nodeId; }
        public NodeType getNodeType() { return nodeType; }
        public NetworkStatus getStatus() { return status; }
        public void setStatus(NetworkStatus status) { this.status = status; }
        public double getLatency() { return latency; }
        public void setLatency(double latency) { this.latency = latency; }
        public double getPacketLoss() { return packetLoss; }
        public void setPacketLoss(double packetLoss) { this.packetLoss = packetLoss; }
        public LocalDateTime getLastSeen() { return lastSeen; }
        public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
        public boolean isPartitioned() { return partitioned; }
        public void setPartitioned(boolean partitioned) { this.partitioned = partitioned; }
        public ByzantineType getByzantineType() { return byzantineType; }
        public void setByzantineType(ByzantineType byzantineType) { this.byzantineType = byzantineType; }
        public double getReliabilityScore() { return reliabilityScore; }
        public void setReliabilityScore(double reliabilityScore) { this.reliabilityScore = reliabilityScore; }
    }
    
    public static class NetworkPartition {
        private final String partitionId;
        private final List<String> partition1;
        private final List<String> partition2;
        private final long startTime;
        private final long duration;
        
        public NetworkPartition(String partitionId, List<String> partition1, List<String> partition2, 
                              long startTime, long duration) {
            this.partitionId = partitionId;
            this.partition1 = partition1;
            this.partition2 = partition2;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        // Getters
        public String getPartitionId() { return partitionId; }
        public List<String> getPartition1() { return partition1; }
        public List<String> getPartition2() { return partition2; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return duration; }
    }
    
    public static class SecurityThreat {
        private final String threatId;
        private final ThreatType threatType;
        private final List<String> targetNodes;
        private final int intensity;
        private final long startTime;
        private final long duration;
        
        public SecurityThreat(String threatId, ThreatType threatType, List<String> targetNodes, 
                            int intensity, long startTime, long duration) {
            this.threatId = threatId;
            this.threatType = threatType;
            this.targetNodes = targetNodes;
            this.intensity = intensity;
            this.startTime = startTime;
            this.duration = duration;
        }
        
        // Getters
        public String getThreatId() { return threatId; }
        public ThreatType getThreatType() { return threatType; }
        public List<String> getTargetNodes() { return targetNodes; }
        public int getIntensity() { return intensity; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return duration; }
    }
    
    public static class NetworkStatistics {
        private final int totalNodes;
        private final int onlineNodes;
        private final double averageLatency;
        private final double averagePacketLoss;
        private final int activePartitions;
        private final int activeThreats;
        private final NetworkCondition networkCondition;
        
        public NetworkStatistics(int totalNodes, int onlineNodes, double averageLatency, 
                               double averagePacketLoss, int activePartitions, int activeThreats,
                               NetworkCondition networkCondition) {
            this.totalNodes = totalNodes;
            this.onlineNodes = onlineNodes;
            this.averageLatency = averageLatency;
            this.averagePacketLoss = averagePacketLoss;
            this.activePartitions = activePartitions;
            this.activeThreats = activeThreats;
            this.networkCondition = networkCondition;
        }
        
        // Getters
        public int getTotalNodes() { return totalNodes; }
        public int getOnlineNodes() { return onlineNodes; }
        public double getAverageLatency() { return averageLatency; }
        public double getAveragePacketLoss() { return averagePacketLoss; }
        public int getActivePartitions() { return activePartitions; }
        public int getActiveThreats() { return activeThreats; }
        public NetworkCondition getNetworkCondition() { return networkCondition; }
        public double getNetworkHealthScore() { 
            double nodeHealth = (double) onlineNodes / totalNodes;
            double latencyHealth = Math.max(0, 1.0 - averageLatency / 1000.0);
            double lossHealth = Math.max(0, 1.0 - averagePacketLoss * 10);
            double partitionHealth = activePartitions > 0 ? 0.5 : 1.0;
            double threatHealth = activeThreats > 0 ? 0.3 : 1.0;
            
            return (nodeHealth + latencyHealth + lossHealth + partitionHealth + threatHealth) / 5.0;
        }
    }
} 