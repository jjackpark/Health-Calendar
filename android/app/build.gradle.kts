plugins { id("com.android.application") }
android {
 namespace = "com.jjackpark.healthcalendar"
 compileSdk { version = release(36) { minorApiLevel = 1 } }
 buildToolsVersion = "36.0.0"
 defaultConfig {
  applicationId = "com.jjackpark.healthcalendar"
  minSdk = 28
  targetSdk = 35
  versionCode = 2
  versionName = "1.1.0"
  buildConfigField("String", "SITE_URL", "\"https://health-calendar-jjack.jjackpark.chatgpt.site\"")
 }
 buildFeatures { buildConfig = true }
 signingConfigs {
  create("calendarRelease") {
   val signingFile = System.getenv("CALENDAR_STORE_FILE")
   if (signingFile != null) {
    storeFile = file(signingFile)
    storePassword = System.getenv("CALENDAR_STORE_PASSWORD")
    keyAlias = "health-calendar"
    keyPassword = System.getenv("CALENDAR_STORE_PASSWORD")
   }
  }
 }
 buildTypes { getByName("release") { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("calendarRelease") } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies {
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
 implementation("androidx.health.connect:connect-client:1.1.0")
 implementation("androidx.work:work-runtime-ktx:2.10.2")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 testImplementation("junit:junit:4.13.2")
}

