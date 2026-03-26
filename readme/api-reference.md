# API Reference

Back to entry documents: [../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## Overview

All current APIs are exposed under `/api/*`.

Typical local base URL:

```text
http://127.0.0.1:58080
```

Responses use a unified JSON envelope:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

Known result codes:

- `SUCCESS`
- `PARAM_ERROR`
- `SYSTEM_ERROR`
- `SERVICE_ERROR`

## 1. Get All Printers

### 1.1 Endpoint

```text
GET /api/getAllPrinters
POST /api/getAllPrinters
```

### 1.2 Purpose

Returns the printers visible to the host machine.

### 1.3 Example Request

```bash
curl http://127.0.0.1:58080/api/getAllPrinters
```

### 1.4 Example Success Response

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

## 2. HTML Silent Print

### 2.1 Endpoint

```text
POST /api/print
Content-Type: application/json
```

### 2.2 Purpose

Accepts printable HTML and sends the rendered result to a target printer.

### 2.3 Request Fields

- `html`: required, printable HTML source.
- `printerNameOrId`: required, printer name or identifier.
- `pageWidth`: required, page width in millimeters.
- `pageHeight`: required, page height in millimeters.
- `imports`: optional, map of relative file path to file content.
- `waitJsReady`: optional, wait until `window.printReady === true` before printing.

### 2.4 Example Request

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

### 2.5 Example cURL

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

### 2.6 Example Success Response

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": null
}
```

### 2.7 Example Failure Response

```json
{
  "code": "PARAM_ERROR",
  "message": "ParamException: must provide: html, printerNameOrId, pageWidth, pageHeight",
  "data": null
}
```

## 3. PDF Print

### 3.1 Endpoint

```text
POST /api/printPDF
Content-Type: multipart/form-data
```

### 3.2 Purpose

Uploads a PDF file and sends it directly to a local printer.

### 3.3 Form Fields

- `printerNameOrId`: required, target printer name.
- file field: required, uploaded PDF file.

### 3.4 Example cURL

```bash
curl -X POST http://127.0.0.1:58080/api/printPDF \
  -F "printerNameOrId=Brother_QL_820NWB" \
  -F "file=@./sample.pdf"
```

### 3.5 Example Success Response

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": null
}
```

## 4. Version

### 4.1 Endpoint

```text
GET /api/version
POST /api/version
```

### 4.2 Purpose

Returns the current application version and attempts to include the latest published version.

### 4.3 Example Request

```bash
curl http://127.0.0.1:58080/api/version
```

### 4.4 Example Response

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

## Notes For Integrators

- This service is designed for trusted local or LAN use.
- The target printer must exist on the machine that runs Muppet Print.
- HTML print layout depends on the local rendering environment, fonts, and printer driver behavior.
- Bundled helper assets live under `imports/` in the application resources, and custom request imports are materialized relative to the temporary rendering workspace.
