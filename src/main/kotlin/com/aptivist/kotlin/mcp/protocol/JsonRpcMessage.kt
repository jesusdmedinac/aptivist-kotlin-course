
package com.aptivist.kotlin.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 🧑‍🏫: Este archivo demuestra SEALED CLASSES y DATA CLASSES - conceptos fundamentales en Kotlin
 * 
 * Las SEALED CLASSES son como enums super-powered que nos permiten:
 * - Definir un conjunto cerrado de tipos relacionados
 * - Garantizar pattern matching exhaustivo con `when`
 * - Cada subclase puede tener propiedades diferentes
 * 
 * Las DATA CLASSES automáticamente generan:
 * - equals(), hashCode(), toString()
 * - Función copy() para crear copias inmutables
 * - Destructuring declarations (component functions)
 */

// 🧑‍🏫: Sealed class para representar todos los tipos de mensajes JSON-RPC
// El modificador `sealed` garantiza que todas las subclases estén en el mismo archivo o módulo
@Serializable
sealed class JsonRpcMessage {
    
    /**
     * 🧑‍🏫: DATA CLASS - clase optimizada para contener datos
     * - Todas las propiedades son inmutables (val) por buena práctica
     * - @Serializable permite convertir automáticamente a/desde JSON
     * - Propiedades opcionales usando tipos nullable con defaults
     */
    @Serializable
    data class Request(
        val jsonrpc: String = "2.0",
        val method: String,
        val params: JsonObject? = null,
        val id: String? = null  // null = notification, non-null = request
    ) : JsonRpcMessage()

    @Serializable
    data class Response(
        val jsonrpc: String = "2.0",
        val id: String,
        val result: JsonObject? = null,
        val error: ErrorObject? = null
    ) : JsonRpcMessage()

    @Serializable 
    data class ErrorObject(
        val code: Int,
        val message: String,
        val data: JsonObject? = null
    )
}

/**
 * 🧑‍🏫: EXTENSION FUNCTIONS - añadir funcionalidad a clases existentes sin herencia
 * Estas funciones se pueden llamar como si fueran métodos de la clase JsonRpcMessage.Request
 */

// Extension function para verificar si un Request es una notification
fun JsonRpcMessage.Request.isNotification(): Boolean = this.id == null

// Extension function para verificar si un Request requiere respuesta
fun JsonRpcMessage.Request.requiresResponse(): Boolean = this.id != null

// 🧑‍🏫: Extension function que demuestra WHEN EXPRESSION con sealed class
fun JsonRpcMessage.getMessageType(): String = when (this) {
    is JsonRpcMessage.Request -> if (isNotification()) "notification" else "request"
    is JsonRpcMessage.Response -> "response"
}

/**
 * 🧑‍🏫: OBJECT SINGLETON para constantes relacionadas con JSON-RPC
 * Los objects son singletons thread-safe por defecto en Kotlin
 */
object JsonRpcConstants {
    const val VERSION = "2.0"
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
}
