# YourTube

YourTube is being redirected into a ReVanced-style Android patch manager, based on the upstream ReVanced Manager / ReVanced Patcher project.

**Important:** the current repository is still the earlier standalone video-player prototype. It is not yet a working APK patch manager and must not be installed or described as one.

## Target functionality
- Select a compatible APK supplied by the user.
- Load and display available patch bundles and compatibility information.
- Select patches and supported patch options.
- Patch locally, sign the resulting APK, and install or export it through Android's package installer.
- Use upstream-supported GmsCore integration for Google account sign-in where applicable. Never request or store a Google password.
- Verify patch provenance and preserve upstream security checks.

## Upstream project and licensing
The implementation is intended to build on [ReVanced Manager](https://github.com/ReVanced/revanced-manager) and [ReVanced CLI](https://github.com/ReVanced/revanced-cli). Both are GPL-3.0 projects. Any copied or modified GPL-covered code must retain notices and comply with GPL-3.0, including providing corresponding source and build instructions.

YourTube is an independent project and is not affiliated with or endorsed by Google, YouTube, or the ReVanced team.

## APK sourcing
YourTube will not bundle or redistribute the proprietary YouTube APK. The user must provide a compatible APK or use an upstream-supported lawful downloader source. Patch compatibility depends on the selected upstream patch bundle and app version.

## Current status
The migration is not complete. The existing Compose/Media3 player prototype is legacy code; no APK patching capability is claimed until the manager codebase has been migrated and verified with a successful build and end-to-end patch test.
