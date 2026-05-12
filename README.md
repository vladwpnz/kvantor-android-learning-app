# Kvantor

Kvantor is an Android learning application for programming courses.  
The app is focused on interactive learning, course navigation, user profile management, progress tracking, and AI-assisted learning.

## Overview

The application allows users to choose a programming course, open learning modules, manage their profile, switch between light and dark themes, and use an AI assistant through a chat-style interface.

The project was created as a portfolio Android application to demonstrate Kotlin, Jetpack Compose, Firebase integration, and mobile app architecture basics.

## Features

- Course selection screen for programming courses
- Python and JavaScript learning sections
- AI assistant with a chat-style interface
- User profile screen
- Avatar support
- Dark and light theme switching
- Firebase Authentication integration
- Cloud Firestore integration for user data and progress
- Jetpack Compose UI
- Material 3 components
- Retrofit-based communication with a local backend API
- Kotlin Coroutines
- Basic UI testing support using Compose test tags

## Tech Stack

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Firebase Authentication
- Cloud Firestore
- Retrofit
- OkHttp
- Kotlin Coroutines
- Gradle Kotlin DSL

## Main Screens

- Welcome screen
- Authentication screen
- Registration screen
- Profile setup screen
- Course selection screen
- Python course screen
- JavaScript course screen
- Lesson screen
- AI assistant screen
- Profile screen
- Shop screen

## Project Structure

```text
app/
 ├── src/main/
 │   ├── java/com/bambiloff/kvantor/
 │   │   ├── AuthActivity.kt
 │   │   ├── RegisterActivity.kt
 │   │   ├── WelcomeActivity.kt
 │   │   ├── ProfileSetupActivity.kt
 │   │   ├── CourseSelectionActivity.kt
 │   │   ├── MainActivity.kt
 │   │   ├── JavaScriptMainActivity.kt
 │   │   ├── LessonActivity.kt
 │   │   ├── AiAssistantActivity.kt
 │   │   ├── AiAssistantScreen.kt
 │   │   ├── AiAssistantViewModel.kt
 │   │   ├── AiRepository.kt
 │   │   ├── ProfileActivity.kt
 │   │   └── ShopActivity.kt
 │   └── AndroidManifest.xml
 └── build.gradle.kts
```

## AI Assistant

The AI assistant screen uses Retrofit to send user prompts to a local backend API.

Default backend URL:

```text
http://10.0.2.2:5000/
```

For Android Emulator, `10.0.2.2` is used to access the host machine localhost.

The backend service is expected to provide an endpoint for processing user prompts and returning AI-generated responses.

## Firebase Integration

The application uses Firebase for authentication and user-related data storage.

Used Firebase services:

- Firebase Authentication
- Cloud Firestore

Firestore is used for storing user data, selected course information, avatar data, and learning progress.

## Getting Started

### Prerequisites

Before running the project, make sure you have:

- Android Studio
- JDK 11 or higher
- Android SDK
- Firebase project
- Firebase configuration file

### Installation

1. Clone the repository:

```bash
git clone https://github.com/vladwpnz/Kvantor.git
```

2. Open the project in Android Studio.

3. Sync Gradle dependencies.

4. Add Firebase configuration file if needed:

```text
app/google-services.json
```

5. Run the project on an Android Emulator or a physical Android device.

## Running with AI Assistant

To use the AI assistant, a local backend server should be running on the host machine.

The Android Emulator accesses the host machine through:

```text
http://10.0.2.2:5000/
```

If you run the application on a physical device, the backend URL should be changed to the local network IP address of your computer.

## Notes

This project is a portfolio Android application created for learning and demonstration purposes.  
It shows practical usage of Kotlin, Jetpack Compose, Firebase, Firestore, Retrofit, and basic Android application structure.

## Author

Vladyslav Spyrydonov
