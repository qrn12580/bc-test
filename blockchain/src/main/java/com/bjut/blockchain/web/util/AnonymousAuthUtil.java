package com.bjut.blockchain.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 匿名认证工具类
 * 实现基于零知识证明的匿名认证机制
 */
public class AnonymousAuthUtil {

    private static final Logger logger = LoggerFactory.getLogger(AnonymousAuthUtil.class);
    private static final SecureRandom random = new SecureRandom();

    // 椭圆曲线参数（简化版本，用于演示）
    private static final BigInteger P = new BigInteger("2147483647"); // 素数
    private static final BigInteger G = new BigInteger("2"); // 生成元
    private static final BigInteger Q = P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(2)); // 阶

    /**
     * 生成承诺值
     * Commitment = g^value * h^blinding mod p
     */
    public static CommitmentData generateCommitment(String value, String blindingFactor) {
        try {
            BigInteger val = new BigInteger(sha256Hash(value), 16);
            BigInteger blind = blindingFactor != null ? 
                new BigInteger(blindingFactor, 16) : 
                new BigInteger(256, random);

            // 计算承诺值 g^val * h^blind mod p
            BigInteger commitment = G.modPow(val, P)
                .multiply(G.add(BigInteger.ONE).modPow(blind, P))
                .mod(P);

            return new CommitmentData(
                commitment.toString(16),
                blind.toString(16),
                val.toString(16)
            );
        } catch (Exception e) {
            logger.error("生成承诺值失败", e);
            throw new RuntimeException("生成承诺值失败", e);
        }
    }

    /**
     * 验证承诺值
     */
    public static boolean verifyCommitment(String commitmentValue, String originalValue, String blindingFactor) {
        try {
            CommitmentData newCommitment = generateCommitment(originalValue, blindingFactor);
            return commitmentValue.equals(newCommitment.getCommitmentValue());
        } catch (Exception e) {
            logger.error("验证承诺值失败", e);
            return false;
        }
    }

    /**
     * 生成零知识证明
     * 证明用户知道某个值，但不暴露该值
     */
    public static ZKProofData generateZKProof(String secret, String challenge) {
        try {
            BigInteger s = new BigInteger(sha256Hash(secret), 16);
            BigInteger c = new BigInteger(sha256Hash(challenge), 16);
            BigInteger r = new BigInteger(256, random);

            // Schnorr签名协议
            // t = g^r mod p
            BigInteger t = G.modPow(r, P);

            // e = H(g^s || t || challenge)
            String hashInput = G.modPow(s, P).toString(16) + t.toString(16) + challenge;
            BigInteger e = new BigInteger(sha256Hash(hashInput), 16);

            // z = r + e*s mod q
            BigInteger z = r.add(e.multiply(s)).mod(Q);

            return new ZKProofData(
                t.toString(16),
                e.toString(16),
                z.toString(16),
                G.modPow(s, P).toString(16) // 公钥
            );
        } catch (Exception ex) {
            logger.error("生成零知识证明失败", ex);
            throw new RuntimeException("生成零知识证明失败", ex);
        }
    }

    /**
     * 验证零知识证明
     */
    public static boolean verifyZKProof(ZKProofData proof, String challenge, String publicKey) {
        try {
            BigInteger t = new BigInteger(proof.getT(), 16);
            BigInteger e = new BigInteger(proof.getE(), 16);
            BigInteger z = new BigInteger(proof.getZ(), 16);
            BigInteger pk = new BigInteger(publicKey, 16);

            // 验证 g^z = t * pk^e mod p
            BigInteger left = G.modPow(z, P);
            BigInteger right = t.multiply(pk.modPow(e, P)).mod(P);

            // 验证挑战值
            String hashInput = pk.toString(16) + t.toString(16) + challenge;
            BigInteger expectedE = new BigInteger(sha256Hash(hashInput), 16);

            return left.equals(right) && e.equals(expectedE);
        } catch (Exception ex) {
            logger.error("验证零知识证明失败", ex);
            return false;
        }
    }

    /**
     * 生成假名（pseudonym）
     * 使用用户密钥和随机数生成一个不可关联的假名
     */
    public static String generatePseudonym(String userSecret, String randomSeed) {
        try {
            String input = userSecret + randomSeed;
            String hash = sha256Hash(input);
            // 使用Base64编码使其更适合存储
            return Base64.getEncoder().encodeToString(hash.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("生成假名失败", e);
            throw new RuntimeException("生成假名失败", e);
        }
    }

    /**
     * 生成匿名凭证
     */
    public static AnonymousCredentialData generateAnonymousCredential(
            String issuerDid, 
            String userSecret, 
            String credentialSchema,
            Map<String, String> attributes) {
        try {
            // 生成假名
            String randomSeed = generateRandomString(32);
            String pseudonym = generatePseudonym(userSecret, randomSeed);

            // 生成属性承诺
            Map<String, String> attributeCommitments = new HashMap<>();
            String blindingFactor = generateRandomString(64);

            for (Map.Entry<String, String> attr : attributes.entrySet()) {
                CommitmentData commitment = generateCommitment(attr.getValue(), blindingFactor);
                attributeCommitments.put(attr.getKey(), commitment.getCommitmentValue());
            }

            // 生成证明数据
            String challenge = sha256Hash(pseudonym + credentialSchema + System.currentTimeMillis());
            ZKProofData proof = generateZKProof(userSecret, challenge);

            return new AnonymousCredentialData(
                generateRandomString(32), // credentialId
                issuerDid,
                pseudonym,
                credentialSchema,
                attributeCommitments,
                proof,
                blindingFactor,
                randomSeed
            );
        } catch (Exception e) {
            logger.error("生成匿名凭证失败", e);
            throw new RuntimeException("生成匿名凭证失败", e);
        }
    }

    /**
     * 验证匿名凭证
     */
    public static boolean verifyAnonymousCredential(AnonymousCredentialData credential, String challenge) {
        try {
            // 验证零知识证明
            return verifyZKProof(credential.getProof(), challenge, credential.getProof().getPublicKey());
        } catch (Exception e) {
            logger.error("验证匿名凭证失败", e);
            return false;
        }
    }

    /**
     * 生成挑战值
     */
    public static String generateChallenge() {
        return generateRandomString(32);
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
        return Base64.getEncoder().encodeToString(bytes).substring(0, length);
    }

    /**
     * 承诺数据内部类
     */
    public static class CommitmentData {
        private final String commitmentValue;
        private final String blindingFactor;
        private final String hashedValue;

        public CommitmentData(String commitmentValue, String blindingFactor, String hashedValue) {
            this.commitmentValue = commitmentValue;
            this.blindingFactor = blindingFactor;
            this.hashedValue = hashedValue;
        }

        public String getCommitmentValue() { return commitmentValue; }
        public String getBlindingFactor() { return blindingFactor; }
        public String getHashedValue() { return hashedValue; }
    }

    /**
     * 零知识证明数据内部类
     */
    public static class ZKProofData {
        private final String t;
        private final String e;
        private final String z;
        private final String publicKey;

        public ZKProofData(String t, String e, String z, String publicKey) {
            this.t = t;
            this.e = e;
            this.z = z;
            this.publicKey = publicKey;
        }

        public String getT() { return t; }
        public String getE() { return e; }
        public String getZ() { return z; }
        public String getPublicKey() { return publicKey; }
    }

    /**
     * 匿名凭证数据内部类
     */
    public static class AnonymousCredentialData {
        private final String credentialId;
        private final String issuerDid;
        private final String pseudonym;
        private final String credentialSchema;
        private final Map<String, String> attributeCommitments;
        private final ZKProofData proof;
        private final String blindingFactor;
        private final String randomSeed;

        public AnonymousCredentialData(String credentialId, String issuerDid, String pseudonym, 
                                     String credentialSchema, Map<String, String> attributeCommitments,
                                     ZKProofData proof, String blindingFactor, String randomSeed) {
            this.credentialId = credentialId;
            this.issuerDid = issuerDid;
            this.pseudonym = pseudonym;
            this.credentialSchema = credentialSchema;
            this.attributeCommitments = attributeCommitments;
            this.proof = proof;
            this.blindingFactor = blindingFactor;
            this.randomSeed = randomSeed;
        }

        // Getters
        public String getCredentialId() { return credentialId; }
        public String getIssuerDid() { return issuerDid; }
        public String getPseudonym() { return pseudonym; }
        public String getCredentialSchema() { return credentialSchema; }
        public Map<String, String> getAttributeCommitments() { return attributeCommitments; }
        public ZKProofData getProof() { return proof; }
        public String getBlindingFactor() { return blindingFactor; }
        public String getRandomSeed() { return randomSeed; }
    }
} 