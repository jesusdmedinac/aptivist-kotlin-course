
// 📝 CLASE DE CONFIGURACIÓN (Phase 1.1)
// Esta clase demuestra cómo leer archivos de propiedades y usar companion objects en Kotlin.
// Los companion objects son equivalentes a métodos estáticos en Java, pero más poderosos.

package com.aptivist.kotlin

import org.slf4j.LoggerFactory
import java.util.Properties

// 📝 CLASE CONFIG con companion object
// Esta clase encapsula la lógica de carga de configuración de la aplicación
class Config {
    
    // 📝 COMPANION OBJECT: Similar a métodos estáticos pero con más funcionalidades
    // Un companion object puede implementar interfaces y tener herencia
    companion object {
        
        // 📝 LOGGER usando companion object - patrón común en aplicaciones Kotlin
        // El logger se crea una sola vez por clase y se comparte entre todas las instancias
        private val logger = LoggerFactory.getLogger(Config::class.java)
        
        // 📝 PROPIEDAD para almacenar las properties cargadas
        private val properties = Properties()
        
        // 📝 BLOQUE INIT: Se ejecuta cuando la clase se carga por primera vez
        // Equivalent a static initialization block en Java
        init {
            loadProperties()
        }
        
        // 📝 FUNCIÓN PARA CARGAR PROPIEDADES desde resources
        // Demuestra el uso de try-with-resources (use function) en Kotlin
        private fun loadProperties() {
            try {
                // 📝 USE FUNCTION: Kotlin's equivalent to try-with-resources
                // Automáticamente cierra el resource cuando termina el bloque
                Config::class.java.getResourceAsStream("/application.properties")?.use { stream ->
                    properties.load(stream)
                    logger.info("✅ Configuración cargada exitosamente")
                    logger.debug("📝 Propiedades cargadas: ${properties.size}")
                } ?: run {
                    logger.warn("⚠️  No se pudo encontrar application.properties")
                }
            } catch (e: Exception) {
                logger.error("❌ Error cargando configuración: ${e.message}")
            }
        }
        
        // 📝 FUNCIÓN para obtener una propiedad con valor por defecto
        // Demuestra el uso del operador Elvis (?:) para valores por defecto
        fun getProperty(key: String, defaultValue: String = ""): String {
            return properties.getProperty(key) ?: defaultValue
        }
        
        // 📝 FUNCIÓN para obtener propiedades booleanas
        // Demuestra conversión de tipos y manejo seguro de nulos
        fun getBooleanProperty(key: String, defaultValue: Boolean = false): Boolean {
            return properties.getProperty(key)?.toBooleanStrictOrNull() ?: defaultValue
        }
        
        // 📝 FUNCIÓN para obtener propiedades enteras
        // Muestra manejo de excepciones y conversiones seguras
        fun getIntProperty(key: String, defaultValue: Int = 0): Int {
            return try {
                properties.getProperty(key)?.toInt() ?: defaultValue
            } catch (e: NumberFormatException) {
                logger.warn("⚠️  Valor inválido para '$key', usando default: $defaultValue")
                defaultValue
            }
        }
        
        // 📝 FUNCIÓN para mostrar toda la configuración (útil para debugging)
        fun showConfig() {
            logger.info("📋 === CONFIGURACIÓN DE LA APLICACIÓN ===")
            properties.forEach { (key, value) ->
                logger.info("📝 $key = $value")
            }
            logger.info("📋 === FIN CONFIGURACIÓN ===")
        }
    }
}

// 📝 NOTAS PEDAGÓGICAS sobre companion objects:
// 1. Se declaran con 'companion object' dentro de una clase
// 2. Sus miembros son accesibles sin crear una instancia de la clase
// 3. Pueden tener nombre: companion object Factory { ... }
// 4. Pueden implementar interfaces y heredar de otras clases
// 5. Son más flexibles que los métodos estáticos de Java
