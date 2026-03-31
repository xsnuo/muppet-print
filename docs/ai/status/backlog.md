# Backlog

## Documentation Backlog

- Document the desktop start/stop lifecycle in more operational detail.
- Document resource import conventions for HTML printing, including expected relative paths.
- Document platform-specific auto-start behavior and packaging expectations.
- Add troubleshooting examples with real-world printer, font, and Playwright runtime failures.
- Add future documentation if Vert.x cluster-based print command exchange is implemented.

## Code And Architecture Backlog

- Review whether `UiStarter` should be decomposed to reduce UI and lifecycle coupling.
- Review whether print-job observability should include IDs, timing, and structured logs.
- Review whether `SystemException` should be integrated into the central error taxonomy or removed.
- Review whether printer lookup and print dispatch need stronger fault reporting for operators.
- Review whether `/api/printPDF` should also offload printer work through `executeBlocking` for consistency with `/api/print`.
- Review whether local file-lock based single-instance control is robust across Windows and macOS packaging/runtime edge cases, and add fallback diagnostics if field feedback reveals false positives or false negatives.

## Product Backlog

- Decide whether authentication is needed for LAN deployment scenarios.
- Decide whether queued or retryable print jobs are required.
- Decide whether printer capability profiles or templates are needed for future integrations.
