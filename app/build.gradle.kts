plugins { id("com.android.application") }

android {
 namespace = "de.sschermer.kugelspiel"
 compileSdk = 35
 defaultConfig {
    applicationId = "de.sschermer.kugelspiel"
    minSdk = 24
    targetSdk = 35
    versionCode = 8
    versionName = "0.8.0"
 }
 compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
 }
}
