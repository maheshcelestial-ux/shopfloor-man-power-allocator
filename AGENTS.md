# MES Shopfloor Manpower Allocation Rules

This file captures project-specific configurations, architectures, and rules for future agent iterations to ensure flawless maintenance of the **Shopfloor Manpower Allocation App**.

## Architecture & Data Flow

- **Primary DB (Offline-First)**: Room SQLite database handles instantaneous offline reads/writes on the shop floor.
- **Cloud Sync Layer**: `FirebaseSyncHelper` listens to Firestore collections (`operators`, `machines`, `assembly_stations`, `allocations`, `settings`) in real-time, feeding updates directly into Room. Room flows automatically refresh active Jetpack Compose layouts instantly on all synchronized devices.
- **No-Crash Fallback**: Bypasses Firebase initialization dynamically if `google-services.json` is missing or unconfigured. In this state, the app falls back cleanly to fully functional local-only mode, showing a yellow "Local-Only" indicator on the top bar instead of crashing.

## Master Collections (Dynamic Keys)

- Operator skills are saved as serialized JSON mapping strings (`{"CNC 1": 5, "VMC 1": 3}`) inside the `skillsJson` text column.
- This allows administrators to add/remove machines or stations on-the-fly. The Operator skills update instantly without requiring database migrations or APK reinstalls.

## Key Pin Logins
- **Admin**: PIN `1111` (Full master access, add/edit operators, settings)
- **Supervisor**: PIN `2222` (Attendance entry, requirements checkbox, run auto-allocation engine)
- **Production Manager**: PIN `3333` (Dashboard view, report sharing/excel exports, attendance adjustments)
- **Viewer**: PIN `4444` (Read-only dashboard access, report prints)
