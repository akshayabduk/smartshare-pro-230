androidApplication {
    namespace = "com.smartshare.app"
    applicationId = "com.smartshare.app"
    versionCode = 1
    versionName = "1.0.0"

    // Required Android configuration
    jdkVersion = 17
    compileSdk = 34
    minSdk = 30
    targetSdk = 34

    // Note: viewBinding not available in declarative prototype; not required by our code.

    dependencies {
        // AndroidX
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.viewpager2:viewpager2:1.1.0")
        implementation("androidx.fragment:fragment-ktx:1.8.2")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

        // Kotlin coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

        // Firebase Auth (explicit versions - no BOM in declarative DSL)
        implementation("com.google.firebase:firebase-auth-ktx:23.0.0")

        // Google Play services auth (for Google Sign-In)
        implementation("com.google.android.gms:play-services-auth:21.2.0")

        // Networking (for Dropbox/Drive REST calls)
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("com.squareup.retrofit2:retrofit:2.11.0")
        implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
        implementation("com.squareup.moshi:moshi:1.15.1")

        // File preview helpers (Glide for images)
        implementation("com.github.bumptech.glide:glide:4.16.0")

        // Local database for transfers and metadata (Room compiler not used in this sample)
        implementation("androidx.room:room-runtime:2.6.1")
        implementation("androidx.room:room-ktx:2.6.1")

        // WorkManager for background transfers
        implementation("androidx.work:work-runtime-ktx:2.9.1")
    }
}
