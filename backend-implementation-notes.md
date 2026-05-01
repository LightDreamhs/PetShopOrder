# Admin 系统配置接口后端实现注意事项（P0）

## 1. Webhook SSRF 防护（必须）

前端已限制仅允许企业微信机器人地址，但**后端必须做最终校验**：

1. 仅允许 `https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=...`
2. 强制 `https`，禁止 `http`
3. 禁止重定向到其他域名/IP（跟随重定向前后都要校验）
4. 禁止内网地址与特殊地址（如 `127.0.0.1`、`10.0.0.0/8`、`172.16.0.0/12`、`192.168.0.0/16`、`169.254.169.254`）
5. Webhook 测试接口应限频（防止被滥用）

## 2. GET 配置接口脱敏返回（必须）

`GET /api/admin/system-config` 不应返回明文 key。

建议返回形态：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "config": {
      "qywxWebhookUrl": "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=***",
      "hasQywxWebhook": true
    }
  }
}
```

## 3. 更新接口字段语义（建议明确）

前端已按以下语义提交：

1. `qywxWebhookUrl` **缺失**：表示“不修改当前 webhook”
2. `qywxWebhookUrl: ""`：表示“清空 webhook”
3. `qywxWebhookUrl: "https://..."`：表示“更新为新 webhook”

后端请按该语义处理，避免误覆盖已有配置。

## 4. 存储与审计（建议）

1. webhook key 建议加密存储（至少应用层加密）
2. 配置变更需记录：操作者、时间、变更前后差异
3. 变更日志不要记录明文 key

## 5. 前后端校验一致性（建议）

后端需复用同等业务校验，不能仅依赖前端：

1. `deliveryRadiusKm > 0`
2. `orderEndTime > orderStartTime`（启用接单时段时）
3. 分段运费规则：无重叠、无缺口、覆盖配送半径
