
// 📝 ARCHIVO PRINCIPAL DE LA APLICACIÓN KOTLIN
// Este es el punto de entrada de nuestro programa y será nuestro laboratorio
// para explorar todos los conceptos fundamentales de Kotlin a medida que avanzamos.

package com.aptivist.kotlin

// 📝 IMPORTS para logging (Phase 1.1)
import org.slf4j.LoggerFactory

// 📝 CLASE PRINCIPAL DE LA APLICACIÓN (Phase 1.1)
// Usamos una clase con companion object para demostrar este concepto fundamental de Kotlin
class App {
    
    // 📝 COMPANION OBJECT con logger - Patrón común en aplicaciones Kotlin
    // El companion object permite acceso a miembros sin crear instancias (como static en Java)
    companion object {
        // 📝 LOGGER: Instancia única compartida para toda la clase
        // LoggerFactory.getLogger() crea un logger específico para esta clase
        private val logger = LoggerFactory.getLogger(App::class.java)
        
        // 📝 FUNCIÓN MAIN: Punto de entrada de toda aplicación Kotlin/JVM
        // Está dentro del companion object para acceso directo al logger
        @JvmStatic
        fun main(args: Array<String>) {
            // 📝 LOGGING: Reemplazamos println con logging estructurado
            // Al estar dentro del companion object, podemos acceder al logger directamente
            logger.info("🚀 Iniciando el Curso de Kotlin para Aptivistas!")
            
            // 📝 USO DE CONFIGURACIÓN: Leemos propiedades del archivo application.properties
            // Demostramos companion objects en acción con la clase Config
            val appName = Config.getProperty("app.name", "Curso Kotlin")
            val appVersion = Config.getProperty("app.version", "1.0")
            val isDebugEnabled = Config.getBooleanProperty("app.debug.enabled", false)
            
            logger.info("📚 Aplicación: {} v{}", appName, appVersion)
            logger.info("🔧 Modo debug: {}", if (isDebugEnabled) "activado" else "desactivado")
            
            // 📝 VARIABLES Y CONSTANTES (Tema del curso que exploraremos)
            // val = valor inmutable (read-only), var = variable mutable
            val courseName = appName // 📝 val: inmutable, leída de configuración
            var currentPhase = "Phase 1.1: Logging & Config" // 📝 var: mutable, actualizada
            
            logger.debug("📝 Variables: courseName=$courseName, currentPhase=$currentPhase")
            
            // 📝 STRING TEMPLATES con logging
            logger.info("✅ Proyecto inicializado correctamente en {}", getCurrentTimestamp())
            
            // 📝 LLAMADA A FUNCIÓN: Ejemplo de funciones definidas por el usuario
            displayWelcomeMessage()
            
            // 📝 MOSTRAR CONFIGURACIÓN completa si debug está habilitado
            if (isDebugEnabled) {
                logger.debug("📋 Mostrando configuración completa:")
                Config.showConfig()
            }
            
            // 📝 SEGURIDAD CONTRA NULOS: Kotlin es null-safe por defecto
            val projectGoal: String = Config.getProperty("app.description", "Construir un Agente MCP paso a paso")
            logger.info("🎯 Objetivo: {}", projectGoal)
        }
        
        // 📝 FUNCIÓN AUXILIAR pública dentro del companion object (accesible como App.getCurrentTimestamp())
        fun getCurrentTimestamp(): String {
            // 📝 Esta función retorna un String simple
            // En fases posteriores, exploraremos fechas y tiempo de forma más robusta
            // Al estar en el companion object, accedemos al logger directamente
            logger.debug("📅 Obteniendo timestamp actual")
            return "2025-08-12" // Actualizado para Phase 1.1
        }
        
        // 📝 FUNCIÓN AUXILIAR dentro del companion object
        private fun displayWelcomeMessage(): Unit {
            // 📝 Al estar dentro del companion object, acceso directo al logger
            logger.info("📋 Mostrando mensaje de bienvenida")
            
            val separator = "=".repeat(50)
            logger.info(separator)
            logger.info("🎓 CURSO DE KOTLIN PARA APTIVISTAS")
            logger.info(separator)
            logger.info("Durante este curso aprenderemos:")
            logger.info("• Variables y constantes (var, val, const)")
            logger.info("• Tipos de datos y null safety")
            logger.info("• Funciones, clases y objetos")
            logger.info("• 📝 NUEVO: Logging y configuración (companion objects)")
            logger.info("• 📝 NUEVO: Manejo de archivos y recursos (File I/O)")
            logger.info("• Programación asíncrona con coroutines")
            logger.info("• Y mucho más...")
            logger.info(separator)
            logger.info("")
        }
    }
}

// 📝 COMENTARIOS PARA FUTURAS FASES:
// Este archivo crecerá progresivamente en cada fase del curso:
// - ✅ Phase 1.1: COMPLETADA - Logging básico y configuración (companion objects, File I/O)
// - Phase 1.2: Exploraremos tipos de datos y variables en profundidad
// - Phase 2: Implementaremos clases y objetos avanzados
// - Phase 3: Trabajaremos con colecciones y control de flujo
// - Phase 4: Introduciremos programación asíncrona
// - Phase 5+: Construiremos el Agente MCP completo

/*
📝 NOTAS PEDAGÓGICAS Phase 1.1:
- ✅ Implementamos companion objects para el logger (patrón estático en Kotlin)
- ✅ Demostramos File I/O con Properties y getResourceAsStream()
- ✅ Usamos la función 'use' como equivalent de try-with-resources
- ✅ Reemplazamos println() con logging estructurado (SLF4J + Logback)
- ✅ Separamos configuración del código usando archivos .properties
- ✅ Mostramos diferencias entre object singleton y clases regulares
- ✅ Aplicamos null safety con operador Elvis (?:) para valores por defecto
- La estructura del código está diseñada para ser fácil de seguir y modificar
*/
