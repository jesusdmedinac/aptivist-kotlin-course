
// PED: buildSrc es un módulo especial de Gradle que permite escribir lógica de build
// reutilizable usando Kotlin. Es una demostración práctica de modularización.

plugins {
    `kotlin-dsl` // PED: Plugin que habilita Kotlin DSL para scripts de build
}

repositories {
    gradlePluginPortal() // PED: Repositorio oficial de plugins de Gradle
    mavenCentral()
}

// PED: Dependencias para nuestras extension functions y utilidades de build
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
}
