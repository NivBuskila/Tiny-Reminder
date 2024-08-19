# TinyReminder

TinyReminder is a family-oriented mobile application designed to enhance child safety, particularly in preventing incidents of children being left in cars. The app uses location tracking, real-time notifications, and family management features to keep families connected and alert.

## Features

- **User Authentication**: Secure sign-in using Firebase Authentication.
- **Family Management**: Create and manage family groups, add/remove members.
- **Real-time Location Tracking**: View family members' locations on a map.
- **Parking Detection**: Automatically detect when a vehicle has parked and send reminders.
- **Notifications**: Send alerts to family members in case of potential safety concerns.
- **Profile Management**: Update user profiles, including profile pictures.

## Technical Stack

- **Platform**: Android
- **Language**: Java
- **Backend**: Firebase (Authentication, Realtime Database, Cloud Storage)
- **Maps**: Google Maps API
- **Notifications**: Firebase Cloud Messaging (FCM)

## Setup

1. Clone the repository:
   ```
   git clone https://github.com/NivBuskila/TinyReminder.git
   ```

2. Open the project in Android Studio.

3. Set up Firebase:
   - Create a new Firebase project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app to your Firebase project and follow the setup instructions.
   - Download the `google-services.json` file and place it in the `app/` directory.

4. Enable necessary Firebase services:
   - Authentication (Email/Password, Phone, Google Sign-In)
   - Realtime Database
   - Cloud Storage
   - Cloud Messaging

5. Set up Google Maps:
   - Get a Google Maps API key from the [Google Cloud Console](https://console.cloud.google.com/).
   - Add the API key to your `AndroidManifest.xml` file:
     ```xml
     <meta-data
         android:name="com.google.android.geo.API_KEY"
         android:value="YOUR_API_KEY"/>
     ```

6. Build and run the application.

## Project Structure

- `MainActivity`: Main entry point of the application.
- `fragments/`: Contains UI fragments for different screens.
- `models/`: Data models (User, Family, ParkingEvent, etc.).
- `services/`: Background services for location tracking and parking detection.
- `utils/`: Utility classes including DatabaseManager and NotificationHelper.
- `receivers/`: Broadcast receivers for handling notifications and system events.
- `adapters/`: RecyclerView adapters for list displays.

## Contributing

Contributions to TinyReminder are welcome. Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature.
3. Commit your changes.
4. Push to your branch.
5. Create a new Pull Request.


---

Remember to keep your API keys and sensitive information secure and not expose them in public repositories.
