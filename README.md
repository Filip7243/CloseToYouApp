# CloseToYou mobile APP

"CloseToYou" is a mobile application that allows real-time tracking of friends' locations. App requires access to user's contacts. Users can view their friends' locations on a map. The app offers a feature to set a radius, within which, upon a friend's appearance, the user receives a push notification. It is an ideal solution for coordinating meetings and monitoring the safety of loved ones.

## Used Tools

- Java/Kotlin
- [OSMDROID MAP](https://osmdroid.github.io/osmdroid/How-to-use-the-osmdroid-library.html)
- Android Biometric API
- SQL Lite
- Android Studio

## Requirements

- Android 10 or newer
- Min. 4GB RAM
- GPS
- Internet Connection

## How to run

App uses API, you can get it by executing command `git clone https://github.com/Filip7243/CloseToYouAPI`, to configure API and run DB check here: [API](https://github.com/Filip7243/CloseToYouAPI)

Execute command `git clone https://github.com/Filip7243/CloseToYouApp` and run app in your IDE, or get the .apk file from [architecture](architecture/app.apk) and install on your phone.

## UML Diagrams

- UseCase Diagram

![UseCase_Diagram](architecture/UseCase_Diagram.png?raw=true)

- Activity Diagram for Check map UseCase

![Activity_Diagram](architecture/Activity_Diagram.png?raw=true)

## GUI

- Set Phone Number view

![PhoneNumber_view](architecture/set_phone.png?raw=true)

- Main Panel after Login

![Main_Panel](architecture/main_panel.png?raw=true)
