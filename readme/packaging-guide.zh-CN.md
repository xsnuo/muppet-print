# 打包说明

返回入口文档：[../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## 概览

项目已经在 Maven 中配置了桌面应用打包能力。主类为 `com.xuesinuo.muppet.UiStarter`，并且在 `pom.xml` 中提供了 Windows 和 macOS 的 `jpackage-maven-plugin` 打包配置。

## 通用要求

- JDK 21
- Maven
- JDK 自带并可用的 `jpackage`
- 已存在于 `src/main/resources` 中的平台图标资源

## 构建应用 JAR

```bash
mvn clean package
```

构建后会在 `target/` 下生成 Spring Boot 应用 JAR。

## Windows 打包

`windows` Maven Profile 配置了：

- 应用名：`MuppetPrint`
- 输出目录：`target/dist`
- 图标：`src/main/resources/app.ico`
- 开始菜单与桌面快捷方式相关配置

### Windows 打包命令示例

```bash
mvn clean package -Pwindows
```

### Windows 说明

- Windows 打包需要在 Windows 环境中进行，或使用支持 Windows 打包的环境。
- 生成结果预期是带桌面 UI 的本地应用。

## macOS 打包

`macos` Maven Profile 配置了：

- 应用名：`MuppetPrint`
- 输出目录：`target/dist`
- 图标：`src/main/resources/app.icns`

### macOS 打包命令示例

```bash
mvn clean package -Pmacos
```

### macOS 说明

- macOS 打包需要在 macOS 环境中进行，且 JDK 需要提供 `jpackage`。
- UI 已包含基于 LaunchAgent 的 macOS 开机自启逻辑。

## Linux 打包

`pom.xml` 中还没有单独的 Linux `jpackage` Profile。

更实际的做法包括：

- 在 Linux 上直接以 JDK 21 运行 JAR。
- 如果后续需要桌面安装包，再补充 Linux 打包 Profile。

## 打包验证清单

- 桌面 UI 能正常启动。
- 托盘行为符合目标平台预期。
- 可以修改端口并成功启动服务。
- `/api/getAllPrinters` 能返回本地打印机列表。
- HTML 与 PDF 打印链路能在目标打印机上工作。
- 在目标操作系统上验证开机自启行为。

## 后续打包文档扩展

如果后续增加 Linux 原生打包、企业托管安装方式、或集群传输组件，需要继续扩展本说明。
