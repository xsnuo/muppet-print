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

## 5. Operational Behavior

- Missing `release.server.host` must not cause runtime errors. Muppet Print skips callback push directly.
- Callback push failures must not break local API failure responses. The local API still returns its own `ApiResult` failure envelope.
