# Server API Requirements

Back to entry documents: [../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

This document describes server-side callback API contracts that Muppet Print requires when release integrations are enabled.

## 1. Scope

This is not a local Muppet Print API.
It is a required API contract that the upstream server must provide for release callback scenarios.

## 2. Related Release Configuration

Muppet Print reads these Spring Boot properties:

- `release.server.host`: callback server host.
- `release.server.prefix`: optional uniform path prefix when calling the callback server.
- `release.server.token`: callback token sent in header `Muppet-Token`.

Default values are blank in `application.yml`.
If `release.server.host` is blank, callback pushing is skipped.

## 3. Required Endpoint

Server must provide:

```text
POST /{prefix}/muppet/log
```

Path calculation rules:

- If `release.server.prefix` is blank: `/muppet/log`
- If `release.server.prefix=/api`: `/api/muppet/log`
- Prefix is normalized to a single leading slash and no trailing slash.

## 4. Required Request Format

### 4.1 Headers

- `Content-Type: application/json`
- `Muppet-Token: <release.server.token>`

`token` is no longer sent in the request body.
Future direct-to-server callbacks should also carry token via `Muppet-Token` header.

### 4.2 JSON Body

Current error-log callback body keys are fixed:

```json
{
  "level": "error",
  "version": "1.0.3",
  "message": "MuppetApi error [abcd1234]..."
}
```

Notes:

- `level` is fixed to `error` for this callback.
- `version` is the running Muppet Print version.
- `message` contains the generated error summary and stack details.

## 5. Required Response Envelope (Global Rule)

All server APIs that are called by Muppet Print release integrations must return a unified JSON envelope:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

Requirements:

- HTTP status should be `200` for business-level success/failure responses.
- `code` is required. Success must use `SUCCESS`.
- `message` should be a human-readable error message on failure; may be `null` on success.
- `data` should carry the business payload object; if no payload, use `null` or `{}` consistently.

This envelope rule is global and should be reused for all future server callback APIs.

## 6. Printer Signin Endpoints (Release Integration)

When `release.signin.enable=true`, Muppet Print may call these server endpoints.

### 6.1 Signin Local Printer

```text
POST /{prefix}/muppet/signin
Content-Type: application/json
```

Request body:

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

Success response:

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

### 6.2 Query Signed Printers

```text
GET /muppet/signed?mac=<local-mac>
```

Success response:

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

`prints` should be an array; return an empty array when no records exist.

## 7. Operational Behavior

- Missing `release.server.host` must not cause runtime errors. Muppet Print skips callback push directly.
- Callback push failures must not break local API failure responses. The local API still returns its own `ApiResult` failure envelope.
