
// 📝 ARCHIVO PRINCIPAL DE LA APLICACIÓN KOTLIN
// Este es el punto de entrada de nuestro programa y será nuestro laboratorio
// para explorar todos los conceptos fundamentales de Kotlin a medida que avanzamos.

package com.aptivist.kotlin

// 📝 FUNCIÓN MAIN: Punto de entrada de toda aplicación Kotlin/JVM
// En Kotlin, no necesitamos una clase para tener la función main (a diferencia de Java)
// Esto demuestra que Kotlin soporta programación funcional además de OOP.
fun main() {
    println("🚀 ¡Bienvenido al Curso de Kotlin para Aptivistas!")
    
    // 📝 VARIABLES Y CONSTANTES (Tema del curso que exploraremos)
    // val = valor inmutable (read-only), var = variable mutable
    val courseName = "Aptivist Kotlin Course" // 📝 val: inmutable
    var currentPhase = "Phase 0: Setup"        // 📝 var: mutable
    
    println("📚 Curso: $courseName")
    println("🔧 Estado actual: $currentPhase")
    
    // 📝 STRING TEMPLATES: Una característica poderosa de Kotlin
    // Podemos insertar expresiones en strings usando $variable o ${expresion}
    println("✅ Proyecto inicializado correctamente en ${getCurrentTimestamp()}")
    
    // 📝 LLAMADA A FUNCIÓN: Ejemplo de funciones definidas por el usuario
    displayWelcomeMessage()
    
    // 📝 SEGURIDAD CONTRA NULOS: Kotlin es null-safe por defecto
    // Esta variable no puede ser null sin especificarlo explícitamente
    val projectGoal: String = "Construir un Agente MCP paso a paso"
    println("🎯 Objetivo: $projectGoal")
}

// 📝 DEFINICIÓN DE FUNCIÓN con tipo de retorno explícito
// En Kotlin, las funciones se definen con la palabra clave 'fun'
fun getCurrentTimestamp(): String {
    // 📝 Esta función retorna un String simple
    // En fases posteriores, exploraremos fechas y tiempo de forma más robusta
    return "2025-08-11" // Placeholder por ahora
}

// 📝 FUNCIÓN SIN VALOR DE RETORNO
// Unit es equivalente a 'void' en Java, pero en Kotlin es opcional especificarlo
fun displayWelcomeMessage(): Unit {
    println()
    println("=" .repeat(50))
    println("🎓 CURSO DE KOTLIN PARA APTIVISTAS")
    println("=" .repeat(50))
    println("Durante este curso aprenderemos:")
    println("• Variables y constantes (var, val, const)")
    println("• Tipos de datos y null safety")
    println("• Funciones, clases y objetos")
    println("• Programación asíncrona con coroutines")
    println("• Y mucho más...")
    println("=" .repeat(50))
    println()
}

// 📝 COMENTARIOS PARA FUTURAS FASES:
// Este archivo crecerá progresivamente en cada fase del curso:
// - Phase 1: Exploraremos tipos de datos y variables en profundidad
// - Phase 2: Implementaremos clases y objetos
// - Phase 3: Trabajaremos con colecciones y control de flujo
// - Phase 4: Introduciremos programación asíncrona
// - Phase 5+: Construiremos el Agente MCP completo

/*
📝 NOTAS PEDAGÓGICAS:
- Este código usa características básicas de Kotlin que iremos profundizando
- Cada comentario 📝 marca un concepto que estudiaremos en detalle
- Los println() muestran información del programa de forma educativa
- La estructura del código está diseñada para ser fácil de seguir y modificar
*/
