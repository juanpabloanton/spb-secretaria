<!-- REPORTE DE SINCRONIZACIÓN
Cambio de versión: 1.0.0 → 1.1.0 (MINOR: traducción completa al español + refinamiento de contenido)
Principios modificados: Todos los títulos y cuerpos traducidos al español
Secciones añadidas: N/A
Secciones eliminadas: N/A
Plantillas revisadas y actualizadas:
  - .specify/memory/constitution.md         ✅ actualizado
  - .specify/templates/plan-template.md     ✅ actualizado (verificaciones de constitución en español)
  - .specify/templates/spec-template.md     ✅ actualizado (guía BDD en español)
  - .specify/templates/tasks-template.md    ✅ actualizado (tareas en español)
TODOs pendientes: Ninguno — todas las secciones completamente definidas.
-->

# Constitución SPB Secretaría

## Principios Fundamentales

### I. Arquitectura Limpia (Robert C. Martin)

El proyecto DEBE seguir la Arquitectura Limpia definida por Robert C. Martin. La regla de
dependencia es absoluta: las dependencias del código fuente SIEMPRE deben apuntar hacia adentro,
desde las capas externas hacia el núcleo del dominio. Las cuatro capas obligatorias son:

- **Dominio (Entidades)**: Entidades de negocio y reglas empresariales. Cero dependencias externas.
- **Aplicación (Casos de Uso)**: Reglas de negocio específicas de la aplicación. Depende únicamente del Dominio.
- **Adaptadores de Interfaz**: Controladores, Presentadores, Gateways. Depende de Aplicación y Dominio.
- **Frameworks y Drivers**: Spring Boot, JPA, bases de datos, web. Depende únicamente de los Adaptadores.

**Reglas no negociables:**
- Las capas de Dominio y Aplicación NO DEBEN importar Spring, JPA, Hibernate ni ninguna clase de framework.
- Los Casos de Uso DEBEN invocarse a través de interfaces de puerto de entrada y DEBEN retornar
  resultados a través de interfaces de puerto de salida (no acoplamiento directo controlador-servicio).
- Los Repositorios DEBEN definirse como interfaces en la capa de Aplicación e implementarse
  en la capa de Frameworks y Drivers.
- La comunicación entre capas DEBE utilizar DTOs o interfaces de Puerto; está prohibido
  exponer entidades del dominio hacia las capas externas.
- La estructura de paquetes DEBE reflejar los límites de capa
  (ej.: `dominio`, `aplicacion`, `adaptadores`, `infraestructura`).

### II. Estrategia de Pruebas con BDD (Unitarias · Integración · Funcionales)

Todos los escenarios de prueba DEBEN expresarse usando notación BDD estilo Gherkin:
**Dado** (precondición / estado inicial) / **Cuando** (acción o evento) / **Entonces** (resultado esperado).

Las tres categorías de pruebas son obligatorias para cada funcionalidad:

- **Pruebas Unitarias**: Cubren cada Caso de Uso y Entidad de Dominio de forma aislada.
  Todos los colaboradores son simulados (Mockito). Se escriben ANTES de la implementación (TDD).
  Cada prueba DEBE corresponder a un escenario BDD del `spec.md`.
- **Pruebas de Integración**: Verifican la comunicación entre capas (ej.: Controlador → Caso de Uso →
  Repositorio). Usan contexto real de Spring (`@SpringBootTest`) o slices enfocados
  (`@WebMvcTest`, `@DataJpaTest`). Las interacciones con base de datos DEBEN usar una base
  de datos en memoria o contenedorizada (Testcontainers).
- **Pruebas Funcionales**: Escenarios de API de extremo a extremo dirigidos por el contrato OpenAPI.
  Validan ciclos completos de petición HTTP → respuesta contra la aplicación en ejecución
  (RestAssured o Karate). Cada escenario DEBE corresponder a un escenario de aceptación del `spec.md`.

**Reglas no negociables:**
- Las pruebas DEBEN escribirse y DEBEN FALLAR antes de implementar el código de producción
  (el ciclo Rojo → Verde → Refactorizar se aplica estrictamente).
- Los escenarios BDD definidos en `spec.md` DEBEN tener una clase y método de prueba correspondientes.
- Los nombres de las clases de prueba DEBEN usar anotaciones `@DisplayName` descriptivas que
  expresen la frase BDD completa (Dado/Cuando/Entonces) o seguir el patrón `[Sujeto]Debe[Resultado]Test`.
- JaCoCo DEBE estar configurado y la compilación DEBE FALLAR si no se cumplen los umbrales
  de cobertura (ver Principio V).

### III. Buenas Prácticas de Programación (SOLID · YAGNI · DRY)

Todo el código de producción DEBE cumplir con las siguientes prácticas:

**SOLID:**
- **S – Responsabilidad Única**: Cada clase tiene exactamente una razón para cambiar.
  Casos de Uso, Entidades y Adaptadores representan cada uno un concepto cohesivo único.
- **O – Abierto/Cerrado**: El comportamiento se extiende agregando nuevas implementaciones
  de una interfaz, no modificando clases existentes.
- **L – Sustitución de Liskov**: Los subtipos DEBEN ser completamente sustituibles por sus
  tipos base sin alterar la corrección del programa.
- **I – Segregación de Interfaces**: Los clientes NO DEBEN depender de métodos que no utilizan.
  Las interfaces de Puerto DEBEN ser específicas y de propósito único.
- **D – Inversión de Dependencias**: Los módulos de alto nivel NO DEBEN depender de módulos
  de bajo nivel. Ambos DEBEN depender de abstracciones. Las implementaciones concretas
  se inyectan mediante Spring DI.

**YAGNI (No Lo Vas a Necesitar):**
- NO DEBE implementarse funcionalidad hasta que sea explícitamente requerida por una historia
  de usuario aprobada.
- No se permiten generalizaciones especulativas, abstracciones prematuras ni módulos
  de marcador de posición.
- Las banderas de características para funcionalidades inexistentes NO DEBEN confirmarse en el repositorio.

**DRY (No Te Repitas):**
- Cada pieza de conocimiento DEBE tener una única representación autoritativa en el código fuente.
- La duplicación de lógica es un problema bloqueante en la revisión de código y DEBE
  refactorizarse antes de fusionar.

**Reglas no negociables:**
- Las revisiones de PR DEBEN verificar explícitamente el cumplimiento de SOLID para cada clase modificada.
- Las violaciones de DRY identificadas en revisión DEBEN refactorizarse antes de aprobar el PR.
- Las violaciones de YAGNI (código sin usar, ramas muertas, abstracciones especulativas)
  DEBEN eliminarse antes de fusionar.

### IV. API First con Contrato OpenAPI

Toda API DEBE diseñarse con enfoque contrato-primero usando la especificación OpenAPI 3.x
ANTES de comenzar cualquier implementación. El contrato es la única fuente de verdad para
la forma de la API.

**Flujo de trabajo obligatorio:**
1. Crear o actualizar el contrato OpenAPI en `src/main/resources/openapi/`.
2. Revisar y aprobar el contrato mediante PR antes de escribir cualquier código de implementación.
3. Ejecutar `openapi-generator-maven-plugin` (vinculado a la fase `generate-sources`) para
   generar stubs del servidor, clases de modelo y SDKs de cliente.
4. Implementar la lógica de negocio dentro de los stubs generados; los archivos fuente
   generados NO DEBEN modificarse manualmente.
5. Las pruebas funcionales DEBEN validar el comportamiento contra el contrato aprobado.

**Reglas no negociables:**
- Ningún endpoint de API DEBE implementarse sin un contrato OpenAPI previo y fusionado.
- El código generado reside en `target/generated-sources/openapi/` y está excluido de
  ediciones manuales y del análisis de cobertura de JaCoCo.
- Los cambios de contrato que rompan compatibilidad (eliminación de campos, cambio de tipo,
  eliminación de endpoint) DEBEN incrementar la versión mayor de la API y requerir un
  aviso de obsolescencia en la versión anterior.
- La configuración del generador DEBE mantenerse en
  `src/main/resources/openapi/generator-config.yaml`.
- Los archivos de contrato DEBEN validarse con un linter (ej.: `spectral`) en el pipeline de CI.

### V. Métricas de Cobertura y Calidad (JaCoCo)

La cobertura de código se aplica automáticamente mediante JaCoCo integrado en la compilación
de Maven. La compilación DEBE FALLAR si se viola cualquier umbral definido a continuación.

**Umbrales de cobertura:**
- **Cobertura por clase**: DEBE ser > 80% (cobertura de línea + rama por clase).
- **Cobertura global del proyecto**: DEBE ser ≥ 80% (cobertura de línea + rama agregada).

**Reglas de configuración de JaCoCo:**
- JaCoCo DEBE configurarse en `pom.xml` con el objetivo `check` vinculado a la fase `verify`,
  usando elementos `<rules>` con `BUNDLE` y `CLASS`.
- Los reportes de cobertura DEBEN generarse en formato HTML (para revisión humana) y XML
  (para integración con CI / SonarQube).
- El código generado (por openapi-generator) DEBE excluirse del análisis de JaCoCo
  mediante configuración `<excludes>`.
- El pipeline de CI DEBE publicar los reportes HTML de JaCoCo como artefactos de
  construcción para cada PR.
- La cobertura NO DEBE inflarse artificialmente escribiendo pruebas vacías o triviales;
  esto constituye una violación durante la revisión de código.

## Gobernanza del Contrato API

El contrato OpenAPI es un artefacto de primera clase con su propio ciclo de vida:

- **Ubicación**: Los contratos residen en `src/main/resources/openapi/` (definición del servidor).
  Las pruebas funcionales los referencian desde `src/test/resources/`.
- **Convención de nombres**: Los archivos DEBEN seguir el patrón `[dominio]-api-v[MAYOR].yaml`
  (ej.: `secretaria-api-v1.yaml`).
- **Versionado**: Los contratos siguen Versionado Semántico. Los cambios aditivos (nuevos campos
  opcionales, nuevos endpoints) son MENORES; cualquier eliminación o cambio incompatible es MAYOR
  y requiere un aviso de obsolescencia previo.
- **Generación de código**: El `openapi-generator-maven-plugin` DEBE configurarse para ejecutarse
  durante la fase `generate-sources`. Las opciones de generación residen en
  `src/main/resources/openapi/generator-config.yaml`.
- **Puerta de revisión**: Los PRs que solo modifican contratos requieren al menos un revisor
  con autoridad de diseño de API antes de abrir PRs de implementación.

## Puertas de Calidad y Métricas

Las siguientes puertas DEBEN superarse todas para cada rama de funcionalidad antes de
fusionar con la rama principal:

| Puerta | Herramienta | Umbral | Acción si falla |
|--------|-------------|--------|-----------------|
| Suite de pruebas unitarias | JUnit 5 + Mockito | 100% pasan | Bloquear fusión |
| Suite de pruebas de integración | Spring Test / Testcontainers | 100% pasan | Bloquear fusión |
| Suite de pruebas funcionales | RestAssured / Karate | 100% pasan | Bloquear fusión |
| Cobertura global | JaCoCo | ≥ 80% | Bloquear fusión |
| Cobertura por clase | JaCoCo | > 80% por clase | Bloquear fusión |
| Análisis estático | Checkstyle / SpotBugs | Cero errores | Bloquear fusión |
| Validación del contrato OpenAPI | Spectral / paso CI | Cero errores | Bloquear fusión |
| Presencia del contrato OpenAPI | Verificación CI | El archivo de contrato existe | Bloquear fusión |

Los reportes de cobertura se publican como artefactos de CI y DEBEN revisarse durante la revisión de código.

## Gobernanza

Esta constitución reemplaza todos los demás acuerdos y lineamientos del equipo para el proyecto
SPB Secretaría. Las enmiendas requieren:

1. Una propuesta escrita que describa el cambio y su justificación (descripción del PR o documento RFC).
2. Revisión y aprobación del equipo (mayoría de votos o firma del arquitecto designado).
3. Un plan de migración para el código existente afectado por el cambio.
4. Incremento de versión siguiendo Versionado Semántico:
   - **MAYOR**: Cambio de gobernanza incompatible hacia atrás o eliminación/redefinición de principio.
   - **MENOR**: Nuevo principio o sección añadida, o guía materialmente expandida.
   - **PARCHE**: Aclaraciones, correcciones de redacción, refinamientos no semánticos.
5. Actualización sincrónica de todas las plantillas dependientes:
   `plan-template.md`, `spec-template.md`, `tasks-template.md`.

**Revisión de cumplimiento:** La descripción de cada PR DEBE listar los principios de la
constitución que satisface o difiere intencionalmente, con justificación para cualquier diferimiento.

**Guía de ejecución**: Los detalles del flujo de trabajo de desarrollo, comandos de shell e
instrucciones de configuración del proyecto se mantienen en `CLAUDE.md` en la raíz del repositorio.

**Versión**: 1.1.0 | **Ratificado**: 2026-06-28 | **Última Enmienda**: 2026-06-28
