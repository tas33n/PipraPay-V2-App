# PipraPay V2 Enhanced — Open Source Android SMS Forwarding App

PipraPay V2 Enhanced is an advanced, open-source Android application designed to forward incoming SMS messages (such as bKash, Nagad, and Rocket payment confirmations) to your custom webhook server. 

This repository contains the complete Android application source code. It is intended to be used alongside a backend bot or AI agent to automate payment verification and digital goods delivery.

## 📝 Disclaimer & Our Use Case

**Trademark Notice**: The PipraPay name, logo, and overall brand identity are trademarks of QubePlug Bangladesh. This is a modified fork designed specifically to integrate with our self-hosted AI agent bot project. Please see [TRADEMARK.md](TRADEMARK.md) for full trademark policies.

**Why we use this approach:**
We built this integration because getting official Merchant APIs for bKash and Nagad for auto-payment verification is currently very difficult or unavailable for small projects. Setting up the complete, official PipraPay payment portal dashboard was too lengthy of a process just to verify client payments for our AI agent customer support service. 

Instead, we forked the PipraPay app source and integrated the webhook logic *directly* into our existing AI bot server. This allows us to bypass building a separate payment dashboard (since we already have one for our AI agent portal) while still achieving 100% automated payment verification. We are sharing this approach because many other developers face the same issue with MFS automation in Bangladesh!

## 🚀 Enhanced Features

- **Advanced Logger UI**: In-app logging with standard logcat coloring and copy/share capabilities (exports as `.log`). Proper rendering of Bangla and Unicode text.
- **Security & Filters**: Define a Sender Whitelist (e.g., `bKash,Nagad`) or a Keyword Blacklist (e.g., block messages containing `OTP` or `password`) right from the settings.
- **Transaction History Database**: A local SQLite database tracks the status of every successful SMS (Pending, Success, Failed). View, delete, and clear records from the new History page. Includes **CSV Export** functionality to save history locally.
- **Offline Resilience & Webhook Safety**: Uses Android WorkManager to queue webhooks and automatically retry with exponential backoff if the network is down. Webhooks can be safely deleted locally when toggled off.
- **Persistent Foreground Service**: The SMS interceptor now runs as a highly resilient foreground service (`dataSync` compliant for Android 14) to prevent aggressive battery optimizers (like MIUI) from killing it. Features live-updating notifications.
- **Multi-SIM Support**: Automatically detects which SIM received the SMS.
- **Automated CI/CD**: Fully configured GitHub Actions workflow (`build.yml`) that automatically generates Debug and Release APKs (with SHA-256 hashes) for every new `v*` tag release.

## 🛠️ How to Build and Install

1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. Let Gradle sync dependencies, then build the APK (`Build > Build Bundle(s) / APK(s) > Build APK(s)`).
4. Install the APK on your Android device.
5. Open the app, grant SMS permissions, and disable battery optimizations.

---

## 📖 Documentation & Integration Guides

The complete Webhook API Documentation, including server implementation examples, bot/AI agent integration blueprints, and database schemas, has been moved to our dedicated documentation file.

👉 **[Read the Full Documentation (docs.md)](docs.md)**

## 🧪 Test Server Included

To make testing incredibly easy, we have included a fully functional Node.js test server right in this repository! 

Instead of building your own backend from scratch just to test if the app works, you can use the provided test server. It uses a local `db.json` file to store intercepted messages so you can immediately see the app working in real-time.

1. Navigate to the `server/` directory: `cd server`
2. Install dependencies (if any): `npm install`
3. Run the server: `node server.js`
4. Point your PipraPay app webhook to `http://<YOUR_PC_IP>:3000/`

For more details on how the API contracts work between the app and the server, please refer to the `docs.md` file.
