package com.bjut.blockchain.crossdomain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 跨域认证请求DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrossDomainAuthRequest {

    @NotBlank(message = "用户ID不能为空")
    @Size(max = 200, message = "用户ID长度不能超过200个字符")
    private String userId;

    @NotBlank(message = "源域ID不能为空")
    @Size(max = 100, message = "源域ID长度不能超过100个字符")
    private String sourceDomainId;

    @NotBlank(message = "目标域ID不能为空")
    @Size(max = 100, message = "目标域ID长度不能超过100个字符")
    private String targetDomainId;

    @NotBlank(message = "认证令牌不能为空")
    private String authToken;

    @NotBlank(message = "签名不能为空")
    private String signature;

    @NotBlank(message = "随机数不能为空")
    @Size(max = 128, message = "随机数长度不能超过128个字符")
    private String nonce;

    @Size(max = 500, message = "权限范围长度不能超过500个字符")
    private String scope;

    private String additionalClaims; // JSON格式的额外声明

    private Long timestamp;
} 