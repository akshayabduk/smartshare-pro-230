# SmartShare Android App (mobile_frontend)

An advanced AI-powered file sharing Android application that enables users to share files, music, videos, photos, and contacts between mobile devices, PCs, and laptops.

Features:
- AI-powered file categorization and recommendations.
- Cross-device sharing for files, music, videos, photos, and contacts.
- Secure authentication via Firebase Auth (Google Sign-In).
- Cloud storage integration (Google Drive, Dropbox) via REST (placeholders).
- File preview and management (basic image preview).
- Real-time transfer status with LiveData.
- Direct device-to-device sharing:
  - Bluetooth Classic (RFCOMM) client with bonded device selection.
  - Wi‑Fi Direct (P2P) discovery and connection with in-app chooser.
- Modern, minimal, light UI with primary #6200EE, secondary #03DAC6, accent #FF0266.

Prerequisites:
- JDK 17
- Android SDK 34
- Firebase project with Android app configured
- google-services.json placed at app/google-services.json
- OAuth 2.0 Client ID for Android (for Google Sign-In)

Build:
- ./gradlew build

Install/Run:
- ./gradlew :app:installDebug
- Launch "SmartShare"

Configuration:
- Place google-services.json under mobile_frontend/app/
- Ensure string resource default_web_client_id is supplied by google-services.json
- For Drive/Dropbox: add access tokens via your backend or secure OAuth flow (not included in this sample). Update the domain/rest clients in the app for production.

Direct Sharing Notes:
- Bluetooth: requires a server app running on the receiving device using the same UUID to accept RFCOMM connections. Pair devices in system settings first.
- Wi‑Fi Direct: the receiving peer must run a server socket on port 8988; this client connects to the group owner’s address after connection.
- Runtime permissions are requested on demand (BLUETOOTH_* / NEARBY_WIFI_DEVICES or ACCESS_FINE_LOCATION depending on Android version).
- This is a reference implementation for discovery/connection and client-side send; production apps should implement robust negotiation, resume, integrity checks, and receiver UI.

AI Notes:
- AI categorization uses heuristics; integrate on-device ML as needed.