plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.gnix.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.gnix.app"
        minSdk = 27
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["test"].java.srcDir("../tests")
}
dependencies { testImplementation("junit:junit:4.13.2") }
