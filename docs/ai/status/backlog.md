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
- Review whether hard-coded remote endpoints and tokens should move to configuration.
- Review whether printer lookup and print dispatch need stronger fault reporting for operators.
- Review whether `/api/printPDF` should also offload printer work through `executeBlocking` for consistency with `/api/print`.
- Review whether packaged-app process signatures are sufficiently stable across Windows and macOS installers, and add stronger matching if field feedback reveals false positives or false negatives.

## Product Backlog

- Decide whether authentication is needed for LAN deployment scenarios.
- Decide whether queued or retryable print jobs are required.
- Decide whether printer capability profiles or templates are needed for future integrations.
