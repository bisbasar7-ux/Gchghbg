# LICHI AI — REAL-TIME VOICE CALLING COMPANION

LICHI AI is a full-stack, production-grade Android mobile application featuring an emotionally expressive, friendly 3D virtual female companion named **Lichi**. It is powered by real-time Gemini voice capabilities, Android CameraX, and local Android device control.

---

## 🌟 Key Highlights & Voice Calling Features

1. **One-Tap Real-Time Voice Calling Mode**
   - **Full-Screen Calling Interface**: Minimalist, distraction-free calling mode with a prominent Start Call button (“কল শুরু করুন”).
   - **Continuous Hands-Free Dialogue**: The microphone stays open throughout the call; users talk naturally without pressing a button for each sentence.
   - **Voice Activity Detection & Turn-Taking**: Automatically detects when the user finishes speaking and responds immediately.
   - **Natural Barge-In / Interruption**: When the user speaks while Lichi is talking, Lichi instantly pauses her speech and listens attentively.
   - **Call State Machine**: Clear visual indicators for `Connecting`, `Connected`, `Listening`, `Thinking`, and `Speaking`.
   - **Live Call Duration Timer**: Real-time counter (`01:42`) and one-tap **End Call** and **Mute** controls.
   - **Network Resilience**: Reconnects automatically after temporary network drops with clear Bengali notifications.

2. **Dedicated Gemini API Settings & Model Verification**
   - **Password-Style Input**: Secure API Key field with show/hide password toggle.
   - **Live Backend Validation**: **Test Connection** button validates model capabilities through the backend with Bengali success/error feedback.
   - **Feature Verification**: Confirms support for real-time duplex voice (`gemini-3.8-live`) and Bengali speech (`gemini-3.8-flash-lite-tts`).
   - **Direct Call Launch**: Once validated, users tap **“লিচুর সঙ্গে কথা বলো”** to jump straight into a hands-free voice call.
   - **Secure Storage**: Credentials are proxy-managed on the backend and never bundled into the APK or leaked in client logs.
   - **Google AI Studio Setup Guide**: In-app instructions for obtaining a free API key from Google AI Studio.

3. **Sweet, Feminine Bengali Voice & Personality**
   - Lichi speaks in natural, warm, sweet, gentle Bengali.
   - Affectionate and respectful phrases: *“হ্যালো, বলো, আমি শুনছি”*, *“তোমার দিনটা কেমন গেল?”*, *“আমি সবসময় তোমার পাশে আছি।”*
   - Empathetic responses when the caller is stressed or tired, with light, natural laughter where appropriate.
   - Configurable voice speed, pitch, and volume.

3. **Animated 3D Virtual Companion**
   - Interactive 3D avatar with breathing idle animations, head tilting, blinking eyes, and speech viseme mouth sync.
   - 5 dynamic emotional expressions: `Happy`, `Calm`, `Curious`, `Surprised`, and `Empathetic`.
   - Touch drag rotation in 3D space with an instant reset button.

4. **CameraX Vision & Device Actions**
   - Explicit user-authorized CameraX snapshot review and Gemini multimodal analysis.
   - Safe Android device control (launch YouTube, WhatsApp, Chrome, toggle Flashlight, adjust volume, schedule alarms).

---

## 🚀 How to Build and Run in Android Studio

### 1. Requirements
- **Android Studio**: Koala, Ladybug, or newer.
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+), Target SDK 35 (Android 15).
- **JDK**: Java 17 or Java 21 (bundled with Android Studio).
- **Backend**: Node.js v18+ running the Lichi backend (`npm run dev`).

### 2. Open Project
1. Extract the project ZIP or open the `android-project/` directory.
2. In Android Studio, select **File > Open...** and choose the `android-project` folder.
3. Allow Gradle to sync dependencies via `gradle/libs.versions.toml`.

### 3. Connect to the Real-Time AI Backend
- **Android Emulator**: Uses `http://10.0.2.2:3000/` automatically.
- **Physical Phone (USB)**: Run the ADB reverse port forwarding command:
  ```bash
  adb reverse tcp:3000 tcp:3000
  ```
- **Physical Phone (Wi-Fi)**: Enter your computer's local network IP (e.g. `http://192.168.1.50:3000/`) in the in-app **Settings** screen.

### 4. Build and Install Debug APK
Run via terminal:
```bash
./gradlew assembleDebug
```
(On Windows: `gradlew.bat assembleDebug`)

Install directly onto your connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Security & Privacy Guarantees
- **No Hardcoded Keys**: Gemini API keys reside strictly on the server backend.
- **Transparent Mic Access**: The microphone is only active while on a voice call with a visible glowing audio indicator, and stops immediately when the call ends.
- **Zero Hidden Recordings**: No background audio or camera surveillance.
