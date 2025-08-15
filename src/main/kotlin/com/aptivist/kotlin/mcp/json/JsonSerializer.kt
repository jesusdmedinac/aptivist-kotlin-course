
package com.aptivist.kotlin.mcp.json

import com.aptivist.kotlin.mcp.protocol.JsonRpcMessage
import com.aptivist.kotlin.mcp.protocol.McpMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerializationException

/**
 * 🧑‍🏫: Este archivo demuestra JSON SERIALIZATION en Kotlin y ERROR HANDLING
 * 
 * Conceptos clave:
 * - kotlinx.serialization para conversión automática JSON ↔ Kotlin objects
 * - Result<T> para manejo funcional de errores
 * - Extension functions para API más limpia
 * - When expressions para pattern matching de tipos
 */

/**
 * 🧑‍🏫: OBJECT SINGLETON para configuración global de JSON
 * 
 * Object vs Class:
 * - Object es singleton thread-safe automáticamente
 * - No se puede instanciar (no tiene constructor)
 * - Ideal para utilities y configuración global
 */
object JsonSerializer {
    
    /**
     * 🧑‍🏫: Configuración del serializer JSON con kotlinx.serialization
     * - ignoreUnknownKeys: ignora propiedades JSON que no existen en Kotlin class
     * - prettyPrint: formatea JSON para legibilidad (útil para debugging)
     * - encodeDefaults: incluye propiedades con valores por defecto
     */
    val jsonConfig = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    
    /**
     * 🧑‍🏫: GENERIC FUNCTION con REIFIED TYPE PARAMETER
     * 
     * `inline` + `reified T` permite acceso al tipo T en runtime
     * Sin esto, el type erasure de la JVM eliminaría la información del tipo
     */
    inline fun <reified T> serialize(obj: T): Result<String> {
        return try {
            /**
             * 🧑‍🏫: RESULT.success() - patrón funcional para manejar éxito
             * Result<T> es una sealed class que puede ser Success o Failure
             */
            Result.success(jsonConfig.encodeToString(obj))
        } catch (e: SerializationException) {
            /**
             * 🧑‍🏫: RESULT.failure() - manejo funcional de errores
             * Evita exceptions y hace el error handling explícito
             */
            Result.failure(e)
        }
    }
    
    /**
     * 🧑‍🏫: GENERIC FUNCTION para deserialización con manejo de errores
     */
    inline fun <reified T> deserialize(json: String): Result<T> {
        return try {
            Result.success(jsonConfig.decodeFromString<T>(json))
        } catch (e: SerializationException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }
    }
}

/**
 * 🧑‍🏫: EXTENSION FUNCTIONS que hacen la API más fluida y fácil de usar
 * 
 * Estas funciones se pueden llamar como métodos en cualquier objeto:
 * val json = myObject.toJson()
 * val obj = jsonString.fromJson<MyClass>()
 */

/**
 * Extension function genérica para convertir cualquier objeto a JSON
 */
inline fun <reified T> T.toJson(): Result<String> = JsonSerializer.serialize(this)

/**
 * Extension function para String que permite deserializar a cualquier tipo
 */
inline fun <reified T> String.fromJson(): Result<T> = JsonSerializer.deserialize<T>(this)

/**
 * 🧑‍🏫: SPECIALIZED EXTENSION FUNCTIONS para tipos específicos del protocolo
 * Estas funciones muestran cómo crear APIs type-safe para casos específicos
 */

fun JsonRpcMessage.Request.toJsonString(): String {
    return this.toJson().getOrThrow() // 🧑‍🏫: getOrThrow() convierte Result<T> a T o lanza exception
}

fun String.parseJsonRpcRequest(): Result<JsonRpcMessage.Request> {
    return this.fromJson<JsonRpcMessage.Request>()
}

fun JsonRpcMessage.Response.toJsonString(): String {
    return this.toJson().getOrThrow()
}

fun String.parseJsonRpcResponse(): Result<JsonRpcMessage.Response> {
    return this.fromJson<JsonRpcMessage.Response>()
}

/**
 * 🧑‍🏫: UTILITY FUNCTIONS que demuestran WHEN EXPRESSIONS con sealed classes
 * 
 * Estas funciones muestran pattern matching exhaustivo y type-safe casting
 */

/**
 * Function que determina el tipo de mensaje JSON-RPC y lo deserializa apropiadamente
 */
fun String.parseJsonRpcMessage(): Result<JsonRpcMessage> {
    // 🧑‍🏫: Primero parseamos como estructura genérica para determinar el tipo
    val genericResult = this.fromJson<Map<String, Any?>>()
    
    return genericResult.fold(
        onSuccess = { map ->
            /**
             * 🧑‍🏫: WHEN EXPRESSION con pattern matching en propiedades
             * Kotlin smart cast nos permite usar las propiedades sin casting explícito
             */
            when {
                map.containsKey("method") -> this.fromJson<JsonRpcMessage.Request>()
                map.containsKey("result") || map.containsKey("error") -> this.fromJson<JsonRpcMessage.Response>()
                else -> Result.failure(IllegalArgumentException("Unknown JSON-RPC message type"))
            }
        },
        onFailure = { Result.failure(it) }
    )
}

/**
 * 🧑‍🏫: Extension function que demuestra uso de RESULT.fold()
 * fold() es una higher-order function que maneja ambos casos Success/Failure
 */
fun Result<String>.printJson() {
    this.fold(
        onSuccess = { json -> println("✅ JSON: $json") },
        onFailure = { error -> println("❌ Error serializing to JSON: ${error.message}") }
    )
}
