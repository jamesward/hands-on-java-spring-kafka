import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.confluent:kafka-json-schema-serializer:6.1.1")
    implementation("io.projectreactor.kafka:reactor-kafka")
    implementation("org.webjars:bootstrap:4.5.3")

    testImplementation("org.testcontainers:kafka:1.15.2")
    testImplementation(kotlin("reflect"))
    testImplementation(kotlin("stdlib-jdk8"))

    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    dependsOn("testClasses")
    dependsOn(":ws-to-kafka:bootBuildImage")
    args("--spring.profiles.active=dev")
    classpath += sourceSets["test"].runtimeClasspath
}

application {
    mainClass.set("jsk.Main")
}
