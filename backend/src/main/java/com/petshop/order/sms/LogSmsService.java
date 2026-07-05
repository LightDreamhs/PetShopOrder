package com.petshop.order.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 兜底实现：{@code sms.provider=log}（默认）时生效。
 *
 * <p>仅打日志、用固定码 {@code 1234} 放行，保证 AK 未到位也能启动联调。
 * 生产环境务必切换到 {@code aliyun}。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "sms", name = "provider", havingValue = "log", matchIfMissing = true)
public class LogSmsService implements SmsVerifyService {

    /** 开发期固定验证码，前端/联调用 */
    private static final String DEV_CODE = "1234";

    @Override
    public void send(String phone) {
        log.info("[SMS-LOG] phone={} 发送验证码（开发期固定码 {}，未真实下发）", phone, DEV_CODE);
    }

    @Override
    public boolean verify(String phone, String code) {
        boolean ok = DEV_CODE.equals(code);
        log.info("[SMS-LOG] phone={} code={} 核验结果={}", phone, code, ok);
        return ok;
    }
}
