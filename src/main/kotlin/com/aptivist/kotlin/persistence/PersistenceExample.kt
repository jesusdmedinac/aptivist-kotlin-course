
package com.aptivist.kotlin.persistence

import com.aptivist.kotlin.persistence.db.*
import com.aptivist.kotlin.persistence.repository.CreateUserRequest
import com.aptivist.kotlin.state.AppState
import com.aptivist.kotlin.state.StateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 * PED: PERSISTENCE EXAMPLE - DEMOSTRACIÓN COMPLETA
 * 
 * Este ejemplo demuestra el uso completo del sistema de persistencia:
 * 1. **Database Operations**: CRUD operations con Exposed ORM
 * 2. **Caching Integration**: Cache automático con Caffeine
 * 3. **File Persistence**: Backup y recovery con JSON storage
 * 4. **State Management**: Integración con StateManager
 * 5. **Error Handling**: Manejo robusto de errores
 * 6. **Performance Monitoring**: Métricas y estadísticas
 * 7. **Lifecycle Management**: Inicialización y cleanup
 * 8. **Configuration**: Configuración flexible por ambiente
 * 9. **Background Tasks**: Tareas automáticas de mantenimiento
 * 10. **Health Checks**: Monitoreo de salud del sistema
 */
object PersistenceExample {
    
    private val logger = LoggerFactory.getLogger(PersistenceExample::class.java)
    
    /**
     * PED: MAIN EXAMPLE FUNCTION
     * Demuestra todas las características del sistema de persistencia
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        logger.info("🚀 Starting Persistence Example...")
        
        try {
            // PED: 1. Setup - Configuración del sistema
            setupExample()
            
            // PED: 2. Database Operations - Operaciones de base de datos
            databaseOperationsExample()
            
            // PED: 3. Caching Example - Ejemplo de caching
            cachingExample()
            
            // PED: 4. File Storage Example - Ejemplo de almacenamiento de archivos
            fileStorageExample()
            
            // PED: 5. Integration Example - Ejemplo de integración completa
            integrationExample()
            
            // PED: 6. Performance Monitoring - Monitoreo de rendimiento
            performanceMonitoringExample()
            
            // PED: 7. Error Handling - Manejo de errores
            errorHandlingExample()
            
            logger.info("✅ Persistence Example completed successfully!")
            
        } catch (e: Exception) {
            logger.error("❌ Persistence Example failed", e)
        }
    }
    
    /**
     * PED: SETUP EXAMPLE
     * Configuración inicial del sistema
     */
    private suspend fun setupExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("📋 1. SETUP EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Configuración para desarrollo
        val devConfig = persistenceConfig {
            h2InMemory("example_db")
            enableCaching()
            cacheSize(100)
            cacheExpiration(Duration.ofMinutes(5))
            enableFileBackup()
            backupInterval(Duration.ofMinutes(1))
            fileStorageBaseDir("./data/example")
        }
        
        logger.info("🔧 Development config created:")
        logger.info("   Database: ${devConfig.database}")
        logger.info("   Caching: ${devConfig.enableCaching}")
        logger.info("   Cache Size: ${devConfig.cacheSize}")
        logger.info("   File Backup: ${devConfig.enableFileBackup}")
        
        // PED: Configuración para producción
        val prodConfig = PersistenceConfig.production()
        logger.info("🏭 Production config:")
        logger.info("   Database: ${prodConfig.database}")
        logger.info("   Cache Size: ${prodConfig.cacheSize}")
        logger.info("   Backup Interval: ${prodConfig.backupInterval}")
    }
    
    /**
     * PED: DATABASE OPERATIONS EXAMPLE
     * Demuestra operaciones CRUD con Exposed ORM
     */
    private suspend fun databaseOperationsExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("💾 2. DATABASE OPERATIONS EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Crear database factory y conexión
        val database = DatabaseFactory.createH2InMemory(
            databaseName = "crud_example",
            enableLogging = true
        ) {
            // PED: Inicializar schema usando extension function
            initializeSchema(Users, Projects, Tasks)
        }
        
        val userRepository = com.aptivist.kotlin.persistence.repository.UserRepository()
        
        // PED: CREATE - Crear usuarios
        logger.info("➕ Creating users...")
        val createRequests = listOf(
            CreateUserRequest("alice", "alice@example.com", "hash1", "Alice", "Smith"),
            CreateUserRequest("bob", "bob@example.com", "hash2", "Bob", "Johnson"),
            CreateUserRequest("charlie", "charlie@example.com", "hash3", "Charlie", "Brown")
        )
        
        val createdUsers = userRepository.createUsers(createRequests)
        createdUsers.onSuccess { users ->
            logger.info("✅ Created ${users.size} users:")
            users.forEach { user ->
                logger.info("   - ${user.fullName} (${user.username})")
            }
        }.onError { error ->
            logger.error("❌ Failed to create users", error)
        }
        
        // PED: READ - Leer usuarios
        logger.info("\n📖 Reading users...")
        val allUsers = userRepository.findAll()
        allUsers.onSuccess { users ->
            logger.info("📋 Found ${users.size} users:")
            users.forEach { user ->
                logger.info("   - ID: ${user.id.value}, Name: ${user.fullName}, Status: ${user.status}")
            }
        }
        
        // PED: SEARCH - Búsqueda de usuarios
        logger.info("\n🔍 Searching users...")
        val searchResult = userRepository.searchUsers("alice", EntityStatus.Pending)
        searchResult.onSuccess { users ->
            logger.info("🎯 Search results for 'alice':")
            users.forEach { user ->
                logger.info("   - ${user.fullName} (${user.email})")
            }
        }
        
        // PED: UPDATE - Actualizar usuarios
        logger.info("\n✏️ Updating user status...")
        val firstUser = allUsers.getOrNull()?.firstOrNull()
        firstUser?.let { user ->
            val updateResult = userRepository.activateUser(user.id.value)
            updateResult.onSuccess { updatedUser ->
                logger.info("✅ User activated: ${updatedUser.fullName} -> ${updatedUser.status}")
            }
        }
        
        // PED: STATISTICS - Estadísticas
        logger.info("\n📊 Getting statistics...")
        val stats = userRepository.getUserStatistics()
        stats.onSuccess { statistics ->
            logger.info("📈 User Statistics:")
            logger.info("   Total Users: ${statistics.totalUsers}")
            logger.info("   Active Users: ${statistics.activeUsers}")
            logger.info("   Active Percentage: ${"%.1f".format(statistics.activePercentage)}%")
        }
        
        database.close()
    }
    
    /**
     * PED: CACHING EXAMPLE
     * Demuestra el uso del sistema de cache
     */
    private suspend fun cachingExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("🎯 3. CACHING EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Crear cache con configuración personalizada
        val userCache = com.aptivist.kotlin.persistence.cache.CacheManager.getOrCreate<String, String>(
            name = "example_cache",
            config = com.aptivist.kotlin.persistence.cache.cacheConfig("example_cache") {
                maximumSize(50)
                expireAfterAccess(Duration.ofMinutes(2))
                recordStats()
            }
        )
        
        // PED: PUT operations
        logger.info("💾 Storing values in cache...")
        userCache.put("user1", "Alice Smith")
        userCache.put("user2", "Bob Johnson")
        userCache.put("user3", "Charlie Brown")
        
        // PED: GET operations
        logger.info("📖 Reading values from cache...")
        val user1 = userCache.get("user1")
        val user2 = userCache.get("user2")
        val nonExistent = userCache.get("user999")
        
        logger.info("✅ Cache results:")
        logger.info("   user1: $user1")
        logger.info("   user2: $user2")
        logger.info("   user999: $nonExistent")
        
        // PED: GET with loader
        logger.info("🔄 Using cache with loader...")
        val loadedUser = userCache.get("user4") { key ->
            logger.info("   🔧 Loading $key from external source...")
            "David Wilson" // Simulated external load
        }
        logger.info("✅ Loaded user: $loadedUser")
        
        // PED: Cache statistics
        logger.info("📊 Cache statistics:")
        val stats = userCache.stats()
        logger.info("   Hit Rate: ${"%.2f".format(stats.hitRate * 100)}%")
        logger.info("   Miss Rate: ${"%.2f".format(stats.missRate * 100)}%")
        logger.info("   Request Count: ${stats.requestCount}")
        logger.info("   Cache Size: ${userCache.size()}")
        
        // PED: Batch operations
        logger.info("📦 Batch operations...")
        val batchData = mapOf(
            "batch1" to "User One",
            "batch2" to "User Two",
            "batch3" to "User Three"
        )
        userCache.putAll(batchData)
        
        val batchResults = userCache.getAll(setOf("batch1", "batch2", "batch3", "missing"))
        logger.info("✅ Batch results: $batchResults")
    }
    
    /**
     * PED: FILE STORAGE EXAMPLE
     * Demuestra el almacenamiento de archivos con backup
     */
    private suspend fun fileStorageExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("📁 4. FILE STORAGE EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Crear file storage
        val storage = com.aptivist.kotlin.persistence.file.FileStorageFactory.createJsonStorage<Map<String, Any>>(
            config = com.aptivist.kotlin.persistence.file.fileStorageConfig("./data/example") {
                enableBackups()
                maxBackups(3)
                atomicWrites()
                prettyPrint()
            }
        )
        
        // PED: SAVE operations
        logger.info("💾 Saving data to file storage...")
        val sampleData = mapOf(
            "application" to "Kotlin Course",
            "version" to "4.1.0",
            "timestamp" to System.currentTimeMillis(),
            "features" to listOf("database", "caching", "file-storage"),
            "config" to mapOf(
                "debug" to true,
                "maxUsers" to 1000
            )
        )
        
        val saveResult = storage.save("app_config", sampleData)
        saveResult.onSuccess {
            logger.info("✅ Data saved successfully")
        }.onError { error ->
            logger.error("❌ Failed to save data", error)
        }
        
        // PED: LOAD operations
        logger.info("📖 Loading data from file storage...")
        val loadResult = storage.load("app_config")
        loadResult.onSuccess { data ->
            logger.info("✅ Data loaded:")
            data?.forEach { (key, value) ->
                logger.info("   $key: $value")
            }
        }
        
        // PED: BACKUP operations
        logger.info("💾 Creating backup...")
        val backupResult = storage.backup("app_config")
        backupResult.onSuccess { backupId ->
            logger.info("✅ Backup created: $backupId")
            
            // PED: List backups
            val backupsResult = storage.listBackups("app_config")
            backupsResult.onSuccess { backups ->
                logger.info("📋 Available backups:")
                backups.forEach { backup ->
                    logger.info("   - $backup")
                }
            }
        }
        
        // PED: LIST operations
        logger.info("📋 Listing all files...")
        val listResult = storage.list()
        listResult.onSuccess { files ->
            logger.info("📁 Files in storage:")
            files.forEach { file ->
                logger.info("   - $file")
            }
        }
    }
    
    /**
     * PED: INTEGRATION EXAMPLE
     * Demuestra la integración completa con StateManager
     */
    private suspend fun integrationExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("🔗 5. INTEGRATION EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Crear StateManager
        val stateManager = StateManager()
        
        // PED: Crear PersistenceManager
        val persistenceManager = PersistenceManagerFactory.createForDevelopment(stateManager)
        
        // PED: Inicializar
        logger.info("🚀 Initializing integrated system...")
        persistenceManager.initialize()
        
        // PED: Crear algunos usuarios de ejemplo
        logger.info("👥 Creating example users...")
        val database = DatabaseFactory.createH2InMemory("integration_example") {
            initializeSchema(Users, Projects, Tasks)
        }
        
        val user1 = User.create(
            username = "integration_user1",
            email = "user1@integration.com",
            passwordHash = "hash1",
            firstName = "Integration",
            lastName = "User One"
        )
        
        val user2 = User.create(
            username = "integration_user2",
            email = "user2@integration.com",
            passwordHash = "hash2",
            firstName = "Integration",
            lastName = "User Two"
        )
        
        // PED: Usar PersistenceManager para operaciones
        logger.info("💾 Using PersistenceManager operations...")
        val saved1 = persistenceManager.saveUser(user1)
        val saved2 = persistenceManager.saveUser(user2)
        
        logger.info("✅ Users saved: $saved1, $saved2")
        
        // PED: Leer usuarios (debería usar cache)
        logger.info("📖 Reading users (should hit cache)...")
        val retrieved1 = persistenceManager.getUser(user1.id.value)
        val retrieved2 = persistenceManager.getUser(user2.id.value)
        
        logger.info("✅ Users retrieved:")
        logger.info("   User 1: ${retrieved1?.fullName}")
        logger.info("   User 2: ${retrieved2?.fullName}")
        
        // PED: Guardar estado de aplicación
        logger.info("💾 Saving application state...")
        val stateSaved = persistenceManager.saveAppState()
        logger.info("✅ State saved: $stateSaved")
        
        // PED: Crear backup
        logger.info("💾 Creating state backup...")
        val backupId = persistenceManager.backupAppState()
        logger.info("✅ Backup created: $backupId")
        
        // PED: Health check
        logger.info("🏥 Performing health check...")
        val health = persistenceManager.healthCheck()
        logger.info("✅ Health status:")
        health.forEach { (component, status) ->
            val icon = if (status) "✅" else "❌"
            logger.info("   $icon $component: $status")
        }
        
        // PED: Cleanup
        logger.info("🧹 Cleaning up...")
        persistenceManager.shutdown()
        database.close()
    }
    
    /**
     * PED: PERFORMANCE MONITORING EXAMPLE
     * Demuestra el monitoreo de rendimiento
     */
    private suspend fun performanceMonitoringExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("📊 6. PERFORMANCE MONITORING EXAMPLE")
        logger.info("=".repeat(50))
        
        val stateManager = StateManager()
        val persistenceManager = PersistenceManagerFactory.createForDevelopment(stateManager)
        persistenceManager.initialize()
        
        // PED: Simular carga de trabajo
        logger.info("⚡ Simulating workload...")
        val database = DatabaseFactory.createH2InMemory("perf_example") {
            initializeSchema(Users)
        }
        
        repeat(10) { i ->
            val user = User.create(
                username = "perf_user_$i",
                email = "perf$i@example.com",
                passwordHash = "hash$i",
                firstName = "Performance",
                lastName = "User $i"
            )
            persistenceManager.saveUser(user)
            
            // PED: Leer usuario múltiples veces para generar cache hits
            repeat(3) {
                persistenceManager.getUser(user.id.value)
            }
        }
        
        // PED: Obtener estadísticas
        logger.info("📈 Getting performance statistics...")
        val cacheStats = persistenceManager.getCacheStatistics()
        
        logger.info("🎯 Cache Performance:")
        cacheStats.forEach { (cacheName, stats) ->
            logger.info("   Cache: $cacheName")
            if (stats is Map<*, *>) {
                stats.forEach { (metric, value) ->
                    logger.info("     $metric: $value")
                }
            }
        }
        
        persistenceManager.shutdown()
        database.close()
    }
    
    /**
     * PED: ERROR HANDLING EXAMPLE
     * Demuestra el manejo robusto de errores
     */
    private suspend fun errorHandlingExample() {
        logger.info("\n" + "=".repeat(50))
        logger.info("⚠️ 7. ERROR HANDLING EXAMPLE")
        logger.info("=".repeat(50))
        
        // PED: Intentar operaciones que fallarán
        logger.info("🧪 Testing error scenarios...")
        
        // PED: Database error simulation
        try {
            val invalidConfig = DatabaseFactory.DatabaseConfig.PostgreSQL(
                host = "nonexistent-host",
                database = "nonexistent-db",
                username = "invalid",
                password = "invalid"
            )
            
            // PED: Esto debería fallar gracefully
            val database = DatabaseFactory.create(invalidConfig)
            logger.info("❌ This should not succeed")
        } catch (e: Exception) {
            logger.info("✅ Database error handled gracefully: ${e.message}")
        }
        
        // PED: File storage error simulation
        val storage = com.aptivist.kotlin.persistence.file.FileStorageFactory.createJsonStorage<String>(
            config = com.aptivist.kotlin.persistence.file.fileStorageConfig("/invalid/path/that/does/not/exist") {
                enableBackups(false) // Disable backups to avoid additional errors
            }
        )
        
        val saveResult = storage.save("test", "data")
        saveResult.onSuccess {
            logger.info("❌ This should not succeed")
        }.onError { error ->
            logger.info("✅ File storage error handled gracefully: ${error.message}")
        }
        
        // PED: Cache error simulation
        logger.info("🎯 Testing cache resilience...")
        val cache = com.aptivist.kotlin.persistence.cache.CacheManager.getOrCreate<String, String>(
            name = "error_test_cache"
        )
        
        // PED: Normal operation should work
        cache.put("test", "value")
        val retrieved = cache.get("test")
        logger.info("✅ Cache operation successful: $retrieved")
        
        logger.info("✅ Error handling examples completed")
    }
}

