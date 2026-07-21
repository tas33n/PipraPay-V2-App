# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.1-E1] - 2026-07-21

### Added
- **CSV Export:** Added an export button to the History view to easily export transaction history to a CSV file.
- **Webhook Hostname Display:** The History view now displays the target webhook hostname for each SMS record, making it easy to identify which webhook processed which message.
- **GitHub Actions CI/CD:** Fully automated build pipeline for both Debug and Release APKs, including automatic GitHub Releases on `v*` tags with APK attachments and SHA-256 verification hashes.
- **Smart Notification Metadata:** Persistent notifications now extract and display live payment metadata (Sender, Amount, TrxID, Reference, Balance) directly in the notification drawer instead of generic "Processing" text.

### Changed
- **App Rebranding:** Renamed the app to "PipraPay Enhanced" and updated the `applicationId` to `com.qube.piprapay_enhanced` to allow side-by-side installation with the original PipraPay app.
- **Settings Cleanup:** Replaced the legacy PipraPay support links with direct links to the Source Code (Enhanced Fork) and the Original Forked repo.
- **History Filtering:** Ignored and unmatched SMS messages are no longer saved to the SQLite History database to prevent clutter. They are now only logged briefly in the Logger.
- **Webhook Deletion Safety:** The webhook delete button is now disabled while a webhook is active (ON). To delete, users must first toggle it OFF. This fixes a bug where offline/unreachable servers prevented webhook deletion entirely (it now safely deletes locally).

## [1.1.0] - 2026-07-01

### Added
- **In-App Persistent Logger:** Created `AppLogger.java` and `LoggerActivity` to capture and view logs on-device. Deep logging injected into `SmsBroadcastReceiver` and `RequestWorker` to track background HTTP pushes.
- **Persistent Foreground Service:** Added a highly resilient foreground service with live-updating notifications to prevent aggressive battery optimizers (like MIUI) from killing the background worker.

### Fixed
- **Ngrok Support:** App can now correctly point to `http://<PC-IP>:3000/` or an ngrok URL.
- **Wildcard Sender Support:** Fixed an issue where the `sms-transmit-sender` returned empty senders by defaulting to a `["*"]` wildcard sender to capture ALL SMS.
- **JSON Parsing Errors on Launch:** Fixed missing dashboard fields (`pending`, `stored`, `used`, `error`) in the `account-information` response that caused a toast error on launch.
- **ForwardingConfig.getAll() Bugs:** Fixed `ClassCastException`, `StringIndexOutOfBoundsException`, and `NullPointerException` when retrieving settings by adding strict null and type safety checks.
