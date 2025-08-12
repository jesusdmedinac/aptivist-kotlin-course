
# Curso a Profundidad de Kotlin

## ✅ Progreso del Curso

**Fase Actual:** Phase 1.2 - Gradle Build System Setup ✅ COMPLETADA

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
  - [ ] Argumentos Nombrados y No Nombrados
  - [ ] Parámetros `vararg`
  - [ ] Tipos de Retorno
  - [ ] Chequeo de Tipos y `Smart Casts`

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
  - [ ] Condicionales: `if`, `when`
  - [ ] Bucles: `for`, `while`

### Programación Asíncrona
- **Coroutines**
  - [ ] Constructores (`Builders`): `launch`, `async`, `runBlocking`
  - [ ] `Coroutine Scopes`
  - [ ] `suspend` Functions
- **Flow**
  - [ ] `Hot Streams` vs. `Cold Streams`
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
