ChatApp
A real-time Android chat application built with Kotlin and Firebase, offering smooth messaging, user authentication, and a modern UI. This app is designed as a beginner-to-intermediate real-world project, showcasing mobile app development, Firebase integration, and clean Android UI design.
Features

🔐 Authentication
Sign up and log in using Firebase Authentication (Email & Password).
Automatically detects existing users and logs them in.


💬 Real-Time Chat
Instant messaging powered by Cloud Firestore.
Optimistic message sending (messages appear instantly).
Seen/Delivered status using tick icons (WhatsApp-style).


👤 User Management
Real-time user list with live updates.
Search users by name or email with a built-in SearchView.
Displays last message preview and unread message count.


🎨 Modern UI/UX
WhatsApp-style chat UI with a gradient AppBar.
Fullscreen immersive mode for a distraction-free experience.
Custom vector-based app icon and styled logout dialog.
Minimal, clean typography with Poppins fonts.


⚡ Performance
Efficient Firestore snapshot listeners.
Listeners are removed in onDestroy to prevent memory leaks.
Optimized RecyclerView for smooth chat scrolling.



Tech Stack

Kotlin
Firebase Authentication
Cloud Firestore
Firebase Realtime Database (for future extensions)
Material Design
Android Jetpack (ViewBinding, RecyclerView)

Getting Started

Clone the Repository:git clone https://github.com/VisheshKamble/ChatApp.git


Open the Project:
Open the project in Android Studio.


Connect Your Firebase Project:
Add your own google-services.json file in the app/ directory.
Enable Firebase Authentication and Firestore in the Firebase Console.


Run the App:
Run the app on an emulator or physical device.



Author

Vishesh KambleGitHub Profile"Built by an 18-year-old aspiring software engineer."

License
MIT License - Feel free to modify and distribute.
Contributing
Submit issues or pull requests on the GitHub repository for bug fixes or new features.
Acknowledgments

Inspired by WhatsApp's intuitive design and real-time functionality.
Thanks to the Firebase and Kotlin communities for their resources.

