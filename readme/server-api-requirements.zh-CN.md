# 服务器 API 要求文档

返回入口文档：[../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

本文档用于描述在发行版集成场景下，服务端必须提供的回调接口契约。

## 1. 适用范围

这不是 Muppet Print 本地对外 API。
这是“上游服务器”需要提供的接口能力要求。

## 2. 对应 Release 配置

Muppet Print 读取以下 Spring Boot 配置项：

- `release.server.host`：回调服务器域名。
- `release.server.token`：放入请求头 `Muppet-Token` 的访问 token。

`application.yml` 中默认均为空字符串。
当 `release.server.host` 为空时，回调上报会被直接跳过。
`release.server.host` 可以直接包含上游服务基础路径（例如 `http://host/api`），Muppet Print 会在其后拼接固定 `/muppet/*` 接口路径。

## 3. 统一响应包装要求（全局规则）

Muppet Print 在发布集成模式下调用的所有服务器接口，都必须返回统一 JSON 包装结构：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

包装字段含义：

- `code`：必填，业务结果码。
- `message`：可读说明，成功时通常为 `null`。
- `data`：业务数据对象，无业务数据时可为 `null` 或 `{}`。

Muppet Print 侧约定的 `code` 取值：

- `SUCCESS`：请求处理成功。
- `PARAM_ERROR`：参数校验失败。
- `SERVICE_ERROR`：业务处理失败（如打印机或业务流程异常）。
- `SYSTEM_ERROR`：系统内部未预期异常。

约束：

- 业务成功或失败建议统一返回 HTTP `200`。
- 响应体应始终保持统一包装结构。

## 4. 异常日志回调接口

服务器必须提供：

```text
POST /muppet/log
```

路径计算规则：

- Muppet Print 会在 `release.server.host` 后拼接 `/muppet/log`。
- 例如：`release.server.host=http://host/api`，实际回调 URL 为 `http://host/api/muppet/log`。

### 4.1 请求头

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

### 4.2 请求体

请求体字段说明：

- `level`（string）：日志级别，固定为 `error`。
- `version`（string）：运行中的 Muppet Print 版本号。
- `message`（string）：系统拼接的错误摘要与堆栈信息。

异常日志回调请求体：

```json
{
  "level": "error",
  "version": "1.0.4",
  "message": "MuppetApi error [abcd1234]..."
}
```

## 5. 打印机注册接口（发布集成）

当 `release.signin.enable=true` 时，Muppet Print 可能调用以下服务器接口。

### 5.1 注册本机打印机

```text
POST /muppet/signin
```

请求头：

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

请求体字段说明：

- `mac`（string）：设备 MAC 地址，用作机器标识。
- `pcName`（string）：本机主机名。
- `printerName`（string）：本机选择的打印机名称。
- `url`（string）：本机暴露的 Muppet Print 服务地址。
- `group`（string）：从服务器分组选项中选择的分组值。
- `pageWidth`（number）：打印页面宽度，单位毫米。
- `pageHeight`（number）：打印页面高度，单位毫米。

请求体：

```json
{
  "mac": "xx-xx-xx-xx-xx-xx",
  "pcName": "HOSTNAME",
  "printerName": "Brother_QL_820NWB",
  "url": "http://HOSTNAME:58080",
  "group": "default",
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

### 5.2 查询已注册打印机

```text
GET /muppet/signed?mac=<本机mac>
```

请求头：

- `Muppet-Token: <release.server.token>`

查询参数说明：

- `mac`（string）：本机 MAC 地址，用于筛选查询记录。

响应 `data` 字段说明：

- `printers`（array）：已注册打印机记录列表。

`printers[]` 字段说明：

- `mac`（string）：记录所属机器的 MAC 地址。
- `pcName`（string）：记录所属机器主机名。
- `printerName`（string）：已注册打印机名称。
- `url`（string）：已注册本地打印服务地址。
- `group`（string）：已注册分组值。
- `pageWidth`（number）：已注册页面宽度，单位毫米。
- `pageHeight`（number）：已注册页面高度，单位毫米。

成功响应：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "printers": [
      {
        "mac": "xx-xx-xx-xx-xx-xx",
        "pcName": "HOSTNAME",
        "printerName": "Brother_QL_820NWB",
        "url": "http://HOSTNAME:58080",
        "group": "default",
        "pageWidth": 40,
        "pageHeight": 60
      }
    ]
  }
}
```

`printers` 应始终返回数组，无记录时返回空数组。

### 5.3 查询分组选项

```text
GET /muppet/groups
```

请求头：

- `Muppet-Token: <release.server.token>`

响应 `data` 字段说明：

- `list`（array）：可用分组选项列表。

`list[]` 字段说明：

- `label`（string）：分组展示名称。
- `value`（string）：用于注册与查询记录的分组值。

成功响应：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "list": [
      {
        "label": "默认分组",
        "value": "default"
      }
    ]
  }
}
```

### 5.4 注销已注册打印机

```text
POST /muppet/signout
```

请求头：

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

请求体字段说明：

- `mac`（string）：设备 MAC 地址。
- `printerName`（string）：要注销的打印机名称。
- `group`（string）：要注销记录所属的分组值。

请求体：

```json
{
  "mac": "xx-xx-xx-xx-xx-xx",
  "printerName": "Brother_QL_820NWB",
  "group": "default"
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

## 6. 运行行为要求

- `release.server.host` 缺失时，不可导致运行时报错，必须直接跳过回调上报。
- 回调上报失败不可影响本地 API 的失败响应，Muppet Print 仍按本地 `ApiResult` 规范返回。
