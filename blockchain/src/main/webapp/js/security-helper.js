/**
 * 区块链安全管理辅助函数
 * 提供安全管理功能的API调用和工具函数
 */

// 安全管理API基础URL
const SECURITY_API_BASE = '/api/security';
const TRUST_API_BASE = '/api/trust';

/**
 * 安全管理API封装类
 */
class SecurityAPI {
    constructor() {
        this.baseURL = 'http://localhost:8080';
    }

    /**
     * 通用API调用方法
     */
    async call(endpoint, method = 'GET', data = null) {
        try {
            const options = {
                method: method,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Auth-Token': 'authenticated',
                    'X-Auth-Session': sessionStorage.getItem('authSession') || 'security_session',
                    'X-Auth-DID': sessionStorage.getItem('userDid') || 'system_admin'
                }
            };

            if (data && method !== 'GET') {
                options.body = JSON.stringify(data);
            }

            console.log(`[SecurityAPI] 调用: ${method} ${endpoint}`, data);
            
            const response = await fetch(`${this.baseURL}${endpoint}`, options);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();
            console.log(`[SecurityAPI] 响应: ${endpoint}`, result);
            
            return result;
        } catch (error) {
            console.error(`[SecurityAPI] 错误: ${endpoint}`, error);
            throw error;
        }
    }

    // 安全总览相关API
    async getSecurityOverview() {
        return await this.call(`${SECURITY_API_BASE}/overview`);
    }

    async initializeSecurityEnvironment(config = {}) {
        const defaultConfig = {
            networkCondition: 'NORMAL',
            initialNodes: [
                { nodeId: 'node1', publicKey: 'demo_key_1', nodeType: 'FULL_NODE' },
                { nodeId: 'node2', publicKey: 'demo_key_2', nodeType: 'VALIDATOR' },
                { nodeId: 'node3', publicKey: 'demo_key_3', nodeType: 'FULL_NODE' }
            ]
        };
        
        return await this.call(`${SECURITY_API_BASE}/initialize`, 'POST', { ...defaultConfig, ...config });
    }

    async performSecurityCheck() {
        return await this.call(`${SECURITY_API_BASE}/check`, 'POST');
    }

    async getSecurityRecommendations() {
        return await this.call(`${SECURITY_API_BASE}/recommendations`);
    }

    // 网络环境管理API
    async setNetworkCondition(condition) {
        return await this.call(`${SECURITY_API_BASE}/network/condition`, 'POST', { condition });
    }

    async simulateAttack(attackType, config = {}) {
        const attackConfig = {
            attackType: attackType,
            ...config
        };
        
        return await this.call(`${SECURITY_API_BASE}/simulation/attack`, 'POST', attackConfig);
    }

    async simulateDDoSAttack(targetNodes = ['node1', 'node2'], intensity = 3, duration = 5) {
        return await this.simulateAttack('DDOS', {
            targetNodes: targetNodes,
            intensity: intensity,
            duration: duration
        });
    }

    async simulateNetworkPartition(nodes = ['node1', 'node2', 'node3', 'node4'], duration = 10) {
        return await this.simulateAttack('PARTITION', {
            targetNodes: nodes,
            duration: duration
        });
    }

    async simulateByzantineNode(nodeId, byzantineType = 'RANDOM') {
        return await this.simulateAttack('BYZANTINE', {
            targetNodes: [nodeId],
            byzantineType: byzantineType
        });
    }

    // 节点信任管理API
    async registerNode(nodeId, publicKey = null) {
        if (!publicKey) {
            publicKey = 'generated_key_' + Date.now();
        }
        
        return await this.call(`${TRUST_API_BASE}/nodes/register`, 'POST', {
            nodeId: nodeId,
            publicKey: publicKey
        });
    }

    async getNodeTrust(nodeId) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}`);
    }

    async getTrustedNodes() {
        return await this.call(`${TRUST_API_BASE}/nodes/trusted`);
    }

    async getActiveNodes() {
        return await this.call(`${TRUST_API_BASE}/nodes/active`);
    }

    async getRiskNodes() {
        return await this.call(`${TRUST_API_BASE}/nodes/risk`);
    }

    async getNetworkTrustStatistics() {
        return await this.call(`${TRUST_API_BASE}/network/statistics`);
    }

    async recordNodeMining(nodeId) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/mining`, 'POST');
    }

    async recordValidTransaction(nodeId) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/transactions/valid`, 'POST');
    }

    async recordInvalidTransaction(nodeId) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/transactions/invalid`, 'POST');
    }

    async blacklistNode(nodeId, reason) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/blacklist`, 'POST', { reason });
    }

    async removeFromBlacklist(nodeId) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/blacklist`, 'DELETE');
    }

    async updateConsensusParticipation(nodeId, participationRate) {
        return await this.call(`${TRUST_API_BASE}/nodes/${nodeId}/consensus`, 'POST', {
            participationRate: participationRate.toString()
        });
    }

    // 匿名认证API
    async generateAnonymousCredential(issuerDid, userSecret, credentialSchema = 'VotingCredential', attributes = {}) {
        const defaultAttributes = {
            eligibility: 'voter',
            jurisdiction: 'district1'
        };
        
        return await this.call(`${SECURITY_API_BASE}/anonymous/credential`, 'POST', {
            issuerDid: issuerDid,
            userSecret: userSecret,
            credentialSchema: credentialSchema,
            attributes: { ...defaultAttributes, ...attributes }
        });
    }

    async verifyAnonymousCredential(challenge, credential) {
        return await this.call(`${SECURITY_API_BASE}/anonymous/verify`, 'POST', {
            challenge: challenge,
            credential: credential
        });
    }

    // 门限认证API
    async createThresholdGroup(groupId, threshold, totalParticipants) {
        return await this.call(`${SECURITY_API_BASE}/threshold/group`, 'POST', {
            groupId: groupId,
            threshold: threshold,
            totalParticipants: totalParticipants
        });
    }
}

/**
 * 安全事件日志管理器
 */
class SecurityLogger {
    constructor() {
        this.logs = [];
        this.maxLogs = 1000;
        this.listeners = [];
    }

    /**
     * 添加日志条目
     */
    log(level, message, details = '', category = 'general') {
        const timestamp = new Date();
        const logEntry = {
            id: Date.now() + Math.random(),
            timestamp: timestamp,
            level: level,
            message: message,
            details: details,
            category: category,
            formatted: this.formatLogEntry(timestamp, level, message, details)
        };

        this.logs.unshift(logEntry);
        
        // 保持日志数量限制
        if (this.logs.length > this.maxLogs) {
            this.logs = this.logs.slice(0, this.maxLogs);
        }

        // 通知监听器
        this.notifyListeners(logEntry);
        
        // 控制台输出
        console.log(`[Security] ${logEntry.formatted}`);
    }

    /**
     * 格式化日志条目
     */
    formatLogEntry(timestamp, level, message, details) {
        const timeStr = timestamp.toLocaleString('zh-CN');
        const levelStr = level.toUpperCase().padEnd(7);
        let formatted = `[${timeStr}] ${levelStr} ${message}`;
        
        if (details) {
            formatted += `\n                     详情: ${details}`;
        }
        
        return formatted;
    }

    /**
     * 添加日志监听器
     */
    addListener(callback) {
        this.listeners.push(callback);
    }

    /**
     * 移除日志监听器
     */
    removeListener(callback) {
        const index = this.listeners.indexOf(callback);
        if (index > -1) {
            this.listeners.splice(index, 1);
        }
    }

    /**
     * 通知所有监听器
     */
    notifyListeners(logEntry) {
        this.listeners.forEach(callback => {
            try {
                callback(logEntry);
            } catch (error) {
                console.error('日志监听器错误:', error);
            }
        });
    }

    /**
     * 获取过滤后的日志
     */
    getFilteredLogs(filter = {}) {
        let filteredLogs = [...this.logs];

        if (filter.level && filter.level !== 'all') {
            filteredLogs = filteredLogs.filter(log => log.level === filter.level);
        }

        if (filter.category && filter.category !== 'all') {
            filteredLogs = filteredLogs.filter(log => log.category === filter.category);
        }

        if (filter.search) {
            const searchTerm = filter.search.toLowerCase();
            filteredLogs = filteredLogs.filter(log => 
                log.message.toLowerCase().includes(searchTerm) ||
                log.details.toLowerCase().includes(searchTerm)
            );
        }

        if (filter.limit) {
            filteredLogs = filteredLogs.slice(0, filter.limit);
        }

        return filteredLogs;
    }

    /**
     * 清空日志
     */
    clear() {
        this.logs = [];
        this.notifyListeners({ type: 'clear' });
    }

    /**
     * 导出日志
     */
    export(format = 'text') {
        if (format === 'json') {
            return JSON.stringify(this.logs, null, 2);
        } else {
            return this.logs.map(log => log.formatted).join('\n');
        }
    }

    // 便捷方法
    info(message, details = '', category = 'general') {
        this.log('info', message, details, category);
    }

    warn(message, details = '', category = 'general') {
        this.log('warning', message, details, category);
    }

    error(message, details = '', category = 'general') {
        this.log('error', message, details, category);
    }

    success(message, details = '', category = 'general') {
        this.log('success', message, details, category);
    }
}

/**
 * 安全指标计算器
 */
class SecurityMetrics {
    static calculateNetworkHealth(stats) {
        if (!stats || stats.totalNodes === 0) return 0;
        
        const nodeHealth = stats.onlineNodes / stats.totalNodes;
        const latencyHealth = Math.max(0, 1 - stats.averageLatency / 1000);
        const lossHealth = Math.max(0, 1 - stats.averagePacketLoss * 10);
        const partitionHealth = stats.activePartitions > 0 ? 0.5 : 1.0;
        const threatHealth = stats.activeThreats > 0 ? 0.3 : 1.0;
        
        return (nodeHealth + latencyHealth + lossHealth + partitionHealth + threatHealth) / 5;
    }

    static calculateTrustScore(trustStats) {
        if (!trustStats || trustStats.totalNodes === 0) return 0;
        
        const trustRate = trustStats.trustedNodes / trustStats.totalNodes;
        const blacklistRate = trustStats.blacklistedNodes / trustStats.totalNodes;
        const avgTrust = trustStats.averageTrustScore || 0;
        
        return Math.max(0, (trustRate * 0.4 + (1 - blacklistRate) * 0.3 + avgTrust * 0.3));
    }

    static calculateOverallSecurity(networkHealth, trustScore) {
        return (networkHealth * 0.5 + trustScore * 0.5);
    }

    static getSecurityLevel(score) {
        if (score >= 0.9) return { level: 'excellent', label: '优秀', color: '#10b981' };
        if (score >= 0.8) return { level: 'good', label: '良好', color: '#3b82f6' };
        if (score >= 0.6) return { level: 'fair', label: '一般', color: '#f59e0b' };
        if (score >= 0.4) return { level: 'poor', label: '较差', color: '#ef4444' };
        return { level: 'critical', label: '危险', color: '#dc2626' };
    }

    static formatBytes(bytes, decimals = 2) {
        if (bytes === 0) return '0 B';
        
        const k = 1024;
        const dm = decimals < 0 ? 0 : decimals;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }

    static formatDuration(milliseconds) {
        const seconds = Math.floor(milliseconds / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);
        
        if (days > 0) return `${days}天 ${hours % 24}小时`;
        if (hours > 0) return `${hours}小时 ${minutes % 60}分钟`;
        if (minutes > 0) return `${minutes}分钟 ${seconds % 60}秒`;
        return `${seconds}秒`;
    }
}

/**
 * 安全模拟器
 */
class SecuritySimulator {
    constructor(api, logger) {
        this.api = api;
        this.logger = logger;
        this.activeSimulations = new Map();
    }

    async runDDoSSimulation(config = {}) {
        const simulationId = 'ddos_' + Date.now();
        const defaultConfig = {
            targetNodes: ['node1', 'node2'],
            intensity: 3,
            duration: 5,
            description: 'DDoS攻击模拟'
        };
        
        const simConfig = { ...defaultConfig, ...config };
        
        try {
            this.logger.warn(`开始${simConfig.description}`, 
                `目标节点: ${simConfig.targetNodes.join(', ')}, 强度: ${simConfig.intensity}, 持续时间: ${simConfig.duration}分钟`);
            
            const result = await this.api.simulateDDoSAttack(
                simConfig.targetNodes, 
                simConfig.intensity, 
                simConfig.duration
            );
            
            if (result.success) {
                this.activeSimulations.set(simulationId, {
                    type: 'ddos',
                    config: simConfig,
                    startTime: Date.now(),
                    endTime: Date.now() + simConfig.duration * 60 * 1000
                });
                
                this.logger.success(`${simConfig.description}启动成功`, `模拟ID: ${simulationId}`);
                
                // 设置自动结束
                setTimeout(() => {
                    this.endSimulation(simulationId);
                }, simConfig.duration * 60 * 1000);
            } else {
                this.logger.error(`${simConfig.description}启动失败`, result.message);
            }
            
            return { simulationId, result };
        } catch (error) {
            this.logger.error(`${simConfig.description}时发生错误`, error.message);
            throw error;
        }
    }

    async runNetworkPartitionSimulation(config = {}) {
        const simulationId = 'partition_' + Date.now();
        const defaultConfig = {
            nodes: ['node1', 'node2', 'node3', 'node4'],
            duration: 10,
            description: '网络分区模拟'
        };
        
        const simConfig = { ...defaultConfig, ...config };
        
        try {
            this.logger.warn(`开始${simConfig.description}`, 
                `分区节点数: ${simConfig.nodes.length}, 持续时间: ${simConfig.duration}分钟`);
            
            const result = await this.api.simulateNetworkPartition(simConfig.nodes, simConfig.duration);
            
            if (result.success) {
                this.activeSimulations.set(simulationId, {
                    type: 'partition',
                    config: simConfig,
                    startTime: Date.now(),
                    endTime: Date.now() + simConfig.duration * 60 * 1000
                });
                
                this.logger.success(`${simConfig.description}启动成功`, `模拟ID: ${simulationId}`);
                
                // 设置自动结束
                setTimeout(() => {
                    this.endSimulation(simulationId);
                }, simConfig.duration * 60 * 1000);
            } else {
                this.logger.error(`${simConfig.description}启动失败`, result.message);
            }
            
            return { simulationId, result };
        } catch (error) {
            this.logger.error(`${simConfig.description}时发生错误`, error.message);
            throw error;
        }
    }

    async runByzantineNodeSimulation(nodeId, byzantineType = 'RANDOM', config = {}) {
        const simulationId = 'byzantine_' + Date.now();
        const defaultConfig = {
            duration: 15,
            description: `拜占庭节点模拟 (${byzantineType})`
        };
        
        const simConfig = { ...defaultConfig, ...config };
        
        try {
            this.logger.warn(`开始${simConfig.description}`, 
                `目标节点: ${nodeId}, 行为类型: ${byzantineType}`);
            
            const result = await this.api.simulateByzantineNode(nodeId, byzantineType);
            
            if (result.success) {
                this.activeSimulations.set(simulationId, {
                    type: 'byzantine',
                    config: { ...simConfig, nodeId, byzantineType },
                    startTime: Date.now(),
                    endTime: Date.now() + simConfig.duration * 60 * 1000
                });
                
                this.logger.success(`${simConfig.description}启动成功`, `模拟ID: ${simulationId}`);
                
                // 设置自动结束
                setTimeout(() => {
                    this.endSimulation(simulationId);
                }, simConfig.duration * 60 * 1000);
            } else {
                this.logger.error(`${simConfig.description}启动失败`, result.message);
            }
            
            return { simulationId, result };
        } catch (error) {
            this.logger.error(`${simConfig.description}时发生错误`, error.message);
            throw error;
        }
    }

    endSimulation(simulationId) {
        const simulation = this.activeSimulations.get(simulationId);
        if (simulation) {
            this.activeSimulations.delete(simulationId);
            this.logger.info(`模拟结束: ${simulation.config.description}`, `模拟ID: ${simulationId}`);
        }
    }

    getActiveSimulations() {
        return Array.from(this.activeSimulations.entries()).map(([id, sim]) => ({
            id,
            ...sim,
            remainingTime: Math.max(0, sim.endTime - Date.now())
        }));
    }

    stopSimulation(simulationId) {
        this.endSimulation(simulationId);
        this.logger.warn(`手动停止模拟`, `模拟ID: ${simulationId}`);
    }

    stopAllSimulations() {
        const activeIds = Array.from(this.activeSimulations.keys());
        activeIds.forEach(id => this.stopSimulation(id));
        this.logger.info('已停止所有活跃模拟');
    }
}

// 全局实例
const securityAPI = new SecurityAPI();
const securityLogger = new SecurityLogger();
const securitySimulator = new SecuritySimulator(securityAPI, securityLogger);

// 导出到全局作用域
window.SecurityAPI = SecurityAPI;
window.SecurityLogger = SecurityLogger;
window.SecurityMetrics = SecurityMetrics;
window.SecuritySimulator = SecuritySimulator;
window.securityAPI = securityAPI;
window.securityLogger = securityLogger;
window.securitySimulator = securitySimulator;

// 初始化日志
securityLogger.info('安全管理系统已加载', '所有模块初始化完成');

// 导出模块
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        SecurityAPI,
        SecurityLogger,
        SecurityMetrics,
        SecuritySimulator,
        securityAPI,
        securityLogger,
        securitySimulator
    };
} 