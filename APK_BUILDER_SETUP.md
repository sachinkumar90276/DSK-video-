# DSK VIDEO — Automatic GitHub Actions APK Builder Setup

## Overview
This repository includes a complete automated Android APK build system (`.github/workflows/build-apk.yml`) alongside Capacitor configuration (`capacitor.config.ts`, `package.json`) and the in-app **DSK VIDEO APK BUILDER** dashboard.

- **App Name:** `DSK VIDEO`
- **Package / App ID:** `com.dskvideo.ott`
- **Web Directory:** `dist` (auto-detects `dist`, `build`, `out`, `www`)
- **Debug APK Output:** `app-debug.apk`
- **Signed Release APK Output:** `app-release.apk`
- **GitHub Actions Artifact Name:** `DSK-VIDEO-APK-build-<run_number>`

---

## 1. GitHub Repository Secrets

To build a **Debug APK**, no signing secrets are required (`debug.keystore.base64` is used safely for debug builds).

To build a **Signed Release APK** (`app-release.apk`), add these secrets in **GitHub → Settings → Secrets and variables → Actions**:

| Secret Name | Description |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded `.jks` upload/release keystore (`base64 -w 0 my-upload-key.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias (set to `upload` to match `app/build.gradle.kts`) |
| `ANDROID_KEY_PASSWORD` | Key password |
| `SUPABASE_ANON_KEY` | *(Optional)* Your Supabase anon/publishable key injected into `.env` during CI |

> **Security Note:** Never commit `.jks` keystores, `.env`, GitHub tokens, or Supabase `service_role` keys to Git.

---

## 2. Running the APK Build on GitHub Actions

1. Push your code to the `main` branch, **or**
2. Go to **GitHub → Actions → DSK VIDEO Android APK Builder → Run workflow** (`workflow_dispatch`).
3. Choose `both`, `debug`, or `release`.
4. After completion, download **`DSK-VIDEO-APK-build-<run_number>`** from the GitHub Actions run Artifacts section.

---

## 3. Using the In-App "DSK VIDEO APK BUILDER" Dashboard

1. Open **DSK VIDEO** on Android → **Profile → Admin Panel** (or **Profile → APK Builder**).
2. Enter your GitHub **Owner / Organization**, **Repository Name**, and **Branch** (default: `main`).
3. *(Optional for private repos or triggering Rebuild APK)* Enter a fine-grained GitHub Personal Access Token with `Actions: Read & Write` and `Metadata: Read` permissions.
4. Click **Connect & Inspect** to view live GitHub connection status, latest build number, build status, build date, APK version, pre-flight repository error diagnostics, **Download APK**, **View Build Logs**, and **Rebuild APK**.
