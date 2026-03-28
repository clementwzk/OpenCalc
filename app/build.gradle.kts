// Gradle build configuration for OpenCalc Android application

// Applied plugins for Android app development with Kotlin and Compose
plugins {
    alias(libs.plugins.androidApplication)  // Android application plugin
    alias(libs.plugins.kotlin)              // Kotlin language support
    alias(libs.plugins.compose.compiler)    // Jetpack Compose compiler
}

// Android application configuration
android {
    namespace = "com.darkempire78.opencalculator"
    compileSdk = 35  // SDK version used for compilation

    defaultConfig {
        applicationId = "com.darkempire78.opencalculator"
        
        // Include only specific language resources to reduce APK size (40+ languages supported)
        resourceConfigurations += listOf("ar", "az", "be", "bn", "bs", "cs", "de", "el", "es", "fa", "fr", "hi", "hr", "hu", "in", "it", "ja", "kn", "mk", "ml", "nb-rNO", "nl", "or", "pl", "pt-rBR", "ro", "ru", "sat", "sr", "sv", "tr", "uk", "vi", "zh-rCN", "zh-rHK", "zh-rTW")
        
        minSdk = 21      // Android 5.0 (Lollipop) minimum requirement
        targetSdk = 35   // Targeting Android 15 for latest features and behaviors
        versionCode = 54 // Internal version number for Play Store
        versionName = "3.2.1"  // User-visible version string

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Build type configurations for release and debug variants
    buildTypes {
        release {
            isMinifyEnabled = true       // Enable code shrinking with R8
            isShrinkResources = true     // Remove unused resources from APK
            signingConfig = signingConfigs.getByName("debug")  // Use debug signing for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),  // Default Android optimization rules
                "proguard-rules.pro"  // Custom ProGuard/R8 rules
            )
        }
        debug {
            applicationIdSuffix = ".debug"  // Allows debug and release versions to coexist
            isDebuggable = true             // Enable debugging capabilities
        }
    }

    // View Binding configuration (deprecated syntax, prefer buildFeatures)
    viewBinding {
        enable = true
    }

    // Enable Android build features
    buildFeatures {
        viewBinding = true   // Type-safe view binding instead of findViewById
        buildConfig = true   // Generate BuildConfig class with version info
    }

    // Java version compatibility settings
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8  // Java 8 source code compatibility
        targetCompatibility = JavaVersion.VERSION_1_8  // Java 8 bytecode target
    }
    
    // Kotlin compiler JVM target (must match Java target)
    kotlinOptions {
        jvmTarget = "1.8"  // Generate Java 8 compatible bytecode
    }
}

// Project dependencies
dependencies {
    // Local JAR files in libs/ directory
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    
    // AndroidX core libraries
    implementation(libs.androidx.runtime)           // Android runtime components
    implementation(libs.androidx.core.ktx)          // Kotlin extensions for Android core
    implementation(libs.androidx.appcompat)         // Backward compatibility support
    implementation(libs.androidx.constraintlayout)  // Flexible layout manager
    
    // UI and Material Design
    implementation(libs.material)  // Material Design components
    
    // Architecture components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)  // ViewModel with Kotlin extensions
    
    // Settings and preferences
    implementation(libs.androidx.preference.ktx)  // Preference library with Kotlin extensions
    
    // Third-party UI components
    implementation(libs.androidslidinguppanel)  // Sliding up panel for history drawer
    
    // JSON serialization
    implementation(libs.gson)  // Google's JSON library for history persistence
    
    // Unit testing (runs on JVM)
    testImplementation(libs.junit)  // JUnit 4 testing framework
    
    // Instrumented testing (runs on Android device/emulator)
    androidTestImplementation(libs.androidx.junit)         // AndroidX JUnit extensions
    androidTestImplementation(libs.androidx.espresso.core) // UI testing framework
}
