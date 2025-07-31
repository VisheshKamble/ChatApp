# 💬 Chat App (Android - Kotlin + Firebase)

A real-time Android chat application built with **Kotlin** and **Firebase**, offering smooth messaging, user authentication, and a modern WhatsApp-inspired UI.  
This app is a beginner-to-intermediate real-world project that demonstrates mobile app development, Firebase integration, and clean Android UI design.

---

## 📌 Features

### 🔐 Authentication
- Sign up and log in using **Firebase Authentication (Email & Password)**
- Automatically detects existing users and logs them in

### 💬 Real-Time Chat
- Instant messaging powered by **Cloud Firestore**
- **Optimistic message sending** (messages appear instantly)
- **Seen/Delivered** status using tick icons like WhatsApp

### 👤 User Management
- Real-time user list with live updates
- **Search users** by name or email with a built-in SearchView
- Displays **last message preview** and **unread message count**

### 🎨 Modern UI/UX
- WhatsApp-style chat UI with a **gradient AppBar**
- Fullscreen immersive mode for distraction-free experience
- Custom vector-based app icon and styled logout dialog
- Minimal, clean typography with **Poppins fonts**

### ⚡ Performance
- Efficient **Firestore snapshot listeners**
- Listeners are removed on `onDestroy` to prevent memory leaks
- Optimized **RecyclerView** for smooth chat scrolling

---

## 📱 Tech Stack

- **Kotlin**
- **Firebase Authentication**
- **Cloud Firestore**
- **Firebase Realtime Database** (for future extensions)
- **Material Design**
- **Android Jetpack (ViewBinding, RecyclerView)**

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/VisheshKamble/ChatApp.git

2. Open the project in Android Studio.

3. Connect your Firebase project:

Add your own google-services.json in app/

Enable Firebase Authentication and Firestore in the Firebase Console

4. Run the app on an emulator or physical device.
5. 

## 🙋‍♂️ Author

Vishesh Kamble -> https://github.com/VisheshKamble

"Built by an 18-year-old aspiring software engineer."

