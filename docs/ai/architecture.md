# Architecture

## Runtime Shape

The application is a desktop-oriented Java process that combines a local GUI shell with an embedded HTTP service.

At runtime it is composed of four main layers:

1. Desktop shell and lifecycle entry.
2. Spring Boot dependency injection and startup.
3. Vert.x HTTP routing and async execution.
4. Print rendering and printer dispatch.

## Main Components

### UiStarter

`UiStarter` is the effective entrypoint for packaged use.

Responsibilities:

- Create and manage the AWT frame.
- Enforce single-instance startup using a local file lock under the current user home directory.
- Initialize tray integration.
- Manage application start/stop state.
- Trigger Playwright availability check.
- Start or stop the Spring application context.
- Surface operational messages to the local user.

### Spring Boot context

Spring Boot provides bean lifecycle and component discovery. It wires the router, Vert.x instance, web client, APIs, and verticles.

### Vert.x infrastructure

Vert.x is used for:

- Router creation.
- HTTP server creation.
- Async offloading with `executeBlocking`.
- Unified API failure handling.

This is a hybrid architecture: Spring manages object construction, while Vert.x handles request execution.

### API root handling

`ApiRootVerticle` defines shared behavior for `/api/*` routes.

Responsibilities:

- Apply body handling.
- Set JSON response defaults.
- Log incoming API access.
- Close requests at the end of the route chain.
- Convert exceptions into the standard `ApiResult` envelope.
- Forward unexpected errors to a remote logging endpoint.

### Business API components

`PrinterApi` provides:

- Printer listing.
- HTML print submission.
- PDF upload print submission.

`VersionApi` provides:

- Local application version reporting.
- Remote latest-version lookup.

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

## Deployment Shape

The project is intended to be packaged as a desktop application for Windows, macOS, and Linux. The current repository also builds a Spring Boot JAR, but the operator model clearly assumes a local GUI host with printer access.

The startup path now enforces a single-instance desktop model by acquiring a local file lock before UI initialization. A later-launched process shows an already-running prompt and then exits.

The start-state transition now waits for Vert.x HTTP listen success before marking the UI as ready. If listen fails (for example because the port is occupied), the UI returns to `Stopped` and re-enables the port input.

## External Dependencies And Integrations

- Playwright Chromium runtime for rendering.
- Local OS printer subsystem.
- Remote version endpoint at `www.xuesinuo.com`.
- Remote error log endpoint at `test-wms.foodsup.com`.

## Architectural Strengths

- Directly optimized for local silent printing.
- Clear separation between request intake and print execution utilities.
- Stable API envelope for callers.
- Cross-platform intent at the packaging and startup level.

## Architectural Risks

- UI, lifecycle, and application startup are tightly coupled in `UiStarter`.
- Some external endpoints and tokens are hard-coded.
- The system assumes trusted callers and permissive CORS.
- The current implementation depends on host-specific printer and browser behavior.
- There is limited observable job lifecycle beyond synchronous success/failure.
