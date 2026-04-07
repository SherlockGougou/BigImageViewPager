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
    val keyId = System.getenv("SIGNING_KEY_ID")
    val password = System.getenv("SIGNING_PASSWORD")
    val keyRingFile = System.getenv("SIGNING_SECRET_KEY_RING_FILE")

    if (keyId != null && password != null && keyRingFile != null && File(keyRingFile).exists()) {
        sign(publishing.publications)
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    dependsOn(javadocJar)
}

