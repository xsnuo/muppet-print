# Current Iteration

## Iteration Goal

Improve code quality, harden the AI workflow rules, and align human-facing documentation with the AI memory system.

## Completed In This Iteration

- Reviewed the current codebase and identified high-priority implementation issues.
- Fixed `PrinterUtil` so bundled import resources are optional rather than a hard failure condition.
- Fixed `PrinterUtil.printPdf` so blank printer names no longer trigger a null-pointer path and print failures surface as service-level errors.
- Restored recursive temporary-directory cleanup in the HTML print flow.
- Improved `/api/printPDF` validation and response flow so it participates more cleanly in the shared API handling model.
- Strengthened `AGENTS.md` to require workflow recording and synchronized maintenance of `docs/ai/`, `README.md`, `README.zh-CN.md`, and `readme/`.
- Rewrote the human-facing English and Chinese README entry documents.
- Added `readme/` documentation for API reference, usage guidance, packaging guidance, and documentation navigation.
- Expanded `readme/` into an English-and-Chinese paired documentation set.
- Added the `human-rules/` sanctuary and recorded that human-authored Markdown rules override AI-generated repository rules.
- Reworked single-instance startup control to use local file-lock ownership, removing dependency on startup signature or local control ports.
- Changed web-port conflict handling so the UI remains open and shows an English message instead of treating the conflict as duplicate startup.
- Changed UI readiness transition so `Status: Ready !` is set only after Vert.x HTTP listen succeeds; if startup fails, UI now returns to `Status: Stopped` and keeps web port input editable.
- Moved remote error-log callback host, path prefix, and token to Spring Boot configuration under `release.server.*`.
- Changed remote error-log callback route to `/muppet/log` (resolved from `release.server.host`) and moved token passing from request body into `Muppet-Token` header.
- Added bilingual human-facing server API requirement documents for release callback contracts.
- Added `release.signin.enable` configuration and desktop UI entry buttons for printer signin management.
- Implemented desktop "Signin local printer" form flow with local device/printer prefill, numeric validation, and server POST `/muppet/signin` handling.
- Implemented desktop "Signed" list dialog with server GET `/muppet/signed` loading, table rendering, and mismatch highlighting for pc name/printer/url.
- Split desktop UI layout into `MuppetPrinterUi`, `SigninLocalPrinterUi`, and `SignedUi`, keeping `UiStarter` focused on lifecycle and state wiring.
- Reworked all three desktop pages to use the same half-font-size absolute positioning style, including red inline error feedback consistent with the main page.
- Fixed `SigninLocalPrinterUi` and `SignedUi` dialog sizing/close behavior by removing premature tiny-window show flow and setting robust minimum window sizes.
- Refactored runtime hierarchy to `SpringBoot -> (UI beans, Vertx web bean)`, and reduced `UiStarter` to pure Spring Boot bootstrap.
- Converted `MuppetPrinterUi`, `SigninLocalPrinterUi`, and `SignedUi` into Spring `@Component` beans with non-static interaction methods and bean-state-controlled close behavior.
- Added `TrayUi` bean and moved tray/window-close UI integration out of `UiStarter`.
- Converted `WebVerticle` into a Spring-managed lifecycle bean with explicit `start/stop` methods; `Run/Stop` now controls web service directly without starting/stopping Spring context.
- Added `UiMessageService` bean and removed static `UiStarter` UI message coupling from `VersionApi` and `ApiRootVerticle`.
- Set Spring Boot desktop startup to non-headless mode to prevent AWT `HeadlessException` during UI bean initialization.
- Hardened tray `Open` behavior to reliably restore and foreground the main window on macOS (menu click and tray icon click both use the same restore flow).
- Refactored tray integration so `MuppetPrinterUi` owns main-window show/hide behavior: close button now hides the main UI (without disposing), and tray `Open` re-shows the same main frame.
- Updated `additional-spring-configuration-metadata.json` with Chinese descriptions and expanded release properties, including an explicit ongoing-maintenance rule in AI memory.
- Updated server API requirement documents (English/Chinese) with a global unified response envelope rule (`code/message/data`) and printer signin endpoint contracts.
- Added `com.xuesinuo.muppet.webclient` wrappers to centralize outbound HTTP calls by business function (`VersionWebClient`, `SigninWebClient`, `SignedWebClient`, `LogWebClient`).
- Refactored `VersionApi` and `ApiRootVerticle` to consume webclient wrappers instead of issuing direct `WebClient` calls.
- Refactored `SigninLocalPrinterUi` and `SignedUi` to remove `java.net.http.HttpClient` usage and perform requests through injected webclient wrappers.
- Updated UI request flow so network responses are handled asynchronously and then rendered on the AWT UI thread, avoiding blocking Vert.x EventLoop callbacks.
- Centralized `release.server.*` property resolution into `ServerAccessConfig` under `com.xuesinuo.muppet.webclient`.
- Removed `release.server.*` injection and server URL assembly from `MuppetPrinterUi` and `ApiRootVerticle`; they now call webclient wrappers only.
- Kept `release.signin.enable` as a desktop UI-only toggle for signin button visibility.
- Reworked human-facing API and usage documents to remove historical interface wording and keep endpoint-specific request details inside the corresponding endpoint sections.
- Fixed signed-printer query callback path to include `/{prefix}` and aligned server docs accordingly.
- Reordered server API requirement docs so unified response envelope is documented before specific callback endpoints.
- Added per-field documentation for callback request/response payloads in server API requirement docs.
- Updated `SignedPrintRecord` to a JavaBean style DTO with `@Data`, private fields, and Chinese JavaDoc field descriptions.
- Changed `/muppet/signed` callback response key from `prints` to `printers` and aligned code/docs.
- Moved callback response parsing, success/failure handling, and result-typing into webclient layer with unified `Future<WebClientResult<T>>` return shape.
- Removed `release.server.prefix`; callback URLs are now resolved by appending `/muppet/*` endpoints directly to `release.server.host` (which may include a base path).
- Added `GET /muppet/groups` callback integration and wired desktop signin flow to require group loading before submit.
- Added `group` field to signin/signed callback payload handling and added group label mapping column in signed-printer UI.
- Added unified server-api observability logs in webclient wrappers using `@Slf4j`: request/response payloads now log at info level, and all non-success outcomes log at error level.
- Added signed-printer delete flow backed by `POST /muppet/signout` with payload `{mac, printerName, group}`, and refreshes signed list by reloading `/muppet/signed` after successful signout.
- Updated signed-printer desktop page layout: widened `pc name` and `url` columns, renamed `page width/page height` headers to `width/height`, widened dialog to fit all columns, increased table area height, and relies on scrollable table viewport for overflow rows.
- Added URL mode toggle button in signin UI (`use IP` / `use pc name`): default uses pc-name URL, click toggles URL text to local IP URL and back, and submit now sends the currently displayed URL value.
- Updated signin URL toggle to `use IP` / `use PC`, made URL textbox editable by operator, and added save-time URL validation (`http://` or `https://` prefix plus non-blank content after protocol).
- Hardened local print HTML generation to inject a UTF-8 meta charset declaration when missing, reducing Windows-side garbled Chinese text in local helper pages.
- Reworked signin IP-mode URL generation to preserve port/path/query segments even when the original host name is not URI-parser-friendly.
- Normalized LAN hostname URL generation with a unified rule: for URL host names, append `.local` whenever the suffix is not `.local`, regardless of operating system.
- Reworked duplicate-startup shutdown flow: after showing the "already running" dialog, the process now exits directly instead of closing Spring context during bean init, avoiding extra JVM-shutdown error dialogs on Windows.
- Completed production desktop UI migration from AWT controls to Swing controls while preserving existing business flow and absolute-position layout in main window, signin dialog, and signed-list dialog.
- Removed temporary Chinese-rendering troubleshooting paths and custom font injection logic from production UI code; Swing now uses system default font rendering behavior with system look-and-feel.
- Removed temporary standalone `TestUi` troubleshooting entry class after migration completion.

## Current Product Understanding

- The product remains a desktop-oriented local print bridge for trusted environments.
- Human-facing documentation is now explicitly split between entry README files and detailed guides under `readme/`.
- The repository now expects AI workflow memory and external documentation to evolve together.
- The repository now also has a human-authored rule space that AI must read but not modify.
- The desktop application is now intended to run as a single local instance per host user session, enforced by local file-lock ownership rather than process-signature matching.

## Immediate Risks Or Gaps

- Linux native packaging is not yet defined as a dedicated Maven packaging profile.
- There is still no formal example set for advanced HTML asset strategies beyond the API guide.

## Recommended Next Iteration Focus

- Decide whether to add Linux native packaging support and document it.
- Consider documenting cluster-based print instruction exchange if Vert.x cluster mode becomes a real feature.
