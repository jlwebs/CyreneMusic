import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("key.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}
fun keystoreProperty(name: String): String? =
    keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val defaultKeystoreFile = rootProject.file("cyrene-release.jks")
val configuredStoreFile = keystoreProperty("storeFile")?.let { rootProject.file(it) }
val resolvedStoreFile = when {
    configuredStoreFile?.exists() == true -> configuredStoreFile
    defaultKeystoreFile.exists() -> defaultKeystoreFile
    else -> null
}
val releaseStorePassword = keystoreProperty("storePassword")
val releaseKeyAlias = keystoreProperty("keyAlias")
val releaseKeyPassword = keystoreProperty("keyPassword")
val hasReleaseSigning = resolvedStoreFile != null
    && releaseStorePassword != null
    && releaseKeyAlias != null
    && releaseKeyPassword != null

plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.cyrene.music"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // 启用核心库脱糖支持（flutter_local_notifications 需要）
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    defaultConfig {
        applicationId = "com.cyrene.music"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion  // 核心库脱糖需要至少 API 21
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // 默认应用名称
        manifestPlaceholders["appName"] = "Cyrene Music"
    }

    signingConfigs {
        create("release") {
            // 只有在签名信息完整时才启用 release keystore，否则回退到 debug signing。
            if (hasReleaseSigning) {
                storeFile = resolvedStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Debug 版本增加后缀，实现共存
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Cyrene Music (Debug)"
        }

        release {
            // key.properties 或 CI secrets 不完整时，使用 debug signing，避免 packageRelease 因缺少密码失败。
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            manifestPlaceholders["appName"] = "Cyrene Music"
        }
    }
}

flutter {
    source = "../.."
}

dependencies {
    // 核心库脱糖支持（flutter_local_notifications 需要 2.1.4+）
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // 媒体兼容库：提供 MediaBrowserCompat / MediaControllerCompat / MediaStyle 等
    implementation("androidx.media:media:1.7.0")
    
    // Android 12+ Splash Screen API 向后兼容库
    implementation("androidx.core:core-splashscreen:1.0.1")
}
