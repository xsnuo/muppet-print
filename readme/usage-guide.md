# Usage Guide

Back to entry documents: [../README.md](../README.md) | [../README.zh-CN.md](../README.zh-CN.md)

## Typical Usage Scenario

1. Install and start Muppet Print on a machine that can access the target printers.
2. Open the desktop UI and confirm the service is running.
3. From your business system, query available printers.
4. Submit HTML or PDF print jobs to the local service.
5. Confirm output on the printer.

## Desktop UI

The desktop shell provides:

- Port input.
- Start and stop actions.
- Optional printer registration actions (`Signin local printer`, `Signed`) when `release.signin.enable=true`.
- Running status display.
- Error message display.
- Auto-start toggle on supported platforms.
- Tray minimization behavior where supported.
- Single-instance startup interception. A later launch is blocked by a local file lock and exits with an already-running prompt.

## Starting The Service

### Development

```bash
mvn spring-boot:run
```

### Packaged Application

Start the packaged app, then confirm the UI shows a running state. The embedded HTTP service will listen on the configured port.

If the configured web port is already occupied by another application, Muppet Print keeps the UI open and shows an English warning so the user can change the port and try again.
The UI enters `Stopped` instead of `Ready` when HTTP startup fails, and the web port input remains editable.

## Release Server Configuration

Release builds can configure remote callback behavior through Spring Boot config:

- `release.server.host`: server host name. If blank, Muppet Print skips remote error-log push.
- `release.server.token`: token used in callback headers with key `Muppet-Token`.
- `release.signin.enable`: controls desktop printer signin button visibility only (no remote dynamic loading for this flag).

The remote error callback target path is `/muppet/log`, appended directly to `release.server.host`.
`release.server.host` can include a base path. Example: `http://host/api` -> callback URL `http://host/api/muppet/log`.
See [server-api-requirements.md](server-api-requirements.md) for server contract details.

When printer signin is enabled:

- `Signin local printer` first loads groups from `GET /muppet/groups`; if group loading fails, signin is blocked and an error is shown.
- `Signin local printer` submits to `POST /muppet/signin` after local validation.
- The signin form validates printer selection and page width/height locally before any server request is sent.
- `Signed` opens the machine's signed-printer list loaded from `GET /muppet/signed?mac=<local-mac>`.
- The signed-printer list displays a `group` column and maps group values to group labels from `GET /muppet/groups`; unmatched values are shown as-is.
- The signed-printer list highlights local mismatch fields in red for quick operator review.

## Integration Flow

### Step 1. List Printers

Call `/api/getAllPrinters` and choose the correct printer name.

### Step 2. Submit HTML Or PDF

- Use `/api/print` for HTML-based templates, dynamic labels, and browser-style layouts.
- Use `/api/printPDF` for already-generated PDF files.

### Step 3. Handle Errors

Check the `code` and `message` fields in the JSON response. For example:

- `PARAM_ERROR` means the request is incomplete or invalid.
- `SERVICE_ERROR` usually means a printer or print execution problem.
- `SYSTEM_ERROR` means an unexpected internal failure occurred.

## HTML Printing Notes

- `pageWidth` and `pageHeight` are specified in millimeters.
- You can send extra files through the `imports` object.
- The runtime also copies bundled helper assets from the application `imports/` resources.
- If client-side rendering must finish before printing, set `waitJsReady=true` and make the page set `window.printReady = true`.

## Operational Recommendations

- Run the service on a stable workstation with consistent printers and fonts.
- Keep printer names stable if external systems rely on them.
- Prefer local or trusted LAN access only.
- Test HTML templates on the exact operating system and printer model used in production.

## Troubleshooting Hints

### Printer not found

- Confirm the printer is installed on the host machine.
- Use `/api/getAllPrinters` to verify the exact printer name.

### HTML layout differs from expectation

- Confirm fonts are installed or bundled.
- Confirm referenced assets resolve correctly.
- Re-check page width and height in millimeters.

### PDF upload fails

- Confirm multipart form submission is used.
- Confirm the uploaded file is a readable PDF.
- Confirm `printerNameOrId` is present.
