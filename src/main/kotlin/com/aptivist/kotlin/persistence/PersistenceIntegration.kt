
package com.aptivist.kotlin.persistence

import com.aptivist.kotlin.persistence.cache.CacheManager
import com.aptivist.kotlin.persistence.cache.cacheConfig
import com.aptivist.kotlin.persistence.db.*
import com.aptivist.kotlin.persistence.file.FileStorageFactory
import com.aptivist.kotlin.persistence.file.fileStorageConfig
import com.aptivist.kotlin.persistence.repository.UserRepository
import com.aptivist.kotlin.state.AppState
import com.aptivist.kotlin.state.StateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

/**
 * PED: PERSISTENCE INTEGRATION CON STATE MANAGER
 * 
 * Este archivo demuestra la integración completa de persistencia con el sistema de estado:
 * 1. **Integration Patterns**: Conexión entre diferentes capas de persistencia
 * 2. **State Synchronization**: Sincronización entre cache, DB y file storage
 * 3. **Lifecycle Management**: Manejo del ciclo de vida de recursos de persistencia
 * 4. **Error Recovery**: Estrategias de recuperación ante fallos
 * 5. **Performance Optimization**: Optimizaciones de rendimiento con caching
 * 6. **Transaction Coordination**: Coordinación de transacciones entre sistemas
 * 7. **Backup Strategies**: Estrategias de backup automático
 * 8. **Monitoring & Metrics**: Monitoreo de performance y métricas
 * 9. **Configuration Management**: Gestión centralizada de configuración
 * 10. **Dependency Injection**: Inyección de dependencias para testabilidad
 */

/**
 * PED: DATA CLASS para configuración de persistencia
 * Centraliza toda la configuración de persistencia
 */
data class PersistenceConfig(
    val database: DatabaseFactory.DatabaseConfig = DatabaseFactory.DatabaseConfig.H2InMemory(),
    val enableCaching: Boolean = true,
    val enableFileBackup: Boolean = true,
    val backupInterval: Duration = Duration.ofMinutes(30),
    val cacheSize: Long = 1000,
    val cacheExpiration: Duration = Duration.ofHours(1),
    val fileStorageBaseDir: String = "./data/persistence"
) {
    companion object {
        fun development(): PersistenceConfig = PersistenceConfig(
            database = DatabaseFactory.DatabaseConfig.H2InMemory("devdb"),
            enableCaching = true,
            enableFileBackup = true,
            backupInterval = Duration.ofMinutes(5),
            cacheSize = 500
        )
        
        fun production(): PersistenceConfig = PersistenceConfig(
            database = DatabaseFactory.DatabaseConfig.H2Embedded("proddb"),
            enableCaching = true,
            enableFileBackup = true,
            backupInterval = Duration.ofHours(1),
            cacheSize = 5000,
            cacheExpiration = Duration.ofHours(2)
        )
        
        fun testing(): PersistenceConfig = PersistenceConfig(
            database = DatabaseFactory.DatabaseConfig.H2InMemory("testdb"),
            enableCaching = false,
            enableFileBackup = false
        )
    }
}

/**
 * PED: PERSISTENCE MANAGER - FACADE PATTERN
 * Proporciona una interfaz unificada para todas las operaciones de persistencia
 */
class PersistenceManager(
    private val config: PersistenceConfig,
    private val stateManager: StateManager
) {
    
    private val logger = LoggerFactory.getLogger(PersistenceManager::class.java)
    
    /**
     * PED: COROUTINE SCOPE para operaciones asíncronas
     * SupervisorJob para aislar errores entre operaciones
     */
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * PED: LAZY INITIALIZATION de componentes costosos
     */
    private val database by lazy {
        DatabaseFactory.create(
            config = config.database,
            enableLogging = true
        ) {
            // PED: Schema initialization usando extension function
            initializeSchema(Users, Projects, Tasks)
        }
    }
    
    private val userRepository by lazy { UserRepository() }
    
    private val userCache by lazy {
        if (config.enableCaching) {
            CacheManager.getOrCreate<UUID, User>(
                name = "users",
                config = cacheConfig("users") {
                    maximumSize(config.cacheSize)
                    expireAfterAccess(config.cacheExpiration)
                    recordStats()
                }
            )
        } else null
    }
    
    private val stateFileStorage by lazy {
        if (config.enableFileBackup) {
            FileStorageFactory.createJsonStorage<AppState>(
                config = fileStorageConfig(config.fileStorageBaseDir) {
                    enableBackups()
                    maxBackups(10)
                    backupInterval(config.backupInterval)
                    atomicWrites()
                    prettyPrint()
                }
            )
        } else null
    }
    
    /**
     * PED: INITIALIZATION METHOD
     * Inicializa todos los componentes de persistencia
     */
    suspend fun initialize() {
        logger.info("🚀 Initializing Persistence Manager...")
        
        try {
            // PED: Initialize database connection
            database.let {
                logger.info("✅ Database initialized: ${config.database::class.simpleName}")
            }
            
            // PED: Initialize cache if enabled
            userCache?.let {
                logger.info("✅ User cache initialized with size: ${config.cacheSize}")
            }
            
            // PED: Initialize file storage if enabled
            stateFileStorage?.let {
                logger.info("✅ File storage initialized at: ${config.fileStorageBaseDir}")
            }
            
            // PED: Start background tasks
            startBackgroundTasks()
            
            logger.info("🎉 Persistence Manager initialized successfully")
            
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize Persistence Manager", e)
            throw e
        }
    }
    
    /**
     * PED: USER OPERATIONS con caching integrado
     */
    suspend fun getUser(userId: UUID): User? {
        return try {
            // PED: Try cache first
            userCache?.get(userId)?.let { cachedUser ->
                logger.debug("🎯 Cache HIT for user: $userId")
                return cachedUser
            }
            
            // PED: Fallback to database
            logger.debug("💾 Cache MISS, loading from database: $userId")
            val user = userRepository.findById(userId).getOrNull()
            
            // PED: Update cache if user found
            user?.let { userCache?.put(userId, it) }
            
            user
        } catch (e: Exception) {
            logger.error("❌ Failed to get user: $userId", e)
            null
        }
    }
    
    suspend fun saveUser(user: User): Boolean {
        return try {
            // PED: Save to database
            val result = userRepository.save(user)
            
            if (result.isSuccess) {
                // PED: Update cache
                userCache?.put(user.id.value, user)
                logger.debug("💾 User saved and cached: ${user.id.value}")
                true
            } else {
                logger.error("❌ Failed to save user to database: ${user.id.value}")
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to save user: ${user.id.value}", e)
            false
        }
    }
    
    suspend fun deleteUser(userId: UUID): Boolean {
        return try {
            val result = userRepository.delete(userId)
            
            if (result.isSuccess && result.getOrNull() == true) {
                // PED: Remove from cache
                userCache?.invalidate(userId)
                logger.debug("🗑️ User deleted and cache invalidated: $userId")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to delete user: $userId", e)
            false
        }
    }
    
    /**
     * PED: STATE PERSISTENCE OPERATIONS
     * Integración con StateManager para persistir estado de aplicación
     */
    suspend fun saveAppState(): Boolean {
        return try {
            val currentState = stateManager.currentState.value
            
            stateFileStorage?.save("app_state", currentState)?.let { result ->
                if (result.isSuccess) {
                    logger.debug("💾 App state saved to file storage")
                    true
                } else {
                    logger.error("❌ Failed to save app state: ${result}")
                    false
                }
            } ?: run {
                logger.warn("⚠️ File storage not enabled, skipping state save")
                false
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to save app state", e)
            false
        }
    }
    
    suspend fun loadAppState(): AppState? {
        return try {
            stateFileStorage?.load("app_state")?.getOrNull()?.also {
                logger.debug("📖 App state loaded from file storage")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to load app state", e)
            null
        }
    }
    
    suspend fun backupAppState(): String? {
        return try {
            stateFileStorage?.backup("app_state")?.getOrNull()?.also { backupId ->
                logger.info("💾 App state backup created: $backupId")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to backup app state", e)
            null
        }
    }
    
    /**
     * PED: CACHE OPERATIONS
     */
    suspend fun getCacheStatistics(): Map<String, Any> {
        return try {
            val stats = mutableMapOf<String, Any>()
            
            userCache?.stats()?.let { cacheStats ->
                stats["user_cache"] = mapOf(
                    "hit_rate" to cacheStats.hitRate,
                    "miss_rate" to cacheStats.missRate,
                    "request_count" to cacheStats.requestCount,
                    "eviction_count" to cacheStats.evictionCount,
                    "size" to userCache.size()
                )
            }
            
            stats
        } catch (e: Exception) {
            logger.error("❌ Failed to get cache statistics", e)
            emptyMap()
        }
    }
    
    suspend fun clearAllCaches() {
        try {
            userCache?.invalidateAll()
            logger.info("🧹 All caches cleared")
        } catch (e: Exception) {
            logger.error("❌ Failed to clear caches", e)
        }
    }
    
    /**
     * PED: BACKGROUND TASKS
     * Tareas en segundo plano para mantenimiento
     */
    private fun startBackgroundTasks() {
        if (config.enableFileBackup) {
            // PED: Periodic state backup
            persistenceScope.launch {
                kotlinx.coroutines.delay(config.backupInterval.toMillis())
                while (true) {
                    try {
                        backupAppState()
                        kotlinx.coroutines.delay(config.backupInterval.toMillis())
                    } catch (e: Exception) {
                        logger.error("❌ Background backup task failed", e)
                        kotlinx.coroutines.delay(Duration.ofMinutes(5).toMillis()) // Retry after 5 minutes
                    }
                }
            }
        }
        
        if (config.enableCaching) {
            // PED: Periodic cache statistics logging
            persistenceScope.launch {
                kotlinx.coroutines.delay(Duration.ofMinutes(10).toMillis())
                while (true) {
                    try {
                        val stats = getCacheStatistics()
                        logger.info("📊 Cache Statistics: $stats")
                        kotlinx.coroutines.delay(Duration.ofMinutes(10).toMillis())
                    } catch (e: Exception) {
                        logger.error("❌ Cache statistics task failed", e)
                        kotlinx.coroutines.delay(Duration.ofMinutes(5).toMillis())
                    }
                }
            }
        }
    }
    
    /**
     * PED: HEALTH CHECK
     */
    suspend fun healthCheck(): Map<String, Boolean> {
        val health = mutableMapOf<String, Boolean>()
        
        try {
            // PED: Database health check
            health["database"] = userRepository.count().isSuccess
        } catch (e: Exception) {
            health["database"] = false
            logger.error("❌ Database health check failed", e)
        }
        
        try {
            // PED: Cache health check
            health["cache"] = userCache?.let { 
                it.size() >= 0 // Simple check that cache is responsive
            } ?: true // If cache is disabled, consider it healthy
        } catch (e: Exception) {
            health["cache"] = false
            logger.error("❌ Cache health check failed", e)
        }
        
        try {
            // PED: File storage health check
            health["file_storage"] = stateFileStorage?.exists("health_check")?.isSuccess ?: true
        } catch (e: Exception) {
            health["file_storage"] = false
            logger.error("❌ File storage health check failed", e)
        }
        
        return health
    }
    
    /**
     * PED: CLEANUP METHOD
     * Proper resource cleanup
     */
    suspend fun shutdown() {
        logger.info("🔒 Shutting down Persistence Manager...")
        
        try {
            // PED: Save final state
            saveAppState()
            
            // PED: Clear caches
            clearAllCaches()
            
            // PED: Close database connection
            database.close()
            
            // PED: Cancel background tasks
            persistenceScope.coroutineContext.job.cancel()
            
            logger.info("✅ Persistence Manager shutdown complete")
            
        } catch (e: Exception) {
            logger.error("❌ Error during Persistence Manager shutdown", e)
        }
    }
}

/**
 * PED: PERSISTENCE MANAGER FACTORY
 * Factory pattern para crear PersistenceManager con diferentes configuraciones
 */
object PersistenceManagerFactory {
    
    private val logger = LoggerFactory.getLogger(PersistenceManagerFactory::class.java)
    
    fun create(
        config: PersistenceConfig,
        stateManager: StateManager
    ): PersistenceManager {
        logger.info("🏭 Creating Persistence Manager with config: ${config::class.simpleName}")
        return PersistenceManager(config, stateManager)
    }
    
    fun createForDevelopment(stateManager: StateManager): PersistenceManager =
        create(PersistenceConfig.development(), stateManager)
    
    fun createForProduction(stateManager: StateManager): PersistenceManager =
        create(PersistenceConfig.production(), stateManager)
    
    fun createForTesting(stateManager: StateManager): PersistenceManager =
        create(PersistenceConfig.testing(), stateManager)
}

/**
 * PED: EXTENSION FUNCTIONS para StateManager integration
 */
suspend fun StateManager.enablePersistence(
    persistenceManager: PersistenceManager
) {
    // PED: Load initial state from persistence
    persistenceManager.loadAppState()?.let { savedState ->
        // PED: Restore state (this would need to be implemented in StateManager)
        // dispatch(AppAction.System.RestoreState(savedState))
    }
    
    // PED: Subscribe to state changes for automatic persistence
    subscribe { newState ->
        kotlinx.coroutines.launch {
            persistenceManager.saveAppState()
        }
    }
}

/**
 * PED: DSL FUNCTION para configuración fluida
 */
fun persistenceConfig(configure: PersistenceConfigBuilder.() -> Unit): PersistenceConfig =
    PersistenceConfigBuilder().apply(configure).build()

/**
 * PED: BUILDER para PersistenceConfig
 */
class PersistenceConfigBuilder {
    private var database: DatabaseFactory.DatabaseConfig = DatabaseFactory.DatabaseConfig.H2InMemory()
    private var enableCaching: Boolean = true
    private var enableFileBackup: Boolean = true
    private var backupInterval: Duration = Duration.ofMinutes(30)
    private var cacheSize: Long = 1000
    private var cacheExpiration: Duration = Duration.ofHours(1)
    private var fileStorageBaseDir: String = "./data/persistence"
    
    fun database(config: DatabaseFactory.DatabaseConfig): PersistenceConfigBuilder = apply {
        database = config
    }
    
    fun h2InMemory(name: String = "testdb"): PersistenceConfigBuilder = apply {
        database = DatabaseFactory.DatabaseConfig.H2InMemory(name)
    }
    
    fun h2File(name: String = "appdb"): PersistenceConfigBuilder = apply {
        database = DatabaseFactory.DatabaseConfig.H2Embedded(name)
    }
    
    fun enableCaching(enable: Boolean = true): PersistenceConfigBuilder = apply {
        enableCaching = enable
    }
    
    fun cacheSize(size: Long): PersistenceConfigBuilder = apply {
        cacheSize = size
    }
    
    fun cacheExpiration(duration: Duration): PersistenceConfigBuilder = apply {
        cacheExpiration = duration
    }
    
    fun enableFileBackup(enable: Boolean = true): PersistenceConfigBuilder = apply {
        enableFileBackup = enable
    }
    
    fun backupInterval(interval: Duration): PersistenceConfigBuilder = apply {
        backupInterval = interval
    }
    
    fun fileStorageBaseDir(dir: String): PersistenceConfigBuilder = apply {
        fileStorageBaseDir = dir
    }
    
    fun build(): PersistenceConfig = PersistenceConfig(
        database = database,
        enableCaching = enableCaching,
        enableFileBackup = enableFileBackup,
        backupInterval = backupInterval,
        cacheSize = cacheSize,
        cacheExpiration = cacheExpiration,
        fileStorageBaseDir = fileStorageBaseDir
    )
}

