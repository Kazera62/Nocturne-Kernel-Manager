plugins {
    id("com.android.application")
}

android {
    namespace = "id.kazera.nocturne.kernelmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.kazera.nocturne.kernelmanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
