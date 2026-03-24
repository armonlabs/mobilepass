# PassFlow Migration Roadmap and Handoff Prompt

## Immediate Goal (implemented)

Prevent crash caused by null access on `activeQRCodeContent` during async callbacks.

What was done in `PassFlowManager`:

- Added null-safe helper accessors:
  - `getActiveDirection()`
  - `getActiveClubId()`
  - `getActiveClubName()`
- Replaced direct `activeQRCodeContent.qrCode` and `activeQRCodeContent.clubInfo` completion payload reads with helper accessors.
- Behavior now: if active QR data is unavailable, completion payload fields become `null` instead of throwing `NullPointerException`.

## Planned Architecture Change

Target architecture:

- Use `lastQRCodeContent` as main QR context for processing.
- Keep a single event list (instead of separate `states` and `logStates`) without breaking external API signatures.
- Introduce `flowId` and callback guards to prevent stale callback writes.
- Remove `setQRData` and derive fallback IDs from `lastQRCodeContent`/flow metadata.

## Constraints

- Do not break external API contracts used by app layer.
- Preserve existing delegate callback shapes and result semantics.
- Prefer incremental migration in safe phases.

## Phased Plan

1. Compatibility Layer

- Add internal `flowId` fields and completion guard (`completeFlowOnce` style helper).
- Keep current public methods (`getStates`, `getLogStates`, `getLastClubId`, `getLastQRCodeId`) unchanged.

2. Flow Guarding

- Generate `activeFlowId` for each `processQRCode`.
- Capture request flow id in all async boundaries:
  - BLE delegate callbacks
  - BLE timeout runnable
  - location timeout runnable
  - `remoteOpen` callbacks
- Ignore callbacks when flow id mismatches.

3. Single List Transition

- Introduce one internal event store with timestamp.
- Make `getStates()` and `getLogStates()` return projections from same store.
- Keep method signatures unchanged to avoid external impact.

4. lastQRCodeContent Transition

- Add `lastQRCodeContent` and migrate reads from `activeQRCodeContent` gradually.
- Keep null-safe fallback helpers during transition.

5. Remove `setQRData`

- Stop writing `lastQRCodeId/lastClubId` via `setQRData`.
- Derive these from `lastQRCodeContent` or flow metadata.
- Keep getter methods, but update their source.

6. Cleanup

- Remove obsolete fields and helper paths after behavior parity is confirmed.
- Add retention policy for event list if needed.

## Testing Checklist

- Cancel flow while remote request is in-flight: no crash, no stale completion overwrite.
- Start a new QR flow before old callbacks return: old callbacks ignored.
- BLE timeout and location timeout still report expected result codes.
- Analytics payload is tied to correct flow context.
- `getStates()` and `getLogStates()` remain stable for existing delegate consumers.

## Prompt for New Chat

Use this in a new chat to continue implementation:

"I have a MobilePass Android SDK codebase. Continue migration in `android/mobilepasssdk/src/main/java/com/armongate/mobilepasssdk/manager/PassFlowManager.java` using the roadmap in `docs/passflow-migration-roadmap.md`.

Current status:

- Immediate null crash was patched by replacing direct completion payload reads with null-safe helper accessors.
- Next priority is phased migration with no external API break.

Please implement Phase 1 and Phase 2 only:

1. add flowId-based async callback guards,
2. add completion idempotency guard,
   while preserving current public method signatures and delegate behavior.

After coding:

- run compile/error checks,
- summarize changed lines and residual risks,
- do not start single-list or lastQRCodeContent migration yet."
