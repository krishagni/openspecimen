# Global Codex Instructions

- The UI code in `www` sub-directory is deprecated. This directory should be ignored. No changes should be made to any file within this sub-directory.

## Code organization

- Methods that are not used by templates/views should start with `_`. Applies only to the UI code.
- Methods should occur in the order of their usage.
- For UI components, keep template/view-facing methods first, in template usage order, followed by internal helper methods.
- Avoid duplicating logic. When multiple code paths perform substantially the same operation, extract and reuse a common abstraction while keeping path-specific behaviour at the call sites.
- Avoid redundant guards and checks when the preceding control flow has already established the required invariant.
- Order method parameters from broader or higher-level context to narrower or derived details, while preserving established API conventions and prioritising clarity at call sites.
- Match the existing formatting and indentation style of every file you modify; do not introduce a different whitespace convention into edited sections.
- Add an empty line after each control-flow block, including every `if`, `else if`, and `else` branch, before the next statement.
- When an enum spans multiple lines, add one empty line after each constant or enum value except the last one.
- Liquibase change sets should always be appended. They cannot be added in the middle in ad-hoc manner.
- When adding messages to `messages.properties`, ensure the message is in the same order as the error code constant. Also ensure there is an empty line before and after the message.

## Frontend routing/state

- Prefer passing route query values as component props from the route definition.
- When a component prop exists, use the prop directly instead of mirroring it through `ctx` or another local state object.

## Verification

- If Gradle fails with `Failed to load native library 'libnative-platform.dylib'` because it cannot write under `~/.gradle/native`, rerun the Gradle command with elevated permission.
