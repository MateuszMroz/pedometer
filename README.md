# Pedometr - Android Step Counter App

A modern Android application for automatic step counting with Material Design 3.

## Features

✅ **Automatic background step counting** - The app runs as a background service and continuously counts your steps
✅ **Material Design 3** - Modern, beautiful interface following the latest Material Design guidelines
✅ **Step history** - Browse your step history day by day
✅ **3-month data storage** - Automatic cleanup of old data
✅ **Daily goal** - Default goal of 10,000 steps per day with visual progress indicator
✅ **Notifications** - Discreet notification showing your current step count

## Requirements

- Android 8.0 (API 26) or newer
- Step Counter sensor in the device
- ACTIVITY_RECOGNITION permission (Android 10+)

## Technologies

- **Kotlin** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - UI design system
- **Room Database** - Local database
- **Coroutines & Flow** - Asynchronous programming
- **ViewModel** - MVVM architecture
- **Foreground Service** - Background service

## Architecture

The app uses MVVM (Model-View-ViewModel) architecture:

```
app/
├── data/                      # Data layer
│   ├── StepEntry.kt          # Data model
│   ├── StepDao.kt            # Data Access Object
│   ├── StepDatabase.kt       # Room configuration
│   └── StepRepository.kt     # Repository
├── service/                   # Services
│   └── StepCounterService.kt # Background step counting service
├── ui/                        # UI layer
│   ├── components/           # Reusable components
│   │   ├── StepCard.kt       # Today's steps card
│   │   └── StepHistoryItem.kt # History list item
│   ├── screens/              # Screens
│   │   └── HomeScreen.kt     # Main screen
│   └── theme/                # Material Design 3 theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── viewmodel/                 # ViewModels
│   └── StepViewModel.kt      # Step ViewModel
├── MainActivity.kt            # Main activity
└── PedometrApplication.kt    # Application class
```

## How to Build

1. Clone the repository
2. Open the project in Android Studio
3. Wait for Gradle synchronization
4. Run the app on a device with a step counter sensor

```bash
./gradlew assembleDebug
```

## How It Works

1. **Step Sensor**: The app uses `Sensor.TYPE_STEP_COUNTER`, which counts all steps since the last device restart
2. **Background Service**: `StepCounterService` runs as a Foreground Service, ensuring continuous operation
3. **New Day Detection**: The app automatically resets the counter at midnight
4. **Data Storage**: Room Database stores step history locally
5. **Automatic Cleanup**: Data older than 3 months is automatically deleted

## Permissions

The app requires the following permissions:

- `ACTIVITY_RECOGNITION` - To access the step counter sensor (Android 10+)
- `FOREGROUND_SERVICE` - To run in the background
- `FOREGROUND_SERVICE_HEALTH` - For health services
- `POST_NOTIFICATIONS` - To display notifications
- `WAKE_LOCK` - To maintain background operation

## License

This project is open source and available under the Apache License 2.0.

## Author

Created with ❤️ for walking enthusiasts!
