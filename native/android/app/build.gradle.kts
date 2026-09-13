plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val webOrigin = providers.gradleProperty("senditOrigin").orElse("https://sendit.example.com").get().trimEnd('/')
require(webOrigin.matches(Regex("https://[a-zA-Z0-9.-]+(:[0-9]+)?"))) { "senditOrigin must be an HTTPS origin without a path" }
android {
    namespace = "com.sendit.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.sendit.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "WEB_ORIGIN", "\"$webOrigin\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }
