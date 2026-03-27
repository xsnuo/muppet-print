# Muppet Print

Muppet Print is a desktop-hosted local print gateway for silent printing. It accepts HTML or PDF payloads over HTTP, renders printable output on the host machine, and dispatches jobs to locally installed printers without opening the browser print dialog.

The project is designed for local workstation or trusted LAN scenarios such as shipping labels, warehouse task sheets, reception forms, and device-driven printing from browser-based business systems.

## What It Does

- Exposes local HTTP APIs for printer discovery and print submission.
- Renders HTML with Playwright Chromium before sending the result to a physical printer.
- Prints uploaded PDF files directly through the local print stack.
- Provides a desktop UI with tray support, start/stop controls, and auto-start support for supported platforms.
- Enforces single-instance startup through a local file lock, so repeated launches do not create duplicate processes.

## Architecture At A Glance

- Spring Boot manages application bootstrap and bean wiring.
- Vert.x handles routing, HTTP serving, and asynchronous request execution.
- Playwright Chromium renders HTML into print-ready PDF output.
- Java Print Service and PDFBox send output to local printers.

For the AI-maintained project memory and architecture notes, see [docs/ai/project-overview.md](docs/ai/project-overview.md) and [docs/ai/architecture.md](docs/ai/architecture.md).

## Documentation Map

- Chinese entry: [README.zh-CN.md](README.zh-CN.md)
- API reference: [readme/api-reference.md](readme/api-reference.md)
- Usage guide: [readme/usage-guide.md](readme/usage-guide.md)
- Packaging guide: [readme/packaging-guide.md](readme/packaging-guide.md)
- Documentation index and future integration topics: [readme/README.md](readme/README.md)

## Quick Start

### 1. Requirements

- JDK 21
- Maven 3.9 or compatible version
- A machine with local printer access
- Playwright runtime available to the packaged app or development environment

### 2. Run In Development

```bash
mvn spring-boot:run
```

The application uses `com.xuesinuo.muppet.UiStarter` as the main class, so local development starts the desktop shell as well as the embedded HTTP service.

### 3. Default Local Address

The UI defaults to port `58080`, so a typical local endpoint is:

```text
http://127.0.0.1:58080
```

## Main API Endpoints

- `GET/POST /api/getAllPrinters`
- `POST /api/print`
- `POST /api/printPDF`
- `GET/POST /api/version`

Full request and response examples are in [readme/api-reference.md](readme/api-reference.md).

## Intended Integration Pattern

```text
Business System -> HTTP request -> Muppet Print -> Local Printer
```

This avoids relying on `window.print()` and gives external systems a stable local print bridge.

## Current Scope

The current codebase is optimized for:

- local or trusted LAN use
- silent printing workflows
- desktop-packaged deployment

It is not currently designed as a public internet-facing print platform.

## Development Notes

- Code comments and engineering communication in this project should use Chinese.
- AI-maintained memory is stored in visible repository paths under `docs/ai/` and guided by [AGENTS.md](AGENTS.md).
- When functionality, API behavior, packaging, or usage changes, both AI memory and human-facing documentation under `readme/` should be updated together.
