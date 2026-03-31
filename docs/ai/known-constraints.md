# Known Constraints

## Runtime Constraints

- The service is designed to run on a machine that has direct access to local printers.
- HTML rendering depends on Playwright and a usable Chromium runtime.
- Printing behavior may vary by operating system, installed printers, fonts, and drivers.
- The application assumes a desktop-capable environment because it includes AWT UI and tray behavior.

## API Constraints

- All API endpoints are currently exposed under `/api/*`.
- Request body handling for API routes is capped at 1 MB by the shared body handler.
- API success payloads are expected to use object-shaped data because `ApiResult.ok` rejects several primitive-like result types.
- The current CORS configuration is permissive and allows all origins.

## Operational Constraints

- The default packaged port implied by the UI is `58080`.
- Single-instance ownership is enforced through a local lock file under the current user home directory.
- The actual HTTP server port is controlled through `WebVerticle.port` and startup flow; this coupling should be preserved carefully.
- Printer names are the practical addressing key for print dispatch.
- Temporary files are created during rendering and PDF handling.

## Startup Constraints

- Duplicate process detection should rely on file-lock ownership rather than the configurable web service port.
- Web port conflicts should not be treated as proof of another Muppet Print instance; they should be surfaced in the UI so the user can choose a different port.

## Codebase Constraints

- The project uses a hybrid Spring Boot + Vert.x model rather than pure Spring MVC.
- Error handling is centralized in Vert.x route failure handling rather than distributed controller advice.
- Release callback server connectivity depends on `release.server.*` properties, and callback push is skipped when `release.server.host` is blank.
- `SystemException` exists but is not distinctly handled in the current API root failure flow.

## Packaging Constraints

- The project is expected to support Windows, macOS, and Linux packaging scenarios.
- Auto-start support is implemented only for specific operating system paths and conventions.
- Host machines must allow local GUI/process execution; this is not currently a headless server-first architecture.

## Documentation Constraints

- Durable AI memory must remain in visible repository paths.
- AI narrative documents may use English.
- Programming communication and code comments should use Chinese.
