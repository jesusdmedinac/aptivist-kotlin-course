
// 📝 CONFIGURACIÓN GRADLE CON KOTLIN DSL
// Este archivo define cómo se construye nuestro proyecto de Kotlin.
// Usamos Kotlin DSL (Domain Specific Language) en lugar de Groovy para consistency con Kotlin.

plugins {
    // 📝 Plugin de Kotlin: Permite compilar código Kotlin a bytecode JVM
    kotlin("jvm") version "1.9.20"
    
    // 📝 Plugin de aplicación: Proporciona tareas para ejecutar y distribuir aplicaciones
    application
}

group = "com.aptivist"
version = "1.0-SNAPSHOT"

// 📝 REPOSITORIOS: Dónde buscar las dependencias de nuestro proyecto
repositories {
    mavenCentral() // Repositorio principal de Maven donde están la mayoría de librerías
}

// 📝 DEPENDENCIAS: Librerías externas que nuestro proyecto necesita
dependencies {
    // Kotlin standard library (viene automáticamente con el plugin kotlin("jvm"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // 📝 DEPENDENCIAS PARA TESTING
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

// 📝 CONFIGURACIÓN DE LA APLICACIÓN
application {
    // Especifica cuál es la clase principal (punto de entrada) de nuestra aplicación
    mainClass.set("com.aptivist.kotlin.AppKt")
}

// 📝 CONFIGURACIÓN DEL COMPILADOR KOTLIN
tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "11" // Compilamos para Java 11
    }
}

// 📝 CONFIGURACIÓN DE TESTS
tasks.test {
    useJUnitPlatform() // Usa JUnit 5 Platform para ejecutar tests
}

// 📝 CONFIGURACIÓN ADICIONAL para hacer el JAR ejecutable
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.aptivist.kotlin.AppKt"
    }
}
