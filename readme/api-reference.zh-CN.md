# API 接口文档

返回入口文档：[../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## 概览

当前所有接口都暴露在 `/api/*` 路径下。

典型本地地址：

```text
http://127.0.0.1:58080
```

接口统一返回 JSON 包装结构：

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

当前已知返回码：

- `SUCCESS`
- `PARAM_ERROR`
- `SYSTEM_ERROR`
- `SERVICE_ERROR`

与 Muppet Print 发行版回调相关的“服务器接口契约要求”，请见：[server-api-requirements.zh-CN.md](server-api-requirements.zh-CN.md)。

## 1. 获取打印机列表

### 1.1 接口地址

```text
GET /api/getAllPrinters
POST /api/getAllPrinters
```

### 1.2 用途

返回当前宿主机器可见的打印机列表。

### 1.3 示例请求

```bash
curl http://127.0.0.1:58080/api/getAllPrinters
```

### 1.4 成功响应示例

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "printers": [
      {
        "id": "Brother_QL_820NWB",
        "name": "Brother_QL_820NWB",
        "description": "Brother label printer"
      }
    ]
  }
}
```

## 2. HTML 静默打印

### 2.1 接口地址

```text
POST /api/print
Content-Type: application/json
```

### 2.2 用途

提交可打印 HTML，并将渲染结果发送到目标打印机。

### 2.3 请求字段

- `html`：必填，待打印的 HTML 内容。
- `printerNameOrId`：必填，目标打印机名称或标识。
- `pageWidth`：必填，页面宽度，单位毫米。
- `pageHeight`：必填，页面高度，单位毫米。
- `imports`：可选，相对路径到文件内容的映射。
- `waitJsReady`：可选，是否等待 `window.printReady === true` 后再打印。

### 2.4 示例请求

```json
{
  "html": "<!DOCTYPE html><html><head><link rel=\"stylesheet\" href=\"./css/print.css\"></head><body><div class=\"print-page\">Hello</div></body></html>",
  "printerNameOrId": "Brother_QL_820NWB",
  "pageWidth": 100,
  "pageHeight": 150,
  "imports": {
    "css/custom.css": ".print-page { color: black; }"
  },
  "waitJsReady": true
}
```

### 2.5 cURL 示例

```bash
curl -X POST http://127.0.0.1:58080/api/print \
  -H "Content-Type: application/json" \
  -d '{
    "html": "<!DOCTYPE html><html><body><div>Hello</div><script>window.printReady=true</script></body></html>",
    "printerNameOrId": "Brother_QL_820NWB",
    "pageWidth": 100,
    "pageHeight": 150,
    "waitJsReady": true
  }'
```

### 2.6 成功响应示例

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": null
}
```

### 2.7 失败响应示例

```json
{
  "code": "PARAM_ERROR",
  "message": "ParamException: must provide: html, printerNameOrId, pageWidth, pageHeight",
  "data": null
}
```

## 3. PDF 打印

### 3.1 接口地址

```text
POST /api/printPDF
Content-Type: multipart/form-data
```

### 3.2 用途

上传 PDF 文件，并直接发送到本地打印机。

### 3.3 表单字段

- `printerNameOrId`：必填，目标打印机名称。
- 文件字段：必填，上传的 PDF 文件。

### 3.4 cURL 示例

```bash
curl -X POST http://127.0.0.1:58080/api/printPDF \
  -F "printerNameOrId=Brother_QL_820NWB" \
  -F "file=@./sample.pdf"
```

### 3.5 成功响应示例

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": null
}
```

## 4. 版本接口

### 4.1 接口地址

```text
GET /api/version
POST /api/version
```

### 4.2 用途

返回当前应用版本，并尝试带出远端最新版本号。

### 4.3 示例请求

```bash
curl http://127.0.0.1:58080/api/version
```

### 4.4 响应示例

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "version": "1.0.3",
    "newVersion": "1.0.3"
  }
}
```

## 对接注意事项

- 本服务当前主要面向可信的本地或局域网环境。
- 目标打印机必须安装在运行 Muppet Print 的机器上。
- HTML 打印效果会受到本地渲染环境、字体和打印机驱动影响。
- 内置辅助资源位于应用资源目录中的 `imports/` 下，请求里传入的自定义 `imports` 会和临时渲染目录一起生效。
