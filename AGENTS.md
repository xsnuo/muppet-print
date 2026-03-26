# AGENTS.md

## Purpose

This repository hosts Muppet Print, a local or LAN-accessible print gateway. The application exposes HTTP APIs that accept HTML or PDF payloads, renders printable content locally, and sends output to installed printers without user interaction in the browser.

This file defines how AI agents should understand the project, how they should modify code, and how they should maintain durable project memory under `docs/ai/`.

## Source Of Truth

When working in this repository, AI agents should treat the following files as the maintained project memory set:

1. `AGENTS.md`
2. `docs/ai/project-overview.md`
3. `docs/ai/architecture.md`
4. `docs/ai/domain-model.md`
5. `docs/ai/decisions/ADR-001-memory-driven-documentation.md`
6. `docs/ai/known-constraints.md`
7. `docs/ai/status/current-iteration.md`
8. `docs/ai/status/backlog.md`
9. `docs/ai/status/open-questions.md`

AI agents must also read any human-authored rule files under `human-rules/` before taking non-trivial action. If human-authored rules conflict with AI-maintained rules or documentation conventions, the human-authored rules take precedence.

Any meaningful code change should be evaluated against these files and, if needed, the relevant documents should be updated in the same task.

## Project Summary

The current implementation combines:

- Spring Boot for application bootstrap and bean management.
- Vert.x for HTTP routing and async request handling.
- Playwright Chromium for HTML rendering and PDF generation.
- Java Print Service plus PDFBox for physical printer delivery.
- AWT desktop UI and tray integration for local operator control.

The product is primarily a desktop-packaged local service, not a generic public web service.

## Coding Style Observed In Existing Code

AI agents should preserve the existing code style unless a targeted refactor is explicitly required.

- Language: Java 21.
- Framework style: Spring bean wiring with Vert.x router/verticle runtime.
- Existing comments are primarily Chinese. Keep code comments and programming communication in Chinese.
- AI narrative documents may use English for clarity and consistency.
- Class organization is simple and direct; avoid over-abstraction.
- Lombok is already used and should remain acceptable for DTOs and boilerplate reduction.
- APIs generally return a uniform `ApiResult` JSON envelope.
- Exceptions are used to drive API failure handling through a shared Vert.x failure handler.
- Utility classes are mostly static and pragmatic.
- Existing naming mixes concise English technical names with a few legacy spellings. Preserve public API compatibility unless a change is explicitly required.

## Programming Constraints For Future Changes

- Prefer minimal, local changes over wide refactors.
- Preserve current public API routes unless the task explicitly asks for API changes.
- Keep response contracts compatible with `ApiResult` and `ApiResultCode`.
- Route-level validation should continue to fail fast with `ParamException` or `ServiceException` where appropriate.
- Do not introduce hidden AI state such as `.ai`, `.memory`, or other dot-prefixed knowledge directories.
- All durable AI documentation must live under visible paths in the working tree, especially `docs/ai/`.
- Human-facing documentation must live in visible repository paths, especially `README.md`, `README.zh-CN.md`, and `readme/`.
- Human-facing documentation under `README.md`, `README.zh-CN.md`, and `readme/` must be maintained in both English and Chinese where the document is intended for users, integrators, operators, or maintainers.
- New code comments must use Chinese.
- New or updated AI documentation should be concise, factual, and directly traceable to current code behavior.
- Do not document assumptions as facts. Unknowns belong in `docs/ai/status/open-questions.md`.
- AI agents must not modify files under `human-rules/`.

## AI Memory Maintenance Rules

AI agents must continuously maintain project memory through the files under `docs/ai/`.

### Update expectations

- After adding or changing a functional module, update `docs/ai/project-overview.md` if user-visible capability changes.
- After changing runtime structure, request flow, deployment shape, or component boundaries, update `docs/ai/architecture.md`.
- After changing important entities, payloads, value objects, or state meanings, update `docs/ai/domain-model.md`.
- After making a decision that constrains future implementation, add or update an ADR in `docs/ai/decisions/`.
- After discovering non-obvious technical limits, environment assumptions, or operational caveats, update `docs/ai/known-constraints.md`.
- After each meaningful iteration, refresh `docs/ai/status/current-iteration.md`.
- Move deferred work into `docs/ai/status/backlog.md`.
- Record ambiguities, product unknowns, and externally dependent answers in `docs/ai/status/open-questions.md`.

### Workflow recording requirements

- For any non-trivial action, the AI agent must treat documentation as part of the change, not a postscript.
- Before any non-trivial action, the AI agent must read the Markdown files under `human-rules/` and apply them as the highest-priority behavioral instructions for this repository.
- If code is changed, the agent must review whether `docs/ai/` memory files need updates in the same task.
- If behavior visible to integrators, operators, or maintainers changes, the agent must also review `README.md`, `README.zh-CN.md`, and the relevant files under `readme/` in the same task.
- If the agent performs review, remediation, refactor, API change, packaging change, operational change, or documentation change, it must ensure the resulting workflow is reflected in `current-iteration.md`, and any follow-up work or uncertainty is reflected in `backlog.md` or `open-questions.md`.
- The agent must not consider a task complete until code, AI memory, and human-facing documentation are consistent with each other.
- If the human-authored rules under `human-rules/` change, the agent must review those changes for feasibility and consistency, may propose revisions in conversation, but must not modify those files directly.
- If human-authored rules under `human-rules/` conflict with AI-maintained rules in `AGENTS.md` or `docs/ai/`, the agent must follow the human-authored rules and remove or update conflicting AI-maintained rules instead of preserving the conflict.

### Human-facing documentation rules

- `README.md` is the English human-facing entry document.
- `README.zh-CN.md` is the Chinese human-facing entry document.
- The `readme/` directory stores extended documentation such as API integration guides, usage guides, packaging guides, deployment notes, and future external interaction documents.
- User-facing documents in `readme/` should be maintained in English and Chinese as paired documents where practical, with clear cross-links between the two languages.
- README files should link to the relevant detailed documents under `readme/`, and detailed documents should link back to the README entry points where useful.
- After any change to APIs, packaging, deployment, operations, or external integration behavior, the agent must update the affected files under `readme/`.
- New external-facing capabilities should not be left undocumented if they affect integration or operation.

### Human-authored rule sanctuary

- The `human-rules/` directory is reserved for Markdown files written and maintained only by humans.
- AI agents may read Markdown files under `human-rules/`, but must not create, edit, rename, or delete files there after the directory baseline has been established.
- Files under `human-rules/` are repository-local behavioral authority for AI work in this project.
- If a rule in `human-rules/` conflicts with AGENTS.md, docs/ai conventions, or other AI-generated instructions, the human rule wins.
- When such a conflict is detected, the agent must update AI-generated rules outside `human-rules/` to remove the conflict.
- If the agent believes a human-authored rule is unsafe, contradictory, or impractical, it may raise a recommendation to the user, but it must not directly modify the human-authored rule files.

### Writing rules

- Use English for AI memory documents unless a section is clearer in Chinese.
- Keep sections scannable and operational.
- Distinguish clearly between `Current State`, `Decision`, `Constraint`, `Planned Work`, and `Open Question`.
- Avoid repeating the same fact in every file; link concepts by reference and keep each file purpose-specific.
- Prefer updating an existing document over creating redundant new documents.

### Minimum iteration discipline

When an AI agent completes a non-trivial code task, it should ensure:

1. `current-iteration.md` reflects the latest completed changes.
2. New deferred follow-up work is reflected in `backlog.md`.
3. Any unresolved ambiguity is reflected in `open-questions.md`.

## Implementation Guidance

- Treat `UiStarter` as the operational entrypoint for packaged desktop usage.
- Treat Vert.x router configuration and root API failure handling as central integration points.
- Treat `PrinterUtil` as the core printing domain service boundary.
- Keep rendering/printing responsibilities separate from HTTP request parsing where possible.
- Be careful with OS-specific behavior such as tray support and auto-start integration.
- Be careful with printing behavior that depends on locally installed browsers, printers, fonts, or native OS facilities.

## Documentation Priority

If code and documentation diverge, AI agents should:

1. Verify actual code behavior first.
2. Update the relevant `docs/ai/` files.
3. Note unresolved ambiguity in `open-questions.md` instead of inventing certainty.
