# Curso a Profundidad de Kotlin

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
