# ArenaPilot

Android 客户端：**手动访问 Arena 官方 Agent 页面**。当前工作区为 `WHUT666/jess`，历史版本 v1.0.0 含对战自动化代码；v1.0.1 以安全、真实状态和键盘可用性为修复目标。

## 本版行为与明确限制

- 首页访问 `https://arena.ai/agent`；登录按钮一键进入官方页面或续用 **同一 WebView** 的已登录会话。首次 Google / 邮箱验证必须由本人按官网完成，应用不会以猜测的接口代替认证。
- Google 登录若在内嵌 WebView 中受限制，点「在系统浏览器打开 Agent」并在浏览器完成登录和对话。系统浏览器与 WebView 的登录态**不共享**，不会伪造 Cookie 同步。
- 打字时隐藏顶部、底部栏；WebView 布局按 IME 调整，避免浮层挡住输入框。内嵌页面支持系统文件选择器；外部浏览器也可用于上传。
- 旧版本地对战档案只读可见；已有提示词可手动复制、也能在本机新建（不会自动发送）。Agent 会话历史由 Arena 官方网站管理。本版不采集新对话内容、不给旧记录填充演示数据。
- Arena Agent 模式官方不公布所用协调模型。回答自述或网页目录不是可靠身份；本版显示 **未公开**，不推测具体型号；HTTP 403、一般 Cookie 或 429 也不能证明账号封禁。
- v1.0.0 的自动提示注入、批量抽模、指纹规避、明文密码填入、猜测登录 API 及无障碍自动化入口已停用；保留已有设备数据，**不会自动删除或读取旧账号密码**。

## 安全提醒

旧版公开源码曾包含疑似账号凭据。请所有相关账号所有者自行更换密码；仅从当前分支删除文字**不能清除 Git 历史和旧 tag**。请勿提交 `.env`、Keystore、Cookie、口令或会话令牌。升级不清空旧数据库；新的构建禁用应用数据备份，以降低再次泄露风险。

## 运行与测试

在具备 JDK 21、Android SDK 与 Gradle 依赖的环境中：

```sh
./gradlew testDebugUnitTest assembleDebug
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。GitHub Actions 的每次测试成功只上传 **debug 工作流产物**，不将其冒充可稳定升级的发布 APK。

## 正式发布的签名门禁

`v1.0.1` 发布须使用 **与 v1.0.0 release APK 相同的私钥**，并设置仓库 Actions Secrets：`KEYSTORE_BASE64`、`STORE_PASSWORD`、`KEY_PASSWORD`；Actions Variable `RELEASE_CERT_SHA256` 需填入旧 APK 签名证书的 SHA-256 指纹。密钥、密码只能由管理员通过 GitHub 自行配置，不能提供给聊天助手。缺任何材料、指纹不同、测试或构建失败时流水线必须阻止正式发布。旧版若使用 CI 随机生成后未留存的签名密钥，**无法保证覆盖升级**；此时请先解决签名连续性，不要随意打 tag。

官方资料：[Agent 模式](https://help.arena.ai/articles/5432423882-how-to-use-agent-mode)、[登录方式](https://help.arena.ai/articles/7258407731-arena-how-to-create-an-account)、[服务条款](https://help.arena.ai/articles/5629909088-terms-of-use)。
