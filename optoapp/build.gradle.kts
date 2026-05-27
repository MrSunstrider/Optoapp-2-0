import java.util.Properties
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun escapeForBuildConfigField(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.optoapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.optoapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 5
        versionName = "1.1.3"
        
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val supabaseUrl = escapeForBuildConfigField(localProperties.getProperty("supabase.url", ""))
        val supabaseAnonKey = escapeForBuildConfigField(localProperties.getProperty("supabase.anon.key", ""))
        val supabaseRedirectScheme = escapeForBuildConfigField(
            localProperties.getProperty("supabase.redirect.scheme", "optoapp")
        )
        val supabaseRedirectHost = escapeForBuildConfigField(
            localProperties.getProperty("supabase.redirect.host", "auth")
        )
        val forceProDev = localProperties.getProperty("optoapp.dev.force_pro", "false").equals("true", ignoreCase = true)
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SUPABASE_REDIRECT_SCHEME", "\"$supabaseRedirectScheme\"")
        buildConfigField("String", "SUPABASE_REDIRECT_HOST", "\"$supabaseRedirectHost\"")
        buildConfigField("Boolean", "FORCE_PRO_DEV", "$forceProDev")
        manifestPlaceholders["SUPABASE_REDIRECT_SCHEME"] = supabaseRedirectScheme
        manifestPlaceholders["SUPABASE_REDIRECT_HOST"] = supabaseRedirectHost
    }

    signingConfigs {
        create("release") {
            storeFile = localProperties.getProperty("keystore.path")?.let { rootProject.file(it) }
            storePassword = localProperties.getProperty("keystore.password", "")
            keyAlias = localProperties.getProperty("key.alias", "")
            keyPassword = localProperties.getProperty("key.password", "")
        }
    }

    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlin.reflect)

    implementation(libs.billing.ktx)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.cio)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockk)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room testing
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)

    // E2E test infrastructure
    androidTestImplementation(libs.espresso.idling.resource)
    androidTestImplementation(libs.androidx.test.rules)

    // Hilt testing (for @UninstallModules in future Compose UI test phases)
    androidTestImplementation(libs.hilt.android.testing)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

val jacocoFileFilter = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/databinding/*",
    "**/android/databinding/*",
    "**/di/*",
    "**/Dagger*",
    "**/*Hilt*",
    "**/*_Factory*",
    "**/*_MembersInjector*",
    "**/BR.class"
)

val jacocoKotlinClasses = layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
val jacocoJavaClasses = layout.buildDirectory.dir("intermediates/javac/debug/classes")
val jacocoExecData = files(layout.buildDirectory.dir("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))

// JaCoCo coverage report (unit tests)
val jacocoTestReport by tasks.registering(JacocoReport::class) {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    sourceDirectories.setFrom(files("src/main/java"))
    classDirectories.setFrom(
        fileTree(jacocoKotlinClasses) { exclude(jacocoFileFilter) },
        fileTree(jacocoJavaClasses) { exclude(jacocoFileFilter) }
    )
    executionData.setFrom(jacocoExecData)
}

// JaCoCo coverage verification (CI gate)
val jacocoCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    dependsOn("testDebugUnitTest")
    violationRules {
        rule {
            limit {
                minimum = "0.05".toBigDecimal()
                counter = "INSTRUCTION"
            }
        }
    }
    classDirectories.setFrom(
        fileTree(jacocoKotlinClasses) { exclude(jacocoFileFilter) },
        fileTree(jacocoJavaClasses) { exclude(jacocoFileFilter) }
    )
    executionData.setFrom(jacocoExecData)
}

