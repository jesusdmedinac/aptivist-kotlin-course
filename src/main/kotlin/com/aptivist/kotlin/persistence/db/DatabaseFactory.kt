
package com.aptivist.kotlin.persistence.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * PED: DATABASE FACTORY - PATRÓN FACTORY CON CONFIGURACIÓN AVANZADA
 * 
 * Esta clase demuestra varios conceptos avanzados de Kotlin:
 * 1. **Object Singleton**: DatabaseFactory como singleton thread-safe
 * 2. **Factory Pattern**: Métodos factory para diferentes configuraciones de DB
 * 3. **Builder Pattern**: HikariConfig con DSL-like configuration
 * 4. **Resource Management**: Proper lifecycle management de connections
 * 5. **Extension Functions**: Para configuración fluida de DataSource
 * 6. **Sealed Classes**: Para diferentes tipos de configuración de DB
 * 7. **Higher-Order Functions**: Para configuración con lambdas
 * 8. **Companion Object**: Para constantes y configuración estática
 * 9. **Lazy Initialization**: Para inicialización diferida de recursos costosos
 * 10. **Type-Safe Configuration**: DSL para configuración de base de datos
 */
object DatabaseFactory {
    
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    /**
     * PED: SEALED CLASS PARA CONFIGURACIÓN TYPE-SAFE
     * 
     * Sealed classes permiten definir un conjunto cerrado de configuraciones
     * posibles, garantizando exhaustividad en when expressions y type safety.
     */
    sealed class DatabaseConfig {
        /**
         * PED: DATA CLASS para configuración inmutable
         * Data classes proporcionan equals, hashCode, toString y copy automáticamente
         */
        data class H2Embedded(
            val databaseName: String = "testdb",
            val mode: H2Mode = H2Mode.FILE,
            val options: Map<String, String> = emptyMap()
        ) : DatabaseConfig()
        
        data class H2InMemory(
            val databaseName: String = "testdb",
            val options: Map<String, String> = mapOf(
                "DB_CLOSE_DELAY" to "-1", // Mantener DB abierta mientras JVM esté activa
                "MODE" to "PostgreSQL"    // Compatibilidad con PostgreSQL
            )
        ) : DatabaseConfig()
        
        data class PostgreSQL(
            val host: String = "localhost",
            val port: Int = 5432,
            val database: String,
            val username: String,
            val password: String,
            val ssl: Boolean = false
        ) : DatabaseConfig()
        
        data class MySQL(
            val host: String = "localhost",
            val port: Int = 3306,
            val database: String,
            val username: String,
            val password: String,
            val ssl: Boolean = false
        ) : DatabaseConfig()
    }
    
    /**
     * PED: ENUM CLASS para modos de H2
     * Enum classes en Kotlin son type-safe y pueden tener propiedades y métodos
     */
    enum class H2Mode(val urlPrefix: String) {
        FILE("jdbc:h2:file:"),
        MEMORY("jdbc:h2:mem:");
        
        /**
         * PED: EXTENSION FUNCTION en enum
         * Demuestra cómo extender funcionalidad de enums
         */
        fun buildUrl(databaseName: String, options: Map<String, String> = emptyMap()): String {
            val optionsString = if (options.isNotEmpty()) {
                ";" + options.entries.joinToString(";") { "${it.key}=${it.value}" }
            } else ""
            
            return "$urlPrefix$databaseName$optionsString"
        }
    }
    
    /**
     * PED: DATA CLASS para configuración de connection pool
     * Demuestra uso de default parameters y named arguments
     */
    data class PoolConfig(
        val maximumPoolSize: Int = 10,
        val minimumIdle: Int = 2,
        val connectionTimeout: Long = 30_000, // 30 segundos
        val idleTimeout: Long = 600_000,      // 10 minutos
        val maxLifetime: Long = 1_800_000,    // 30 minutos
        val leakDetectionThreshold: Long = 60_000 // 1 minuto
    )
    
    /**
     * PED: LAZY PROPERTY para configuración por defecto
     * lazy {} garantiza inicialización thread-safe y única
     */
    private val defaultPoolConfig by lazy {
        PoolConfig()
    }
    
    /**
     * PED: FACTORY METHOD principal con HIGHER-ORDER FUNCTION
     * 
     * @param config Configuración de base de datos usando sealed class
     * @param poolConfig Configuración del pool de conexiones
     * @param enableLogging Si habilitar logging de SQL queries
     * @param schemaSetup Lambda para configuración de schema (Higher-Order Function)
     * @return Database instance configurada
     */
    fun create(
        config: DatabaseConfig,
        poolConfig: PoolConfig = defaultPoolConfig,
        enableLogging: Boolean = false,
        schemaSetup: (Database.() -> Unit)? = null
    ): Database {
        logger.info("🔧 Creando conexión de base de datos: ${config::class.simpleName}")
        
        // PED: WHEN EXPRESSION exhaustiva con sealed classes
        // Smart casts automáticos para cada branch
        val dataSource = when (config) {
            is DatabaseConfig.H2Embedded -> createH2DataSource(
                url = H2Mode.FILE.buildUrl("./data/${config.databaseName}", config.options),
                poolConfig = poolConfig
            )
            
            is DatabaseConfig.H2InMemory -> createH2DataSource(
                url = H2Mode.MEMORY.buildUrl(config.databaseName, config.options),
                poolConfig = poolConfig
            )
            
            is DatabaseConfig.PostgreSQL -> createPostgreSQLDataSource(config, poolConfig)
            
            is DatabaseConfig.MySQL -> createMySQLDataSource(config, poolConfig)
        }
        
        // PED: SCOPE FUNCTION 'also' para side effects
        // Permite configurar el objeto manteniendo el contexto
        val database = Database.connect(dataSource).also { db ->
            if (enableLogging) {
                // PED: TRANSACTION BLOCK con lambda
                transaction(db) {
                    addLogger(StdOutSqlLogger)
                }
            }
            
            // PED: SAFE CALL operator con lambda execution
            schemaSetup?.invoke(db)
            
            logger.info("✅ Base de datos configurada exitosamente")
        }
        
        return database
    }
    
    /**
     * PED: EXTENSION FUNCTION para HikariConfig
     * Permite configuración fluida usando DSL-like syntax
     */
    private fun HikariConfig.applyPoolConfig(poolConfig: PoolConfig): HikariConfig = apply {
        maximumPoolSize = poolConfig.maximumPoolSize
        minimumIdle = poolConfig.minimumIdle
        connectionTimeout = poolConfig.connectionTimeout
        idleTimeout = poolConfig.idleTimeout
        maxLifetime = poolConfig.maxLifetime
        leakDetectionThreshold = poolConfig.leakDetectionThreshold
        
        // PED: Configuración adicional para production
        isAutoCommit = false // Mejor control de transacciones
        transactionIsolation = "TRANSACTION_READ_COMMITTED"
        
        // PED: Health check query para validar conexiones
        connectionTestQuery = "SELECT 1"
        validationTimeout = 5000
    }
    
    /**
     * PED: PRIVATE FACTORY METHODS para diferentes tipos de DataSource
     * Demuestra encapsulación y separation of concerns
     */
    private fun createH2DataSource(url: String, poolConfig: PoolConfig): DataSource {
        logger.debug("🔧 Configurando H2 DataSource: $url")
        
        return HikariConfig().apply {
            jdbcUrl = url
            driverClassName = "org.h2.Driver"
            username = "sa"
            password = ""
            
            // PED: Extension function call
            applyPoolConfig(poolConfig)
            
        }.let { config ->
            // PED: SCOPE FUNCTION 'let' para transformación
            HikariDataSource(config)
        }
    }
    
    private fun createPostgreSQLDataSource(
        config: DatabaseConfig.PostgreSQL,
        poolConfig: PoolConfig
    ): DataSource {
        logger.debug("🔧 Configurando PostgreSQL DataSource: ${config.host}:${config.port}/${config.database}")
        
        return HikariConfig().apply {
            jdbcUrl = buildString {
                append("jdbc:postgresql://${config.host}:${config.port}/${config.database}")
                if (config.ssl) append("?ssl=true&sslmode=require")
            }
            driverClassName = "org.postgresql.Driver"
            username = config.username
            password = config.password
            
            applyPoolConfig(poolConfig)
            
        }.let(::HikariDataSource)
    }
    
    private fun createMySQLDataSource(
        config: DatabaseConfig.MySQL,
        poolConfig: PoolConfig
    ): DataSource {
        logger.debug("🔧 Configurando MySQL DataSource: ${config.host}:${config.port}/${config.database}")
        
        return HikariConfig().apply {
            jdbcUrl = buildString {
                append("jdbc:mysql://${config.host}:${config.port}/${config.database}")
                append("?useSSL=${config.ssl}&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8")
            }
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = config.username
            password = config.password
            
            applyPoolConfig(poolConfig)
            
        }.let(::HikariDataSource)
    }
    
    /**
     * PED: CONVENIENCE METHODS con default parameters
     * Demuestra named arguments y default values
     */
    fun createH2InMemory(
        databaseName: String = "testdb",
        enableLogging: Boolean = true,
        schemaSetup: (Database.() -> Unit)? = null
    ): Database = create(
        config = DatabaseConfig.H2InMemory(databaseName),
        enableLogging = enableLogging,
        schemaSetup = schemaSetup
    )
    
    fun createH2File(
        databaseName: String = "appdb",
        enableLogging: Boolean = false,
        schemaSetup: (Database.() -> Unit)? = null
    ): Database = create(
        config = DatabaseConfig.H2Embedded(databaseName),
        enableLogging = enableLogging,
        schemaSetup = schemaSetup
    )
    
    /**
     * PED: UTILITY FUNCTION para inicializar schema
     * Demuestra uso de vararg y reified generics
     */
    inline fun <reified T> Database.initializeSchema(vararg tables: org.jetbrains.exposed.sql.Table) {
        transaction(this) {
            // PED: Spread operator (*) para vararg
            SchemaUtils.create(*tables)
            
            logger.info("📋 Schema inicializado para ${tables.size} tablas: ${tables.joinToString { it.tableName }}")
        }
    }
    
    /**
     * PED: EXTENSION FUNCTION para Database cleanup
     * Demuestra resource management y proper cleanup
     */
    fun Database.close() {
        try {
            // PED: Safe cast con 'as?' operator
            (connector.invoke() as? HikariDataSource)?.close()
            logger.info("🔒 Conexión de base de datos cerrada correctamente")
        } catch (e: Exception) {
            logger.error("❌ Error cerrando conexión de base de datos", e)
        }
    }
}

/**
 * PED: DSL BUILDER para configuración fluida
 * Demuestra cómo crear DSLs type-safe en Kotlin
 */
class DatabaseConfigBuilder {
    
    /**
     * PED: DSL FUNCTION con lambda receiver
     * @DslMarker previene uso incorrecto del DSL
     */
    @DslMarker
    annotation class DatabaseDsl
    
    @DatabaseDsl
    fun h2InMemory(
        name: String = "testdb",
        configure: DatabaseFactory.DatabaseConfig.H2InMemory.() -> DatabaseFactory.DatabaseConfig.H2InMemory = { this }
    ): DatabaseFactory.DatabaseConfig.H2InMemory {
        return DatabaseFactory.DatabaseConfig.H2InMemory(name).configure()
    }
    
    @DatabaseDsl
    fun h2File(
        name: String = "appdb",
        configure: DatabaseFactory.DatabaseConfig.H2Embedded.() -> DatabaseFactory.DatabaseConfig.H2Embedded = { this }
    ): DatabaseFactory.DatabaseConfig.H2Embedded {
        return DatabaseFactory.DatabaseConfig.H2Embedded(name).configure()
    }
    
    @DatabaseDsl
    fun postgresql(
        database: String,
        configure: DatabaseFactory.DatabaseConfig.PostgreSQL.() -> DatabaseFactory.DatabaseConfig.PostgreSQL = { this }
    ): DatabaseFactory.DatabaseConfig.PostgreSQL {
        return DatabaseFactory.DatabaseConfig.PostgreSQL(database = database).configure()
    }
}

/**
 * PED: TOP-LEVEL FUNCTION para DSL entry point
 * Demuestra cómo crear APIs fluidas y expresivas
 */
fun databaseConfig(configure: DatabaseConfigBuilder.() -> DatabaseFactory.DatabaseConfig): DatabaseFactory.DatabaseConfig {
    return DatabaseConfigBuilder().configure()
}

