# Vault - Gaming Marketplace Android Application

Vault is a comprehensive Android application designed as a marketplace for gamers. It facilitates buying and selling of game accounts, game top-ups, and joki (game boosting) services. The application provides a seamless experience for both buyers and sellers with integrated chat, real-time transaction tracking, and cloud-based image uploads.

## Features

*   **User Authentication**: Secure login and registration using Firebase Authentication.
*   **Marketplace Listing**: View, search, and filter products by categories (Top Up, Account, Joki, Other).
*   **Real-time Chat**: Integrated messaging system allowing buyers and sellers to communicate before and during transactions.
*   **Transaction Management**: Track orders, upload proof of delivery, and manage order statuses (Pending, Proof Uploaded, Completed, Cancelled).
*   **Gaming News Portal**: Stay updated with the latest gaming news pulled from a public JSON Web Service API.
*   **Local Search History & Bookmarks**: Save recent search queries and bookmark news articles offline using SQLite Database.
*   **Cloud Media Upload**: Seamless image uploads for products and delivery proofs using Cloudinary API.
*   **Network Detection**: Implements a Broadcast Receiver to detect internet connection changes dynamically.

## Tech Stack & Architecture

*   **Language**: Kotlin
*   **UI Architecture**: Activity & Fragment based architecture with Bottom Navigation and Material Design components.
*   **Backend & Database**: Firebase Firestore (NoSQL Document Database).
*   **Local Database**: SQLite (for search history and news bookmarks).
*   **Media Storage**: Cloudinary (Image Hosting).
*   **Networking**: HttpURLConnection for REST API fetching (JSON).
*   **List Implementation**: RecyclerView with Grid and Linear Layout Managers.

## Setup Instructions

To run this project locally, you need to configure the required API keys and credentials.

### 1. Firebase Configuration
1. Create a Firebase Project at Google Firebase Console.
2. Enable **Firestore Database** and **Authentication** (Email/Password).
3. Download the `google-services.json` file.
4. Place the `google-services.json` file inside the `app/` directory of this project.

### 2. Cloudinary Credentials (Required)
The `res/raw` directory is excluded from version control (.gitignore) for security purposes. You must create the credentials file manually.

1. Navigate to `app/src/main/res/raw/`. (Create the `raw` folder if it does not exist).
2. Create a new file named `cloudinary_credentials.json`.
3. Add your Cloudinary API details in the following JSON format:

```json
{
  "cloud_name": "YOUR_CLOUD_NAME",
  "api_key": "YOUR_API_KEY",
  "api_secret": "YOUR_API_SECRET"
}
```

### 3. Build and Run
1. Open the project in Android Studio.
2. Sync Project with Gradle Files.
3. Build and Run the application on an emulator or a physical Android device.

## Application Flow

1. **Splash Screen**: Initial loading screen showing the Vault logo.
2. **Authentication**: Users must log in or register an account.
3. **Market Dashboard**: Displays available products in a grid. Users can search and filter.
4. **Item Detail**: View product specifics and initiate a chat with the seller.
5. **My Store**: Sellers can add new products and manage their listings.
6. **Orders**: Both buyers and sellers can track active and completed transactions.
7. **Profile**: Manage account details and access the Gaming News portal or saved offline data.

## Fulfillment of Course Requirements

This project is built to fulfill the final examination (UAS) requirements:
1. Custom Splash Screen and consistent Material Theme.
2. Implementation of Activities, Intents, and Fragments.
3. Advanced RecyclerView implementation (Grid & List).
4. Local Data persistence utilizing SQLite Database.
5. Web Service Integration via JSON parsing.
6. Implementation of Broadcast Receiver.

## License

This project is developed for educational purposes.
