plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

android {
    namespace = "cc.shinichi.library"
    compileSdk = 34
    ndkVersion = "25.2.9519653"

    defaultConfig {
        minSdk = 24
        multiDexEnabled = true
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlin.stdlib)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.glide.okhttp3)

    // AVIF support
    api(libs.avif)
    api(libs.glide.avif) {
        exclude(group = "org.aomedia.avif.android", module = "avif")
    }

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
}

// =============================================================================
// --- MAVEN CENTRAL PUBLISHING CONFIGURATION ---
// =============================================================================

// Helper function to get property with default value
fun prop(name: String, default: String = ""): String =
    project.properties[name]?.toString() ?: default

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaJavadoc"))
    from(tasks.named("dokkaJavadoc").get().outputs)
    archiveClassifier.set("javadoc")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = prop("GROUP_ID", "com.gouqinglin")
                artifactId = prop("ARTIFACT_ID", "BigImageViewPager")
                version = prop("VERSION_NAME", "1.0.0")

                artifact(sourcesJar)
                artifact(javadocJar)

                pom {
                    name.set(artifactId)
                    description.set(prop("POM_DESCRIPTION"))
                    url.set(prop("POM_URL"))

                    licenses {
                        license {
                            name.set(prop("POM_LICENSE_NAME"))
                            url.set(prop("POM_LICENSE_URL"))
                        }
                    }

                    developers {
                        developer {
                            id.set(prop("POM_DEVELOPER_ID"))
                            name.set(prop("POM_DEVELOPER_NAME"))
                            email.set(prop("POM_DEVELOPER_EMAIL"))
                        }
                    }

                    scm {
                        connection.set(prop("POM_SCM_CONNECTION"))
                        developerConnection.set(prop("POM_SCM_DEV_CONNECTION"))
                        url.set(prop("POM_SCM_URL"))
                    }
                }
            }
        }
    }
}

signing {
    val keyId = findProperty("signing.keyId")?.toString()
    val password = findProperty("signing.password")?.toString()
    val keyRingFile = findProperty("signing.secretKeyRingFile")?.toString()

    if (keyId != null && password != null && keyRingFile != null && File(keyRingFile).exists()) {
        sign(publishing.publications)
    }
}
