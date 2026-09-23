plugins {
    id("com.android.application")
}

android {
    namespace = "io.remonode.notifytest"
    compileSdk = 34

    defaultConfig {
        // This is the value to paste into the workflow's "Package / Bundle ID" field.
        applicationId = "io.remonode.notifytest"
        minSdk = 24
        targetSdk = 34
        // CI stamps the build it came from, so the screenshot the canary mails back names the build it ran.
        versionCode = (findProperty("appVersionCode") as String? ?: "1").toInt()
        versionName = (findProperty("appVersionName") as String? ?: "dev")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug keystore, which the Android Gradle Plugin generates on any machine that
            // does not have one. Unsigned, `assembleRelease` yields app-release-UNSIGNED.apk, and an unsigned
            // APK cannot be installed on a device — so the canary would download a file it can never run.
            // This is a test build for our own devices, not something that goes to Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
