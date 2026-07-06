# SPB Secretaria — Centralizar Información Académica

Servicio REST construido con Spring Boot que permite a la secretaria académica cargar y
consultar la información académica centralizada por período, eliminando el trabajo con
archivos dispersos.

---
specify init --here --force --ai claude --offline

## Tecnologías

| Componente | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Gradle | 8.x |
| PostgreSQL | 15 |
| openapi-generator | 7.6.0 |
| JaCoCo | Plugin Gradle |
| Testcontainers | `postgresql` |
| RestAssured | Pruebas funcionales |
| Checkstyle + SpotBugs | Análisis estático |

---

## Qué se hizo paso a paso

### Paso 0 — Constitución del Proyecto (`/speckit-constitution`)
"genera un constitution con las siguieentes reglas: 1.- la arquitectura es la arquitectura limpia planteada por robert martin. 2.-Generar pruebas unitarias , integracion y funcionales utlizando concepto BDD 3.- Utilizar buenas practicas
  de programacion que es SOLID, yagni , dry. , 4.- Utilizar el concepto de API frist para generar las apis, siempre debe estar un contrato openapi y utilizar openapi-generator para la generacion de las mismas. 5.- validar las siguientes metricas: coverage por
  clase > 80% y  global >=80% y utilizar jacoco para generar los reportes de pruebas"

Lo primero que se hizo fue ejecutar `/speckit-constitution` para establecer las reglas y
principios que gobiernan **todo** el desarrollo del proyecto. Estas reglas son verificadas
en cada fase del plan y ninguna tarea puede considerarse completa si las viola.

#### Principio I — Arquitectura Limpia (Robert C. Martin)

El código se organiza en cuatro capas con dependencias que apuntan únicamente hacia adentro:

```
dominio/          ← Núcleo: entidades y excepciones. Sin imports de Spring ni JPA.
aplicacion/       ← Casos de uso y puertos (interfaces). Sin imports de Spring ni JPA.
adaptadores/      ← Controladores REST, repositorios JPA. Implementa los puertos.
infraestructura/  ← Configuración Spring. Conecta las piezas.
```

- Los modelos de dominio son `record` Java puros, sin anotaciones de framework.
- Las interfaces de repositorio viven en `aplicacion/puerto/salida/` y son implementadas en `adaptadores/salida/persistencia/`.
- `GlobalExceptionHandler` vive en `adaptadores/entrada/rest/` porque traduce excepciones de dominio al protocolo HTTP — es un adaptador, no infraestructura.

#### Principio II — Pruebas BDD (Unitarias, Integración y Funcionales)

Cada historia de usuario tiene tres niveles de prueba, todos expresados en notación **Dado / Cuando / Entonces** mediante `@DisplayName`:

| Nivel | Herramienta | Qué cubre |
|-------|-------------|-----------|
| Unitaria | JUnit 5 + Mockito | Servicios de aplicación aislados con mocks |
| Integración | @SpringBootTest + Testcontainers + `@Sql` | Controlador + base de datos PostgreSQL 15 real |
| Funcional | RestAssured + @SpringBootTest(RANDOM_PORT) + Testcontainers | Flujos HTTP completos contra la API |

Regla TDD: las pruebas **deben escribirse primero y fallar** antes de que exista el código de producción.

#### Principio III — Buenas Prácticas: SOLID, YAGNI, DRY

- **SOLID**: Un caso de uso por clase de servicio. Interfaces de repositorio separadas por entidad. Controlador delegando toda lógica de negocio al caso de uso.
- **YAGNI**: Sin historial de versiones de archivos en v1. Sin módulo de autenticación completo en v1 (el usuario responsable se envía como header HTTP `X-Usuario-Responsable`). Sin almacenamiento en la nube.
- **DRY**: La validación del CSV ocurre únicamente en `CargarInformacionAcademicaService`. Los scripts `db/schema.sql` y `db/data.sql` son la única fuente de verdad del esquema y datos semilla.

#### Principio IV — API First con OpenAPI Generator

El contrato de la API **se escribe antes de cualquier código de implementación**:

1. Se genera `specs/.../contracts/secretaria-api-v1.yaml` con los endpoints, parámetros y esquemas de respuesta.
2. El contrato se copia a `src/main/resources/openapi/secretaria-api-v1.yaml`.
3. Se ejecuta `./gradlew openApiGenerate` para generar la interfaz `InformacionAcademicaApi`.
4. El controlador implementa esa interfaz generada — nunca al revés.

Los archivos generados **no se modifican manualmente**.

#### Principio V — Cobertura con JaCoCo

JaCoCo se configura en `build.gradle` con los siguientes umbrales obligatorios:

| Métrica | Umbral | Alcance |
|---------|--------|---------|
| `INSTRUCTION` | > 80% | Por clase |
| `LINE` | >= 80% | Global |

El directorio `build/generated-sources/` se excluye del análisis (código generado por OpenAPI).
La compilación **falla automáticamente** si algún umbral no se alcanza.

---

### Paso 1 — Especificación de la funcionalidad (`/speckit-specify`)

Se describió la necesidad en lenguaje natural:

> "Como secretaria académica, quiero cargar la información académica en una única fuente
> para el período seleccionado, para preparar cuadros finales, promociones y abanderados
> sin trabajar con archivos dispersos."

El agente generó `specs/001-centralizar-info-academica/spec.md` con la especificación completa:

**Épica**: E-01 | **Historia**: US-01 | **Puntos**: 5 | **Estado**: Borrador

#### Historia de Usuario 1 — Carga de Información Académica (Prioridad: P1)

La secretaria académica selecciona un período lectivo y carga un archivo con la información
académica de todos los estudiantes de ese período. Una vez cargado, el archivo queda
registrado como la fuente central de información oficial para ese período, reemplazando
cualquier versión anterior si existiera.

**Por qué esta prioridad**: Es el flujo central que habilita todas las demás funcionalidades
del sistema (cuadros finales, promociones y abanderados). Sin la carga, no hay información
centralizada que procesar.

**Escenarios de Aceptación**:

1. **Dado** un archivo de datos académicos válido para el período lectivo 2025-II,
   **Cuando** la secretaria selecciona el período 2025-II y carga el archivo,
   **Entonces** el sistema registra el archivo como la fuente central del período 2025-II
   y confirma a la secretaria que la carga fue exitosa.

2. **Dado** que ya existe una fuente central para el período 2025-II,
   **Cuando** la secretaria carga un archivo actualizado para el mismo período,
   **Entonces** el sistema reemplaza la fuente anterior con la nueva versión y registra
   la fecha y hora de actualización.

3. **Dado** un archivo con formato inválido o datos incompletos,
   **Cuando** la secretaria intenta cargarlo para un período seleccionado,
   **Entonces** el sistema rechaza el archivo, informa el motivo del rechazo con detalle
   suficiente para corregirlo, y la fuente central anterior permanece sin cambios.

#### Historia de Usuario 2 — Consulta de la Fuente Central (Prioridad: P2)

La secretaria puede consultar en cualquier momento qué información académica está registrada
como fuente central para un período dado, visualizando su estado, fecha de carga y
la posibilidad de revisarla antes de generar cuadros finales o listados de promoción.

**Por qué esta prioridad**: Permite a la secretaria verificar que la información centralizada
es correcta antes de usarla para procesos críticos como cuadros finales y abanderados.

**Escenarios de Aceptación**:

1. **Dado** que existe información académica cargada para el período 2025-II,
   **Cuando** la secretaria consulta el período 2025-II,
   **Entonces** el sistema muestra la versión vigente de la información con la fecha de carga
   y el estado "Disponible para revisión".

2. **Dado** que no existe información cargada para el período 2025-I,
   **Cuando** la secretaria consulta el período 2025-I,
   **Entonces** el sistema informa que no hay fuente central registrada para ese período
   y sugiere realizar una carga.

#### Casos Borde

- Archivo supera el tamaño máximo (10 MB) → el sistema informa el límite y rechaza la carga sin guardar datos parciales.
- Pérdida de conexión durante la carga → carga considerada fallida; la fuente central anterior permanece intacta.
- Período inexistente o no habilitado → el sistema impide la selección y muestra solo los períodos habilitados.

#### Requisitos Funcionales

| ID | Descripción |
|----|-------------|
| RF-001 | El sistema DEBE permitir a la secretaria seleccionar un período académico habilitado antes de realizar la carga. |
| RF-002 | El sistema DEBE permitir cargar un archivo de datos académicos para el período seleccionado. |
| RF-003 | El sistema DEBE registrar el archivo cargado como la fuente central oficial para ese período. |
| RF-004 | El sistema DEBE reemplazar la fuente central existente al cargar una versión nueva, conservando los metadatos (fecha, hora, usuario que cargó). |
| RF-005 | El sistema DEBE validar el formato y la integridad del archivo antes de aceptarlo. |
| RF-006 | El sistema DEBE rechazar archivos con formato inválido o datos incompletos, informando el motivo específico. |
| RF-007 | El sistema DEBE permitir consultar la fuente central vigente de cualquier período habilitado. |
| RF-008 | El sistema DEBE mostrar el estado de la fuente central (sin información / disponible para revisión) y los metadatos de la última carga. |

#### Criterios de Éxito

| ID | Criterio |
|----|----------|
| CE-001 | La secretaria completa la carga en menos de 3 minutos desde que selecciona el período hasta recibir la confirmación de éxito. |
| CE-002 | El 100% de los datos del archivo cargado quedan disponibles para revisión inmediatamente después de la confirmación. |
| CE-003 | La secretaria puede consultar el estado de la fuente central de cualquier período en menos de 5 segundos. |
| CE-004 | La tasa de errores en cargas con archivos válidos es menor al 1%. |
| CE-005 | La secretaria elimina por completo la necesidad de consultar archivos dispersos para preparar cuadros finales, promociones y abanderados. |

#### Supuestos

- La secretaria ya está autenticada antes de acceder a esta funcionalidad.
- El sistema ya cuenta con una lista de períodos académicos habilitados previamente configurada.
- El formato del archivo CSV es predefinido, documentado y conocido por la secretaria.
- Cada período académico tiene exactamente una fuente central activa a la vez.
- El sistema conserva solo la versión más reciente; el historial de versiones anteriores queda fuera del alcance (YAGNI).
- La secretaria tiene permisos exclusivos para cargar y actualizar la fuente central; otros roles solo pueden consultar.

---

### Paso 2 — Investigación técnica (`/speckit-plan` — Fase 0)

El agente generó `specs/001-centralizar-info-academica/research.md` con las decisiones técnicas:

- **Stack**: Java 25 + Spring Boot 4.1.0 + Gradle (el `build.gradle` existente revelaba Gradle, no Maven).
- **Almacenamiento**: CSV como BLOB en PostgreSQL (`bytea`) para garantizar atomicidad de la carga.
- **Formato de archivo**: CSV con delimitador `;`, codificación UTF-8, primera fila de encabezados.
- **Pruebas de integración**: Testcontainers con PostgreSQL 15 real (no H2, para evitar divergencias de dialectos SQL).
- **Pruebas funcionales**: RestAssured con `@SpringBootTest(webEnvironment=RANDOM_PORT)`.

---

### Paso 3 — Modelo de datos y contrato OpenAPI (`/speckit-plan` — Fase 1)

Se generaron tres artefactos de diseño antes de escribir una sola línea de código de producción:

**`specs/.../data-model.md`** — Entidades del dominio:

```
PeriodoAcademico (1) ←——— (0..1) InformacionAcademica
```

- `periodo_academico`: id, codigo, nombre, estado (`HABILITADO`/`CERRADO`), fechas
- `informacion_academica`: id, periodo_academico_id (UNIQUE), contenido (BYTEA), nombre_archivo, tamanio_bytes, estado, fecha_carga, usuario_responsable

**`specs/.../contracts/secretaria-api-v1.yaml`** — Contrato OpenAPI (API First):

- `POST /periodos/{codigoPeriodo}/informacion-academica` — carga multipart con header `X-Usuario-Responsable`
- `GET /periodos/{codigoPeriodo}/informacion-academica` — consulta del estado de la fuente central

**`specs/.../quickstart.md`** — Guía de verificación manual con `curl` para validar todos los escenarios.

---

### Paso 4 — Lista de verificación de requisitos (`/speckit-checklist`)

Se generó `specs/.../checklists/requirements.md` validando la alineación entre la especificación,
el contrato OpenAPI y los principios de la constitución antes de avanzar a las tareas.

---

### Paso 5 — Plan de tareas (`/speckit-tasks`)

Se generó `specs/.../tasks.md` con 38 tareas organizadas en 5 fases con dependencias y
oportunidades de paralelismo:

| Fase | Tareas | Propósito |
|------|--------|-----------|
| 1 | T001–T010 | Configuración del proyecto (Gradle, JaCoCo, OpenAPI, DB scripts) |
| 2 | T011–T021 | Dominio, puertos y persistencia (compartido por ambas HU) |
| 3 | T022–T029 | Historia de Usuario 1 — Carga de información |
| 4 | T030–T035 | Historia de Usuario 2 — Consulta de la fuente central |
| 5 | T036–T038 | Pulimiento: cobertura, análisis estático, validación manual |

---

### Paso 6 — Implementación (`/speckit-implement`)

El agente ejecutó las tareas T001–T035 siguiendo el orden de dependencias:

#### Fase 1 — Configuración del proyecto

```bash
# T001: build.gradle actualizado con todas las dependencias
./gradlew dependencies

# T003: JaCoCo configurado con umbrales
./gradlew jacocoTestCoverageVerification

# T004: Checkstyle y SpotBugs configurados
./gradlew checkstyleMain spotbugsMain

# T006: Stubs OpenAPI generados desde el contrato YAML
./gradlew openApiGenerate
# → build/generated-sources/openapi/InformacionAcademicaApi.java
```

#### Fase 2 — Dominio, puertos y persistencia

Se creó la estructura de Arquitectura Limpia:

```
src/main/java/com/cienciayfe/secretaria/
├── dominio/
│   ├── modelo/        PeriodoAcademico.java, InformacionAcademica.java  (records Java, sin Spring/JPA)
│   └── excepcion/     ArchivoInvalidoException.java, PeriodoNoHabilitadoException.java
├── aplicacion/
│   ├── puerto/
│   │   ├── entrada/   CargarInformacionAcademicaUseCase.java, ConsultarInformacionAcademicaUseCase.java
│   │   └── salida/    PeriodoAcademicoRepositorio.java, InformacionAcademicaRepositorio.java
│   └── servicio/      CargarInformacionAcademicaService.java, ConsultarInformacionAcademicaService.java
├── adaptadores/
│   ├── entrada/rest/  InformacionAcademicaController.java, GlobalExceptionHandler.java
│   └── salida/persistencia/  *RepositorioAdapter.java, entidad/*.java
└── infraestructura/configuracion/  AppConfig.java
```

#### Fase 3 — Historia de Usuario 1 (TDD BDD)

Orden TDD: pruebas primero (deben fallar), luego implementación.

```bash
# Escribir y ejecutar pruebas que DEBEN FALLAR antes de implementar
./gradlew test --tests "com.cienciayfe.secretaria.aplicacion.CargarInformacionAcademicaServiceTest"
./gradlew test --tests "com.cienciayfe.secretaria.adaptadores.entrada.InformacionAcademicaControllerIT"
./gradlew test --tests "com.cienciayfe.secretaria.funcional.CentralizarInformacionAcademicaFuncionalTest"

# Implementar y verificar que todas pasan
./gradlew test
```

#### Fase 4 — Historia de Usuario 2 (TDD BDD)

Mismo ciclo TDD: pruebas unitarias, de integración y funcionales escritas antes de la implementación.

```bash
./gradlew test --tests "com.cienciayfe.secretaria.aplicacion.ConsultarInformacionAcademicaServiceTest"
./gradlew test
```

---

### Paso 7 — Informe final de calidad (Claude Quality Agent + Semgrep)

La última verificación integral fue ejecutada el **5 de julio de 2026** sobre la versión
`0.0.1-SNAPSHOT` y el commit `6bdf523`. El análisis combinó la suite automatizada, la
verificación de cobertura con JaCoCo, el análisis estático de seguridad con Semgrep y la
trazabilidad de los requisitos de la especificación.

#### Resumen ejecutivo

| Control de calidad | Resultado | Estado |
|--------------------|-----------|--------|
| Pruebas automatizadas | **49 de 49** aprobadas | Cumple |
| Cobertura global de instrucciones | **99,1 %** | Cumple el umbral ≥ 80 % |
| Cobertura de líneas | **99,4 %** (1 de 167 líneas no cubierta) | Cumple el umbral ≥ 80 % |
| Cobertura de ramas | **84 %** (9 de 58 ramas no cubiertas) | Informativo |
| Verificación JaCoCo | `BUILD SUCCESSFUL` | Cumple |
| Semgrep | **0 hallazgos** en 21 archivos Java | Cumple |
| Vulnerabilidades críticas/altas | **0 / 0** | Cumple |
| Secretos en código de producción | **0** | Cumple |
| Requisitos funcionales | **8 de 8** con evidencia de prueba | Cumple |
| Casos borde evaluados | **3 de 3** con evidencia de prueba | Cumple |

#### Alcance del análisis Semgrep

El Claude Quality Agent ejecutó **Semgrep CLI 1.168.0** con configuración automática sobre
`src/main/java`. Se aplicó un catálogo de **343 reglas** a los 21 archivos Java de producción:

```bash
uvx --from semgrep semgrep --config auto src/main/java --json
```

El resultado fue `Findings: 0 (0 blocking)`, sin errores de análisis. Como verificación
complementaria se buscaron credenciales embebidas en `src/main`: las contraseñas encontradas
en `application.yaml` son referencias a variables de entorno (`DB_PASSWORD` y
`APP_SECRETARIA_PASSWORD`), no secretos en texto claro. La credencial presente en
`src/test/resources/application-test.yml` corresponde exclusivamente al perfil de pruebas.

La revisión manual confirmó además que los endpoints `/periodos/**` requieren el rol
`SECRETARIA`, utilizan autenticación HTTP Basic y almacenan contraseñas con BCrypt.

#### Trazabilidad y observaciones

La evidencia automatizada cubre los requisitos RF-001 a RF-008 y los casos borde de archivo
mayor a 10 MB, pérdida de conexión y período inexistente. No se identificaron incumplimientos
bloqueantes. Permanecen dos oportunidades de mejora:

1. JaCoCo genera actualmente el informe HTML, pero no el XML exigido por el Principio V de la
   constitución. Se recomienda habilitar `xml.required = true` en `jacocoTestReport`.
2. Las pruebas de integración autentican todas las solicitudes. Se recomienda añadir un caso
   que invoque un endpoint protegido sin credenciales y compruebe explícitamente la respuesta
   HTTP `401 Unauthorized`.

**Conclusión:** la versión evaluada satisface los umbrales de pruebas, cobertura, seguridad
estática y trazabilidad funcional definidos para la entrega. Las observaciones anteriores son
no bloqueantes, pero deberían incorporarse al siguiente ciclo para completar la evidencia
documental y reforzar la regresión del control de acceso.

Artefactos de respaldo:

- [Informe visual de calidad](quality-output/report.html)
- [Evidencia estructurada de verificación](quality-output/verification.json)
- [Resultado crudo de Semgrep](quality-output/semgrep-result.json)

---

## Estructura del proyecto

```
spb-secretaria/
├── build.gradle                          # Dependencias, JaCoCo, OpenAPI generator, Checkstyle, SpotBugs
├── checkstyle.xml                        # Reglas de estilo
├── spotbugs-exclude.xml                  # Exclusiones SpotBugs
├── specs/001-centralizar-info-academica/
│   ├── spec.md                           # Especificación BDD  (Paso 1)
│   ├── research.md                       # Decisiones técnicas  (Paso 2)
│   ├── data-model.md                     # Entidades y esquema SQL  (Paso 3)
│   ├── plan.md                           # Plan de implementación  (Paso 3)
│   ├── quickstart.md                     # Guía de verificación manual  (Paso 3)
│   ├── tasks.md                          # 38 tareas con dependencias  (Paso 5)
│   ├── contracts/secretaria-api-v1.yaml  # Contrato OpenAPI  (Paso 3)
│   └── checklists/requirements.md        # Checklist de requisitos  (Paso 4)
└── src/
    ├── main/
    │   ├── java/com/cienciayfe/secretaria/
    │   └── resources/
    │       ├── application.yaml
    │       ├── db/
    │       │   ├── schema.sql            # DDL (periodo_academico + informacion_academica)
    │       │   └── data.sql              # Datos semilla (4 períodos)
    │       └── openapi/
    │           └── secretaria-api-v1.yaml
    └── test/
        ├── java/com/cienciayfe/secretaria/
        │   ├── aplicacion/               # Pruebas unitarias (JUnit 5 + Mockito)
        │   ├── adaptadores/entrada/      # Pruebas de integración (Testcontainers)
        │   └── funcional/               # Pruebas funcionales (RestAssured)
        └── resources/
            └── application-test.yml      # spring.sql.init.mode: never
```

---

## Prerequisitos

- Java 25 (`java -version`)
- Docker disponible (para Testcontainers)
- PostgreSQL 15 en `localhost:5432` (o iniciar con Docker)

---

## Comandos de uso

### Levantar la base de datos con Docker

```bash
docker run -d \
  --name secretaria-db \
  -e POSTGRES_DB=secretaria \
  -e POSTGRES_USER=secretaria \
  -e POSTGRES_PASSWORD=secretaria \
  -p 5432:5432 \
  postgres:15-alpine
```

### Generar stubs desde el contrato OpenAPI

```bash
./gradlew openApiGenerate
```

Genera `build/generated-sources/openapi/` con la interfaz `InformacionAcademicaApi`.
**No modificar** los archivos generados.

### Compilar y ejecutar todas las pruebas

```bash
./gradlew test
```

### Verificar cobertura JaCoCo

```bash
./gradlew test jacocoTestCoverageVerification
```

Umbrales: > 80% por clase · >= 80% global.
Reporte HTML: `build/reports/jacoco/test/html/index.html`

### Análisis estático

```bash
./gradlew checkstyleMain spotbugsMain
```

### Compilación completa (pruebas + cobertura + análisis)

```bash
./gradlew verify
```

### Levantar el servicio

```bash
./gradlew bootRun
```

Disponible en `http://localhost:8080/api/v1`

---

## Verificación manual de los endpoints

### Cargar información académica

```bash
curl -X POST http://localhost:8080/api/v1/periodos/2025-II/informacion-academica \
  -F "archivo=@datos_academicos_2025II.csv" \
  -H "Content-Type: multipart/form-data" \
  -H "X-Usuario-Responsable: secretaria01"
```

**Ejemplo de CSV válido** (`datos_academicos_2025II.csv`):

```
codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
EST001;García;López;Ana María;18.5;PROMOVIDO
EST002;Torres;Ruiz;Carlos;12.0;PROMOVIDO
EST003;Mendoza;Silva;Lucía;8.5;REPROBADO
EST004;Flores;Castro;Diego;20.0;ABANDERADO
```

**Respuesta exitosa (201 Created)**:

```json
{
  "codigoPeriodo": "2025-II",
  "nombreArchivo": "datos_academicos_2025II.csv",
  "tamanioBytes": 245,
  "fechaCarga": "2026-06-28T14:30:00",
  "usuarioResponsable": "secretaria01",
  "mensaje": "Información académica del período 2025-II registrada exitosamente como fuente central."
}
```

### Consultar la fuente central

```bash
curl http://localhost:8080/api/v1/periodos/2025-II/informacion-academica
```

**Respuesta cuando hay información (200 OK)**:

```json
{
  "codigoPeriodo": "2025-II",
  "estado": "DISPONIBLE",
  "nombreArchivo": "datos_academicos_2025II.csv",
  "tamanioBytes": 245,
  "fechaCarga": "2026-06-28T14:30:00",
  "usuarioResponsable": "secretaria01",
  "mensaje": "Disponible para revisión"
}
```

**Respuesta cuando no hay información (200 OK)**:

```json
{
  "codigoPeriodo": "2025-I",
  "estado": "SIN_INFORMACION",
  "mensaje": "No hay fuente central registrada para este período. Se sugiere realizar una carga."
}
```

### Medir tiempo de respuesta (criterios de éxito)

```bash
# CE-001: carga < 3 minutos
time curl -X POST http://localhost:8080/api/v1/periodos/2025-II/informacion-academica \
  -F "archivo=@datos_academicos_2025II.csv" \
  -H "X-Usuario-Responsable: secretaria01"

# CE-003: consulta < 5 segundos
curl -w "%{time_total}\n" -o /dev/null -s \
  http://localhost:8080/api/v1/periodos/2025-II/informacion-academica
```

---

## Casos de error

| Situación | Código HTTP | Causa |
|-----------|------------|-------|
| Archivo con formato inválido | 400 | Encabezados incorrectos, campos vacíos, calificación fuera de rango, condición no permitida |
| Header `X-Usuario-Responsable` ausente | 400 | Header requerido por el contrato |
| Período no habilitado o inexistente | 404 | Solo períodos con estado `HABILITADO` aceptan cargas |
| Archivo mayor a 10 MB | 413 | Límite configurable en `application.yaml` |

---

## Datos semilla (períodos disponibles)

| Código | Nombre | Estado |
|--------|--------|--------|
| 2024-I | Primer Semestre 2024 | CERRADO |
| 2024-II | Segundo Semestre 2024 | CERRADO |
| 2025-I | Primer Semestre 2025 | HABILITADO |
| 2025-II | Segundo Semestre 2025 | HABILITADO |

Solo `2025-I` y `2025-II` aceptan cargas.
