# Domain Model

## Overview

The project models a small but concrete printing domain centered on local print execution.

The core domain concept is not a persisted entity model. It is an operational model made of request payloads, printer descriptors, result envelopes, and runtime application state.

## Core Concepts

### Application State

The desktop shell tracks coarse process state through `UiStarter.appState`.

Known values in current code:

- `0`: stopped
- `1`: starting
- `2`: running
- `3`: stopping

This is currently a primitive shared state rather than a dedicated enum.

### Printer

Represented by `PrinterUtil.PrinterInfo`.

Fields:

- `id`: printer identifier, currently the same as the printer name.
- `name`: display/selection name.
- `description`: optional descriptive metadata from the local print service.

This is a discovery DTO exposed through the API.

### HTML Print Request

Represented by `PrinterApi.PrintParam`.

Fields:

- `html`: the HTML document to render.
- `printerNameOrId`: target printer identifier.
- `pageWidth`: page width in millimeters.
- `pageHeight`: page height in millimeters.
- `imports`: optional map of relative asset paths to file content.
- `waitJsReady`: optional flag that tells the renderer to wait until `window.printReady === true`.

This is the main business input for dynamic print rendering.

### PDF Print Request

The PDF print API currently uses multipart upload rather than an explicit DTO.

Effective fields are:

- uploaded PDF file bytes.
- `printerNameOrId` form field.

### API Result Envelope

Represented by `ApiResult<T>` and `ApiResultCode`.

Envelope fields:

- `code`: result code.
- `message`: optional human-readable message.
- `data`: structured object payload.

Result codes:

- `SUCCESS`
- `PARAM_ERROR`
- `SYSTEM_ERROR`
- `SERVICE_ERROR`

Important rule:

The current implementation forbids primitive-like direct payload shapes such as raw strings, numbers, booleans, arrays, collections, and closeables. API success payloads should be wrapped in an object structure.

### Exception Types

Current exception taxonomy:

- `ParamException`: invalid or incomplete client input.
- `ServiceException`: expected business or operational failure.
- `SystemException`: declared but not materially integrated into the central handler.

### Version Information

`VersionApi.VERSION` represents the current packaged application version.

The version domain also includes a remotely fetched latest-version string used for upgrade notification.

## Domain Relationships

- A client submits a print request.
- A print request targets one local printer.
- A print request may include supporting import assets.
- A rendered print job becomes PDF bytes before printer dispatch.
- Every API operation returns an `ApiResult` envelope.
- Exceptions map into envelope-compatible failure responses.

## Implicit Domain Rules

- A target printer must exist locally.
- HTML print jobs must specify page dimensions.
- Imported asset paths are materialized relative to a temporary print workspace.
- Local rendering is trusted to execute required client-side layout logic before printing.
- The service prioritizes operational simplicity over persisted domain history.
