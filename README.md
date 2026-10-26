# ArenaPilot - LMSYS Arena 对战实时监控与智能自动引导系统

[![Android CI/CD - Build & Release](https://github.com/OWNER/REPO/actions/workflows/android-release.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/android-release.yml)
[![Version](https://img.shields.io/badge/version-v1.0.0-blue.svg)](https://github.com/OWNER/REPO/releases/tag/v1.0.0)
[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](LICENSE)

**ArenaPilot** 是一款专为 [LMSYS Chatbot Arena](https://lmarena.ai/) 设计的专业级 Android 客户端自动化与监控工具。
告别手动守株待兔与重复复制粘贴，通过实时 DOM 监听、智能流式终止识别与精准静默倒计时，全自动注入多维度预设提示词，引导模型进行高强度多轮对抗评测与指纹盲测剖析。

---

## 🌟 核心特性 (100% 真实可用)

1. **真实 LMSYS 接入与凭证管理**
   - 100% 直连 `https://lmarena.ai/` 生产环境。
   - 内置现代 Pixel 移动端 User-Agent，支持 Google 账号登录、OAuth 回调与 Cookie 完整同步。
   - 支持跨会话凭证持久化与账号信息实时检查。

2. **双引擎实时监控与静默停顿捕获**
   - **DOM MutationObserver 注入探针**：毫秒级监听两个模型的流式输出状态，智能识别 Markdown 渲染完成节点与发送按钮禁用状态。
   - **系统级无障碍辅助服务 (`ArenaAccessibilityService`)**：支持后台/分屏/外部 Chrome 浏览器运行时监控。

3. **智能预设词库与多模式引导调度**
   - 内置包含“深度辩驳”、“极限边界”、“推理逻辑”、“指令遵循”等维度的开箱即用提示词库。
   - 4 种调度策略：
     - **顺序遍历**：按次序轮询发送。
     - **随机探索**：随机抽取，提高盲测样本多样性。
     - **智能深度**：优先调用高难度/辩论性质的深度 Prompt。
     - **单次遍历**：轮完即停。

4. **模型身份探测器 (Blind Test Fingerprint Prober)**
   - 注入精心设计的元提示词，诱导未知模型输出自我架构认同与知识截止期特征。
   - 配合 LMSYS 投票后官方揭牌逻辑，自动提取并记录 Model A 和 Model B 的真实模型型号（如 GPT-4o、Claude 3.5 Sonnet、Gemini 1.5 Pro、DeepSeek-V2 等）。

5. **本地对战档案沉淀与回放 (Room DB)**
   - 自动沉淀每轮对决完整的输入、输出、字数吞吐量、响应耗时、最终揭晓模型与投票胜负。
   - 支持一键导出、文本复制、标题重命名与对战复盘。

6. **安全保护与熔断机制**
   - 最大轮次限制 (默认 10 轮)。
   - 自定义停止关键词检测 (如 `error`, `quota`, `token limit`, `overloaded`)，触发即刻暂停。

---

## 🛠️ GitHub CI/CD 与自动化打包发布

本项目已配置完善的 GitHub Actions 流水线 (`.github/workflows/android-release.yml`)。

### 触发打包与自动发布第一个版本 (v1.0.0)

#### 方法 1：推送 Git Tag (推荐)
在本地终端或提交中打上版本 tag 并推送到 GitHub：
```bash
git tag -a v1.0.0 -m "Release v1.0.0 - ArenaPilot Initial Release"
git push origin v1.0.0
```
GitHub Actions 会自动触发构建：
1. 运行所有 Robolectric 和单元测试；
2. 自动配置签名并打包生成：
   - `ArenaPilot-v1.0.0-release.apk` (生产发布包)
   - `ArenaPilot-v1.0.0-debug.apk` (调试包)
   - `ArenaPilot-v1.0.0-release.aab` (Google Play 格式)
3. 自动在 GitHub Releases 创建 `v1.0.0` 发布页面并上传全部 APK/AAB 附件。

#### 方法 2：在 GitHub 仓库网页端一键手动触发
1. 打开 GitHub 仓库页面，点击 **Actions** 标签；
2. 在左侧选择 **Android CI/CD - Build & Release**；
3. 点击 **Run workflow** 下拉按钮；
4. 输入版本号（默认为 `v1.0.0`），点击 **Run workflow** 即可启动全自动打包并发布 Release。

---

## 📱 本地构建指南

```bash
# 克隆代码
git clone https://github.com/YOUR_USERNAME/ArenaPilot.git
cd ArenaPilot

# 编译 Debug APK
./gradlew assembleDebug

# 运行单元测试
./gradlew testDebugUnitTest
```
产物位置：`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 开源许可
Apache License 2.0
