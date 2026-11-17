# TableTrail — Android Multi-Role Restaurant Discovery & Table Booking

TableTrail is a native Android application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Authentication (Credential Manager Google Sign-In)**, and **Cloud Firestore** with zero-trust role-based security rules.

---

## Architecture & Roles

### 1. Customer
- **Explore & Search**: Browse approved & active restaurants; filter by city, search query, cuisine category tiles, price range (`$`–`$$$$`), minimum rating, and dietary tags (`veg`, `vegan`, `halal`, `gluten-free`).
- **Restaurant Detail & Menu**: Photo gallery, opening hours, holiday closures, Google Maps intent link, categorized menu (`Starters`, `Mains`, `Desserts`, `Drinks`) with veg/non-veg indicators and popular badges, plus guest reviews with owner replies.
- **Real-Time Table Booking**: Choose date, party size, and live time slots backed by Firestore transactions (`slotBookings`) to prevent double-booking.
- **My Bookings**: Upcoming & past bookings, modify or cancel reservations up to **2 hours** before dining time, and submit star ratings + reviews on completed bookings.
- **Favorites & Notifications**: Save favorite restaurants and inspect in-app booking/application notifications.

### 2. Restaurant Owner
- **Partner Onboarding**: Submit a "Become a Partner Restaurant" application with license number, cuisines, address, logo/cover/gallery URLs, and device photo picker preview. View live `Pending`, `Approved`, `Rejected`, or `Info Requested` statuses (with Admin feedback).
- **Owner Studio**:
  - Switch between approved kitchens (or preview all kitchens as Admin).
  - Manage incoming bookings (`Accept / Confirm`, `Decline`, `Mark Completed`, `Mark No-Show`).
  - Full menu & category CRUD with veg/non-veg, popular tags, and availability toggles.
  - Table capacity & time-slot configuration.
  - Analytics summary cards + native Compose Canvas daily booking trend chart.
  - Edit restaurant profile, opening hours, and holiday closures, plus reply to guest reviews.

### 3. Admin (Super Control)
- **Platform Stats**: Live counts of users, restaurants, bookings, and pending partner requests.
- **Requests Queue**: Approve (atomically activates restaurant + elevates owner role + seeds default categories/tables + notifies owner), Request More Info, or Reject with reason.
- **Restaurant & User Governance**: Suspend/reactivate/delete/feature restaurants; block/unblock users or update roles.
- **Moderation, Settings & Activity Logs**: Remove abusive reviews, customize homepage hero banners/cities/cuisines, and inspect the audit trail (`activityLogs`).

---

## Firestore Collections

- `users`, `admins`, `restaurants`, `restaurantRequests`, `categories`, `menuItems`, `tables`, `slotBookings`, `bookings`, `reviews`, `favorites`, `activityLogs`, `notifications`, `platformSettings`

Schema and security rules are defined in:
- `firebase-blueprint.json`
- `firestore.rules`
- `firestore.test.js` (emulator test suite)

---

## Setup & Configuration

1. **Firebase Configuration**:
   - `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` store the provisioned Firebase project and Firestore database ID (`firestore_database_id`).
   - Authentication uses **Google Sign-In via Jetpack Credential Manager** (`default_web_client_id` from `google-services.json`).
2. **Environment Variables / Secrets**:
   - No hardcoded API keys are committed. Any optional third-party secrets belong in AI Studio's **Secrets** panel (`.env` / `BuildConfig`), never `local.properties`.
3. **Run Firestore Security Rules Tests**:
   ```bash
   firebase emulators:exec --only firestore "node --test firestore.test.js"
   ```
4. **Compile Android Debug APK / Kotlin**:
   ```bash
   gradle :app:compileDebugKotlin
   ```

---

## Testing Each Role

1. **Bootstrapped Admin & Sample Data Seeding**:
   - Sign in via Google Sign-In using `govindsinghpratap123@gmail.com`.
   - On first sign-in, sample data (**6 curated restaurants with menus/tables, 2 sample owners, 3 sample customers, sample bookings & reviews**) is automatically seeded. You can also trigger re-seeding in **Profile > Dev Panel**.
   - Use **Profile > Dev Panel** to switch live between **Customer**, **Owner** (with Admin preview across seeded restaurants), and **Admin** workspaces.
2. **Customer & Owner End-to-End Flows**:
   - Sign in with any Google account to test the standard **Customer** flow, then submit a **Partner Restaurant** application from Explore or Profile.
   - Sign in as the Admin account to approve the partner request and watch the Owner Studio unlock automatically for that user.
