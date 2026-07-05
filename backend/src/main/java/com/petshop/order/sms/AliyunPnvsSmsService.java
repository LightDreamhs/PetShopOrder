package com.petshop.order.sms;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.petshop.order.common.BusinessException;
import com.petshop.order.config.SmsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云号码认证服务（PNVS）实现：{@code sms.provider=aliyun} 时生效。
 *
 * <p>验证码的生成、有效期、防刷全部托管在阿里云：
 * <ul>
 *   <li>{@code send} 调 {@code SendSmsVerifyCode}，由系统生成验证码并下发；</li>
 *   <li>{@code verify} 调 {@code CheckSmsVerifyCode}，依据返回的 {@code verifyResult}（PASS/FAIL）判定。</li>
 * </ul>
 * 不向后端返回明文验证码（{@code returnVerifyCode=false}）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "aliyun")
public class AliyunPnvsSmsService implements SmsVerifyService {

    private final SmsProperties smsProperties;

    private Client client;

    @PostConstruct
    public void init() {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        validate(aliyun);
        Config config = new Config()
                .setAccessKeyId(aliyun.getAccessKeyId())
                .setAccessKeySecret(aliyun.getAccessKeySecret());
        // 号码认证服务 endpoint
        config.endpoint = "dypnsapi.aliyuncs.com";
        try {
            this.client = new Client(config);
        } catch (Exception e) {
            throw new IllegalStateException("初始化阿里云 PNVS 客户端失败", e);
        }
        log.info("[SMS-ALIYUN] 客户端初始化成功，签名={}，模板={}", aliyun.getSignName(), aliyun.getTemplateCode());
    }

    private void validate(SmsProperties.Aliyun aliyun) {
        if (isBlank(aliyun.getAccessKeyId()) || isBlank(aliyun.getAccessKeySecret())
                || isBlank(aliyun.getSignName()) || isBlank(aliyun.getTemplateCode())) {
            throw new IllegalStateException(
                    "sms.provider=aliyun 时必须配置 access-key-id / access-key-secret / sign-name / template-code");
        }
    }

    @Override
    public void send(String phone) {
        SmsProperties.Aliyun aliyun = smsProperties.getAliyun();
        try {
            SendSmsVerifyCodeRequest req = new SendSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setSignName(aliyun.getSignName())
                    .setTemplateCode(aliyun.getTemplateCode())
                    // 验证码由阿里云动态生成（##code##），后续才能用 CheckSmsVerifyCode 核验
                    // 模板内容含 ${code} 与 ${min}，故参数名固定为 code / min
                    .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                    // 配合占位符使用，指定验证码生成规则：1=纯数字（注意 setter 入参为 Long）
                    .setCodeType(1L)
                    // 验证码长度 6 位
                    .setCodeLength(6L)
                    // 关键：不把明文验证码返回后端，核验一律走 CheckSmsVerifyCode
                    .setReturnVerifyCode(false);
            SendSmsVerifyCodeResponse resp = client.sendSmsVerifyCode(req);
            // Code 返回 OK 才是请求成功（注意：不是 200）
            String code = resp.getBody() == null ? null : resp.getBody().getCode();
            if (!"OK".equals(code)) {
                String msg = resp.getBody() == null ? "无响应体" : resp.getBody().getMessage();
                log.warn("[SMS-ALIYUN] 发送失败 phone={} code={} msg={}", phone, code, msg);
                throw new BusinessException("验证码发送失败：" + msg);
            }
            log.info("[SMS-ALIYUN] 验证码已下发 phone={}", phone);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 发送异常 phone={}", phone, e);
            throw new BusinessException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    public boolean verify(String phone, String code) {
        try {
            CheckSmsVerifyCodeRequest req = new CheckSmsVerifyCodeRequest()
                    .setPhoneNumber(phone)
                    .setVerifyCode(code);
            CheckSmsVerifyCodeResponse resp = client.checkSmsVerifyCode(req);
            // verifyResult 嵌套在 Body.Model 内：PASS（核验成功）/ UNKNOWN（核验失败）
            CheckSmsVerifyCodeResponseBody body = resp.getBody();
            String result = (body == null || body.getModel() == null) ? null : body.getModel().getVerifyResult();
            boolean ok = "PASS".equalsIgnoreCase(result);
            log.info("[SMS-ALIYUN] 核验 phone={} result={} ok={}", phone, result, ok);
            return ok;
        } catch (Exception e) {
            log.error("[SMS-ALIYUN] 核验异常 phone={}", phone, e);
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
