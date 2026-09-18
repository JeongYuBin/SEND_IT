import java.util.Properties
import java.io.FileInputStream

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val keystoreProperties = Properties().apply {
    load(FileInputStream(rootProject.file("keystore.properties")))
}
val webOrigin = providers.gradleProperty("senditOrigin").orElse("https://sendit.example.com").get().trimEnd('/')
require(webOrigin.matches(Regex("https://[a-zA-Z0-9.-]+(:[0-9]+)?"))) { "senditOrigin must be an HTTPS origin without a path" }
android {
    namespace = "com.sendit.mobile"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.sendit.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "WEB_ORIGIN", "\"$webOrigin\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
dependencies { implementation("androidx.webkit:webkit:1.12.1") }
