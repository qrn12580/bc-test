package com.bjut.blockchain.crossdomain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * 域信任关系请求DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DomainTrustRequest {

    @NotBlank(message = "源域ID不能为空")
    @Size(max = 100, message = "源域ID长度不能超过100个字符")
    private String sourceDomainId;

    @NotBlank(message = "目标域ID不能为空")
    @Size(max = 100, message = "目标域ID长度不能超过100个字符")
    private String targetDomainId;

    @NotNull(message = "信任级别不能为空")
    @Min(value = 1, message = "信任级别最小为1")
    @Max(value = 10, message = "信任级别最大为10")
    private Integer trustLevel;

    @NotBlank(message = "信任类型不能为空")
    @Size(max = 50, message = "信任类型长度不能超过50个字符")
    private String trustType; // BIDIRECTIONAL, UNIDIRECTIONAL

    private String sharedSecret;

    private String trustCertificate;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    @NotNull(message = "是否激活状态不能为空")
    private Boolean isActive = true;
} 