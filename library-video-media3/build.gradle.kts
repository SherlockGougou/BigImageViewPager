import org.gradle.api.publish.tasks.GenerateModuleMetadata

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.dokka)
}

android {
    namespace = "cc.shinichi.library.video.media3"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":library"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.ui)
}

fun prop(name: String, default: String = ""): String =
    project.properties[name]?.toString() ?: default

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaJavadoc"))
    from(tasks.named("dokkaJavadoc").get().outputs)
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = prop("GROUP_ID", "com.gouqinglin")
                artifactId = prop("ARTIFACT_ID_MEDIA3", "${prop("ARTIFACT_ID", "BigImageViewPager")}-media3")
                version = prop("VERSION_NAME", "1.0.0")

                artifact(javadocJar)

                pom {
                    name.set(artifactId)
                    description.set("Media3 optional plugin for ${prop("ARTIFACT_ID", "BigImageViewPager")}")
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
    val isPublishingToSonatype = gradle.startParameter.taskNames.any {
        it.contains("publishToSonatype") || it.contains("closeAndReleaseSonatypeStagingRepository")
    }

    val keyId = System.getenv("SIGNING_KEY_ID")?.trim()
    val password = System.getenv("SIGNING_PASSWORD")?.trim()
    val keyRingPathRaw = System.getenv("SIGNING_SECRET_KEY_RING_FILE")?.trim()
    val keyRingPath = keyRingPathRaw
        ?.takeIf { it.isNotEmpty() }
        ?.let { if (it.startsWith("~/")) "${System.getProperty("user.home")}/${it.removePrefix("~/")}" else it }

    val missingVars = buildList {
        if (keyId.isNullOrBlank()) add("SIGNING_KEY_ID")
        if (password.isNullOrBlank()) add("SIGNING_PASSWORD")
        if (keyRingPath.isNullOrBlank()) add("SIGNING_SECRET_KEY_RING_FILE")
    }

    if (missingVars.isNotEmpty()) {
        if (isPublishingToSonatype) {
            throw GradleException("Missing signing environment variables: ${missingVars.joinToString(", ")}")
        }
    } else {
        val resolvedKeyRingPath = keyRingPath!!
        val keyRingFile = File(resolvedKeyRingPath)
        if (!keyRingFile.exists()) {
            if (isPublishingToSonatype) {
                throw GradleException("SIGNING_SECRET_KEY_RING_FILE does not exist: $resolvedKeyRingPath")
            }
        } else {
            project.extensions.extraProperties["signing.keyId"] = keyId
            project.extensions.extraProperties["signing.password"] = password
            project.extensions.extraProperties["signing.secretKeyRingFile"] = resolvedKeyRingPath
            sign(publishing.publications)
        }
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    dependsOn(javadocJar)
}

