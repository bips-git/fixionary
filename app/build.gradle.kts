plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bips.dictionary"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bips.dictionary"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // Compose is several times slower in a debug build, so judge scrolling and animation on this one.
            // Signed with the debug key so it installs over your debug build and keeps your favourites.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Gradle runs on JDK 26; the app targets Java 17 bytecode.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // F-Droid and reproducible builds: no signed dependency-metadata blob inside the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)
}

// dictionary.db (about 76 MB) is built by pipeline/ and kept out of git. Before every build it is copied
// into src/main/assets (also git-ignored). Default: <project>/dict-data/build/dictionary.db;
// override with  ./gradlew assembleDebug -PdictionaryDb=/path/to/dictionary.db
val dictionaryDbFile: Provider<File> = providers.gradleProperty("dictionaryDb")
    .map { File(it) }
    .orElse(rootProject.layout.projectDirectory.file("dict-data/build/dictionary.db").asFile)

val copyDictionaryDb by tasks.registering(Copy::class) {
    from(dictionaryDbFile)
    into(layout.projectDirectory.dir("src/main/assets"))
    rename { "dictionary.db" }
    inputs.file(dictionaryDbFile) // missing database fails the build with a clear message
}
tasks.named("preBuild") { dependsOn(copyDictionaryDb) }
