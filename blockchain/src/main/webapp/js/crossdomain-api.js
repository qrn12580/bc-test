/**
 * 跨域认证API工具类
 * 提供跨域认证相关的API调用方法
 */
class CrossDomainAPI {
    constructor(baseUrl = 'http://localhost:8080/api/crossdomain') {
        this.baseUrl = baseUrl;
    }

    /**
     * 域管理相关API
     */
    async registerDomain(domainData) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/domains/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(domainData)
        });
        return await response.json();
    }

    async getDomain(domainId) {
        const response = await fetch(`${this.baseUrl}/domains/${domainId}`);
        return await response.json();
    }

    async getActiveDomains() {
        const response = await fetch(`${this.baseUrl}/domains/active`);
        return await response.json();
    }

    async getDomainsByType(domainType) {
        const response = await fetch(`${this.baseUrl}/domains/type/${domainType}`);
        return await response.json();
    }

    async updateDomain(domainId, domainData) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/domains/${domainId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(domainData)
        });
        return await response.json();
    }

    async deactivateDomain(domainId) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/domains/${domainId}/deactivate`, {
            method: 'POST'
        });
        return await response.json();
    }

    async activateDomain(domainId) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/domains/${domainId}/activate`, {
            method: 'POST'
        });
        return await response.json();
    }

    async validateDomainCertificate(domainId) {
        const response = await fetch(`${this.baseUrl}/domains/${domainId}/validate-certificate`);
        return await response.json();
    }

    /**
     * 信任关系管理相关API
     */
    async establishTrust(trustData) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/trust/establish`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(trustData)
        });
        return await response.json();
    }

    async checkTrustRelation(sourceDomainId, targetDomainId) {
        const response = await fetch(
            `${this.baseUrl}/trust/check?sourceDomainId=${sourceDomainId}&targetDomainId=${targetDomainId}`
        );
        return await response.json();
    }

    async getValidTrustRelation(sourceDomainId, targetDomainId) {
        const response = await fetch(
            `${this.baseUrl}/trust/valid?sourceDomainId=${sourceDomainId}&targetDomainId=${targetDomainId}`
        );
        return await response.json();
    }

    async getDomainTrustRelations(domainId) {
        const response = await fetch(`${this.baseUrl}/trust/domain/${domainId}`);
        return await response.json();
    }

    async updateTrustLevel(sourceDomainId, targetDomainId, newTrustLevel) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/trust/trust-level`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                sourceDomainId,
                targetDomainId,
                newTrustLevel: newTrustLevel.toString()
            })
        });
        return await response.json();
    }

    async revokeTrust(sourceDomainId, targetDomainId) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/trust/revoke`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                sourceDomainId,
                targetDomainId
            })
        });
        return await response.json();
    }

    /**
     * 跨域认证相关API
     */
    async generateCrossDomainChallenge(sourceDomainId, targetDomainId, userId) {
        const response = await fetch(`${this.baseUrl}/auth/challenge`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                sourceDomainId,
                targetDomainId,
                userId
            })
        });
        return await response.json();
    }

    async verifyCrossDomainAuth(authRequest) {
        const response = await fetch(`${this.baseUrl}/auth/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(authRequest)
        });
        return await response.json();
    }

    async validateCrossDomainToken(tokenId, targetDomainId) {
        const response = await fetch(`${this.baseUrl}/auth/validate-token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                tokenId,
                targetDomainId
            })
        });
        return await response.json();
    }

    async getUserTokens(userId) {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/auth/tokens/${userId}`);
        return await response.json();
    }

    async cleanupExpiredTokens() {
        const response = await AuthHelper.fetchWithAuth(`${this.baseUrl}/auth/cleanup-expired`, {
            method: 'POST'
        });
        return await response.json();
    }

    /**
     * 工具方法
     */
    static generateKeyPair() {
        return window.crypto.subtle.generateKey(
            {
                name: "RSASSA-PKCS1-v1_5",
                modulusLength: 2048,
                publicExponent: new Uint8Array([0x01, 0x00, 0x01]),
                hash: "SHA-256",
            },
            true,
            ["sign", "verify"]
        );
    }

    static async exportPublicKey(publicKey) {
        const exported = await window.crypto.subtle.exportKey("spki", publicKey);
        return this.arrayBufferToBase64(exported);
    }

    static async signData(privateKey, data) {
        const encoder = new TextEncoder();
        const dataBuffer = encoder.encode(JSON.stringify(data));
        
        const signature = await window.crypto.subtle.sign(
            "RSASSA-PKCS1-v1_5",
            privateKey,
            dataBuffer
        );
        
        return this.arrayBufferToBase64(signature);
    }

    static arrayBufferToBase64(buffer) {
        let binary = '';
        const bytes = new Uint8Array(buffer);
        for (let i = 0; i < bytes.byteLength; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return window.btoa(binary);
    }

    static generateNonce() {
        const array = new Uint8Array(16);
        window.crypto.getRandomValues(array);
        return Array.from(array, byte => byte.toString(16).padStart(2, '0')).join('');
    }

    /**
     * 创建完整的跨域认证请求
     */
    async createCrossDomainAuthRequest(userId, sourceDomainId, targetDomainId, privateKey, authToken) {
        const nonce = CrossDomainAPI.generateNonce();
        const timestamp = Date.now();

        const authRequest = {
            userId,
            sourceDomainId,
            targetDomainId,
            authToken,
            nonce,
            timestamp
        };

        // 生成签名
        const signature = await CrossDomainAPI.signData(privateKey, authRequest);
        authRequest.signature = signature;

        return authRequest;
    }
}

// 导出全局实例
window.CrossDomainAPI = CrossDomainAPI;
window.crossDomainAPI = new CrossDomainAPI(); 