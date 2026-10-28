# Production-Ready Authentication & Security System Implementation Plan

This plan outlines the implementation of a production-ready User Registration, Email OTP Login, Account Management, Data Isolation, and Google Play compliant authentication system for the School Student Management application while preserving all existing offline student-management functionalities.

## User Review Required

> [!IMPORTANT]
> - **Firebase Configuration**: Firebase Auth and Firestore will be integrated. Please ensure `google-services.json` is provided in the `app/` directory if connecting to a custom Firebase project.
> - **Offline-First Guarantee**: Existing Room database operations (Add Student, Edit Student, List, MDM, Exams, TC, Reports, PDF, Print, Export/Import) will remain 100% functional offline.

## Open Questions

- Should cloud backup/sync be enabled by default or kept as an optional manual setting in Settings? (Defaulting to manual optional cloud sync to keep offline performance fast and secure).

## Proposed Changes

### Authentication & Core Architecture (`com.brijby77.ui.auth`)
- [NEW] `AuthActivity` or Auth navigation flow (Splash -> Auth Check -> Login/Register -> Dashboard).
- [NEW] `LoginFragment`, `RegisterFragment`, `OtpVerificationFragment`, `ProfileFragment`.
- [NEW] `AuthRepository` for managing Firebase Authentication, Email OTP, user registration validation, and CAPTCHA handling.

### Data Isolation & Room Database
- [MODIFY] `AppDatabase` / `StudentRepository` to associate local student records optionally with user ownership UID while maintaining offline Room query compatibility.
- [NEW] Security rules documentation for Firestore / backend storage ensuring strict user data isolation (`users/{uid}`).

### Settings & Compliance
- [MODIFY] `SettingsFragment` (or equivalent settings UI) to include Profile, Privacy Policy, Terms & Conditions, Account Deletion (with re-verification), and Logout.
- [NEW] `PrivacyPolicyFragment` and `TermsFragment`.

## Verification Plan

### Automated Tests
- Gradle build verification (`app:assembleDebug`).

### Manual Verification
- Test First Launch flow (Welcome / Login / Registration).
- Test Registration validation (Name, email, mobile, CAPTCHA, terms).
- Test Email OTP send and verify flow.
- Test offline student management operations (Add/Edit/Export/PDF) without internet connection.
- Test Logout and Account Deletion flows.
