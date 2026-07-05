package com.petshop.order.sms;

/**
 * 短信验证码服务抽象层。
 *
 * <p>业务代码只依赖此接口，具体实现（log 兜底 / aliyun 真实）按 {@code sms.provider} 条件装配。
 * 验证码的生成、有效期、防刷、核验均由实现方托管，接口不持有验证码明文。
 */
public interface SmsVerifyService {

    /**
     * 向指定手机号发送验证码。
     *
     * @param phone 中国大陆手机号（11 位）
     */
    void send(String phone);

    /**
     * 核验用户输入的验证码。
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return 核验通过返回 true
     */
    boolean verify(String phone, String code);
}
