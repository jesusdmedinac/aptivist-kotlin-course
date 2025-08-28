
package com.aptivist.kotlin.persistence.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.stats.CacheStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.reflect.KClass

/**
 * PED: CACHE PROVIDER CON CAFFEINE Y KOTLIN AVANZADO
 * 
 * Este archivo demuestra conceptos avanzados de Kotlin y caching:
 * 1. **Generic Cache Abstraction**: Abstracción type-safe para diferentes tipos de cache
 * 2. **Caffeine Integration**: High-performance caching con eviction policies
 * 3. **Coroutine Support**: Operaciones asíncronas con suspend functions
 * 4. **Builder Pattern**: DSL fluido para configuración de cache
 * 5. **Sealed Classes**: Para diferentes estrategias de cache
 * 6. **Extension Functions**: Para operaciones domain-specific
 * 7. **Higher-Order Functions**: Para cache loaders y transformaciones
 * 8. **Inline Functions**: Para performance optimization
 * 9. **Reified Generics**: Para type-safe cache operations
 * 10. **Statistics & Monitoring**: Métricas y monitoreo de cache performance
 */

/**
 * PED: SEALED CLASS para estrategias de eviction
 * Define políticas de expulsión type-safe
 */
sealed class EvictionPolicy {
    data class MaximumSize(val size: Long) : EvictionPolicy()
    data class MaximumWeight(val weight: Long, val weigher: (Any, Any) -> Int) : EvictionPolicy()
    data class ExpireAfterWrite(val duration: Duration) : EvictionPolicy()
    data class ExpireAfterAccess(val duration: Duration) : EvictionPolicy()
    data class ExpireAfter(
        val create: Duration? = null,
        val update: Duration? = null,
        val access: Duration? = null
    ) : EvictionPolicy()
    
    object NoEviction : EvictionPolicy()
}

/**
 * PED: DATA CLASS para configuración de cache
 * Immutable configuration con default values
 */
data class CacheConfig(
    val name: String,
    val evictionPolicy: EvictionPolicy = EvictionPolicy.MaximumSize(1000),
    val recordStats: Boolean = true,
    val refreshAfterWrite: Duration? = null,
    val weakKeys: Boolean = false,
    val weakValues: Boolean = false,
    val softValues: Boolean = false,
    val executor: Executor? = null
) {
    companion object {
        fun builder(name: String): CacheConfigBuilder = CacheConfigBuilder(name)
    }
}

/**
 * PED: BUILDER PATTERN para configuración fluida
 * DSL-like configuration para cache setup
 */
class CacheConfigBuilder(private val name: String) {
    private var evictionPolicy: EvictionPolicy = EvictionPolicy.MaximumSize(1000)
    private var recordStats: Boolean = true
    private var refreshAfterWrite: Duration? = null
    private var weakKeys: Boolean = false
    private var weakValues: Boolean = false
    private var softValues: Boolean = false
    private var executor: Executor? = null
    
    fun maximumSize(size: Long): CacheConfigBuilder = apply {
        evictionPolicy = EvictionPolicy.MaximumSize(size)
    }
    
    fun maximumWeight(weight: Long, weigher: (Any, Any) -> Int): CacheConfigBuilder = apply {
        evictionPolicy = EvictionPolicy.MaximumWeight(weight, weigher)
    }
    
    fun expireAfterWrite(duration: Duration): CacheConfigBuilder = apply {
        evictionPolicy = EvictionPolicy.ExpireAfterWrite(duration)
    }
    
    fun expireAfterAccess(duration: Duration): CacheConfigBuilder = apply {
        evictionPolicy = EvictionPolicy.ExpireAfterAccess(duration)
    }
    
    fun recordStats(enable: Boolean = true): CacheConfigBuilder = apply {
        recordStats = enable
    }
    
    fun refreshAfterWrite(duration: Duration): CacheConfigBuilder = apply {
        refreshAfterWrite = duration
    }
    
    fun weakKeys(): CacheConfigBuilder = apply {
        weakKeys = true
    }
    
    fun weakValues(): CacheConfigBuilder = apply {
        weakValues = true
    }
    
    fun softValues(): CacheConfigBuilder = apply {
        softValues = true
    }
    
    fun executor(executor: Executor): CacheConfigBuilder = apply {
        this.executor = executor
    }
    
    fun build(): CacheConfig = CacheConfig(
        name = name,
        evictionPolicy = evictionPolicy,
        recordStats = recordStats,
        refreshAfterWrite = refreshAfterWrite,
        weakKeys = weakKeys,
        weakValues = weakValues,
        softValues = softValues,
        executor = executor
    )
}

/**
 * PED: GENERIC CACHE INTERFACE
 * Abstracción type-safe para operaciones de cache
 */
interface CacheProvider<K, V> {
    
    /**
     * PED: SUSPEND FUNCTIONS para operaciones asíncronas
     */
    suspend fun get(key: K): V?
    
    suspend fun get(key: K, loader: suspend (K) -> V): V
    
    suspend fun getAll(keys: Set<K>): Map<K, V>
    
    suspend fun put(key: K, value: V)
    
    suspend fun putAll(map: Map<K, V>)
    
    suspend fun invalidate(key: K)
    
    suspend fun invalidateAll(keys: Set<K>)
    
    suspend fun invalidateAll()
    
    suspend fun size(): Long
    
    suspend fun stats(): CacheStatistics
    
    /**
     * PED: HIGHER-ORDER FUNCTIONS para transformaciones
     */
    suspend fun <R> computeIfAbsent(key: K, mappingFunction: suspend (K) -> R): R where R : V
    
    suspend fun <R> computeIfPresent(key: K, remappingFunction: suspend (K, V) -> R?): R? where R : V
}

/**
 * PED: DATA CLASS para estadísticas de cache
 * Wrapper inmutable para CacheStats de Caffeine
 */
data class CacheStatistics(
    val requestCount: Long,
    val hitCount: Long,
    val hitRate: Double,
    val missCount: Long,
    val missRate: Double,
    val loadCount: Long,
    val loadExceptionCount: Long,
    val loadExceptionRate: Double,
    val totalLoadTime: Long,
    val averageLoadPenalty: Double,
    val evictionCount: Long,
    val evictionWeight: Long
) {
    companion object {
        fun from(stats: CacheStats): CacheStatistics = CacheStatistics(
            requestCount = stats.requestCount(),
            hitCount = stats.hitCount(),
            hitRate = stats.hitRate(),
            missCount = stats.missCount(),
            missRate = stats.missRate(),
            loadCount = stats.loadCount(),
            loadExceptionCount = stats.loadExceptionCount(),
            loadExceptionRate = stats.loadExceptionRate(),
            totalLoadTime = stats.totalLoadTime(),
            averageLoadPenalty = stats.averageLoadPenalty(),
            evictionCount = stats.evictionCount(),
            evictionWeight = stats.evictionWeight()
        )
    }
}

/**
 * PED: CAFFEINE CACHE IMPLEMENTATION
 * Implementación concreta usando Caffeine library
 */
class CaffeineCacheProvider<K, V>(
    private val config: CacheConfig,
    private val cache: Cache<K, V>
) : CacheProvider<K, V> {
    
    private val logger = LoggerFactory.getLogger("Cache.${config.name}")
    
    companion object {
        /**
         * PED: FACTORY METHOD con generic type inference
         */
        fun <K, V> create(config: CacheConfig): CaffeineCacheProvider<K, V> {
            val caffeineBuilder = Caffeine.newBuilder()
            
            // PED: WHEN EXPRESSION para configurar eviction policy
            when (val policy = config.evictionPolicy) {
                is EvictionPolicy.MaximumSize -> caffeineBuilder.maximumSize(policy.size)
                is EvictionPolicy.MaximumWeight -> caffeineBuilder.maximumWeight(policy.weight)
                    .weigher { _: K, _: V -> 1 } // Simplified weigher
                is EvictionPolicy.ExpireAfterWrite -> caffeineBuilder.expireAfterWrite(policy.duration)
                is EvictionPolicy.ExpireAfterAccess -> caffeineBuilder.expireAfterAccess(policy.duration)
                is EvictionPolicy.ExpireAfter -> {
                    // Custom expiry policy would need more complex implementation
                    policy.create?.let { caffeineBuilder.expireAfterWrite(it) }
                }
                EvictionPolicy.NoEviction -> { /* No eviction policy */ }
            }
            
            // PED: SCOPE FUNCTIONS para configuración condicional
            caffeineBuilder.apply {
                if (config.recordStats) recordStats()
                config.refreshAfterWrite?.let { refreshAfterWrite(it) }
                if (config.weakKeys) weakKeys()
                if (config.weakValues) weakValues()
                if (config.softValues) softValues()
                config.executor?.let { executor(it) }
            }
            
            val cache = caffeineBuilder.build<K, V>()
            return CaffeineCacheProvider(config, cache)
        }
        
        /**
         * PED: FACTORY METHOD con loading cache
         */
        fun <K, V> createLoading(
            config: CacheConfig,
            loader: (K) -> V
        ): LoadingCaffeineCacheProvider<K, V> {
            val caffeineBuilder = Caffeine.newBuilder()
            
            // Similar configuration as above...
            when (val policy = config.evictionPolicy) {
                is EvictionPolicy.MaximumSize -> caffeineBuilder.maximumSize(policy.size)
                is EvictionPolicy.ExpireAfterWrite -> caffeineBuilder.expireAfterWrite(policy.duration)
                is EvictionPolicy.ExpireAfterAccess -> caffeineBuilder.expireAfterAccess(policy.duration)
                else -> caffeineBuilder.maximumSize(1000) // Default
            }
            
            caffeineBuilder.apply {
                if (config.recordStats) recordStats()
                config.refreshAfterWrite?.let { refreshAfterWrite(it) }
            }
            
            val loadingCache = caffeineBuilder.build(loader)
            return LoadingCaffeineCacheProvider(config, loadingCache)
        }
    }
    
    /**
     * PED: SUSPEND FUNCTION implementations
     * Todas las operaciones son suspend para consistency
     */
    override suspend fun get(key: K): V? = withContext(Dispatchers.Default) {
        cache.getIfPresent(key).also { value ->
            logger.debug("Cache ${if (value != null) "HIT" else "MISS"} for key: $key")
        }
    }
    
    override suspend fun get(key: K, loader: suspend (K) -> V): V = withContext(Dispatchers.Default) {
        cache.get(key) { k ->
            // PED: CompletableFuture para integrar suspend functions con Caffeine
            CompletableFuture.supplyAsync {
                kotlinx.coroutines.runBlocking {
                    loader(k)
                }
            }.get()
        }.also {
            logger.debug("Cache LOAD for key: $key")
        }
    }
    
    override suspend fun getAll(keys: Set<K>): Map<K, V> = withContext(Dispatchers.Default) {
        cache.getAllPresent(keys).also { result ->
            logger.debug("Cache batch GET: ${result.size}/${keys.size} hits")
        }
    }
    
    override suspend fun put(key: K, value: V): Unit = withContext(Dispatchers.Default) {
        cache.put(key, value)
        logger.debug("Cache PUT for key: $key")
    }
    
    override suspend fun putAll(map: Map<K, V>): Unit = withContext(Dispatchers.Default) {
        cache.putAll(map)
        logger.debug("Cache batch PUT: ${map.size} entries")
    }
    
    override suspend fun invalidate(key: K): Unit = withContext(Dispatchers.Default) {
        cache.invalidate(key)
        logger.debug("Cache INVALIDATE for key: $key")
    }
    
    override suspend fun invalidateAll(keys: Set<K>): Unit = withContext(Dispatchers.Default) {
        cache.invalidateAll(keys)
        logger.debug("Cache batch INVALIDATE: ${keys.size} keys")
    }
    
    override suspend fun invalidateAll(): Unit = withContext(Dispatchers.Default) {
        cache.invalidateAll()
        logger.debug("Cache INVALIDATE ALL")
    }
    
    override suspend fun size(): Long = withContext(Dispatchers.Default) {
        cache.estimatedSize()
    }
    
    override suspend fun stats(): CacheStatistics = withContext(Dispatchers.Default) {
        CacheStatistics.from(cache.stats())
    }
    
    override suspend fun <R> computeIfAbsent(key: K, mappingFunction: suspend (K) -> R): R where R : V =
        withContext(Dispatchers.Default) {
            @Suppress("UNCHECKED_CAST")
            cache.get(key) { k ->
                kotlinx.coroutines.runBlocking {
                    mappingFunction(k)
                }
            } as R
        }
    
    override suspend fun <R> computeIfPresent(
        key: K,
        remappingFunction: suspend (K, V) -> R?
    ): R? where R : V = withContext(Dispatchers.Default) {
        val currentValue = cache.getIfPresent(key)
        if (currentValue != null) {
            val newValue = remappingFunction(key, currentValue)
            if (newValue != null) {
                @Suppress("UNCHECKED_CAST")
                cache.put(key, newValue as V)
                newValue
            } else {
                cache.invalidate(key)
                null
            }
        } else {
            null
        }
    }
}

/**
 * PED: LOADING CACHE IMPLEMENTATION
 * Especialización para caches con auto-loading
 */
class LoadingCaffeineCacheProvider<K, V>(
    private val config: CacheConfig,
    private val loadingCache: LoadingCache<K, V>
) : CacheProvider<K, V> {
    
    private val logger = LoggerFactory.getLogger("LoadingCache.${config.name}")
    
    override suspend fun get(key: K): V? = withContext(Dispatchers.Default) {
        loadingCache.getIfPresent(key)
    }
    
    override suspend fun get(key: K, loader: suspend (K) -> V): V = withContext(Dispatchers.Default) {
        loadingCache.get(key) // Uses the configured loader
    }
    
    override suspend fun getAll(keys: Set<K>): Map<K, V> = withContext(Dispatchers.Default) {
        loadingCache.getAll(keys)
    }
    
    // ... other implementations similar to CaffeineCacheProvider
    
    override suspend fun put(key: K, value: V): Unit = withContext(Dispatchers.Default) {
        loadingCache.put(key, value)
    }
    
    override suspend fun putAll(map: Map<K, V>): Unit = withContext(Dispatchers.Default) {
        loadingCache.putAll(map)
    }
    
    override suspend fun invalidate(key: K): Unit = withContext(Dispatchers.Default) {
        loadingCache.invalidate(key)
    }
    
    override suspend fun invalidateAll(keys: Set<K>): Unit = withContext(Dispatchers.Default) {
        loadingCache.invalidateAll(keys)
    }
    
    override suspend fun invalidateAll(): Unit = withContext(Dispatchers.Default) {
        loadingCache.invalidateAll()
    }
    
    override suspend fun size(): Long = withContext(Dispatchers.Default) {
        loadingCache.estimatedSize()
    }
    
    override suspend fun stats(): CacheStatistics = withContext(Dispatchers.Default) {
        CacheStatistics.from(loadingCache.stats())
    }
    
    override suspend fun <R> computeIfAbsent(key: K, mappingFunction: suspend (K) -> R): R where R : V =
        withContext(Dispatchers.Default) {
            @Suppress("UNCHECKED_CAST")
            loadingCache.get(key) as R
        }
    
    override suspend fun <R> computeIfPresent(
        key: K,
        remappingFunction: suspend (K, V) -> R?
    ): R? where R : V = withContext(Dispatchers.Default) {
        val currentValue = loadingCache.getIfPresent(key)
        if (currentValue != null) {
            val newValue = remappingFunction(key, currentValue)
            if (newValue != null) {
                @Suppress("UNCHECKED_CAST")
                loadingCache.put(key, newValue as V)
                newValue
            } else {
                loadingCache.invalidate(key)
                null
            }
        } else {
            null
        }
    }
}

/**
 * PED: CACHE MANAGER SINGLETON
 * Gestiona múltiples caches con type safety
 */
object CacheManager {
    
    private val logger = LoggerFactory.getLogger(CacheManager::class.java)
    private val caches = mutableMapOf<String, CacheProvider<*, *>>()
    
    /**
     * PED: REIFIED GENERIC FUNCTION para type-safe cache creation
     */
    inline fun <reified K, reified V> getOrCreate(
        name: String,
        config: CacheConfig = CacheConfig.builder(name).build()
    ): CacheProvider<K, V> {
        @Suppress("UNCHECKED_CAST")
        return caches.getOrPut(name) {
            logger.info("🔧 Creating cache: $name")
            CaffeineCacheProvider.create<K, V>(config)
        } as CacheProvider<K, V>
    }
    
    /**
     * PED: REIFIED GENERIC FUNCTION para loading cache
     */
    inline fun <reified K, reified V> getOrCreateLoading(
        name: String,
        noinline loader: (K) -> V,
        config: CacheConfig = CacheConfig.builder(name).build()
    ): CacheProvider<K, V> {
        @Suppress("UNCHECKED_CAST")
        return caches.getOrPut(name) {
            logger.info("🔧 Creating loading cache: $name")
            CaffeineCacheProvider.createLoading(config, loader)
        } as CacheProvider<K, V>
    }
    
    /**
     * PED: CACHE STATISTICS aggregation
     */
    suspend fun getAllStats(): Map<String, CacheStatistics> {
        return caches.mapValues { (_, cache) ->
            cache.stats()
        }
    }
    
    /**
     * PED: CACHE CLEANUP
     */
    suspend fun invalidateAll() {
        caches.values.forEach { it.invalidateAll() }
        logger.info("🧹 All caches invalidated")
    }
    
    fun removeCache(name: String) {
        caches.remove(name)?.let {
            logger.info("🗑️ Cache removed: $name")
        }
    }
}

/**
 * PED: DSL FUNCTIONS para configuración fluida
 */
fun cacheConfig(name: String, configure: CacheConfigBuilder.() -> Unit): CacheConfig =
    CacheConfig.builder(name).apply(configure).build()

/**
 * PED: EXTENSION FUNCTIONS para operaciones comunes
 */
suspend inline fun <K, V> CacheProvider<K, V>.getOrPut(
    key: K,
    crossinline defaultValue: suspend () -> V
): V {
    return get(key) ?: run {
        val value = defaultValue()
        put(key, value)
        value
    }
}

suspend inline fun <K, V> CacheProvider<K, V>.computeIfAbsentSuspend(
    key: K,
    crossinline mappingFunction: suspend (K) -> V
): V {
    return get(key) ?: run {
        val value = mappingFunction(key)
        put(key, value)
        value
    }
}

/**
 * PED: TYPE ALIASES para common cache types
 */
typealias StringCache<V> = CacheProvider<String, V>
typealias LongCache<V> = CacheProvider<Long, V>
typealias UUIDCache<V> = CacheProvider<java.util.UUID, V>

