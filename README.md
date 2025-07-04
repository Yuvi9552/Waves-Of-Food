🍽️ Waves Of Food – Android Food Ordering App

A beautifully designed food ordering Android app developed in Kotlin, featuring cart management, real-time order updates, location services, and Firebase integration.

✨ Features:

🔐 User Authentication (Login & Signup)
📍 Location Selector for personalized delivery
🛒 Cart Management with quantity and price control
⚡ Quick Payment with the PayQuick interface
🔔 Push Notifications for real-time updates (FCM)
📦 Buy Again from previous orders
🌊 Smooth Navigation with splash and intro screens
🧑‍🍳 Modern Material UI components

🧰 Tech Stack:

| Component       | Description                     |
|----------------|---------------------------------|
| 💬 Language     | Kotlin                          |
| 🛠️ Build Tool   | Gradle (Kotlin DSL)             |
| 🖼️ UI           | XML Layouts + Material Design   |
| ☁️ Backend      | Firebase (Auth, Realtime DB, FCM)|
| 📦 Libraries    | RecyclerView, Firebase SDK, ViewBinding |

📁 Project Structure:

WavesOfFood/

├── app/

│   ├── src/

│   │   └── main/

│   │       ├── java/com/example/wavesoffood/

│   │       │   ├── Activities/

│   │       │   │   ├── CartActivity.kt

│   │       │   │   ├── LoginActivity.kt

│   │       │   │   ├── MainActivity.kt

│   │       │   │   └── ...

│   │       │   ├── Adapters/

│   │       │   ├── Fragments/

│   │       ├── res/

│   │       │   ├── layout/

│   │       │   ├── drawable/

│   │       │   └── values/

│   ├── build.gradle.kts

│   └── google-services.json

├── build.gradle.kts

└── settings.gradle.kts

🔧 Setup Instructions:

🛠️ Requirements:

- Android Studio Giraffe or higher
- Firebase Project
- Kotlin 1.8+

⚙️ How to Run:

1. Clone the Repository
   bash: git clone https://github.com/YOUR_USERNAME/waves-of-food.git

2. Open in Android Studio
   - File > Open > Select `WavesOfFood`

3. Setup Firebase
   - Enable Authentication (Email/Password)
   - Enable Realtime Database
   - Enable FCM (Cloud Messaging)
   - Download and place `google-services.json` into `app/`

4. Run the App
   - Connect device/emulator
   - Click Run ▶️

🔐 Firebase Configuration:

Authentication: Email/Password
Database: Firebase Realtime DB (Structure as per app logic)
FCM: For order status updates
Storage (Optional): For images/assets

Usage Guide: 

📲 Sign up / Log in
🗺️ Choose your location
🍔 Browse menu and add to cart
🧾 Proceed to Pay Quick
⏳ Track orders via notifications or order history

🧑‍💻 Developer: Yuvraj Aargade: GitHub: [YuvrajAragade](https://github.com/YuvrajAragade)

📄 License: Yuvraj Aargade

