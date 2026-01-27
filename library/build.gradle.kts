plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

val ndkVersion: String by project
val groupId: String by project
val artifactId: String by project
val versionName: String by project
val pomDescription: String by project
val pomUrl: String by project
val pomScmUrl: String by project
val pomScmConnection: String by project
val pomScmDevConnection: String by project
val pomLicenseName: String by project
val pomLicenseUrl: String by project
val pomDeveloperId: String by project
val pomDeveloperName: String by project
val pomDeveloperEmail: String by project

android {
    ndkVersion = project.properties["NDK_VERSION"]?.toString() ?: "25.2.9519653"
    namespace = "cc.shinichi.library"
    compileSdk = 34

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

    sourceSets {
        named("main") {
            // JNI 源码由 ndkBuild 处理，这里留空
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
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

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

    // ExoPlayer / Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
}

// =============================================================================
// --- MAVEN CENTRAL PUBLISHING CONFIGURATION ---
// =============================================================================

// Use Dokka to generate Javadoc and package it into a JAR.
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

                groupId = project.properties["GROUP_ID"]?.toString() ?: "com.gouqinglin"
                artifactId = project.properties["ARTIFACT_ID"]?.toString() ?: "BigImageViewPager"
                version = project.properties["VERSION_NAME"]?.toString() ?: "1.0.0"

                artifact(sourcesJar)
                artifact(javadocJar)

                pom {
                    name.set(artifactId)
                    description.set(project.properties["POM_DESCRIPTION"]?.toString() ?: "")
                    url.set(project.properties["POM_URL"]?.toString() ?: "")

                    licenses {
                        license {
                            name.set(project.properties["POM_LICENSE_NAME"]?.toString() ?: "")
                            url.set(project.properties["POM_LICENSE_URL"]?.toString() ?: "")
                        }
                    }

                    developers {
                        developer {
                            id.set(project.properties["POM_DEVELOPER_ID"]?.toString() ?: "")
                            name.set(project.properties["POM_DEVELOPER_NAME"]?.toString() ?: "")
                            email.set(project.properties["POM_DEVELOPER_EMAIL"]?.toString() ?: "")
                        }
                    }

                    scm {
                        connection.set(project.properties["POM_SCM_CONNECTION"]?.toString() ?: "")
                        developerConnection.set(project.properties["POM_SCM_DEV_CONNECTION"]?.toString() ?: "")
                        url.set(project.properties["POM_SCM_URL"]?.toString() ?: "")
                    }
                }
            }
        }
    }
}

signing {
    val signingKeyId = findProperty("signing.keyId")?.toString()
    val signingPassword = findProperty("signing.password")?.toString()
    val signingKeyRingFile = findProperty("signing.secretKeyRingFile")?.toString()

    // Only sign if all signing properties are configured and the key ring file exists.
    if (signingKeyId != null && signingPassword != null && signingKeyRingFile != null && File(signingKeyRingFile).exists()) {
        sign(publishing.publications)
    }
}
