package com.bjut.blockchain.crossdomain.service;

import com.bjut.blockchain.crossdomain.dto.DomainTrustRequest;
import com.bjut.blockchain.crossdomain.entity.DomainTrustEntity;
import com.bjut.blockchain.crossdomain.repository.DomainTrustRepository;
import com.bjut.blockchain.crossdomain.repository.DomainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 域间信任管理服务类
 * 负责管理域之间的信任关系
 */
@Service
@Transactional
public class DomainTrustService {

    private static final Logger logger = LoggerFactory.getLogger(DomainTrustService.class);

    @Autowired
    private DomainTrustRepository trustRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainService domainService;

    /**
     * 建立域间信任关系
     */
    public DomainTrustEntity establishTrust(DomainTrustRequest request) {
        logger.info("建立域间信任关系: {} -> {}", request.getSourceDomainId(), request.getTargetDomainId());

        // 验证源域和目标域存在
        if (!domainService.isDomainActiveAndExists(request.getSourceDomainId())) {
            throw new IllegalArgumentException("源域不存在或未激活: " + request.getSourceDomainId());
        }

        if (!domainService.isDomainActiveAndExists(request.getTargetDomainId())) {
            throw new IllegalArgumentException("目标域不存在或未激活: " + request.getTargetDomainId());
        }

        // 检查是否已存在信任关系
        if (trustRepository.existsTrustRelation(request.getSourceDomainId(), request.getTargetDomainId())) {
            throw new IllegalArgumentException("域间信任关系已存在");
        }

        // 创建信任关系实体
        DomainTrustEntity trust = new DomainTrustEntity();
        trust.setSourceDomainId(request.getSourceDomainId());
        trust.setTargetDomainId(request.getTargetDomainId());
        trust.setTrustLevel(request.getTrustLevel());
        trust.setTrustType(request.getTrustType());
        trust.setSharedSecret(request.getSharedSecret());
        trust.setTrustCertificate(request.getTrustCertificate());
        trust.setValidFrom(request.getValidFrom());
        trust.setValidUntil(request.getValidUntil());
        trust.setIsActive(request.getIsActive());

        DomainTrustEntity savedTrust = trustRepository.save(trust);
        logger.info("域间信任关系建立成功: {} -> {} (信任级别: {})", 
                   request.getSourceDomainId(), request.getTargetDomainId(), request.getTrustLevel());

        return savedTrust;
    }

    /**
     * 检查域间是否存在信任关系
     */
    @Transactional(readOnly = true)
    public boolean hasTrustRelation(String sourceDomainId, String targetDomainId) {
        return trustRepository.existsTrustRelation(sourceDomainId, targetDomainId);
    }

    /**
     * 获取有效的信任关系
     */
    @Transactional(readOnly = true)
    public Optional<DomainTrustEntity> getValidTrustRelation(String sourceDomainId, String targetDomainId) {
        return trustRepository.findValidTrustRelation(sourceDomainId, targetDomainId, LocalDateTime.now());
    }

    /**
     * 获取域的所有信任关系
     */
    @Transactional(readOnly = true)
    public List<DomainTrustEntity> getDomainTrustRelations(String domainId) {
        return trustRepository.findAllTrustRelationsByDomain(domainId);
    }

    /**
     * 更新信任级别
     */
    public DomainTrustEntity updateTrustLevel(String sourceDomainId, String targetDomainId, Integer newTrustLevel) {
        logger.info("更新域间信任级别: {} -> {} = {}", sourceDomainId, targetDomainId, newTrustLevel);

        Optional<DomainTrustEntity> trustOpt = trustRepository.findTrustRelation(sourceDomainId, targetDomainId);
        if (!trustOpt.isPresent()) {
            throw new IllegalArgumentException("信任关系不存在");
        }

        DomainTrustEntity trust = trustOpt.get();
        trust.setTrustLevel(newTrustLevel);
        
        DomainTrustEntity updatedTrust = trustRepository.save(trust);
        logger.info("域间信任级别更新成功");
        
        return updatedTrust;
    }

    /**
     * 撤销信任关系
     */
    public void revokeTrust(String sourceDomainId, String targetDomainId) {
        logger.info("撤销域间信任关系: {} -> {}", sourceDomainId, targetDomainId);

        Optional<DomainTrustEntity> trustOpt = trustRepository.findTrustRelation(sourceDomainId, targetDomainId);
        if (!trustOpt.isPresent()) {
            throw new IllegalArgumentException("信任关系不存在");
        }

        DomainTrustEntity trust = trustOpt.get();
        trust.setIsActive(false);
        trustRepository.save(trust);

        logger.info("域间信任关系已撤销");
    }
} 