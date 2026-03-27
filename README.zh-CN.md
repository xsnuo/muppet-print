# Muppet Print

Muppet Print 是一个桌面宿主型的本地打印网关，面向“静默打印”场景。它通过 HTTP 接收 HTML 或 PDF 内容，在本机完成渲染，并把打印任务发送到本地已安装的打印机，而不是弹出浏览器打印对话框。

这个项目适合部署在办公电脑、仓库工作站、前台终端、局域网共享打印节点等场景，供业务系统、浏览器页面、APP 或手持设备统一调用本地打印能力。

## 核心能力

- 提供本地 HTTP API，支持打印机查询和打印任务提交。
- 使用 Playwright Chromium 渲染 HTML，再将结果发送到物理打印机。
- 支持直接上传 PDF 并打印。
- 提供桌面 UI、系统托盘、启动停止控制，以及部分平台的开机自启能力。
- 默认限制为单实例运行，应用会通过本地文件锁拦截重复启动，避免出现多个同程序进程。

## 技术结构

- Spring Boot 负责应用启动与 Bean 管理。
- Vert.x 负责路由、HTTP 服务与异步执行。
- Playwright Chromium 负责 HTML 渲染。
- Java Print Service 与 PDFBox 负责本地打印派发。

AI 持续维护的项目记忆与架构说明见：[docs/ai/project-overview.md](docs/ai/project-overview.md)、[docs/ai/architecture.md](docs/ai/architecture.md)。

## 文档导航

- 英文入口文档：[README.md](README.md)
- API 接口文档：[readme/api-reference.md](readme/api-reference.md)
- 使用说明：[readme/usage-guide.md](readme/usage-guide.md)
- 多系统打包说明：[readme/packaging-guide.md](readme/packaging-guide.md)
- 文档总览与后续扩展主题：[readme/README.md](readme/README.md)

## 快速开始

### 1. 环境要求

- JDK 21
- Maven 3.9 或兼容版本
- 可访问本地打印机的运行机器
- 开发环境或打包环境中可用的 Playwright 运行时

### 2. 开发运行

```bash
mvn spring-boot:run
```

项目主类是 `com.xuesinuo.muppet.UiStarter`，因此开发运行时会同时启动桌面 UI 和嵌入式 HTTP 服务。

### 3. 默认地址

UI 默认端口是 `58080`，典型本地访问地址为：

```text
http://127.0.0.1:58080
```

## 主要接口

- `GET/POST /api/getAllPrinters`
- `POST /api/print`
- `POST /api/printPDF`
- `GET/POST /api/version`

完整的请求与响应示例见：[readme/api-reference.md](readme/api-reference.md)。

## 典型对接方式

```text
业务系统 -> HTTP 请求 -> Muppet Print -> 本地打印机
```

这种方式可以避免依赖浏览器的 `window.print()`，让外部系统通过统一接口调用本地静默打印能力。

## 当前定位

当前代码主要面向：

- 本地或可信局域网环境
- 静默打印场景
- 桌面打包交付模式

它目前并不是一个面向公网的通用打印平台。

## 开发与维护约定

- 本项目的代码注释与工程沟通应使用中文。
- AI 持续维护的“项目记忆”位于可见路径 `docs/ai/`，并由 [AGENTS.md](AGENTS.md) 约束。
- 只要功能、API、打包、操作方式发生变化，就应同时更新 `docs/ai/` 与 `readme/` 下的人类文档。
