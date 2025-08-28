
import java.time.Instant

// 📝 CONFIGURACIÓN GRADLE CON KOTLIN DSL AVANZADA (Phase 1.2)
// Este archivo demuestra características avanzadas del sistema de build de Gradle
// usando Kotlin DSL, incluyendo DSL, lambdas, y configuración programática.

plugins {
    // PED: El bloque 'plugins' es un DSL (Domain Specific Language) que permite
    // una sintaxis clara y type-safe para configurar plugins
    kotlin("jvm")
    application
    
    // PED: Plugin para kotlinx.serialization (Phase 1.3)
    kotlin("plugin.serialization") version "1.9.22"
    
    // PED: Nuevos plugins que demuestran configuración avanzada
    id("org.jetbrains.dokka") version "1.9.10" // Documentación automática
    id("jacoco") // Code coverage reporting
}

// PED: Las propiedades de proyecto se pueden configurar programáticamente
// usando extension functions del objeto Project
group = "com.aptivist"
version = "1.0-SNAPSHOT"

// PED: Extension property personalizada usando delegated properties
val projectDescription: String by project.extra { "Curso avanzado de Kotlin con ejemplos prácticos" }

repositories {
    mavenCentral()
    // PED: Añadimos Maven Google para futuras dependencias de Android
    google()
}

// PED: Configuración de dependencias usando closures (lambdas implícitos)
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect") // Para reflection avanzada
    
    // Logging dependencies (from Phase 1.1)
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // PED: Nuevas dependencias para demostrar configuración avanzada
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Phase 3.1: Dependencias adicionales para state management y async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Phase 1.3: Dependencias para kotlinx.serialization y JSON handling
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Phase 2.1 & 3.2: Ktor dependencies para HTTP server, WebSockets y API REST completa
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")
    implementation("io.ktor:ktor-server-call-logging:2.3.7")
    
    // Phase 3.2: Nuevas dependencias para API REST avanzada
    implementation("io.ktor:ktor-server-status-pages:2.3.7") // Manejo de errores HTTP
    implementation("io.ktor:ktor-server-request-validation:2.3.7") // Validación de requests
    implementation("io.ktor:ktor-server-auth:2.3.7") // Autenticación (para futuras fases)
    implementation("io.ktor:ktor-server-compression:2.3.7") // Compresión de responses
    implementation("io.ktor:ktor-server-caching-headers:2.3.7") // Headers de cache
    implementation("io.ktor:ktor-server-conditional-headers:2.3.7") // Headers condicionales
    implementation("io.ktor:ktor-server-default-headers:2.3.7") // Headers por defecto
    
    // Phase 3.2: Testing para API REST
    testImplementation("io.ktor:ktor-server-tests:2.3.7")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    
    // PED: Jackson dependencies (keeping for future phases)
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8") // Modern mocking for Kotlin
}

// PED: REGION - Configuración de la aplicación usando extension functions
// Esta sección demuestra cómo usar closures (lambdas) para configurar objetos
application {
    // PED: mainClass es un Property<String> que acepta configuración lazy
    mainClass.set("com.aptivist.kotlin.mcp.examples.McpServerExampleKt")
    
    // PED: applicationDefaultJvmArgs es un List<String> mutable
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Djava.awt.headless=true"
    )
}

// PED: REGION - Configuración del compilador usando JVM Toolchain (Phase 1.3)
// JVM Toolchain es la forma moderna y recomendada de configurar JVM target
kotlin {
    jvmToolchain(17) // Phase 1.3: Configuración moderna de JVM target
}

// PED: REGION - Configuración adicional del compilador Kotlin
tasks.compileKotlin {
    // PED: kotlinOptions para características experimentales
    kotlinOptions {
        // jvmTarget se configura automáticamente por jvmToolchain
        // PED: Habilitamos características experimentales de Kotlin
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict"
        )
    }
}

// PED: REGION - Configuración avanzada de testing con jacoco
tasks.test {
    useJUnitPlatform()
    
    // PED: Configuración usando lambda con receiver (extension function)
    systemProperties(
        mapOf(
            "junit.jupiter.execution.parallel.enabled" to true,
            "junit.jupiter.execution.parallel.mode.default" to "concurrent"
        )
    )
    
    // PED: finalizedBy demuestra task dependencies programáticas
    finalizedBy(tasks.jacocoTestReport)
}

// PED: Configuración de JaCoCo usando closure
tasks.jacocoTestReport {
    dependsOn(tasks.test) // PED: Task dependencies explícitas
    
    reports {
        // PED: Cada tipo de reporte se configura con su propio DSL
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// PED: REGION - Task personalizada demostrando DSL y lambdas
// Configuración para crear un JAR con todas las dependencias
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.aptivist.kotlin.App"
    }
    // Incluye todas las dependencias en el JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
    // Para evitar el error de duplicados
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// PED: Esta task personalizada demuestra cómo usar el DSL de Gradle para crear tareas complejas
tasks.register<Copy>("deployResources") {
    // PED: Esta es una Higher-Order Function - una lambda que configura la task
    description = "Copia recursos para deployment usando DSL de Gradle"
    group = "deployment"
    
    // PED: from() y into() son extension functions del tipo Copy
    from("src/main/resources") {
        // PED: Nested closure que demuestra DSL anidado
        include("**/*.properties", "**/*.xml")
        exclude("**/*.tmp")
    }
    
    into(layout.buildDirectory.dir("deploy/resources"))
    
    // PED: doLast es una lambda que se ejecuta al final de la task
    doLast {
        println("✅ Recursos deployados usando Kotlin DSL con lambdas!")
    }
}

// PED: REGION - Task personalizada con Higher-Order Functions
tasks.register("generateBuildInfo") {
    description = "Genera información de build usando programación funcional"
    group = "build"
    
    // PED: outputs.file demuestra lazy evaluation
    val outputFile = layout.buildDirectory.file("buildinfo/build-info.properties")
    outputs.file(outputFile)
    
    // PED: doLast recibe una lambda (Action<Task>) - Higher-Order Function
    doLast {
        // PED: Aquí demostramos el uso de extension functions y lambdas
        outputFile.get().asFile.apply {
            parentFile.mkdirs() // Extension function de File
            
            // PED: writeText es una extension function de Kotlin para File
            writeText(
                // PED: buildString es una Higher-Order Function que toma una lambda
                buildString {
                    appendLine("# Build Information")
                    appendLine("project.name=${project.name}")
                    appendLine("project.version=${project.version}")
                    appendLine("kotlin.version=${kotlin.coreLibrariesVersion}")
                    appendLine("build.timestamp=${System.currentTimeMillis()}")
                    appendLine("description=$projectDescription")
                }
            )
        }
        
        println("📋 Build info generado en: ${outputFile.get().asFile.absolutePath}")
    }
}

// PED: REGION - Configuración avanzada del JAR usando programación funcional
tasks.jar {
    // PED: manifest es un closure que configura el MANIFEST.MF
    manifest {
        attributes(
            // PED: mapOf demuestra literal de Map en Kotlin
            mapOf(
                "Main-Class" to application.mainClass.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to Instant.now().toString()
            )
        )
    }
    
    // PED: archiveBaseName es un Property<String> que usa lazy evaluation
    archiveBaseName.set("aptivist-kotlin-course")
}

// PED: REGION - Task que demuestra scope functions de Kotlin
tasks.register("projectReport") {
    description = "Genera un reporte del proyecto usando Kotlin scope functions"
    group = "reporting"
    
    doLast {
        // PED: run es un scope function que ejecuta código en el contexto del receptor
        project.run {
            println("📊 REPORTE DEL PROYECTO")
            println("=======================")
            println("Nombre: $name")
            println("Versión: $version")
            println("Descripción: $projectDescription")
            
            // PED: let es un scope function útil para transformaciones
            configurations.runtimeClasspath.get().files.let { files ->
                println("Dependencias runtime: ${files.size}")
                files.take(3).forEach { println("  - ${it.name}") }
                if (files.size > 3) println("  ... y ${files.size - 3} más")
            }
            
            // PED: also es útil para side-effects manteniendo el contexto
            tasks.matching { it.group == "build" }.also { buildTasks ->
                println("Tasks de build disponibles: ${buildTasks.map { it.name }}")
            }
        }
    }
}

// PED: REGION - Custom DSL extension para configuración de proyecto
// Esta función demuestra cómo crear DSL personalizados
fun Project.customConfiguration(configure: CustomConfigurationSpec.() -> Unit) {
    val spec = CustomConfigurationSpec(this)
    spec.configure()
    spec.apply()
}

// PED: Clase que representa un DSL personalizado
class CustomConfigurationSpec(private val project: Project) {
    var enableVerboseLogging: Boolean = false
    var customProperties: Map<String, String> = emptyMap()
    
    fun apply() {
        if (enableVerboseLogging) {
            project.tasks.withType<JavaExec> {
                systemProperty("logging.level.root", "DEBUG")
            }
        }
        
        customProperties.forEach { (key, value) ->
            project.ext[key] = value
        }
    }
}

// PED: Ejemplo de uso del DSL personalizado
customConfiguration {
    enableVerboseLogging = true
    customProperties = mapOf(
        "course.phase" to "3.2",
        "course.topic" to "API REST with Ktor"
    )
}

// PED: REGION - Usando las utilidades de buildSrc
// Esta sección demuestra cómo usar las extension functions que creamos

// PED: Configurar opciones de Kotlin usando extension function de buildSrc
configureKotlinOptions {
    kotlinOptions {
        jvmTarget = BuildConfig.jvmTarget
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjsr305=strict"
        )
    }
}

// PED: Usar extension function condicional
onlyIf(!BuildConfig.isCI) {
    logger.info("🏠 Configuración para entorno local detectada")
}

// PED: Configurar por fases usando sealed classes
configureForPhase(BuildPhase.Compile) {
    logger.info("⚙️  Configurando fase de compilación...")
}

// PED: Crear task usando nuestro DSL personalizado
customTask("validateEnvironment") {
    taskGroup = "verification"
    taskDescription = "Valida que el entorno de desarrollo esté correctamente configurado"
    
    whenCondition { 
        // PED: Lambda que determina cuándo ejecutar la task
        !BuildConfig.isCI 
    }
    
    action {
        val envConfig = getEnvironmentConfig()
        println("🔍 VALIDACIÓN DEL ENTORNO")
        println("=" + "=".repeat(29))
        envConfig.forEach { (key, value) ->
            println("$key: $value")
        }
        println("Timestamp: ${BuildConfig.buildTimestamp}")
        println("Es proyecto root: $isRootProject")
    }
}

// PED: Usar custom dependencies DSL (ejemplo comentado para evitar conflictos)
/*
customDependencies {
    impl "org.example:some-library:1.0.0"
    testImpl "org.example:test-utils:1.0.0"
}
*/

// PED: Crear múltiples reportes usando extension function
createReportTask("kotlinReport") {
    buildString {
        appendLine("📋 REPORTE DE KOTLIN")
        appendLine("Versión de Kotlin: ${BuildConfig.kotlinVersion}")
        appendLine("Target JVM: ${BuildConfig.jvmTarget}")
        appendLine("Entorno: ${BuildConfig.buildEnvironment}")
        appendLine("Timestamp: ${Instant.ofEpochMilli(BuildConfig.buildTimestamp)}")
    }
}

createReportTask("dependenciesReport") {
    buildString {
        appendLine("📦 REPORTE DE DEPENDENCIAS")
        val runtimeDeps = configurations.runtimeClasspath.get().allDependencies
        appendLine("Total dependencias: ${runtimeDeps.size}")
        runtimeDeps.take(5).forEach { dep ->
            appendLine("- ${dep.group}:${dep.name}:${dep.version}")
        }
        if (runtimeDeps.size > 5) {
            appendLine("... y ${runtimeDeps.size - 5} más")
        }
    }
}

// PED: Setup defaults usando scope functions
setupProjectDefaults()

// PED: Task que demuestra el uso de operator overloading (String.times)
tasks.register("printBanner") {
    group = "reporting"
    description = "Imprime un banner usando operator overloading de Kotlin"
    
    doLast {
        println("🎓 " + "=".repeat(50))
        println("   CURSO AVANZADO DE KOTLIN - Phase 3.2")
        println("   API REST with Ktor")
        println("🎓 " + "=".repeat(50))
        println()
        println("✨ Conceptos Kotlin demostrados en este build:")
        println("   • Extension Functions & Properties")
        println("   • Higher-Order Functions & Lambdas")
        println("   • DSL (Domain Specific Language)")
        println("   • Scope Functions (apply, let, also, etc.)")
        println("   • Sealed Classes & Pattern Matching")
        println("   • Infix Functions & Operator Overloading")
        println("   • Object Singletons & Computed Properties")
        println("   • Reified Generics")
        println()
    }
}

