package com.bjut.blockchain.crossdomain.service;

import com.bjut.blockchain.crossdomain.dto.DomainRegistrationRequest;
import com.bjut.blockchain.crossdomain.entity.DomainEntity;
import com.bjut.blockchain.crossdomain.repository.DomainRepository;
import com.bjut.blockchain.web.util.CertificateValidator;
import com.bjut.blockchain.web.util.CommonUtil;
import com.bjut.blockchain.web.util.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 域管理服务类
 * 负责域的注册、验证、管理等功能
 */
@Service
@Transactional
public class DomainService {

    private static final Logger logger = LoggerFactory.getLogger(DomainService.class);

    @Autowired
    private DomainRepository domainRepository;

    /**
     * 注册新域
     */
    public DomainEntity registerDomain(DomainRegistrationRequest request) {
        logger.info("注册新域: {}", request.getDomainId());

        // 验证域ID和域名是否已存在
        if (domainRepository.existsByDomainName(request.getDomainName())) {
            throw new IllegalArgumentException("域名已存在: " + request.getDomainName());
        }

        if (domainRepository.existsByEndpointUrl(request.getEndpointUrl())) {
            throw new IllegalArgumentException("端点URL已存在: " + request.getEndpointUrl());
        }

        if (domainRepository.existsById(request.getDomainId())) {
            throw new IllegalArgumentException("域ID已存在: " + request.getDomainId());
        }

        // 验证公钥格式
        if (!isValidPublicKey(request.getPublicKey())) {
            throw new IllegalArgumentException("无效的公钥格式");
        }

        // 验证证书（如果提供）
        if (request.getCertificate() != null && !request.getCertificate().isEmpty()) {
            if (!isValidCertificate(request.getCertificate())) {
                throw new IllegalArgumentException("无效的证书格式");
            }
        }

        // 创建域实体
        DomainEntity domain = new DomainEntity();
        domain.setDomainId(request.getDomainId());
        domain.setDomainName(request.getDomainName());
        domain.setDescription(request.getDescription());
        domain.setEndpointUrl(request.getEndpointUrl());
        domain.setPublicKey(request.getPublicKey());
        domain.setCertificate(request.getCertificate());
        domain.setDomainType(request.getDomainType());
        domain.setNetworkId(request.getNetworkId());
        domain.setIsActive(request.getIsActive());

        DomainEntity savedDomain = domainRepository.save(domain);
        logger.info("域注册成功: {} -> {}", savedDomain.getDomainId(), savedDomain.getDomainName());

        return savedDomain;
    }

    /**
     * 获取域信息
     */
    @Transactional(readOnly = true)
    public Optional<DomainEntity> getDomain(String domainId) {
        return domainRepository.findById(domainId);
    }

    /**
     * 获取所有活跃域
     */
    @Transactional(readOnly = true)
    public List<DomainEntity> getAllActiveDomains() {
        return domainRepository.findByIsActiveTrue();
    }

    /**
     * 根据域类型获取域
     */
    @Transactional(readOnly = true)
    public List<DomainEntity> getDomainsByType(String domainType) {
        return domainRepository.findByDomainTypeAndIsActiveTrue(domainType);
    }

    /**
     * 根据网络ID获取域
     */
    @Transactional(readOnly = true)
    public List<DomainEntity> getDomainsByNetworkId(String networkId) {
        return domainRepository.findByNetworkIdAndIsActiveTrue(networkId);
    }

    /**
     * 更新域信息
     */
    public DomainEntity updateDomain(String domainId, DomainRegistrationRequest request) {
        logger.info("更新域信息: {}", domainId);

        Optional<DomainEntity> existingDomainOpt = domainRepository.findById(domainId);
        if (!existingDomainOpt.isPresent()) {
            throw new IllegalArgumentException("域不存在: " + domainId);
        }

        DomainEntity existingDomain = existingDomainOpt.get();

        // 检查域名是否与其他域冲突
        if (!existingDomain.getDomainName().equals(request.getDomainName()) &&
                domainRepository.existsByDomainName(request.getDomainName())) {
            throw new IllegalArgumentException("域名已被其他域使用: " + request.getDomainName());
        }

        // 检查端点URL是否与其他域冲突
        if (!existingDomain.getEndpointUrl().equals(request.getEndpointUrl()) &&
                domainRepository.existsByEndpointUrl(request.getEndpointUrl())) {
            throw new IllegalArgumentException("端点URL已被其他域使用: " + request.getEndpointUrl());
        }

        // 验证新的公钥格式
        if (!isValidPublicKey(request.getPublicKey())) {
            throw new IllegalArgumentException("无效的公钥格式");
        }

        // 更新域信息
        existingDomain.setDomainName(request.getDomainName());
        existingDomain.setDescription(request.getDescription());
        existingDomain.setEndpointUrl(request.getEndpointUrl());
        existingDomain.setPublicKey(request.getPublicKey());
        existingDomain.setCertificate(request.getCertificate());
        existingDomain.setDomainType(request.getDomainType());
        existingDomain.setNetworkId(request.getNetworkId());
        existingDomain.setIsActive(request.getIsActive());

        DomainEntity updatedDomain = domainRepository.save(existingDomain);
        logger.info("域更新成功: {}", domainId);

        return updatedDomain;
    }

    /**
     * 停用域
     */
    public void deactivateDomain(String domainId) {
        logger.info("停用域: {}", domainId);

        Optional<DomainEntity> domainOpt = domainRepository.findById(domainId);
        if (!domainOpt.isPresent()) {
            throw new IllegalArgumentException("域不存在: " + domainId);
        }

        DomainEntity domain = domainOpt.get();
        domain.setIsActive(false);
        domainRepository.save(domain);

        logger.info("域已停用: {}", domainId);
    }

    /**
     * 激活域
     */
    public void activateDomain(String domainId) {
        logger.info("激活域: {}", domainId);

        Optional<DomainEntity> domainOpt = domainRepository.findById(domainId);
        if (!domainOpt.isPresent()) {
            throw new IllegalArgumentException("域不存在: " + domainId);
        }

        DomainEntity domain = domainOpt.get();
        domain.setIsActive(true);
        domainRepository.save(domain);

        logger.info("域已激活: {}", domainId);
    }

    /**
     * 验证域的证书
     */
    @Transactional(readOnly = true)
    public boolean validateDomainCertificate(String domainId) {
        Optional<DomainEntity> domainOpt = domainRepository.findById(domainId);
        if (!domainOpt.isPresent()) {
            return false;
        }

        DomainEntity domain = domainOpt.get();
        if (domain.getCertificate() == null || domain.getCertificate().isEmpty()) {
            return false;
        }

        return CertificateValidator.validateCertificateByString(domain.getCertificate());
    }

    /**
     * 验证公钥格式
     */
    private boolean isValidPublicKey(String publicKey) {
        try {
            // 这里可以添加更严格的公钥验证逻辑
            if (publicKey == null || publicKey.trim().isEmpty()) {
                return false;
            }
            // 尝试解码Base64格式的公钥
            java.util.Base64.getDecoder().decode(publicKey);
            return true;
        } catch (Exception e) {
            logger.warn("公钥格式验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证证书格式
     */
    private boolean isValidCertificate(String certificate) {
        try {
            if (certificate == null || certificate.trim().isEmpty()) {
                return true; // 证书是可选的
            }
            return CertificateValidator.validateCertificateByString(certificate);
        } catch (Exception e) {
            logger.warn("证书格式验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据公钥查找域
     */
    @Transactional(readOnly = true)
    public Optional<DomainEntity> findDomainByPublicKey(String publicKey) {
        return domainRepository.findByPublicKey(publicKey);
    }

    /**
     * 检查域是否存在且活跃
     */
    @Transactional(readOnly = true)
    public boolean isDomainActiveAndExists(String domainId) {
        Optional<DomainEntity> domainOpt = domainRepository.findById(domainId);
        return domainOpt.isPresent() && domainOpt.get().getIsActive();
    }
} 