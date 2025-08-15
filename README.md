
# Curso a Profundidad de Kotlin

## ✅ Progreso del Curso

**Fase Actual:** Phase 2.1 - HTTP Server with Ktor 🚧 EN PROGRESO

Marca cada tema conforme lo vayas completando:

### Fundamentos
- **Programación Orientada a Objetos (OOP)**
  - [x] Variables y Constantes: `var`, `val`, `const` ✅ Phase 1.1
  - [x] Mutabilidad vs. Inmutabilidad ✅ Phase 1.1
  - [ ] Inicialización: `lazy` vs. `lateinit`
- **Tipos de Datos**
  - [ ] Primitivos: Numéricos, Texto, Booleanos
  - [ ] Tipos Especiales: `Any`, `Unit`, `Nothing`
- **Funciones, Clases y Objetos**
  - [x] Argumentos Nombrados y No Nombrados ✅ Phase 1.3 (constructor parameters, named args)
  - [ ] Parámetros `vararg`
  - [x] Tipos de Retorno ✅ Phase 1.3 (nullable returns, Result<T>, suspend functions)
  - [x] Chequeo de Tipos y `Smart Casts` ✅ Phase 1.3 (when expressions con sealed classes)

### Conceptos Avanzados
- **Clases y Funciones Avanzadas**
  - [x] Tipos de Clases: `open`, `sealed`, `data`, `enum` ✅ Phase 1.2 (sealed classes con pattern matching)
  - [x] `Extension Functions` & `Infix Functions` ✅ Phase 1.2 (buildSrc utilities y DSL)
  - [x] `Higher-Order Functions` (HOFs) ✅ Phase 1.2 (lambdas en Gradle tasks y configuración)
  - [x] `Scope Functions`: `let`, `run`, `with`, `also`, `apply` ✅ Phase 1.2 (apply, let, also, with, takeIf)
- **Seguridad contra Nulos (`Null Safety`)**
  - [x] Operadores: `?`, `!!`, `?:` ✅ Phase 1.1 (Operador Elvis para defaults)
- **Herencia y Modificadores de Acceso**
  - [ ] Herencia y Modificadores de Acceso

### Colecciones y Flujo de Control
- **Colecciones**
  - [ ] `Array`, `List`, `Set`, `Map`
  - [ ] Transformaciones (ej. `map`, `filter`, `groupBy`)
  - [ ] Rangos y Progresiones
- **Flujo de Control**
  - [x] Condicionales: `if`, `when` ✅ Phase 1.3 (when expressions exhaustivo con sealed classes)
  - [ ] Bucles: `for`, `while`

### Programación Asíncrona
- **Coroutines**
  - [x] Constructores (`Builders`): `launch`, `async`, `runBlocking` ✅ Phase 1.3 (server lifecycle y examples)
  - [x] `Coroutine Scopes` ✅ Phase 1.3 (CoroutineScope para server management)
  - [x] `suspend` Functions ✅ Phase 1.3 (async message handling y I/O operations)
- **Flow**
  - [x] `Hot Streams` vs. `Cold Streams` ✅ Phase 1.3 (Channel vs Flow comparison)
  - [ ] `Flow` vs. `SharedFlow` vs. `StateFlow`

## Temas

### Fundamentos
- **Programación Orientada a Objetos (OOP)**
  - Variables y Constantes: `var`, `val`, `const`
  - Mutabilidad vs. Inmutabilidad
  - Inicialización: `lazy` vs. `lateinit`
- **Tipos de Datos**
  - Primitivos: Numéricos, Texto, Booleanos
  - Tipos Especiales: `Any`, `Unit`, `Nothing`
- **Funciones, Clases y Objetos**
  - Argumentos Nombrados y No Nombrados
  - Parámetros `vararg`
  - Tipos de Retorno
  - Chequeo de Tipos y `Smart Casts`

### Conceptos Avanzados
- **Clases y Funciones Avanzadas**
  - Tipos de Clases: `open`, `sealed`, `data`, `enum`
  - `Extension Functions` & `Infix Functions`
  - `Higher-Order Functions` (HOFs)
  - `Scope Functions`: `let`, `run`, `with`, `also`, `apply`
- **Seguridad contra Nulos (`Null Safety`)**
  - Operadores: `?`, `!!`, `?:`
- **Herencia y Modificadores de Acceso**

### Colecciones y Flujo de Control
- **Colecciones**
  - `Array`, `List`, `Set`, `Map`
  - Transformaciones (ej. `map`, `filter`, `groupBy`)
  - Rangos y Progresiones
- **Flujo de Control**
  - Condicionales: `if`, `when`
  - Bucles: `for`, `while`

### Programación Asíncrona
- **Coroutines**
  - Constructores (`Builders`): `launch`, `async`, `runBlocking`
  - `Coroutine Scopes`
  - `suspend` Functions
- **Flow**
  - `Hot Streams` vs. `Cold Streams`
  - `Flow` vs. `SharedFlow` vs. `StateFlow`

## Proyecto del Curso

A lo largo de este curso, construiremos progresivamente un proyecto práctico para aplicar los conceptos aprendidos en cada lección. El objetivo es crear un **Agente MCP**, un servidor local que actúa como un puente seguro entre los sistemas de IA y el entorno local de un usuario.

Para una descripción detallada del proyecto, sus objetivos y arquitectura, por favor consulta el archivo [PROJECT.md](PROJECT.md).

## 🎯 Logros por Fase

### ✅ Phase 1.1: Logging y Configuración Básica (COMPLETADA)

En esta fase implementamos la infraestructura básica de logging y configuración, introduciendo conceptos fundamentales de Kotlin:

**🔧 Nuevos Conceptos Implementados:**
- **Companion Objects**: Implementamos el patrón de logger estático usando `companion object`
- **File I/O**: Lectura de archivos de recursos usando `getResourceAsStream()`
- **Use Function**: Equivalente de try-with-resources con la función `use` de Kotlin
- **Properties**: Carga y manejo de archivos de configuración `.properties`
- **Null Safety**: Aplicación del operador Elvis (`?:`) para valores por defecto
- **Object Singleton**: Conversión de la aplicación a `object App` para demostrar singletons

**📂 Archivos Creados/Modificados:**
- `src/main/resources/logback.xml` - Configuración de logging estructurado
- `src/main/resources/application.properties` - Propiedades de configuración
- `src/main/kotlin/com/aptivist/kotlin/Config.kt` - Clase utilitaria para configuración
- `src/main/kotlin/com/aptivist/kotlin/App.kt` - Integración de logging y configuración
- `build.gradle.kts` - Dependencias SLF4J + Logback

**🎓 Lecciones Aprendidas:**
- Separación de configuración del código fuente
- Logging estructurado vs. `println()` simple
- Patterns de inicialización estática en Kotlin
- Manejo seguro de recursos y archivos
- Aplicación práctica de null safety en configuraciones

### ✅ Phase 1.2: Gradle Build System Setup (COMPLETADA)

En esta fase implementamos un sistema de build avanzado con Gradle, demostrando características avanzadas de Kotlin a través de DSL, extension functions, y programación funcional:

**🔧 Nuevos Conceptos Implementados:**
- **Extension Functions**: Creamos funciones que extienden Project, TaskContainer y otros tipos de Gradle
- **Higher-Order Functions**: Implementamos lambdas y funciones que reciben otras funciones como parámetros
- **DSL (Domain Specific Language)**: Construimos DSLs personalizados para configuración de tasks y dependencias
- **Scope Functions**: Aplicación práctica de `apply`, `let`, `also`, `with`, `takeIf` en configuración de build
- **Sealed Classes**: Uso de sealed classes con pattern matching usando `when` expressions
- **Object Singletons**: BuildConfig como singleton thread-safe con propiedades computadas
- **Infix Functions**: Creación de funciones infix para sintaxis más fluida en DSL
- **Operator Overloading**: Implementación del operador `*` personalizado para String
- **Reified Generics**: Uso de generics reificados en extension functions

**📂 Archivos Creados/Modificados:**
- `buildSrc/build.gradle.kts` - Módulo buildSrc con Kotlin DSL
- `buildSrc/src/main/kotlin/BuildConfig.kt` - Object singleton con configuración centralizada
- `buildSrc/src/main/kotlin/ProjectExtensions.kt` - Extension functions para Project y TaskContainer
- `buildSrc/src/main/kotlin/TaskDsl.kt` - DSL personalizado para tasks y configuración avanzada
- `build.gradle.kts` - Build script principal con tasks personalizados y configuración avanzada
- `gradle/wrapper/gradle-wrapper.properties` - Actualización a Gradle 8.7

**🎓 Lecciones Aprendidas:**
- Creación de DSL type-safe usando Kotlin para configuración declarativa
- Extension functions como mecanismo de extensibilidad sin herencia
- Higher-Order Functions para configuración flexible y reutilizable
- Scope functions para transformación y configuración de objetos
- Pattern matching exhaustivo con sealed classes
- Separación de lógica de build en módulos reutilizables (buildSrc)
- Operator overloading para crear APIs más expresivas
- Lazy evaluation y computed properties para configuración eficiente

### ✅ Phase 1.3: Basic MCP Server Structure (COMPLETADA)

En esta fase implementamos la estructura completa del servidor MCP con protocolos de comunicación JSON-RPC, introduciendo conceptos avanzados de arquitectura de software y programación asíncrona:

**🔧 Nuevos Conceptos Implementados:**
- **Interfaces**: Definición de contratos con McpServer, McpConnection, McpMessageHandler para abstracción y testabilidad
- **Data Classes**: Modelado de mensajes JSON-RPC y protocolo MCP con serialización automática
- **Sealed Classes**: Jerarquías type-safe para JsonRpcMessage y McpMessage con pattern matching exhaustivo
- **Abstract Classes**: BaseMcpServer implementando Template Method Pattern para código reutilizable
- **Coroutines**: Programación asíncrona con launch, runBlocking, suspend functions y CoroutineScope
- **Flow & Channels**: Streams reactivos para message processing y comunicación between coroutines
- **JSON Serialization**: kotlinx.serialization para conversión automática entre Kotlin objects y JSON
- **Extension Functions**: API fluida con toJson(), fromJson() y domain-specific extensions
- **When Expressions**: Pattern matching exhaustivo con smart casts para type-safe handling
- **Builder Pattern**: McpMessageHandlerBuilder con fluent API y method chaining
- **Result<T>**: Functional error handling sin exceptions para operaciones que pueden fallar
- **Mock Implementations**: Patterns para testing y development con placeholder functionality

**📂 Archivos Creados/Modificados:**
- `mcp/protocol/JsonRpcMessage.kt` - Sealed classes y data classes para JSON-RPC básico
- `mcp/protocol/McpProtocol.kt` - Messages y capabilities específicos del protocolo MCP
- `mcp/server/McpServer.kt` - Interfaces principales y abstract server implementation
- `mcp/json/JsonSerializer.kt` - Utilities para serialización JSON con error handling
- `mcp/server/McpServerImpl.kt` - Implementación concreta con coroutines y channels
- `mcp/handler/DefaultMcpMessageHandler.kt` - Message handler con builder pattern
- `mcp/examples/McpServerExample.kt` - Aplicación completa demostrando usage patterns

**🎓 Lecciones Aprendidas:**
- Diseño de APIs type-safe usando sealed classes y interfaces
- Arquitectura basada en composition over inheritance para flexibilidad
- Programación asíncrona reactiva con coroutines, Flow y channels
- JSON serialization automática con annotations y type safety
- Error handling funcional con Result<T> y extension functions
- Builder pattern para object construction con APIs fluidas
- Template Method Pattern para code reuse en abstract classes
- Mock implementations y dependency injection para testabilidad
- Integration de logging structured en arquitecturas asíncronas
