// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.dokka) apply false
}

val isPublishingToSonatype = gradle.startParameter.taskNames.any {
    it.contains("publishToSonatype") || it.contains("closeAndReleaseSonatypeStagingRepository")
}

val ossrhUsername = System.getenv("OSSRH_USERNAME")
val ossrhPassword = System.getenv("OSSRH_PASSWORD")

if (isPublishingToSonatype && (ossrhUsername.isNullOrBlank() || ossrhPassword.isNullOrBlank())) {
    throw GradleException("Missing Sonatype credentials. Please set OSSRH_USERNAME and OSSRH_PASSWORD environment variables.")
}


tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            packageGroup.set("com.gouqinglin")
            username.set(ossrhUsername ?: "")
            password.set(ossrhPassword ?: "")
        }
    }
}
