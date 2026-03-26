# Project Overview

## Product Intent

Muppet Print is a desktop-hosted print bridge for scenarios where business systems need reliable local or LAN printing without relying on browser-native print dialogs.

The application accepts printable payloads over HTTP, renders them locally, and dispatches them to printers installed on the host machine. This makes it suitable for warehouse labels, courier slips, front-desk forms, and other operational print flows that require predictable output and low operator friction.

## Core Capabilities

### 1. Local HTTP print gateway

The application starts a local HTTP server, intended to run on a workstation or packaging target, and exposes APIs under `/api/*`.

### 2. HTML-based silent printing

Clients can submit HTML, dimensions, printer identity, and optional imported assets. The service renders the HTML with Playwright Chromium, produces print-ready PDF content, and sends it to a physical printer.

### 3. PDF direct printing

Clients can upload a PDF file and target printer name. The service reads the uploaded PDF bytes and sends them to the print pipeline.

### 4. Printer discovery

Clients can query the locally installed printers and use the returned names to target subsequent print jobs.

### 5. Desktop operator shell

The application includes an AWT-based desktop shell with tray support, status display, start/stop controls, and auto-start handling for supported operating systems.

### 6. Version check

The service exposes a version API and also checks a remote endpoint for newer published versions.

## Target Usage Pattern

The typical flow is:

1. A web application or local business client sends print data to the local Muppet Print service.
2. Muppet Print resolves assets, renders content, and dispatches the print job on the host machine.
3. The client receives a simple success or error response through a stable JSON contract.

## Primary Functional Modules

### UI bootstrap module

The desktop UI is responsible for local lifecycle control, visual status, startup behavior, and packaged-app usability.

### API module

The API layer exposes endpoints for printer discovery, print submission, PDF submission, and version reporting.

### Runtime integration module

Spring Boot manages components, while Vert.x owns the router, web server, request handling, and failure flow.

### Print execution module

The print execution module converts incoming print requests into rendered PDF output and sends that output to the local print stack.

### Resource support module

Classpath resources under `src/main/resources/imports/` provide CSS and JS assets commonly referenced by printable documents.

## Current Non-Goals

- Multi-tenant cloud hosting.
- Long-term job queue persistence.
- Advanced print job tracking and auditing.
- Fine-grained printer capability negotiation.
- Authentication and authorization for hostile network environments.

## Success Criteria Implied By The Current Design

- Fast local deployment.
- Minimal operator steps.
- Consistent silent printing behavior.
- Simple HTTP integration from browser or business software.
- Cross-platform packaging compatibility for major desktop operating systems.
