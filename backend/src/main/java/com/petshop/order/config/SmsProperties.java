package com.petshop.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 短信验证码配置。
 *
 * <p>通过 {@code sms.provider} 切换实现：
 * <ul>
 *   <li>{@code log}（默认）：兜底实现，打日志 + 固定码 {@code 1234} 放行，便于本地联调。</li>
 *   <li>{@code aliyun}：阿里云号码认证服务（PNVS）真实核验。</li>
 * </ul>
 * AK/SK 走环境变量注入，勿入库入仓。
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /** 短信服务商：log（默认兜底）/ aliyun */
    private String provider = "log";

    private Aliyun aliyun = new Aliyun();

    @Data
    public static class Aliyun {
        /** 访问密钥 ID */
        private String accessKeyId;
        /** 访问密钥 Secret */
        private String accessKeySecret;
        /** 短信签名（赠送签名，如"速通互联验证码"） */
        private String signName;
        /** 短信模板 Code（赠送登录/注册模板，默认 100001） */
        private String templateCode = "100001";
    }
}
