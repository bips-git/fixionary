plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bips.dictionary"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.bips_git.fixionary"
        minSdk = 26
        targetSdk = 37
        versionCode = 10002
        versionName = "1.0.2"
    }

    buildTypes {
        release {
            // Left unsigned on purpose: the release workflow signs it with the real key in a separate job.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    // Gradle runs on JDK 21 in CI (any JDK 17+ works); the app targets Java 17 bytecode.
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
