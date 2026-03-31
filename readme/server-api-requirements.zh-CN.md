# 服务器 API 要求文档

返回入口文档：[../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

本文档用于描述在发行版集成场景下，服务端必须提供的回调接口契约。

## 1. 适用范围

这不是 Muppet Print 本地对外 API。
这是“上游服务器”需要提供的接口能力要求。

## 2. 对应 Release 配置

Muppet Print 读取以下 Spring Boot 配置项：

- `release.server.host`：回调服务器域名。
- `release.server.prefix`：调用服务器时统一附加的可选路径前缀。
- `release.server.token`：放入请求头 `Muppet-Token` 的访问 token。

`application.yml` 中默认均为空字符串。
当 `release.server.host` 为空时，回调上报会被直接跳过。

## 3. 必需接口

服务器必须提供：

```text
POST /{prefix}/muppet/log
```

路径计算规则：

- 当 `release.server.prefix` 为空：`/muppet/log`
- 当 `release.server.prefix=/api`：`/api/muppet/log`
- 前缀会被规范化为“单个前导 `/` + 无尾部 `/`”。

## 4. 请求格式要求

### 4.1 Header

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

请求体中不再携带 `token` 字段。
后续其他“直接访问服务器”的功能也应沿用该 header 传递 token。

### 4.2 JSON Body

当前异常日志回调的 body 字段固定为：

```json
{
  "level": "error",
  "version": "1.0.3",
  "message": "MuppetApi error [abcd1234]..."
}
```

说明：

- `level` 当前固定为 `error`。
- `version` 为运行中的 Muppet Print 版本号。
- `message` 为系统拼接的错误摘要和堆栈信息。

## 5. 统一响应包装要求（全局规则）

Muppet Print 在发布集成模式下调用的所有服务器接口，都必须返回统一 JSON 包装结构：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

要求如下：

- 业务层成功或失败建议统一返回 HTTP `200`。
- `code` 必填。成功必须使用 `SUCCESS`。
- `message` 失败时应给出可读错误信息；成功时可为 `null`。
- `data` 承载业务数据；无数据时可统一为 `null` 或 `{}`。

该包装结构为全局约束，后续新增的任何服务器回调接口都应沿用。

## 6. 打印机注册接口（发布集成）

当 `release.signin.enable=true` 时，Muppet Print 可能调用以下服务器接口。

### 6.1 注册本机打印机

```text
POST /{prefix}/muppet/signin
Content-Type: application/json
```

请求体：

```json
{
  "mac": "xx-xx-xx-xx-xx-xx",
  "pcName": "HOSTNAME",
  "printerName": "Brother_QL_820NWB",
  "url": "http://HOSTNAME:58080",
  "pageWidth": 40,
  "pageHeight": 60
}
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

### 6.2 查询已注册打印机

```text
GET /muppet/signed?mac=<本机mac>
```

成功响应：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "prints": [
      {
        "mac": "xx-xx-xx-xx-xx-xx",
        "pcName": "HOSTNAME",
        "printerName": "Brother_QL_820NWB",
        "url": "http://HOSTNAME:58080",
        "pageWidth": 40,
        "pageHeight": 60
      }
    ]
  }
}
```

`prints` 应始终返回数组，无记录时返回空数组。

## 7. 运行行为要求

- `release.server.host` 缺失时，不可导致运行时报错，必须直接跳过回调上报。
- 回调上报失败不可影响本地 API 的失败响应，Muppet Print 仍按本地 `ApiResult` 规范返回。
