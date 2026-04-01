# Architecture

## Runtime Shape

The application is a desktop-oriented Java process where Spring Boot is the top-level runtime host for both UI and web runtime components.

At runtime it is composed of four main layers:

1. Spring Boot dependency injection and startup.
2. Desktop UI beans and tray integration.
3. Vert.x HTTP router/web runtime beans.
4. Print rendering and printer dispatch.

## Main Components

### UiStarter

`UiStarter` is now a pure Spring Boot bootstrap entrypoint.

Responsibilities:

- Start Spring Boot context only.

UI lifecycle and page layout are split under `com.xuesinuo.muppet.ui` and managed as Spring beans:

- `MuppetPrinterUi`: main desktop shell frame, single-instance lock, UI-state transitions, and Run/Stop actions.
- `SigninLocalPrinterUi`: local printer signin dialog with client-side validation and bean-state-driven open/close behavior.
- `SignedUi`: signed-printer list dialog with mismatch highlighting and bean-state-driven open/close behavior.
- `TrayUi`: system tray integration and application exit bridge.
- `UiMessageService`: shared UI message channel for API/runtime components.

### Spring Boot context

Spring Boot provides bean lifecycle and component discovery. It wires router, Vert.x runtime, API components, and UI components in the same container.

### Vert.x infrastructure

Vert.x is used for:

- Router creation.
- HTTP server creation and shutdown through `WebVerticle` bean start/stop methods.
- Async offloading with `executeBlocking`.
- Unified API failure handling.

This remains a hybrid architecture: Spring manages lifecycle and orchestration, while Vert.x handles HTTP request execution.

### API root handling

`ApiRootVerticle` defines shared behavior for `/api/*` routes.

Responsibilities:

- Apply body handling.
- Set JSON response defaults.
- Log incoming API access.
- Close requests at the end of the route chain.
- Convert exceptions into the standard `ApiResult` envelope.
- Forward unexpected errors to a remote logging endpoint.

Route declaration is split by responsibility:

- Shared `/api/*` cross-cutting handlers remain in `ApiRootVerticle`.
- Functional endpoints are declared in dedicated classes under `com.xuesinuo.muppet.api`.

### Business API components

`PrinterApi` provides:

- Printer listing.
- HTML print submission.
- PDF upload print submission.

`VersionApi` provides:

- Local application version reporting.
- Remote latest-version lookup.

### Outbound webclient components

Outbound HTTP calls are encapsulated under `com.xuesinuo.muppet.webclient`.

- `VersionWebClient`: remote latest-version query.
- `SigninWebClient`: remote signin POST for local printer metadata.
- `SignedWebClient`: remote signed-printer list query.
- `LogWebClient`: remote error-log callback push.
- `ServerAccessConfig`: server host/prefix/token resolution for webclient wrappers.
- `WebClientResult<T>`: unified typed result envelope returned by webclient wrappers.

UI and API/config beans should consume these wrappers instead of direct HTTP client usage.
Desktop UI should not assemble remote server URL/token details.

### Print engine

`PrinterUtil` is the operational core of printing.

Responsibilities:

- Discover installed printers.
- Build a temporary print workspace.
- Materialize incoming import assets.
- Copy bundled classpath import resources.
- Render HTML via headless Chromium.
- Generate PDF bytes.
- Dispatch PDF content through Java print infrastructure.

### PDF generation utility

`PdfGenerator` is a related utility focused on HTML-to-PDF generation. It appears to be an auxiliary capability and example-oriented helper rather than the primary API print path.

## Request Flow

### HTML print request

1. Client calls `/api/print`.
2. `PrinterApi` parses JSON body into `PrintParam`.
3. Required fields are validated.
4. Blocking print work is delegated via Vert.x `executeBlocking`.
5. `PrinterUtil.printHtml` writes HTML and assets into a temporary directory.
6. Bundled import resources are copied into the workspace.
7. Playwright Chromium loads the generated local HTML file.
8. Page output is converted into PDF bytes.
9. PDF bytes are sent to the chosen printer.
10. API response returns success or standardized failure.

### PDF print request

1. Client calls `/api/printPDF` with multipart form data.
2. Uploaded file bytes are read from the temporary upload path.
3. `PrinterUtil.printPdf` dispatches the PDF to the target printer.
4. API response returns success or standardized failure.

### Printer listing request

1. Client calls `/api/getAllPrinters`.
2. Printer discovery runs in a blocking worker path.
3. Results are wrapped under `data.printers` in `ApiResult`.

### Signed-printer callback payload

The release callback query endpoint returns signed-printer records under `data.printers`.

## Deployment Shape

The project is intended to be packaged as a desktop application for Windows, macOS, and Linux. The current repository also builds a Spring Boot JAR, but the operator model clearly assumes a local GUI host with printer access.

The startup path now enforces a single-instance desktop model in `MuppetPrinterUi` by acquiring a local file lock during bean initialization. A later-launched process shows an already-running prompt and closes the Spring context.

The `Run` and `Stop` buttons now control web runtime directly through `WebVerticle` bean start/stop calls, without rebuilding or stopping Spring context.

## External Dependencies And Integrations

- Playwright Chromium runtime for rendering.
- Local OS printer subsystem.
- Remote version endpoint at `www.xuesinuo.com`.
- Optional remote callback integration configured by `release.server.host` and `release.server.token`, with fixed endpoints under `/muppet/*`.

## Architectural Strengths

- Directly optimized for local silent printing.
- Clear separation between request intake and print execution utilities.
- Stable API envelope for callers.
- Cross-platform intent at the packaging and startup level.

## Architectural Risks

- UI lifecycle and operational orchestration remain centralized in `MuppetPrinterUi` and may still require future decomposition.
- The system assumes trusted callers and permissive CORS.
- The current implementation depends on host-specific printer and browser behavior.
- There is limited observable job lifecycle beyond synchronous success/failure.
