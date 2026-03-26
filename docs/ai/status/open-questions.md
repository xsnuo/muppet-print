# Open Questions

## Product Scope

- Is the intended primary deployment still desktop packaging, or should headless service deployment also become a first-class scenario?
- Is the trust model intentionally limited to localhost or trusted LAN callers, or should the product evolve toward authenticated access?

## Printing Behavior

- What are the exact compatibility expectations for label printers, receipt printers, and standard A4 printers?
- Should HTML print requests support richer print options such as margins, copies, orientation, or printer tray selection?
- Is `waitJsReady` considered stable product behavior or still an implementation convenience?

## Runtime And Operations

- What is the intended source of truth for the effective HTTP port when UI defaults and verticle defaults differ?
- Should temporary print artifacts be deleted aggressively, retained for debugging, or made configurable?
- Should remote error reporting remain mandatory, optional, or environment-configured?
- Should printer failures be exposed with more operator-friendly messages in the desktop UI as well as the API layer?
- Should the packaged desktop application expose a more explicit diagnostic for why a process was judged to be an existing Muppet Print instance?

## API Design

- Should `/api/printPDF` be normalized to the same async/failure pattern used by `/api/print`?
- Should the API publish a formal schema or example contract for client integrations?
- Should the API document stricter constraints for multipart field names and supported PDF file size?

## Product Management

- What versioning and release policy should govern API compatibility for downstream clients?
