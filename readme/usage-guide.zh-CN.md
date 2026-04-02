# 使用说明

返回入口文档：[../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## 典型使用流程

1. 在可访问目标打印机的机器上安装并启动 Muppet Print。
2. 打开桌面 UI，确认服务处于运行状态。
3. 从业务系统调用接口获取打印机列表。
4. 提交 HTML 或 PDF 打印任务到本地服务。
5. 在目标打印机上确认输出结果。

## 桌面 UI 功能

桌面界面提供：

- 端口输入。
- 启动与停止按钮。
- 当 `release.signin.enable=true` 时，额外提供打印机注册相关按钮（`Signin local printer`、`Signed`）。
- 运行状态显示。
- 错误消息显示。
- 支持平台上的开机自启开关。
- 支持平台上的托盘最小化行为。
- 单实例启动拦截。重复启动时会由本地文件锁直接拦截，并以“已在运行”的提示后退出该进程。

## 启动服务

### 开发环境

```bash
mvn spring-boot:run
```

### 打包应用

启动打包后的应用后，确认 UI 显示为运行状态，此时嵌入式 HTTP 服务会监听已配置端口。

如果配置的 Web 端口已被其他应用占用，Muppet Print 不会把它当成重复运行，而是保持 UI 打开，并用英文提示用户修改端口后重新启动服务。
Web 启动失败时，UI 状态会回到 `Stopped`，并保持端口输入框可编辑。

## Release 服务器配置

发行版可通过 Spring Boot 配置控制远端回调行为：

- `release.server.host`：服务器域名。为空时，Muppet Print 会跳过远端异常日志上报。
- `release.server.token`：回调请求头 token，header key 固定为 `Muppet-Token`。
- `release.signin.enable`：仅控制桌面端打印机注册按钮显示，不走服务器动态加载该开关。

远端异常回调路径固定为 `/muppet/log`，并直接拼接在 `release.server.host` 之后。
`release.server.host` 可包含基础路径，例如 `http://host/api`，实际回调 URL 为 `http://host/api/muppet/log`。
服务器接口契约详见 [server-api-requirements.zh-CN.md](server-api-requirements.zh-CN.md)。

启用打印机注册后：

- `Signin local printer` 会先调用 `GET /muppet/groups` 加载分组；若分组加载失败，则阻止提交并显示错误。
- `Signin local printer` 会打开注册表单，并提交到 `POST /muppet/signin`。
- 注册表单会先在本地校验打印机选择以及 page width/page height，再决定是否发起服务器请求。
- 注册表单默认优先生成基于本机主机名的 URL；切换到 `use IP` 时，会保留当前端口号以及原 URL 的路径、查询串。
- 用于局域网访问的 URL 主机名会统一规范化：若后缀不是 `.local`，会自动追加 `.local`。
- `Signed` 会打开本机已注册打印机列表，数据来自 `GET /muppet/signed?mac=<本机mac>`。
- 已注册打印机列表新增 `group` 列，并基于 `GET /muppet/groups` 的分组选项把 value 映射为展示名称；映射不到时显示原值。
- 已注册打印机列表会将与本机不一致的关键字段用红字标出，便于操作员检查。

## 对接流程

### 第一步：获取打印机列表

调用 `/api/getAllPrinters`，选择正确的打印机名称。

### 第二步：提交 HTML 或 PDF

- 对于 HTML 模板、动态标签、浏览器样式布局，使用 `/api/print`。
- 对于已经生成好的 PDF 文件，使用 `/api/printPDF`。

### 第三步：处理错误

检查 JSON 响应中的 `code` 和 `message` 字段，例如：

- `PARAM_ERROR` 表示请求缺失参数或参数不合法。
- `SERVICE_ERROR` 通常表示打印机或打印执行过程出现问题。
- `SYSTEM_ERROR` 表示系统内部出现了未预期异常。

## HTML 打印说明

- `pageWidth` 和 `pageHeight` 的单位都是毫米。
- 可以通过 `imports` 传入附加文件。
- 运行时也会复制应用内置的 `imports/` 辅助资源。
- 如果页面必须等前端逻辑完成后再打印，可以设置 `waitJsReady=true`，并在页面里设置 `window.printReady = true`。

## 运行建议

- 尽量将服务部署在打印机、字体环境都稳定的工作站上。
- 如果外部系统依赖打印机名称，请保持打印机名称稳定。
- 建议优先用于本地或可信局域网环境。
- 生产环境中请在真实操作系统和真实打印机型号上验证 HTML 模板效果。

## 常见排查提示

### 找不到打印机

- 确认打印机已安装在宿主机上。
- 使用 `/api/getAllPrinters` 核对打印机名称是否完全一致。

### HTML 排版不符合预期

- 确认所需字体已经安装或随应用一起提供。
- 确认引用资源可以正确解析。
- 重新检查页面宽高是否按毫米传入。
- Windows 上如果打印辅助页面出现中文乱码，升级到包含 UTF-8 页面声明和中文字体回退修复的版本。

### PDF 上传失败

- 确认请求使用的是 multipart/form-data。
- 确认上传文件是可读取的 PDF。
- 确认 `printerNameOrId` 已传入。
