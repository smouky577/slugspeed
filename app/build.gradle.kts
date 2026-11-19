import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

// Ключът за подписване е извън repo-то; без keystore.properties debug версията пак се компилира.
val keystoreFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystoreFile.exists()) load(FileInputStream(keystoreFile))
}

android {
    namespace = "bg.chorno.slugspeed"
    compileSdk = 36

    defaultConfig {
        applicationId = "bg.chorno.slugspeed"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (keystoreFile.exists()) create("slug") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("slug") ?: signingConfig
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("slug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
