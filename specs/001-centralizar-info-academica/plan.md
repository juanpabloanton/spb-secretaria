# Plan de Implementación: Centralizar Información Académica

**Rama**: `001-centralizar-info-academica` | **Fecha**: 2026-06-28 | **Spec**: [spec.md](spec.md)

**Entrada**: Especificación de funcionalidad en `specs/001-centralizar-info-academica/spec.md`

## Resumen

La secretaria académica necesita una única fuente centralizada de información por período
para eliminar el trabajo con archivos dispersos. Esta funcionalidad expone dos operaciones
REST: carga de archivo CSV de datos académicos (POST multipart) y consulta del estado de
la fuente central (GET), construidas bajo Arquitectura Limpia con contrato OpenAPI como
punto de partida y JaCoCo para control de cobertura.

## Contexto Técnico

**Lenguaje/Versión**: Java 17 (LTS)

**Dependencias principales**:
- Spring Boot 3.x (Web, Data JPA, Validation)
- openapi-generator-maven-plugin 7.x (generación de stubs desde contrato OpenAPI)
- JaCoCo Maven Plugin (cobertura ≥ 80% global y > 80% por clase)
- JUnit 5 + Mockito (pruebas unitarias BDD)
- Testcontainers + PostgreSQL 15 (pruebas de integración)
- RestAssured (pruebas funcionales sobre contrato OpenAPI)
- Checkstyle + SpotBugs (análisis estático)

**Almacenamiento**: PostgreSQL 15 vía Spring Data JPA. El contenido del archivo CSV se
almacena como BLOB (`bytea`) en la tabla `informacion_academica` para garantizar atomicidad
de la operación de carga.

**Inicialización de base de datos**: Scripts SQL en `src/main/resources/db/`:
- `schema.sql` — DDL completo de las tablas (`periodo_academico`, `informacion_academica`).
- `data.sql` — Datos semilla: períodos académicos pre-cargados en estado `HABILITADO` y `CERRADO`.
- Activados vía `spring.sql.init.mode: always` y `spring.jpa.hibernate.ddl-auto: none` en dev.
- En pruebas de integración con Testcontainers: `spring.sql.init.mode: never` en
  `application-test.yml`; cada test class usa
  `@Sql(scripts={"classpath:db/schema.sql","classpath:db/data.sql"})` para inicializar
  el contenedor limpiamente y garantizar paridad con el entorno de desarrollo.

**Identificación del usuario responsable**: RF-004 requiere registrar quién realizó la carga
(`usuarioResponsable`). En v1, sin módulo de autenticación (YAGNI), el valor se obtiene del
header HTTP `X-Usuario-Responsable` enviado por el cliente. El controlador extrae este header
y lo pasa como parámetro al caso de uso. El contrato OpenAPI debe incluir este header como
parámetro requerido del endpoint POST.

**Estado SIN_INFORMACION**: Es un **estado de respuesta virtual** — no se almacena en la tabla
`informacion_academica`. `ConsultarInformacionAcademicaService` lo devuelve cuando no existe
ningún registro para el período consultado. Ver `data-model.md §Transiciones de Estado`.

**Pruebas**: JUnit 5 + Mockito (unitarias) · @SpringBootTest + Testcontainers (integración) ·
RestAssured con @SpringBootTest(webEnvironment=RANDOM_PORT) (funcionales)

**Plataforma objetivo**: Servidor Linux · Servicio REST · Spring Boot embebido (Tomcat)

**Tipo de proyecto**: Servicio web (REST API)

**Objetivos de rendimiento**: Carga confirmada en < 3 min (CE-001) · Consulta respondida en
< 5 s (CE-003). Validados manualmente midiendo tiempo con `curl` en el quickstart.md §7
durante el sprint de validación. No se implementa prueba de carga automatizada en v1 (YAGNI).

**Restricciones**: Tamaño máximo de archivo 10 MB (configurable) · Operación de carga
atómica (todo o nada) · Fuente central anterior intacta si la carga falla · Sin historial
de versiones en v1 (YAGNI)

**Escala/Alcance**: Uso de secretaría escolar; 1-2 cargas por semestre; 200-2 000 estudiantes
por período

## Verificación de Constitución

*PUERTA: Debe superarse antes de la Fase 0 de investigación. Re-verificado después del diseño Fase 1.*

- [x] **I. Arquitectura Limpia**: Paquetes `dominio`, `aplicacion`, `adaptadores`, `infraestructura`
  definidos. Dominio y Aplicación sin imports de Spring/JPA. Interfaces de Repositorio en
  `aplicacion.puerto.salida`. `GlobalExceptionHandler` en `adaptadores.entrada.rest` (traduce
  excepciones de dominio a HTTP — rol de adaptador, no de infraestructura). Ver Project Structure.
- [x] **II. BDD Testing**: Los 5 escenarios de spec.md están en Dado/Cuando/Entonces.
  Pruebas Unitaria, Integración y Funcional planificadas para HU1 y HU2.
- [x] **III. SOLID · YAGNI · DRY**: Un Caso de Uso por clase. Solo las 2 historias aprobadas.
  Sin lógica de validación duplicada entre capas. Sin historial de versiones (YAGNI).
  Sin autenticación completa en v1 (YAGNI); `usuarioResponsable` via header HTTP.
- [x] **IV. API First**: Contrato `secretaria-api-v1.yaml` generado ANTES de cualquier
  tarea de implementación. Plugin openapi-generator configurado en la Fase 0 de tareas.
- [x] **V. JaCoCo**: Plugin configurado con umbrales > 80% por clase y ≥ 80% global.
  `target/generated-sources` excluido del análisis.

## Estructura del Proyecto

### Documentación (esta funcionalidad)

```text
specs/001-centralizar-info-academica/
├── plan.md              # Este archivo (/speckit-plan)
├── research.md          # Fase 0 (/speckit-plan)
├── data-model.md        # Fase 1 (/speckit-plan)
├── quickstart.md        # Fase 1 (/speckit-plan)
├── contracts/           # Fase 1 (/speckit-plan)
│   └── secretaria-api-v1.yaml
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks — NO creado por /speckit-plan)
```

### Código Fuente (raíz del repositorio)

```text
src/
├── main/
│   ├── java/com/maestriasoft/secretaria/
│   │   ├── dominio/
│   │   │   ├── modelo/
│   │   │   │   ├── PeriodoAcademico.java
│   │   │   │   └── InformacionAcademica.java
│   │   │   └── excepcion/
│   │   │       ├── ArchivoInvalidoException.java
│   │   │       └── PeriodoNoHabilitadoException.java
│   │   ├── aplicacion/
│   │   │   ├── puerto/
│   │   │   │   ├── entrada/
│   │   │   │   │   ├── CargarInformacionAcademicaUseCase.java
│   │   │   │   │   └── ConsultarInformacionAcademicaUseCase.java
│   │   │   │   └── salida/
│   │   │   │       ├── InformacionAcademicaRepositorio.java
│   │   │   │       └── PeriodoAcademicoRepositorio.java
│   │   │   └── servicio/
│   │   │       ├── CargarInformacionAcademicaService.java
│   │   │       └── ConsultarInformacionAcademicaService.java
│   │   ├── adaptadores/
│   │   │   ├── entrada/
│   │   │   │   └── rest/
│   │   │   │       ├── InformacionAcademicaController.java
│   │   │   │       ├── GlobalExceptionHandler.java    ← adaptador HTTP↔dominio
│   │   │   │       └── dto/
│   │   │   │           ├── CargaResponseDto.java
│   │   │   │           └── FuenteCentralResponseDto.java
│   │   │   └── salida/
│   │   │       └── persistencia/
│   │   │           ├── InformacionAcademicaJpaRepositorio.java
│   │   │           ├── PeriodoAcademicoJpaRepositorio.java
│   │   │           └── entidad/
│   │   │               ├── InformacionAcademicaEntity.java
│   │   │               └── PeriodoAcademicoEntity.java
│   │   └── infraestructura/
│   │       └── configuracion/
│   │           └── AppConfig.java
│   └── resources/
│       ├── application.yml
│       ├── db/
│       │   ├── schema.sql          # DDL: periodo_academico + informacion_academica
│       │   └── data.sql            # DML: períodos semilla (HABILITADO / CERRADO)
│       └── openapi/
│           ├── secretaria-api-v1.yaml
│           └── generator-config.yaml
└── test/
    ├── java/com/maestriasoft/secretaria/
    │   ├── aplicacion/
    │   │   ├── CargarInformacionAcademicaServiceTest.java
    │   │   └── ConsultarInformacionAcademicaServiceTest.java
    │   ├── adaptadores/
    │   │   └── entrada/
    │   │       └── InformacionAcademicaControllerIT.java
    │   └── funcional/
    │       └── CentralizarInformacionAcademicaFuncionalTest.java
    └── resources/
        └── application-test.yml    # Datasource Testcontainers + sql.init.mode: never
```

**Decisión de estructura**: Proyecto único (servicio REST backend) con paquetización por
capa de Arquitectura Limpia. Sin frontend en esta historia (YAGNI).

**Decisión GlobalExceptionHandler**: Ubicado en `adaptadores.entrada.rest` porque traduce
excepciones de dominio (`ArchivoInvalidoException`, `PeriodoNoHabilitadoException`) al
protocolo HTTP (`ErrorResponse`). Es un adaptador de salida de la capa de presentación,
no infraestructura. Depende de Dominio (excepciones) y produce DTOs HTTP — exactamente
el rol de la capa de Adaptadores según el Principio I de la Constitución.

**Decisión DB Init**: Los scripts `db/schema.sql` y `db/data.sql` son la única fuente
de verdad del esquema y datos semilla. Eliminan la necesidad de `ddl-auto: create-drop`
en pruebas y garantizan que el esquema de tests sea idéntico al de producción.

### Contenido de `db/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS periodo_academico (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo           VARCHAR(10)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    estado           VARCHAR(20)  NOT NULL CHECK (estado IN ('HABILITADO', 'CERRADO')),
    fecha_inicio     DATE         NOT NULL,
    fecha_fin        DATE         NOT NULL,
    CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE IF NOT EXISTS informacion_academica (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    periodo_academico_id    UUID         NOT NULL UNIQUE
                                         REFERENCES periodo_academico(id),
    contenido               BYTEA        NOT NULL,
    nombre_archivo          VARCHAR(255) NOT NULL,
    tamanio_bytes           BIGINT       NOT NULL,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    fecha_carga             TIMESTAMP    NOT NULL,
    usuario_responsable     VARCHAR(100) NOT NULL
);
```

### Contenido de `db/data.sql`

```sql
INSERT INTO periodo_academico (id, codigo, nombre, estado, fecha_inicio, fecha_fin)
VALUES
  (gen_random_uuid(), '2024-I',  'Primer Semestre 2024',  'CERRADO',    '2024-03-01', '2024-07-31'),
  (gen_random_uuid(), '2024-II', 'Segundo Semestre 2024', 'CERRADO',    '2024-08-01', '2024-12-20'),
  (gen_random_uuid(), '2025-I',  'Primer Semestre 2025',  'HABILITADO', '2025-03-01', '2025-07-31'),
  (gen_random_uuid(), '2025-II', 'Segundo Semestre 2025', 'HABILITADO', '2025-08-01', '2025-12-20')
ON CONFLICT (codigo) DO NOTHING;
```
