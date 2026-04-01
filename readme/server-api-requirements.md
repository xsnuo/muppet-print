# Server API Requirements

Back to entry documents: [../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

This document describes server-side callback API contracts that Muppet Print requires when release integrations are enabled.

## 1. Scope

This is not a local Muppet Print API.
It is a required API contract that the upstream server must provide for release callback scenarios.

## 2. Related Release Configuration

Muppet Print reads these Spring Boot properties:

- `release.server.host`: callback server host.
- `release.server.token`: callback token sent in header `Muppet-Token`.

Default values are blank in `application.yml`.
If `release.server.host` is blank, callback pushing is skipped.
`release.server.host` can include a base path (for example `http://host/api`) and Muppet Print appends fixed endpoints under `/muppet/*`.

## 3. Required Response Envelope (Global Rule)

All server APIs called by Muppet Print release integrations must return this unified JSON envelope:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

Envelope fields:

- `code`: required business result code.
- `message`: readable description, usually `null` on success.
- `data`: business payload object, `null` or `{}` when no payload is needed.

Supported `code` values from Muppet Print side:

- `SUCCESS`: request processed successfully.
- `PARAM_ERROR`: request validation failed.
- `SERVICE_ERROR`: business processing failed (for example, printer or callback business exception).
- `SYSTEM_ERROR`: unexpected internal error.

Requirements:

- HTTP status should be `200` for business-level success/failure responses.
- Response payload should always follow the same envelope shape.

## 4. Error Log Callback

Server must provide:

```text
POST /muppet/log
```

Path calculation rule:

- Muppet Print appends `/muppet/log` to `release.server.host`.
- Example: `release.server.host=http://host/api` -> callback URL `http://host/api/muppet/log`.

### 4.1 Request Headers

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

### 4.2 Request Body

Request body fields:

- `level` (string): log severity, fixed value `error`.
- `version` (string): running Muppet Print version.
- `message` (string): generated error summary and stack details.

Error-log callback request body:

```json
{
  "level": "error",
  "version": "1.0.4",
  "message": "MuppetApi error [abcd1234]..."
}
```

## 5. Printer Signin Endpoints (Release Integration)

When `release.signin.enable=true`, Muppet Print may call these server endpoints.

### 5.1 Signin Local Printer

```text
POST /muppet/signin
```

Request headers:

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

Request body fields:

- `mac` (string): device MAC address used as the machine identifier.
- `pcName` (string): local host/computer name.
- `printerName` (string): local printer name selected on this machine.
- `url` (string): local Muppet Print service URL exposed by the machine.
- `group` (string): printer group value selected from server-provided group options.
- `pageWidth` (number): print page width in millimeters.
- `pageHeight` (number): print page height in millimeters.

Request body:

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

Success response:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

### 5.2 Query Signed Printers

```text
GET /muppet/signed?mac=<local-mac>
```

Request headers:

- `Muppet-Token: <release.server.token>`

Query fields:

- `mac` (string): local machine MAC address used to query records.

Response `data` fields:

- `printers` (array): signed-printer record list.

`printers[]` fields:

- `mac` (string): machine MAC address for the record.
- `pcName` (string): machine host name for the record.
- `printerName` (string): signed printer name.
- `url` (string): signed local print service URL.
- `group` (string): signed group value.
- `pageWidth` (number): signed print page width in millimeters.
- `pageHeight` (number): signed print page height in millimeters.

Success response:

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

`printers` should be an array; return an empty array when no records exist.

### 5.3 Query Group Options

```text
GET /muppet/groups
```

Request headers:

- `Muppet-Token: <release.server.token>`

Response `data` fields:

- `list` (array): available group options.

`list[]` fields:

- `label` (string): group display label.
- `value` (string): group value used in signin/signed payload.

Success response:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {
    "list": [
      {
        "label": "Default Group",
        "value": "default"
      }
    ]
  }
}
```

### 5.4 Signout Signed Printer

```text
POST /muppet/signout
```

Request headers:

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

Request body fields:

- `mac` (string): device MAC address.
- `printerName` (string): target printer name to sign out.
- `group` (string): group value of the signed record to remove.

Request body:

```json
{
  "mac": "xx-xx-xx-xx-xx-xx",
  "printerName": "Brother_QL_820NWB",
  "group": "default"
}
```

Success response:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

## 6. Operational Behavior

- Missing `release.server.host` must not cause runtime errors. Muppet Print skips callback push directly.
- Callback push failures must not break local API failure responses. The local API still returns its own `ApiResult` failure envelope.
