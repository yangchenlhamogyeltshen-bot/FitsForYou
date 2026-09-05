# Implementation Plan - Milestone 1: Core Wardrobe Features

This plan covers the implementation of the first milestone for the FitsForYou app, focusing on authentication, home dashboard, and basic wardrobe management using Room.

## User Review Required

> [!IMPORTANT]
> - **Room + KSP Integration**: I will add KSP and Room to your `app/build.gradle.kts`.
> - **Activity Routing**: `MainActivity` will be updated to act as a splash/routing screen, directing users to `LoginActivity` or `HomeActivity` based on their auth state.
> - **Local URI Storage**: For this milestone, clothing images will be stored as local URIs in the database.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/gradle/libs.versions.toml)
- Ensure Room and KSP versions are correctly defined (already present).

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/build.gradle.kts)
- Apply `ksp` plugin.
- Add Room dependencies: `androidx-room-runtime`, `androidx-room-ktx`, and `androidx-room-compiler` (via ksp).

---

### Data & Persistence

#### [NEW] [Clothing.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/model/Clothing.kt)
- Create Room entity with fields: `id`, `userId`, `name`, `category`, `color`, `season`, `imageUri`, `isCapsule`, `timesWorn`, `lastWorn`.

#### [NEW] [ClothingDao.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/database/ClothingDao.kt)
- Define Room DAO with methods for CRUD, search, and counting items for the current user.

#### [NEW] [AppDatabase.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/database/AppDatabase.kt)
- Initialize Room database singleton.

---

### Authentication

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/MainActivity.kt)
- Implement `FirebaseAuth.currentUser` check for routing.

#### [MODIFY] [LoginActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/LoginActivity.kt)
- Refine login logic and error handling.

#### [MODIFY] [SignupActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/app/src/main/java/com/example/fitsforyou/SignupActivity.kt)
- Implement `createUserWithEmailAndPassword` with validation.

---

### UI - Home & Wardrobe

#### [MODIFY] [HomeActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/HomeActivity.kt) & [activity_home.xml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/res/layout/activity_home.xml)
- Implement dashboard with dynamic counts (Total clothes, Capsule items).
- Add navigation to Add Clothing and Wardrobe.

#### [NEW] [AddClothingActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/AddClothingActivity.kt) & [activity_add_clothing.xml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/res/layout/activity_add_clothing.xml)
- Implement clothing addition with image picker (Local URI).

#### [NEW] [WardrobeActivity.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/WardrobeActivity.kt) & [activity_wardrobe.xml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/res/layout/activity_wardrobe.xml)
- Implement searchable, filterable list of clothing.

#### [NEW] [ClothingAdapter.kt](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/java/com/example/fitsforyou/adapter/ClothingAdapter.kt) & [item_clothing.xml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/res/layout/item_clothing.xml)
- RecyclerView adapter for clothing cards.

---

### System Integration

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Yangchen/AndroidStudioProjects/FitsForYou/app/src/main/AndroidManifest.xml)
- Register all new activities.

## Verification Plan

### Automated Tests
- Run Gradle Build to ensure KSP and Room are generating code correctly.
- Verify Firebase Auth flow (manual testing recommended due to emulator requirement).

### Manual Verification
1. Fresh Install: Splash -> Login -> Signup -> Create Account -> Home.
2. Home: Check initial counts (0).
3. Add Clothing: Pick image, fill fields, save.
4. Home: Check updated counts.
5. Wardrobe: Search, filter by category, check layout.
6. Persistent Login: Restart app, should go straight to Home.
