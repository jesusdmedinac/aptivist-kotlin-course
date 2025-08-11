
// 📝 ARCHIVO DE PRUEBAS UNITARIAS
// Los tests son fundamentales en cualquier proyecto profesional de Kotlin.
// Aquí aprenderemos a escribir pruebas que validen nuestro código.

package com.aptivist.kotlin

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

// 📝 CLASE DE TEST: Agrupa todas las pruebas relacionadas con nuestra App
class AppTest {
    
    // 📝 ANOTACIÓN @Test: Marca una función como una prueba unitaria
    // JUnit ejecutará automáticamente todas las funciones marcadas con @Test
    @Test
    fun testCurrentTimestamp() {
        // 📝 PRUEBA BÁSICA: Verificamos que nuestra función retorna un valor no nulo
        val timestamp = getCurrentTimestamp()
        
        // 📝 ASSERTIONS: Verificamos que el comportamiento es el esperado
        assertNotNull(timestamp, "El timestamp no debe ser nulo")
        assertEquals("2025-08-11", timestamp, "El timestamp debe coincidir con el valor esperado")
    }
    
    // 📝 FUTUROS TESTS: A medida que agreguemos más funcionalidad, 
    // agregaremos más tests para validar cada nueva característica
    
    /*
    📝 CONCEPTOS QUE EXPLORAREMOS EN FUTURAS FASES:
    - Test-driven development (TDD)
    - Mocking y stubbing
    - Tests de integración
    - Tests para coroutines y código asíncrono
    - Cobertura de código
    */
}
