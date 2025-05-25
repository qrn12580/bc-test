package com.bjut.blockchain.web.service;

import com.bjut.blockchain.web.entity.NodeTrustEntity;
import com.bjut.blockchain.web.repository.NodeTrustRepository;
import com.bjut.blockchain.web.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 节点信任评估服务类
 * 负责管理区块链网络中节点的信任度评估和管理
 */
@Service
@Transactional
public class NodeTrustService {

    private static final Logger logger = LoggerFactory.getLogger(NodeTrustService.class);

    @Autowired
    private NodeTrustRepository nodeTrustRepository;

    // 信任度阈值常量
    private static final BigDecimal TRUSTED_THRESHOLD = BigDecimal.valueOf(0.6);
    private static final BigDecimal HIGH_RISK_THRESHOLD = BigDecimal.valueOf(0.3);
    private static final BigDecimal BLACKLIST_THRESHOLD = BigDecimal.valueOf(0.1);

    /**
     * 注册新节点
     */
    public NodeTrustEntity registerNode(String nodeId, String publicKey) {
        logger.info("注册新节点: {}", nodeId);

        if (nodeTrustRepository.existsByNodeId(nodeId)) {
            throw new IllegalArgumentException("节点ID已存在: " + nodeId);
        }

        if (nodeTrustRepository.existsByPublicKey(publicKey)) {
            throw new IllegalArgumentException("公钥已注册");
        }

        NodeTrustEntity nodeTrust = new NodeTrustEntity();
        nodeTrust.setNodeId(nodeId);
        nodeTrust.setPublicKey(publicKey);
        nodeTrust.setLastActive(LocalDateTime.now());

        NodeTrustEntity savedNode = nodeTrustRepository.save(nodeTrust);
        logger.info("节点注册成功: {}, 初始信任度: {}", nodeId, savedNode.getTrustScore());

        return savedNode;
    }

    /**
     * 更新节点活跃状态
     */
    public void updateNodeActivity(String nodeId) {
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.updateLastActive();
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 记录节点成功挖矿
     */
    public void recordSuccessfulMining(String nodeId) {
        logger.debug("记录节点 {} 成功挖矿", nodeId);
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.incrementBlocksMined();
            node.updateLastActive();
            
            // 挖矿成功增加信任度
            improveTrustScore(node, BigDecimal.valueOf(0.01));
            
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 记录有效交易
     */
    public void recordValidTransaction(String nodeId) {
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.incrementValidTransactions();
            node.updateLastActive();
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 记录无效交易
     */
    public void recordInvalidTransaction(String nodeId) {
        logger.warn("记录节点 {} 的无效交易", nodeId);
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.incrementInvalidTransactions();
            node.updateLastActive();
            
            // 无效交易降低信任度
            decreaseTrustScore(node, BigDecimal.valueOf(0.05));
            
            // 检查是否需要加入黑名单
            checkForBlacklisting(node);
            
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 更新节点的共识参与率
     */
    public void updateConsensusParticipation(String nodeId, BigDecimal participationRate) {
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.setConsensusParticipationRate(participationRate);
            node.updateLastActive();
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 记录证书颁发
     */
    public void recordCertificateIssue(String nodeId) {
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.setCertificatesIssued(node.getCertificatesIssued() + 1);
            node.updateLastActive();
            
            // 证书颁发增加信任度
            improveTrustScore(node, BigDecimal.valueOf(0.005));
            
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 记录证书撤销
     */
    public void recordCertificateRevocation(String nodeId, String reason) {
        logger.warn("记录节点 {} 的证书撤销, 原因: {}", nodeId, reason);
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.setCertificateRevocations(node.getCertificateRevocations() + 1);
            node.updateLastActive();
            
            // 证书撤销降低信任度
            decreaseTrustScore(node, BigDecimal.valueOf(0.02));
            
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 获取所有可信节点
     */
    @Transactional(readOnly = true)
    public List<NodeTrustEntity> getTrustedNodes() {
        return nodeTrustRepository.findTrustedNodes(TRUSTED_THRESHOLD);
    }

    /**
     * 获取活跃节点（最近24小时内活跃）
     */
    @Transactional(readOnly = true)
    public List<NodeTrustEntity> getActiveNodes() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return nodeTrustRepository.findActiveNodes(since);
    }

    /**
     * 获取高风险节点
     */
    @Transactional(readOnly = true)
    public List<NodeTrustEntity> getRiskNodes() {
        LocalDateTime recentTime = LocalDateTime.now().minusHours(24);
        return nodeTrustRepository.findRiskNodes(HIGH_RISK_THRESHOLD, recentTime);
    }

    /**
     * 获取网络信任统计
     */
    @Transactional(readOnly = true)
    public NetworkTrustStatistics getNetworkTrustStatistics() {
        long totalNodes = nodeTrustRepository.count();
        long trustedNodes = nodeTrustRepository.countTrustedNodes(TRUSTED_THRESHOLD);
        long blacklistedNodes = nodeTrustRepository.findByIsBlacklistedTrue().size();
        BigDecimal averageTrust = nodeTrustRepository.getAverageTrustScore();
        
        if (averageTrust == null) {
            averageTrust = BigDecimal.ZERO;
        }

        return new NetworkTrustStatistics(totalNodes, trustedNodes, blacklistedNodes, averageTrust);
    }

    /**
     * 将节点加入黑名单
     */
    public void blacklistNode(String nodeId, String reason) {
        logger.warn("将节点 {} 加入黑名单, 原因: {}", nodeId, reason);
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.blacklist(reason);
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 将节点移出黑名单
     */
    public void removeFromBlacklist(String nodeId) {
        logger.info("将节点 {} 移出黑名单", nodeId);
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        if (nodeOpt.isPresent()) {
            NodeTrustEntity node = nodeOpt.get();
            node.removeFromBlacklist();
            nodeTrustRepository.save(node);
        }
    }

    /**
     * 获取节点信任信息
     */
    @Transactional(readOnly = true)
    public Optional<NodeTrustEntity> getNodeTrust(String nodeId) {
        return nodeTrustRepository.findByNodeId(nodeId);
    }

    /**
     * 检查节点是否可信
     */
    @Transactional(readOnly = true)
    public boolean isNodeTrusted(String nodeId) {
        Optional<NodeTrustEntity> nodeOpt = nodeTrustRepository.findByNodeId(nodeId);
        return nodeOpt.map(NodeTrustEntity::isTrusted).orElse(false);
    }

    /**
     * 提高节点信任度
     */
    private void improveTrustScore(NodeTrustEntity node, BigDecimal improvement) {
        BigDecimal newTrustScore = node.getTrustScore().add(improvement);
        node.setTrustScore(newTrustScore.min(BigDecimal.ONE));

        BigDecimal newReputationScore = node.getReputationScore().add(improvement.multiply(BigDecimal.valueOf(0.5)));
        node.setReputationScore(newReputationScore.min(BigDecimal.ONE));
    }

    /**
     * 降低节点信任度
     */
    private void decreaseTrustScore(NodeTrustEntity node, BigDecimal decrease) {
        BigDecimal newTrustScore = node.getTrustScore().subtract(decrease);
        node.setTrustScore(newTrustScore.max(BigDecimal.ZERO));

        BigDecimal newReputationScore = node.getReputationScore().subtract(decrease.multiply(BigDecimal.valueOf(0.8)));
        node.setReputationScore(newReputationScore.max(BigDecimal.ZERO));
    }

    /**
     * 检查节点是否需要加入黑名单
     */
    private void checkForBlacklisting(NodeTrustEntity node) {
        BigDecimal overallTrust = node.calculateOverallTrust();
        
        if (overallTrust.compareTo(BLACKLIST_THRESHOLD) < 0) {
            node.blacklist("信任度过低，自动加入黑名单");
            logger.warn("节点 {} 因信任度过低自动加入黑名单", node.getNodeId());
        }
        
        // 检查无效交易比例
        long totalTransactions = node.getValidTransactions() + node.getInvalidTransactions();
        if (totalTransactions > 10) {
            double invalidRate = (double) node.getInvalidTransactions() / totalTransactions;
            if (invalidRate > 0.5) {
                node.blacklist("无效交易比例过高");
                logger.warn("节点 {} 因无效交易比例过高自动加入黑名单", node.getNodeId());
            }
        }
    }

    /**
     * 定时任务：更新不活跃节点的信任度
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void updateInactiveNodesTrust() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<NodeTrustEntity> inactiveNodes = nodeTrustRepository.findInactiveNodes(threshold);
        
        for (NodeTrustEntity node : inactiveNodes) {
            // 不活跃节点信任度缓慢下降
            decreaseTrustScore(node, BigDecimal.valueOf(0.001));
            logger.debug("更新不活跃节点 {} 的信任度", node.getNodeId());
        }
        
        if (!inactiveNodes.isEmpty()) {
            nodeTrustRepository.saveAll(inactiveNodes);
            logger.info("更新了 {} 个不活跃节点的信任度", inactiveNodes.size());
        }
    }

    /**
     * 网络信任统计内部类
     */
    public static class NetworkTrustStatistics {
        private final long totalNodes;
        private final long trustedNodes;
        private final long blacklistedNodes;
        private final BigDecimal averageTrustScore;

        public NetworkTrustStatistics(long totalNodes, long trustedNodes, long blacklistedNodes, BigDecimal averageTrustScore) {
            this.totalNodes = totalNodes;
            this.trustedNodes = trustedNodes;
            this.blacklistedNodes = blacklistedNodes;
            this.averageTrustScore = averageTrustScore;
        }

        // Getters
        public long getTotalNodes() { return totalNodes; }
        public long getTrustedNodes() { return trustedNodes; }
        public long getBlacklistedNodes() { return blacklistedNodes; }
        public BigDecimal getAverageTrustScore() { return averageTrustScore; }
        public double getTrustRate() { 
            return totalNodes > 0 ? (double) trustedNodes / totalNodes : 0.0; 
        }
    }
} 