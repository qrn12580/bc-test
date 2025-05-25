package com.bjut.blockchain.web.util;

import com.codahale.shamir.Scheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * 门限认证工具类
 * 实现门限签名、秘密共享和多方认证功能
 */
public class ThresholdAuthUtil {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdAuthUtil.class);
    private static final SecureRandom random = new SecureRandom();

    // 大素数参数
    private static final BigInteger P = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639747");
    private static final BigInteger G = new BigInteger("2");

    /**
     * 生成门限签名配置
     */
    public static ThresholdConfig generateThresholdConfig(int threshold, int totalParticipants, String groupId) {
        try {
            if (threshold <= 0 || threshold > totalParticipants) {
                throw new IllegalArgumentException("门限值必须在1到总参与者数量之间");
            }

            // 生成主密钥
            BigInteger masterSecret = new BigInteger(256, random);
            
            // 使用Shamir秘密共享算法分割密钥
            Scheme scheme = new Scheme(random, totalParticipants, threshold);
            byte[] secretBytes = masterSecret.toByteArray();
            Map<Integer, byte[]> shares = scheme.split(secretBytes);

            // 生成群组公钥
            BigInteger groupPublicKey = G.modPow(masterSecret, P);

            // 为每个参与者生成密钥对
            List<ParticipantInfo> participants = new ArrayList<>();
            for (int i = 1; i <= totalParticipants; i++) {
                BigInteger privateKeyShare = new BigInteger(shares.get(i));
                BigInteger publicKeyShare = G.modPow(privateKeyShare, P);
                
                participants.add(new ParticipantInfo(
                    i,
                    "participant_" + groupId + "_" + i,
                    Base64.getEncoder().encodeToString(shares.get(i)),
                    publicKeyShare.toString(16)
                ));
            }

            return new ThresholdConfig(
                groupId,
                threshold,
                totalParticipants,
                participants,
                groupPublicKey.toString(16),
                masterSecret.toString(16)
            );

        } catch (Exception e) {
            logger.error("生成门限配置失败", e);
            throw new RuntimeException("生成门限配置失败", e);
        }
    }

    /**
     * 门限签名 - 第一阶段：生成部分签名
     */
    public static PartialSignature generatePartialSignature(String message, String privateKeyShare, int participantId) {
        try {
            // 生成随机数k
            BigInteger k = new BigInteger(256, random);
            
            // 计算 r = g^k mod p
            BigInteger r = G.modPow(k, P);
            
            // 计算消息哈希
            String messageHash = sha256Hash(message);
            BigInteger e = new BigInteger(messageHash, 16);
            
            // 计算部分签名 s_i = k + e * x_i mod (p-1)
            BigInteger privateKey = new BigInteger(privateKeyShare, 16);
            BigInteger s = k.add(e.multiply(privateKey)).mod(P.subtract(BigInteger.ONE));
            
            return new PartialSignature(
                participantId,
                r.toString(16),
                s.toString(16),
                messageHash
            );
            
        } catch (Exception e) {
            logger.error("生成部分签名失败", e);
            throw new RuntimeException("生成部分签名失败", e);
        }
    }

    /**
     * 门限签名 - 第二阶段：聚合部分签名
     */
    public static ThresholdSignature aggregatePartialSignatures(
            List<PartialSignature> partialSignatures, 
            ThresholdConfig config,
            String message) {
        try {
            if (partialSignatures.size() < config.getThreshold()) {
                throw new IllegalArgumentException("部分签名数量不足，需要至少" + config.getThreshold() + "个");
            }

            // 验证消息哈希一致性
            String expectedHash = sha256Hash(message);
            for (PartialSignature ps : partialSignatures) {
                if (!expectedHash.equals(ps.getMessageHash())) {
                    throw new IllegalArgumentException("消息哈希不一致");
                }
            }

            // 选择前threshold个签名进行聚合
            List<PartialSignature> selectedSignatures = partialSignatures.subList(0, config.getThreshold());

            // 聚合r值（取第一个，因为应该都相同）
            BigInteger r = new BigInteger(selectedSignatures.get(0).getR(), 16);

            // 使用拉格朗日插值聚合s值
            BigInteger aggregatedS = BigInteger.ZERO;
            for (int i = 0; i < selectedSignatures.size(); i++) {
                PartialSignature ps = selectedSignatures.get(i);
                BigInteger s = new BigInteger(ps.getS(), 16);
                
                // 计算拉格朗日系数
                BigInteger coefficient = calculateLagrangeCoefficient(
                    ps.getParticipantId(), 
                    selectedSignatures,
                    P.subtract(BigInteger.ONE)
                );
                
                aggregatedS = aggregatedS.add(s.multiply(coefficient)).mod(P.subtract(BigInteger.ONE));
            }

            return new ThresholdSignature(
                r.toString(16),
                aggregatedS.toString(16),
                expectedHash,
                selectedSignatures.size(),
                config.getGroupId()
            );

        } catch (Exception e) {
            logger.error("聚合部分签名失败", e);
            throw new RuntimeException("聚合部分签名失败", e);
        }
    }

    /**
     * 验证门限签名
     */
    public static boolean verifyThresholdSignature(ThresholdSignature signature, String message, String groupPublicKey) {
        try {
            // 验证消息哈希
            String expectedHash = sha256Hash(message);
            if (!expectedHash.equals(signature.getMessageHash())) {
                return false;
            }

            BigInteger r = new BigInteger(signature.getR(), 16);
            BigInteger s = new BigInteger(signature.getS(), 16);
            BigInteger publicKey = new BigInteger(groupPublicKey, 16);
            BigInteger e = new BigInteger(expectedHash, 16);

            // 验证 g^s = r * publicKey^e mod p
            BigInteger left = G.modPow(s, P);
            BigInteger right = r.multiply(publicKey.modPow(e, P)).mod(P);

            return left.equals(right);

        } catch (Exception e) {
            logger.error("验证门限签名失败", e);
            return false;
        }
    }

    /**
     * 门限密钥恢复
     */
    public static String recoverSecret(Map<Integer, String> keyShares, int threshold) {
        try {
            if (keyShares.size() < threshold) {
                throw new IllegalArgumentException("密钥分片数量不足");
            }

            // 使用Shamir秘密共享进行恢复
            Scheme scheme = new Scheme(random, keyShares.size(), threshold);
            Map<Integer, byte[]> shares = new HashMap<>();
            
            for (Map.Entry<Integer, String> entry : keyShares.entrySet()) {
                shares.put(entry.getKey(), Base64.getDecoder().decode(entry.getValue()));
            }

            byte[] recoveredSecret = scheme.join(shares);
            return Base64.getEncoder().encodeToString(recoveredSecret);

        } catch (Exception e) {
            logger.error("恢复密钥失败", e);
            throw new RuntimeException("恢复密钥失败", e);
        }
    }

    /**
     * 生成多方认证挑战
     */
    public static MultiPartyChallenge generateMultiPartyChallenge(
            String authGroupId, 
            List<String> requiredParticipants,
            String operation,
            long validityMinutes) {
        try {
            String challengeId = UUID.randomUUID().toString();
            String challengeData = generateRandomString(32);
            
            return new MultiPartyChallenge(
                challengeId,
                authGroupId,
                requiredParticipants,
                operation,
                challengeData,
                System.currentTimeMillis(),
                validityMinutes * 60 * 1000 // 转换为毫秒
            );

        } catch (Exception e) {
            logger.error("生成多方认证挑战失败", e);
            throw new RuntimeException("生成多方认证挑战失败", e);
        }
    }

    /**
     * 验证多方认证响应
     */
    public static boolean verifyMultiPartyResponse(
            MultiPartyChallenge challenge, 
            Map<String, String> responses,
            ThresholdConfig config) {
        try {
            // 检查挑战是否过期
            if (System.currentTimeMillis() > challenge.getCreatedTime() + challenge.getValidityDuration()) {
                logger.warn("多方认证挑战已过期");
                return false;
            }

            // 检查响应数量是否足够
            if (responses.size() < config.getThreshold()) {
                logger.warn("响应数量不足: {} < {}", responses.size(), config.getThreshold());
                return false;
            }

            // 验证每个响应
            for (Map.Entry<String, String> response : responses.entrySet()) {
                String participantId = response.getKey();
                String signature = response.getValue();
                
                // 查找参与者信息
                Optional<ParticipantInfo> participant = config.getParticipants().stream()
                    .filter(p -> p.getParticipantId().equals(participantId))
                    .findFirst();
                
                if (!participant.isPresent()) {
                    logger.warn("未找到参与者: {}", participantId);
                    return false;
                }

                // 验证签名（简化验证）
                String expectedSignature = sha256Hash(challenge.getChallengeData() + participantId);
                if (!expectedSignature.equals(signature)) {
                    logger.warn("参与者 {} 的签名验证失败", participantId);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            logger.error("验证多方认证响应失败", e);
            return false;
        }
    }

    /**
     * 计算拉格朗日插值系数
     */
    private static BigInteger calculateLagrangeCoefficient(int xi, List<PartialSignature> signatures, BigInteger modulus) {
        BigInteger result = BigInteger.ONE;
        
        for (PartialSignature sig : signatures) {
            int xj = sig.getParticipantId();
            if (xi != xj) {
                BigInteger numerator = BigInteger.valueOf(-xj);
                BigInteger denominator = BigInteger.valueOf(xi - xj);
                BigInteger fraction = numerator.multiply(denominator.modInverse(modulus)).mod(modulus);
                result = result.multiply(fraction).mod(modulus);
            }
        }
        
        return result;
    }

    /**
     * SHA256哈希
     */
    private static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 生成随机字符串
     */
    private static String generateRandomString(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes).substring(0, Math.min(length, Base64.getEncoder().encodeToString(bytes).length()));
    }

    // 内部类定义
    public static class ThresholdConfig {
        private final String groupId;
        private final int threshold;
        private final int totalParticipants;
        private final List<ParticipantInfo> participants;
        private final String groupPublicKey;
        private final String masterSecret;

        public ThresholdConfig(String groupId, int threshold, int totalParticipants, 
                             List<ParticipantInfo> participants, String groupPublicKey, String masterSecret) {
            this.groupId = groupId;
            this.threshold = threshold;
            this.totalParticipants = totalParticipants;
            this.participants = participants;
            this.groupPublicKey = groupPublicKey;
            this.masterSecret = masterSecret;
        }

        // Getters
        public String getGroupId() { return groupId; }
        public int getThreshold() { return threshold; }
        public int getTotalParticipants() { return totalParticipants; }
        public List<ParticipantInfo> getParticipants() { return participants; }
        public String getGroupPublicKey() { return groupPublicKey; }
        public String getMasterSecret() { return masterSecret; }
    }

    public static class ParticipantInfo {
        private final int index;
        private final String participantId;
        private final String privateKeyShare;
        private final String publicKeyShare;

        public ParticipantInfo(int index, String participantId, String privateKeyShare, String publicKeyShare) {
            this.index = index;
            this.participantId = participantId;
            this.privateKeyShare = privateKeyShare;
            this.publicKeyShare = publicKeyShare;
        }

        // Getters
        public int getIndex() { return index; }
        public String getParticipantId() { return participantId; }
        public String getPrivateKeyShare() { return privateKeyShare; }
        public String getPublicKeyShare() { return publicKeyShare; }
    }

    public static class PartialSignature {
        private final int participantId;
        private final String r;
        private final String s;
        private final String messageHash;

        public PartialSignature(int participantId, String r, String s, String messageHash) {
            this.participantId = participantId;
            this.r = r;
            this.s = s;
            this.messageHash = messageHash;
        }

        // Getters
        public int getParticipantId() { return participantId; }
        public String getR() { return r; }
        public String getS() { return s; }
        public String getMessageHash() { return messageHash; }
    }

    public static class ThresholdSignature {
        private final String r;
        private final String s;
        private final String messageHash;
        private final int signerCount;
        private final String groupId;

        public ThresholdSignature(String r, String s, String messageHash, int signerCount, String groupId) {
            this.r = r;
            this.s = s;
            this.messageHash = messageHash;
            this.signerCount = signerCount;
            this.groupId = groupId;
        }

        // Getters
        public String getR() { return r; }
        public String getS() { return s; }
        public String getMessageHash() { return messageHash; }
        public int getSignerCount() { return signerCount; }
        public String getGroupId() { return groupId; }
    }

    public static class MultiPartyChallenge {
        private final String challengeId;
        private final String authGroupId;
        private final List<String> requiredParticipants;
        private final String operation;
        private final String challengeData;
        private final long createdTime;
        private final long validityDuration;

        public MultiPartyChallenge(String challengeId, String authGroupId, List<String> requiredParticipants,
                                 String operation, String challengeData, long createdTime, long validityDuration) {
            this.challengeId = challengeId;
            this.authGroupId = authGroupId;
            this.requiredParticipants = requiredParticipants;
            this.operation = operation;
            this.challengeData = challengeData;
            this.createdTime = createdTime;
            this.validityDuration = validityDuration;
        }

        // Getters
        public String getChallengeId() { return challengeId; }
        public String getAuthGroupId() { return authGroupId; }
        public List<String> getRequiredParticipants() { return requiredParticipants; }
        public String getOperation() { return operation; }
        public String getChallengeData() { return challengeData; }
        public long getCreatedTime() { return createdTime; }
        public long getValidityDuration() { return validityDuration; }
    }
} 