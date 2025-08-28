

package com.aptivist.kotlin.http.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 🎯 PHASE 3.2 - TESTS DE INTEGRACIÓN PARA API ROUTES
 * 
 * Esta clase de test demuestra conceptos avanzados de testing de APIs REST:
 * • **Integration Testing**: Tests de endpoints completos con servidor real
 * • **Ktor Testing**: Uso del framework de testing de Ktor
 * • **HTTP Testing**: Verificación de códigos de estado, headers y responses
 * • **JSON Testing**: Parsing y verificación de responses JSON
 * • **Error Testing**: Verificación de manejo de errores en endpoints
 * 
 * PED: Los tests de integración son cruciales para APIs REST porque:
 * - Verifican que todos los componentes funcionen juntos
 * - Prueban el comportamiento real de los endpoints
 * - Validan serialización/deserialización en contexto real
 * - Aseguran que el manejo de errores funcione end-to-end
 * - Documentan el comportamiento esperado de la API
 */
class ApiRoutesTest {
    
    /**
     * PED: JSON PARSER PARA VERIFICAR RESPONSES
     */
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    /**
     * PED: TEST GROUP - ENDPOINTS BÁSICOS
     * 
     * Estos tests verifican que los endpoints básicos del servidor
     * respondan correctamente.
     */
    
    @Test
    fun `GET root should return server information`() = testApplication {
        // PED: Configurar aplicación de test
        application {
            configureApiRoutes()
        }
        
        // PED: Hacer request al endpoint
        val response = client.get("/")
        
        // PED: Verificar response
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"phase\":\"3.2\""))
        assertTrue(responseText.contains("\"framework\":\"Ktor\""))
    }
    
    @Test
    fun `GET health should return health status`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/health")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"status\":\"healthy\""))
        assertTrue(responseText.contains("\"server\":\"ktor-netty\""))
    }
    
    /**
     * PED: TEST GROUP - API STATE ENDPOINTS
     * 
     * Tests para endpoints de estado de la aplicación.
     */
    
    @Test
    fun `GET api v1 state should return application state`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/state")
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"server\""))
        assertTrue(responseText.contains("\"connections\""))
        assertTrue(responseText.contains("\"plugins\""))
        assertTrue(responseText.contains("\"ui\""))
        assertTrue(responseText.contains("\"metadata\""))
        assertTrue(responseText.contains("\"health\""))
    }
    
    @Test
    fun `GET api v1 state summary should return state summary`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/state/summary")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"isHealthy\""))
        assertTrue(responseText.contains("\"serverRunning\""))
        assertTrue(responseText.contains("\"activeConnections\""))
        assertTrue(responseText.contains("\"activePlugins\""))
        assertTrue(responseText.contains("\"lastUpdated\""))
    }
    
    /**
     * PED: TEST GROUP - CONNECTIONS ENDPOINTS
     * 
     * Tests para endpoints de gestión de conexiones.
     */
    
    @Test
    fun `GET api v1 connections should return paginated connections`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/connections")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"data\""))
        assertTrue(responseText.contains("\"pagination\""))
    }
    
    @Test
    fun `GET api v1 connections with pagination parameters should work`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/connections?page=0&size=5&sort=id&direction=asc")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"page\":0"))
        assertTrue(responseText.contains("\"size\":5"))
    }
    
    @Test
    fun `POST api v1 connections should create new connection`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val requestBody = """
            {
                "type": "WEBSOCKET",
                "clientInfo": {
                    "userAgent": "Test Client",
                    "version": "1.0.0",
                    "capabilities": ["test"]
                }
            }
        """.trimIndent()
        
        val response = client.post("/api/v1/connections") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"id\""))
        assertTrue(responseText.contains("\"message\":\"Connection created successfully\""))
    }
    
    @Test
    fun `POST api v1 connections with invalid data should return validation error`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val requestBody = """
            {
                "type": "INVALID_TYPE",
                "clientInfo": {
                    "userAgent": "Test Client",
                    "capabilities": []
                }
            }
        """.trimIndent()
        
        val response = client.post("/api/v1/connections") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"code\":\"VALIDATION_ERROR\""))
        assertTrue(responseText.contains("\"field\":\"type\""))
    }
    
    @Test
    fun `GET api v1 connections with invalid connection id should return not found`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/connections/non-existent-id")
        
        assertEquals(HttpStatusCode.NotFound, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"code\":\"RESOURCE_NOT_FOUND\""))
        assertTrue(responseText.contains("\"resourceType\":\"Connection\""))
    }
    
    /**
     * PED: TEST GROUP - PLUGINS ENDPOINTS
     */
    
    @Test
    fun `GET api v1 plugins should return plugin summary`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/plugins")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"totalPlugins\""))
        assertTrue(responseText.contains("\"activePlugins\""))
        assertTrue(responseText.contains("\"errorPlugins\""))
        assertTrue(responseText.contains("\"pluginStats\""))
    }
    
    @Test
    fun `POST api v1 plugins should accept plugin load request`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val requestBody = """
            {
                "id": "test-plugin",
                "version": "1.0.0",
                "config": {
                    "enabled": "true"
                }
            }
        """.trimIndent()
        
        val response = client.post("/api/v1/plugins") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        assertEquals(HttpStatusCode.Accepted, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"message\":\"Plugin load request accepted\""))
        assertTrue(responseText.contains("\"pluginId\":\"test-plugin\""))
    }
    
    /**
     * PED: TEST GROUP - SERVER ENDPOINTS
     */
    
    @Test
    fun `GET api v1 server should return server information`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/server")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"isRunning\""))
        assertTrue(responseText.contains("\"port\""))
        assertTrue(responseText.contains("\"host\""))
        assertTrue(responseText.contains("\"protocol\""))
        assertTrue(responseText.contains("\"statistics\""))
    }
    
    @Test
    fun `PUT api v1 server config should update server configuration`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val requestBody = """
            {
                "maxConnections": 200,
                "capabilities": ["tools", "resources", "prompts"]
            }
        """.trimIndent()
        
        val response = client.put("/api/v1/server/config") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"message\":\"Server configuration updated successfully\""))
        assertTrue(responseText.contains("\"config\""))
    }
    
    /**
     * PED: TEST GROUP - HEALTH ENDPOINTS
     */
    
    @Test
    fun `GET api v1 health should return comprehensive health check`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/health")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"isHealthy\""))
        assertTrue(responseText.contains("\"status\""))
        assertTrue(responseText.contains("\"checks\""))
        assertTrue(responseText.contains("\"lastCheck\""))
    }
    
    @Test
    fun `GET api v1 health live should return liveness probe`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/health/live")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"status\":\"alive\""))
        assertTrue(responseText.contains("\"timestamp\""))
    }
    
    @Test
    fun `GET api v1 health ready should return readiness probe`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/health/ready")
        
        assertEquals(HttpStatusCode.OK, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"status\":\"ready\""))
        assertTrue(responseText.contains("\"details\""))
    }
    
    /**
     * PED: TEST GROUP - ERROR HANDLING
     * 
     * Tests para verificar que el manejo de errores funcione correctamente.
     */
    
    @Test
    fun `GET non-existent endpoint should return 404 with structured error`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/non-existent")
        
        assertEquals(HttpStatusCode.NotFound, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"code\":\"RESOURCE_NOT_FOUND\""))
        assertTrue(responseText.contains("\"resourceType\":\"endpoint\""))
        assertTrue(responseText.contains("\"path\":\"/api/v1/non-existent\""))
    }
    
    @Test
    fun `POST with invalid JSON should return serialization error`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.post("/api/v1/connections") {
            contentType(ContentType.Application.Json)
            setBody("{ invalid json }")
        }
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"code\":\"VALIDATION_ERROR\""))
        assertTrue(responseText.contains("\"field\":\"request_body\""))
    }
    
    @Test
    fun `GET with invalid pagination parameters should return validation error`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/connections?page=-1&size=0")
        
        assertEquals(HttpStatusCode.BadRequest, response.status)
        
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("\"code\":\"VALIDATION_ERROR\""))
    }
    
    /**
     * PED: TEST GROUP - CONTENT NEGOTIATION
     * 
     * Tests para verificar que la negociación de contenido funcione correctamente.
     */
    
    @Test
    fun `endpoints should return JSON content type`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val response = client.get("/api/v1/state")
        
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
    }
    
    @Test
    fun `endpoints should accept JSON content type for POST requests`() = testApplication {
        application {
            configureApiRoutes()
        }
        
        val requestBody = """
            {
                "type": "HTTP",
                "clientInfo": {
                    "userAgent": "Test Client",
                    "capabilities": ["test"]
                }
            }
        """.trimIndent()
        
        val response = client.post("/api/v1/connections") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }
        
        assertEquals(HttpStatusCode.Created, response.status)
    }
}

