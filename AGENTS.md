# AGENTS.md

- When building or installing an APK, use the release variant only unless the user explicitly asks for debug.
- When installing an APK on a connected Android device, always install for user `0` only.
- Use `adb install -r -g --user 0` for release APK installs unless the user explicitly asks for a different user or install mode.
