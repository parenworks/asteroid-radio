# Asteroid Radio

Android radio app for [asteroid.radio](https://asteroid.radio) — "The Station at the End of Time."

## Features

- **Live streaming** — plays the asteroid.radio AAC stream via Media3/ExoPlayer
- **Background playback** — foreground media service keeps audio playing when screen is off or app is backgrounded
- **Media notification** — lock screen and notification shade controls
- **Sleep timer** — 15/30/45/60 minute presets with countdown
- **Recently played** — fetches and displays track history from the asteroid.radio API
- **Terminal aesthetic** — green-on-black monospace UI

## Tech Stack

- Kotlin + Jetpack Compose
- Media3 / ExoPlayer (audio streaming + media session)
- OkHttp (API requests)
- Min SDK 24, Target SDK 35

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleDebug    # debug APK
./gradlew assembleRelease  # release APK (needs signing config)
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
