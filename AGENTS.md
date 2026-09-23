# AGENTS.md

## Project Overview

- Project name: `yanga3` (Yanga / NGA Android client).
- Goal: Build a modern NGA forum Android client with Kotlin + Jetpack Compose + Material Design 3.
- Current architecture direction:
  - API capability and transport behavior are documented first.
  - UI follows board-first information architecture with 4 primary tabs: Home / Boards / Messages / Profile.

## Key Documentation

### Product / Planning

- PRD: `D:\dev\yanga3\docs\prd.md`
- UI design spec: `D:\dev\yanga3\docs\ui-design-spec.md`
- Test report: `D:\dev\yanga3\docs\test-report.md`

### API Documentation

- Primary API reference: `D:\dev\yanga3\docs\api-reference.md`

## Build / Compile Execution Policy

### Mandatory Rule

- All Gradle compile/build actions for this project must run **outside sandbox**.
- Do not run compile/build tasks in sandbox first.

### Applies To

- `./gradlew assemble`
- `./gradlew build`
- module-scoped assemble/build tasks (for example `:app:assembleDebug`)
- any equivalent compile/package task

### Tool Call Requirement

When invoking build/compile commands, always set:

- `sandbox_permissions: "require_escalated"`
- concise `justification` stating this project requires out-of-sandbox Gradle build execution

## Test Execution Policy

- This repository also requires Gradle test commands to run outside sandbox.
- For any Gradle test command (unit/instrumented/filtered), use:
  - `sandbox_permissions: "require_escalated"`
  - concise `justification`

### Preserve Local App Data (Mandatory)

- Local emulator/device testing must use an in-place APK update (`adb -s <serial> install -r <apk>`), preserving login cookies, preferences, and cached data.
- Never uninstall the app, run `pm clear`, clear its storage, or use a test runner that automatically uninstalls/resets the target app. Do not fix installation errors by uninstalling.
- For instrumentation tests, build the app and test APKs outside sandbox, install both with `adb install -r`, and invoke `adb shell am instrument` directly. Do not use `connectedAndroidTest` / `connectedDebugAndroidTest` or managed-device tasks on the user's local emulator unless data-preserving behavior has been explicitly verified; their cleanup may uninstall the app.
- If a clean installation is necessary, use a separate disposable emulator/application ID. Clearing the user's existing installation requires their explicit approval.

## Commit Message Convention

- Use Conventional Commits-style messages based on the repository history.
- Format commit subjects as `<type>: <summary>`.
- Common types in this repository:
  - `feat:` for user-facing features or behavior additions.
  - `fix:` for bug fixes and regressions.
  - `chore:` for maintenance, metadata, assets, or non-feature project updates.
- Keep the summary short, imperative, and in English.
- Do not add issue references or body text unless the change needs extra context.

## Collaboration Notes for Agents

- Prefer `docs/api-reference.md` for endpoint contract and parameter coverage.
- Use `docs/api-research.md` for transport quirks:
  - headers
  - cookie/session behavior
  - encoding (GBK/GB18030 context)
  - wrapped response cleanup
- Keep code changes aligned with board-first UI structure defined in `docs/ui-design-spec.md`.
