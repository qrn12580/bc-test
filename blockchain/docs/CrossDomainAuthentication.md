# 跨域认证系统使用说明

## 概述

跨域认证系统是区块链网络中一个重要的安全功能，它允许用户在不同的认证域之间进行身份验证和授权。该系统支持多种认证场景，包括联邦认证、委托认证等，并提供完善的信任管理机制。

## 核心概念

### 1. 认证域 (Domain)

认证域是独立的身份认证和授权区域，每个域具有以下特征：
- **域ID**: 唯一标识符
- **域名**: 人类可读的名称
- **端点URL**: 域的服务端点
- **公钥**: 用于验证域签名的公钥
- **证书**: 可选的X.509证书
- **域类型**: BLOCKCHAIN（区块链）、TRADITIONAL（传统系统）、HYBRID（混合系统）

### 2. 域间信任关系 (Trust Relationship)

域间信任关系定义了两个域之间的信任程度和类型：
- **信任级别**: 1-10的数值，10为最高信任
- **信任类型**: UNIDIRECTIONAL（单向）或BIDIRECTIONAL（双向）
- **有效期**: 可设置信任关系的时间范围
- **共享密钥**: 可选的额外安全机制

### 3. 跨域认证令牌 (Cross-Domain Token)

跨域认证令牌用于在域间传递认证信息：
- **联邦令牌**: 用于联邦认证场景
- **委托令牌**: 用于权限委托
- **断言令牌**: 用于身份断言

## 功能特性

### ✅ 已实现的功能

1. **域管理**
   - 域注册和更新
   - 域激活和停用
   - 证书验证
   - 公钥管理

2. **信任关系管理**
   - 建立域间信任
   - 更新信任级别
   - 撤销信任关系
   - 信任关系查询

3. **跨域认证**
   - 挑战-响应认证
   - 数字签名验证
   - 防重放攻击
   - 令牌生成和验证

4. **安全机制**
   - 基于RSA的数字签名
   - 时间戳验证
   - 随机数（nonce）防重放
   - 证书链验证

## 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   域 A (源域)    │    │  跨域认证服务    │    │   域 B (目标域)  │
│                 │    │                 │    │                 │
│ 1. 生成挑战码   │◄──►│ 2. 验证信任关系 │◄──►│ 4. 验证令牌     │
│ 3. 签名认证请求 │    │ 5. 生成认证令牌 │    │ 6. 授权访问     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 使用流程

### 1. 域注册

首先需要注册参与跨域认证的各个域：

```json
{
  "domainId": "blockchain-domain-1",
  "domainName": "区块链域1",
  "description": "主要的区块链认证域",
  "endpointUrl": "https://domain1.blockchain.com/api",
  "publicKey": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMI...",
  "domainType": "BLOCKCHAIN",
  "networkId": "main-network"
}
```

### 2. 建立信任关系

在域之间建立信任关系：

```json
{
  "sourceDomainId": "blockchain-domain-1",
  "targetDomainId": "traditional-domain-1",
  "trustLevel": 8,
  "trustType": "BIDIRECTIONAL",
  "sharedSecret": "optional-shared-secret"
}
```

### 3. 发起跨域认证

用户在源域发起跨域认证请求：

1. **生成挑战码**
   ```javascript
   const challenge = await crossDomainAPI.generateCrossDomainChallenge(
     'blockchain-domain-1',
     'traditional-domain-1', 
     'user123'
   );
   ```

2. **创建认证请求**
   ```javascript
   const authRequest = await crossDomainAPI.createCrossDomainAuthRequest(
     'user123',
     'blockchain-domain-1',
     'traditional-domain-1',
     privateKey,
     'auth-token-from-source-domain'
   );
   ```

3. **验证认证请求**
   ```javascript
   const result = await crossDomainAPI.verifyCrossDomainAuth(authRequest);
   ```

4. **使用认证令牌**
   ```javascript
   const validation = await crossDomainAPI.validateCrossDomainToken(
     result.token,
     'traditional-domain-1'
   );
   ```

## API 接口

### 域管理 API

- `POST /api/crossdomain/domains/register` - 注册新域
- `GET /api/crossdomain/domains/{domainId}` - 获取域信息
- `GET /api/crossdomain/domains/active` - 获取所有活跃域
- `PUT /api/crossdomain/domains/{domainId}` - 更新域信息
- `POST /api/crossdomain/domains/{domainId}/activate` - 激活域
- `POST /api/crossdomain/domains/{domainId}/deactivate` - 停用域

### 信任关系 API

- `POST /api/crossdomain/trust/establish` - 建立信任关系
- `GET /api/crossdomain/trust/check` - 检查信任关系
- `GET /api/crossdomain/trust/valid` - 获取有效信任关系
- `GET /api/crossdomain/trust/domain/{domainId}` - 获取域的信任关系
- `PUT /api/crossdomain/trust/trust-level` - 更新信任级别
- `POST /api/crossdomain/trust/revoke` - 撤销信任关系

### 跨域认证 API

- `POST /api/crossdomain/auth/challenge` - 生成认证挑战码
- `POST /api/crossdomain/auth/verify` - 验证认证请求
- `POST /api/crossdomain/auth/validate-token` - 验证认证令牌
- `GET /api/crossdomain/auth/tokens/{userId}` - 获取用户令牌
- `POST /api/crossdomain/auth/cleanup-expired` - 清理过期令牌

## 前端使用

### 访问跨域认证界面

1. 登录到区块链系统
2. 在主页面点击"8. 跨域认证系统"按钮
3. 或直接访问 `crossdomain.html` 页面

### 界面功能

1. **域管理标签页**
   - 查看所有已注册的域
   - 注册新域
   - 验证域证书

2. **信任关系标签页**
   - 建立域间信任关系
   - 查看和管理现有信任关系

3. **跨域认证标签页**
   - 发起跨域认证
   - 查看认证结果

## 安全考虑

### 1. 密钥管理
- 域的私钥必须安全存储
- 定期轮换密钥
- 使用硬件安全模块（HSM）保护关键密钥

### 2. 证书验证
- 验证域证书的有效性
- 检查证书链的完整性
- 监控证书吊销列表（CRL）

### 3. 防重放攻击
- 使用时间戳验证请求时效性
- 实现随机数（nonce）机制
- 记录已使用的令牌

### 4. 信任管理
- 定期审核域间信任关系
- 设置合理的信任级别
- 及时撤销不再需要的信任关系

## 错误处理

### 常见错误及解决方案

1. **域不存在**
   - 错误: "源域不存在或未激活"
   - 解决: 确认域已正确注册并处于活跃状态

2. **信任关系不存在**
   - 错误: "域间不存在信任关系"
   - 解决: 建立域间信任关系

3. **签名验证失败**
   - 错误: "认证令牌签名验证失败"
   - 解决: 检查私钥和签名算法

4. **令牌过期**
   - 错误: "令牌不存在或已过期"
   - 解决: 重新申请认证令牌

## 监控和日志

### 日志记录
系统记录以下事件：
- 域注册和更新
- 信任关系建立和撤销
- 跨域认证请求
- 令牌生成和验证
- 安全事件和错误

### 监控指标
- 跨域认证成功率
- 平均认证时间
- 令牌使用情况
- 安全事件频率

## 最佳实践

1. **域设计**
   - 使用有意义的域名
   - 设置适当的域类型
   - 定期更新域信息

2. **信任管理**
   - 基于实际业务需求设置信任级别
   - 定期审核信任关系
   - 使用最小权限原则

3. **认证流程**
   - 实现完整的错误处理
   - 设置合理的超时时间
   - 记录审计日志

4. **安全加固**
   - 使用HTTPS通信
   - 实现速率限制
   - 监控异常活动

## 扩展和集成

### 与现有系统集成
- DID身份系统集成
- CA证书管理系统集成
- 区块链网络集成

### 未来扩展方向
- 支持更多签名算法
- 实现零知识证明
- 添加生物识别认证
- 支持联邦身份标准（SAML、OpenID Connect）

## 故障排除

### 调试工具
1. 浏览器开发者工具
2. 服务器日志
3. 网络抓包工具

### 常见问题
1. 跨域CORS问题
2. 时间同步问题
3. 证书验证问题
4. 网络连接问题

详细的故障排除指南请参考技术文档。 