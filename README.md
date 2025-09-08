# ProtecTalk Client

The official **Android app** for ProtecTalk.  
Built with **Kotlin**, **Jetpack Compose**, and **Material 3**, the client provides a secure and modern interface for managing trusted contacts, monitoring alerts, and interacting with the backend.
---
## ⚠️ Problem
Phone scams are a growing threat worldwide, causing people to lose money and sensitive personal data every day. 
Existing protections like call blocking or spam filters are limited, since scammers constantly change phone numbers 
and social engineering techniques. Many victims only realize they've been scammed after it’s too late.

## 💡 Solution
ProtecTalk is a mobile app that helps users identify potential scam calls after the call ends.  
The service records the call audio, converts it to text, and analyzes it with AI models to detect scam patterns.  
Once the analysis is complete, the system alerts both the user and a trusted contact configured in advance.  
This provides timely and reliable feedback that helps protect people from fraudulent activity.
---

## 🛠️ Tech Stack
- **Language**: Kotlin  
- **UI**: Jetpack Compose + Material 3  
- **Architecture**: MVVM + Clean Architecture layers (domain, data, ui)  
- **Async & Workers**: Kotlin Coroutines, WorkManager  
- **Navigation**: Jetpack Navigation Compose  

---

## 🔌 External Integrations
- **Firebase** → Authentication, Push Notifications (FCM)  
- **Google APIs** → Identity & supporting services  
- **OpenAI API** → AI-driven features  

---

## 📦 Key Features

- 📞 **Call Monitoring** → background service for call state + AI-based risk assessment
- 🔔 **Alerts & Notifications** → real-time FCM push integration
- 👥 **Trusted Contacts** → manage, approve, or deny protection requests
- 🔑 **Authentication** → Firebase email/password sign-in
- 🎨 **Modern UI** → Compose + Material 3 theming

---
## 🚀 Deployment & Setup (Client)

### Prerequisites
- Android Studio (Arctic Fox or newer)  
- Gradle 8+  
- Firebase project configured  
- Internet access for external APIs  

### Configuration
Before running the app, prepare the following:  
- **Firebase config** → place `google-services.json` (from Firebase Console) under the `app/` directory.  
- **API keys** → add required keys to `local.properties` (e.g. `openai_api_key`, `google_speech_api_key`).  

### Running the Client
1. Open the project in Android Studio.  
2. Sync Gradle.  
3. Verify that `google-services.json` and API keys are in place.  
4. Build and run on an emulator or physical Android device.  
---
