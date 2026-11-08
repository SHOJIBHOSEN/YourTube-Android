# YourTube Android — Architecture and Implementation Roadmap

## Product decisions
- Product: complete YouTube-style Android client experience.
- Sources: official YouTube integrations plus user-authorized direct media URLs.
- Primary quality attribute: performance and reliability on entry-level Android devices, including limited-RAM devices.
- Non-goals: DRM circumvention, extracting protected YouTube media streams, bypassing access controls, or unauthorized downloads.

## Architecture principles
1. Single app, layered packages first. Avoid premature multi-module complexity until build stability and app size justify it.
2. Unidirectional UI state. Compose screens render immutable screen state and send intents to ViewModels. No network, disk, or player work on the main thread.
3. Repository boundary. UI and domain code do not call Android DownloadManager, Media3, DataStore, or YouTube APIs directly.
4. Explicit source types. A YouTube video ID is not a direct media URL. YouTube playback uses only a permitted official player/integration path; direct MP4/HLS/DASH sources use Media3 where authorized.
5. Lifecycle-safe playback. One playback controller/session owns the player. UI surfaces attach/detach without creating duplicate players. Background playback is added through MediaSessionService only after foreground playback is stable.
6. Performance budgets. Lazy lists, thumbnail loading, bounded caches, no eager preloading, and bounded background concurrency. Heavy I/O and parsing stay off the main thread.
7. Observable failures. Invalid source, unsupported format, network failure, decoder failure, and authorization failure are separate states. Never label a link as watched until playback actually starts.

## Target package layout
- app: application entry point and navigation
- core/model: Video, Channel, Playlist, MediaSource, PlaybackState
- core/common: result/error types, dispatchers, validation
- core/database: Room entities, DAOs, migrations (later)
- core/preferences: DataStore settings
- data/local: history, playlist, settings persistence
- data/youtube: official YouTube Data API and OAuth adapters
- data/media: direct media source repository
- data/download: download queue and platform adapter
- domain/repository: repository contracts
- domain/usecase: search, history, playlist, playback, downloads
- playback/service: MediaSessionService and notification (later)
- playback/controller: Media3 player lifecycle and state
- feature/home, search, player, shorts, subscriptions, library, downloads, settings
- design/theme: colors, typography, reusable components

## Source and playback strategy

### Official YouTube source
- Parse and store canonical YouTube video IDs and watch URLs as metadata.
- Use YouTube Data API for metadata, search, channels, and subscription data within approved scopes and quota.
- Use an officially permitted YouTube playback/player integration. Never turn watch URLs into hidden MP4/HLS URLs.
- OAuth uses Google's supported sign-in flow; never ask users for their Google password.
- Clearly report API key, OAuth consent, quota, region, and playback limitations.

### Authorized direct media source
- Accept HTTP(S) URLs only.
- Media3 handles supported progressive MP4/WebM and adaptive HLS/DASH streams.
- Validate content type safely; do not trust filename extensions alone. Handle query strings and redirects.
- Downloads are restricted to sources the user is authorized to save. HLS/DASH requires segmented-download support and must not be mislabeled as a single MP4.

## Feature roadmap and acceptance gates

### Gate 0 — Build and regression baseline
- Establish reproducible Gradle/Android SDK builds in CI and Codemagic.
- Add unit tests for source parsing, URL validation, history rules, and repository behavior.
- Fix compiler/build failures before feature expansion.
- Acceptance: clean debug build, tests pass, APK artifact exists.

### Gate 1 — Core app foundations
- Introduce navigation, ViewModels, screen state, repository interfaces, and structured errors.
- Replace ad-hoc SharedPreferences persistence with DataStore; introduce Room when queryable history/download metadata requires it.
- Acceptance: navigation and persistence survive process restart; no main-thread I/O.

### Gate 2 — Direct media playback
- Reliable Media3 playback for authorized progressive and adaptive streams.
- Player controls, seek, speed, subtitles where present, orientation/fullscreen, PiP, and playback error UI.
- History is recorded only after the player reports actual playback.
- Acceptance: valid/invalid URLs, buffering, rotation, app background/foreground, playback completion, and network-loss tests.

### Gate 3 — Home, search, and library
- Home feed backed by a repository, search with suggestions and filters, continue watching, playlists, and history.
- YouTube data features remain disabled until API/OAuth setup is complete.
- Acceptance: pagination, empty/error/loading states, bounded memory while scrolling.

### Gate 4 — Official YouTube account integration
- Configure Google OAuth and YouTube Data API.
- Search, metadata, channel/subscription and playlist features allowed by approved scopes and policies.
- Official playback path for YouTube videos.
- Acceptance: consent, revoked/expired token, quota errors, account disconnect, and no false claim of unsupported playback.

### Gate 5 — Downloads
- Queue, progress, retry, cancellation, storage checks, Wi-Fi-only policy, and notifications for authorized direct sources.
- Add segmented HLS/DASH download only if technically and legally supported for the source.
- Acceptance: concurrency limits, process death recovery, insufficient storage, metered network, and file validation.

### Gate 6 — Shorts and subscriptions experience
- Vertical feed and subscription screens using supported source data and playback integrations.
- Acceptance: efficient lazy feed, no autoplay storm, accessibility controls, smooth scrolling on low-memory devices.

### Gate 7 — Background playback and mini-player
- MediaSessionService, media notification, audio focus, headset controls, PiP/mini-player.
- Acceptance: screen off, task swipe, Bluetooth/headset, audio focus interruptions, and service lifecycle.

### Gate 8 — Premium quality and optimization
- SponsorBlock only for compatible sources and within service terms; user-controlled skip behavior.
- Sleep timer, data saver, cache controls, themes, localization, accessibility, diagnostics.
- Profile startup, scrolling, memory, battery, and network usage on low-end hardware.

## Performance guardrails
- Keep one active player instance; release resources deterministically.
- Never parse large payloads, query databases, or perform network calls on the main thread.
- Use lazy lists and thumbnail placeholders; avoid full-resolution images in lists.
- Bound image/metadata caches and download concurrency.
- Avoid unnecessary background polling and autoplay.
- Keep logs free of access tokens, private URLs, and personal account data.
- Prefer stable functionality over adding several untested features in one change.

## Current repository baseline (2026-11-08)
The repository currently contains a single Compose activity with a direct Media3 player, Android DownloadManager integration for direct MP4/M4V/WebM links, and local SharedPreferences history/playlist. Several screens are placeholders. YouTube OAuth/API integration, official YouTube playback, real Shorts feed, background service, mini-player, segmented downloads, and SponsorBlock are not yet implemented.

The latest watch-history correction records an item only after Media3 reports playback. Invalid/non-direct YouTube watch URLs are not treated as direct media sources. This source-level change still requires a successful CI/device build before it can be called verified.
