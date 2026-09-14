# Android builds and updates

Every branch push and pull request runs unit tests and builds a debug APK. Download it from the workflow run's `yanga-debug-<commit>` artifact (14-day retention). Manual workflow runs also build debug only. Debug packages use `com.zegwe.yanga.debug`; they are separate installations and are not offered release upgrades. CI debug signing is temporary and is not a stable upgrade channel.

Only pushing a `v<versionName>` tag publishes a stable GitHub Release. Ordinary pushes never publish a release. For example, change `version.properties` to `versionName=1.1` and `versionCode=2`, commit it, then push tag `v1.1` when ready to publish. Every release must increase Android's versionCode. The workflow checks it against the latest release and rejects mismatched tags. Do not reuse published tags or decrease the code. The initial version remains 1.0 (1).

Configure repository Actions secrets before the first release:

- `ANDROID_KEYSTORE_BASE64`: Base64 of the existing release keystore.
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: existing signing alias.
- `ANDROID_KEY_PASSWORD`: key password.

Use the same key as existing installed release APKs and keep a secure backup. No keys or passwords belong in Git. Existing local `signing/release.properties` remains supported; CI supplies signing through environment variables. Forks must update the application's GitHub repository constants. The update endpoint assumes this repository and its Releases are publicly readable; no GitHub token is embedded in the app.

The workflow tests, builds, verifies the signature, uploads `yanga-<versionName>-<versionCode>-release.apk` into a draft, then publishes it as latest. Configure GitHub Actions to allow this workflow to write repository contents. If publication fails leaving a draft, inspect and delete that draft before rerunning the tag workflow. Signing secrets are used only in the tag release job, never pull request jobs. Protect release tags and restrict who may create them.

Profile → Settings → About shows the installed PackageManager version. Check updates reads GitHub's latest stable release, compares numeric versionCode from the APK asset name, and ignores drafts and prereleases. Missing release, invalid assets, rate limits, network errors, download progress, cancellation and retry have visible states. Downloads use app-private cache and SHA-256 from GitHub release asset metadata. APK package, version and signing certificate must match before invoking Android's installer through FileProvider. Android 8+ may first require allowing this app to install unknown apps; return and press Install again. Installation always requires the user's system confirmation.

Downloads survive screen rotation while the About navigation entry exists. Leaving About or process death cancels foreground work; return and download again. Partial downloads are replaced on retry; there is no background/resumable download service. If Android clears the cached APK, use Download again. This implementation assumes a stable signing certificate (certificate rotation requires a separate migration design).

Device acceptance checks: no release / same version / newer version; airplane mode and retry; cancel a download; rotate during download; leave and re-enter; invalid digest/package/signature; deny then grant unknown-app permission; cancel installer then retry; install a newer release signed with the same key. CI does not emulate the system installer.

References: [GitHub Releases API](https://docs.github.com/en/rest/releases/releases), [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider), [install-source permission](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls()).

## Verification (2026-09-14)

- `:app:testDebugUnitTest`: 195 tests, 0 failures, 4 skipped (existing skipped tests); includes seven release parsing/validation tests.
- `:app:assembleDebug` and `:app:assembleRelease`: passed. Final release also built using the existing local PKCS#12 key through CI-style environment variables; `apksigner verify --verbose` passed (APK Signature Scheme v2, one signer).
- Android 15 device: `MainScreenTest#profileAboutRowShowsInstalledVersionAndUpdateAction` passed, covering About navigation, installed version text, update action and back navigation. This caught and fixed Nav3's missing Application creation extras by using an explicit ViewModel initializer.
- Workflow YAML and embedded Python parsed; first release, tag mismatch, repeated code and downgrade validation cases checked locally. GitHub-hosted execution/publication and a real release download-to-install upgrade were not performed.
