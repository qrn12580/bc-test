package com.bjut.blockchain.crossdomain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 域注册请求DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainRegistrationRequest {

    @NotBlank(message = "域ID不能为空")
    @Size(max = 100, message = "域ID长度不能超过100个字符")
    private String domainId;

    @NotBlank(message = "域名不能为空")
    @Size(max = 200, message = "域名长度不能超过200个字符")
    private String domainName;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "端点URL不能为空")
    @Size(max = 500, message = "端点URL长度不能超过500个字符")
    private String endpointUrl;

    @NotBlank(message = "公钥不能为空")
    private String publicKey;

    private String certificate;

    @NotBlank(message = "域类型不能为空")
    @Size(max = 50, message = "域类型长度不能超过50个字符")
    private String domainType; // BLOCKCHAIN, TRADITIONAL, HYBRID

    @Size(max = 100, message = "网络ID长度不能超过100个字符")
    private String networkId;

    @NotNull(message = "是否激活状态不能为空")
    private Boolean isActive = true;
} 