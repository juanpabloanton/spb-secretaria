# Tasks: Centralizar Información Académica

**Input**: Design documents from `specs/001-centralizar-info-academica/`

**Prerequisites**: spec.md ✅ data-model.md ✅ contracts/ ✅ research.md ✅ quickstart.md ✅

**Tests**: Incluidas — la especificación y la Constitución (Principio II) exigen TDD BDD con pruebas
Unitarias (JUnit 5 + Mockito), de Integración (@SpringBootTest + Testcontainers) y Funcionales
(RestAssured). Las pruebas **DEBEN FALLAR** antes de comenzar la implementación.

**Organization**: Las tareas están agrupadas por historia de usuario para habilitar implementación y
prueba independiente de cada historia.

> **Nota de implementación**: El proyecto usa **Gradle + Spring Boot 4.1.0 + Java 25** (no Maven).
> Paquete base: `com.cienciayfe.secretaria`. Los comandos `mvn` se reemplazan por `./gradlew`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Puede ejecutarse en paralelo (archivos distintos, sin dependencias pendientes)
- **[Story]**: Historia de usuario a la que pertenece la tarea (US1, US2)
- Incluye rutas de archivo exactas en las descripciones

## Path Conventions

- Proyecto Spring Boot único; `src/` en la raíz del repositorio
- Paquete base: `com.cienciayfe.secretaria`
- Base de tests: `src/test/java/com/cienciayfe/secretaria/`

---

## Fase 1: Configuración del Proyecto

**Propósito**: Inicializar el proyecto Gradle, configurar herramientas de calidad, poner en
marcha la generación de stubs OpenAPI y crear los scripts de base de datos.

⚠️ **T005 y T006 DEBEN completarse antes de cualquier tarea de implementación** (Principio IV
de la Constitución — API First). **T009 y T010 DEBEN completarse antes de las pruebas de
integración** (los tests de Testcontainers usan `@Sql` para inicializar el esquema y los datos).

- [X] T001 Actualizar `build.gradle` con Spring Boot 4.1.0, Java 25 y dependencias: Spring Web, Spring Data JPA, Spring Validation, `org.openapi.generator 7.6.0`, JUnit 5, Mockito, Testcontainers (`postgresql`), RestAssured, Checkstyle, SpotBugs
- [X] T002 Crear estructura de paquetes de Arquitectura Limpia en `src/main/java/com/cienciayfe/secretaria/`: `dominio/modelo/`, `dominio/excepcion/`, `aplicacion/puerto/entrada/`, `aplicacion/puerto/salida/`, `aplicacion/servicio/`, `adaptadores/entrada/rest/`, `adaptadores/salida/persistencia/entidad/`, `infraestructura/configuracion/`
- [X] T003 [P] Configurar plugin JaCoCo en `build.gradle`: umbrales `INSTRUCTION > 0.80` por clase y `LINE >= 0.80` global; excluir `**/generated/**` del análisis de cobertura
- [X] T004 [P] Configurar plugins Checkstyle y SpotBugs en `build.gradle`; crear `checkstyle.xml` y `spotbugs-exclude.xml`
- [X] T005 Copiar el contrato `specs/001-centralizar-info-academica/contracts/secretaria-api-v1.yaml` a `src/main/resources/openapi/secretaria-api-v1.yaml` y crear `src/main/resources/openapi/generator-config.yaml` con `interfaceOnly: true`, `useSpringBoot3: true`, `apiPackage: com.cienciayfe.secretaria.adaptadores.entrada.rest.generated`
- [X] T006 Configurar `openApiGenerate` en `build.gradle` apuntando a `src/main/resources/openapi/secretaria-api-v1.yaml`; ejecutar `./gradlew openApiGenerate` y verificar que la interfaz `InformacionAcademicaApi` se genera en `build/generated-sources/openapi/`
- [X] T007 Crear `src/main/resources/application.yaml` con datasource PostgreSQL (`localhost:5432/secretaria`), `spring.jpa.hibernate.ddl-auto: none`, `spring.sql.init.mode: always`, `spring.sql.init.schema-locations: classpath:db/schema.sql`, `spring.sql.init.data-locations: classpath:db/data.sql`, `spring.servlet.multipart.max-file-size: 10MB`, `spring.servlet.multipart.max-request-size: 10MB`
- [X] T008 Crear `src/test/resources/application-test.yml` con `spring.sql.init.mode: never` (los tests de integración inyectan el esquema y datos vía `@Sql` sobre el contenedor Testcontainers con `@ServiceConnection`)
- [X] T009 Crear `src/main/resources/db/schema.sql` con el DDL definido en `specs/001-centralizar-info-academica/data-model.md §Esquema de Base de Datos`, usando `CREATE TABLE IF NOT EXISTS` para `periodo_academico` e `informacion_academica` (PostgreSQL con `gen_random_uuid()`, `BYTEA`, constraints de unicidad y claves foráneas)
- [X] T010 Crear `src/main/resources/db/data.sql` con datos semilla de `periodo_academico`: insertar 4 períodos (`2024-I` CERRADO, `2024-II` CERRADO, `2025-I` HABILITADO, `2025-II` HABILITADO) usando `ON CONFLICT (codigo) DO NOTHING`

---

## Fase 2: Fundacional — Dominio, Puertos y Persistencia

**Propósito**: Modelos de dominio, interfaces de puertos de salida, entidades JPA y repositorios
Spring Data que comparten ambas historias de usuario.

⚠️ **CRÍTICO**: Ninguna historia de usuario puede comenzar hasta que esta fase esté completa.

- [X] T011 Crear modelo de dominio `PeriodoAcademico` (record Java, sin imports de Spring/JPA) con campos `id` (UUID), `codigo`, `nombre`, `estado` (enum `HABILITADO`/`CERRADO`), `fechaInicio`, `fechaFin` en `src/main/java/com/cienciayfe/secretaria/dominio/modelo/PeriodoAcademico.java`
- [X] T012 [P] Crear modelo de dominio `InformacionAcademica` (record Java, sin imports de Spring/JPA) con campos `id` (UUID), `periodoAcademicoId` (UUID), `contenido` (byte[]), `nombreArchivo`, `tamanioBytes` (long), `estado` (enum `DISPONIBLE`/`SIN_INFORMACION`), `fechaCarga` (LocalDateTime), `usuarioResponsable` en `src/main/java/com/cienciayfe/secretaria/dominio/modelo/InformacionAcademica.java`
- [X] T013 [P] Crear excepción de dominio `ArchivoInvalidoException` (unchecked) con campos `codigo` y `mensaje` en `src/main/java/com/cienciayfe/secretaria/dominio/excepcion/ArchivoInvalidoException.java`
- [X] T014 [P] Crear excepción de dominio `PeriodoNoHabilitadoException` (unchecked) con campo `codigoPeriodo` en `src/main/java/com/cienciayfe/secretaria/dominio/excepcion/PeriodoNoHabilitadoException.java`
- [X] T015 Crear puerto de salida `PeriodoAcademicoRepositorio` con método `findByCodigo(String): Optional<PeriodoAcademico>` en `src/main/java/com/cienciayfe/secretaria/aplicacion/puerto/salida/PeriodoAcademicoRepositorio.java`
- [X] T016 [P] Crear puerto de salida `InformacionAcademicaRepositorio` con métodos `save(InformacionAcademica): InformacionAcademica` y `findByPeriodoAcademicoId(UUID): Optional<InformacionAcademica>` en `src/main/java/com/cienciayfe/secretaria/aplicacion/puerto/salida/InformacionAcademicaRepositorio.java`
- [X] T017 Crear entidad JPA `PeriodoAcademicoEntity` con `@Entity`, `@Table(name="periodo_academico")` y todos los campos del data-model.md en `src/main/java/com/cienciayfe/secretaria/adaptadores/salida/persistencia/entidad/PeriodoAcademicoEntity.java`
- [X] T018 [P] Crear entidad JPA `InformacionAcademicaEntity` con `@Entity`, `@Table(name="informacion_academica")`, `@Lob` en campo `contenido`, `@Column(unique=true)` en `periodoAcademicoId` en `src/main/java/com/cienciayfe/secretaria/adaptadores/salida/persistencia/entidad/InformacionAcademicaEntity.java`
- [X] T019 Crear `PeriodoAcademicoJpaRepositorio` (Spring Data, package-private) + `PeriodoAcademicoRepositorioAdapter` (implementa el puerto, mapea entidad↔dominio) en `src/main/java/com/cienciayfe/secretaria/adaptadores/salida/persistencia/`
- [X] T020 [P] Crear `InformacionAcademicaJpaRepositorio` (Spring Data, package-private) + `InformacionAcademicaRepositorioAdapter` (implementa el puerto, upsert atómico) en `src/main/java/com/cienciayfe/secretaria/adaptadores/salida/persistencia/`
- [X] T021 Crear `AppConfig` en `src/main/java/com/cienciayfe/secretaria/infraestructura/configuracion/AppConfig.java` como clase `@Configuration` para wiring explícito si Spring no puede resolver las implementaciones por interfaz

**Checkpoint**: Dominio, puertos y persistencia listos — puede comenzar la implementación de historias de usuario.

---

## Fase 3: Historia de Usuario 1 — Carga de Información Académica (Prioridad: P1) 🎯 MVP

**Objetivo**: La secretaria puede cargar un archivo CSV como fuente central para un período
habilitado. El sistema valida el archivo, lo registra atómicamente y confirma el éxito.
Si ya existe una fuente anterior, la reemplaza. Si el archivo es inválido, rechaza sin
modificar la fuente anterior.

**Prueba independiente**: Cargar un CSV válido para un período de `data.sql` con header
`X-Usuario-Responsable` → 201 con metadatos. Cargar archivo inválido → 400 sin cambios.
Período `CERRADO` o inexistente → 404. Header ausente → 400.

### Preparación HU1 (API First — DEBE ejecutarse antes de las pruebas)

- [X] T022 [US1] Verificar que `src/main/resources/openapi/secretaria-api-v1.yaml` (copiado por T005) ya contiene `components/parameters/UsuarioResponsable` (name: `X-Usuario-Responsable`, in: header, required: true); ejecutar `./gradlew openApiGenerate` y confirmar que la firma generada de `InformacionAcademicaApi.cargarInformacionAcademica()` incluye `@RequestHeader("X-Usuario-Responsable") String xUsuarioResponsable`

### Pruebas HU1 (BDD — DEBEN FALLAR antes de implementar) ⚠️

> **TDD**: Escribir y ejecutar las pruebas PRIMERO. Deben fallar antes de que el código de
> producción exista. Cada clase DEBE tener `@DisplayName` con la frase Dado/Cuando/Entonces.
> Las pruebas de integración y funcionales DEBEN enviar el header `X-Usuario-Responsable`.

- [X] T023 [P] [US1] Escribir prueba unitaria BDD para `CargarInformacionAcademicaService` cubriendo: carga nueva, reemplazo, formato inválido, archivo vacío, período CERRADO con Mockito en `src/test/java/com/cienciayfe/secretaria/aplicacion/CargarInformacionAcademicaServiceTest.java`
- [X] T024 [P] [US1] Escribir prueba de integración BDD para POST `/periodos/{codigoPeriodo}/informacion-academica` con `@SpringBootTest` + Testcontainers (`postgres:15-alpine`) + `@ServiceConnection`, `@Sql`, header `X-Usuario-Responsable: secretaria01`, cubriendo 201, 400, 404 en `src/test/java/com/cienciayfe/secretaria/adaptadores/entrada/InformacionAcademicaControllerIT.java`
- [X] T025 [P] [US1] Escribir prueba funcional BDD con RestAssured (`@SpringBootTest(webEnvironment=RANDOM_PORT)`) + Testcontainers con `@Sql`, incluyendo header `X-Usuario-Responsable: secretaria01` en `src/test/java/com/cienciayfe/secretaria/funcional/CentralizarInformacionAcademicaFuncionalTest.java`

### Implementación HU1

- [X] T026 [US1] Crear puerto de entrada `CargarInformacionAcademicaUseCase` con método `cargar(String codigoPeriodo, byte[] contenido, String nombreArchivo, String usuarioResponsable): InformacionAcademica` en `src/main/java/com/cienciayfe/secretaria/aplicacion/puerto/entrada/CargarInformacionAcademicaUseCase.java`
- [X] T027 [US1] Implementar `CargarInformacionAcademicaService` con: verificación de período habilitado, validación CSV (UTF-8, `;`, encabezados exactos, mínimo 1 fila, campos obligatorios, `calificacion_final` 0-20, `condicion` en {PROMOVIDO,REPROBADO,ABANDERADO}), upsert atómico en `src/main/java/com/cienciayfe/secretaria/aplicacion/servicio/CargarInformacionAcademicaService.java`
- [X] T028 [US1] Implementar `InformacionAcademicaController` como `@RestController` implementando `InformacionAcademicaApi` con handler POST que extrae `xUsuarioResponsable` del stub generado, lee `MultipartFile`, delega al use case y retorna `ResponseEntity<CargaResponse>` 201 en `src/main/java/com/cienciayfe/secretaria/adaptadores/entrada/rest/InformacionAcademicaController.java`
- [X] T029 [US1] Crear `GlobalExceptionHandler` con `@RestControllerAdvice` mapeando `ArchivoInvalidoException` → 400, `PeriodoNoHabilitadoException` → 404, `MaxUploadSizeExceededException` → 413 en `src/main/java/com/cienciayfe/secretaria/adaptadores/entrada/rest/GlobalExceptionHandler.java`

**Checkpoint**: HU1 completamente funcional — `POST /periodos/{codigo}/informacion-academica` opera
correctamente con header `X-Usuario-Responsable`. Verificar con `curl` de quickstart.md §6.1 antes de continuar.

---

## Fase 4: Historia de Usuario 2 — Consulta de la Fuente Central (Prioridad: P2)

**Objetivo**: La secretaria puede consultar el estado de la fuente central de cualquier período.
Si existe información cargada devuelve metadatos y estado `DISPONIBLE`. Si no existe, devuelve
estado `SIN_INFORMACION` con mensaje orientativo.

**Prueba independiente**: Consultar `2025-II` tras carga exitosa → 200 con estado `DISPONIBLE`.
Consultar `2025-I` sin carga previa → 200 con estado `SIN_INFORMACION`.
Período inexistente → 404.

### Pruebas HU2 (BDD — DEBEN FALLAR antes de implementar) ⚠️

- [X] T030 [P] [US2] Escribir prueba unitaria BDD para `ConsultarInformacionAcademicaService` cubriendo: período con información → `DISPONIBLE`, sin información → `SIN_INFORMACION`, período inexistente → excepción en `src/test/java/com/cienciayfe/secretaria/aplicacion/ConsultarInformacionAcademicaServiceTest.java`
- [X] T031 [P] [US2] Agregar pruebas de integración BDD para GET `/periodos/{codigoPeriodo}/informacion-academica` cubriendo 200 DISPONIBLE, 200 SIN_INFORMACION y 404 en `src/test/java/com/cienciayfe/secretaria/adaptadores/entrada/InformacionAcademicaControllerIT.java`
- [X] T032 [P] [US2] Agregar pruebas funcionales BDD para el flujo de consulta con RestAssured: consulta de `2025-II` tras carga y `2025-I` sin carga previa en `src/test/java/com/cienciayfe/secretaria/funcional/CentralizarInformacionAcademicaFuncionalTest.java`

### Implementación HU2

- [X] T033 [US2] Crear puerto de entrada `ConsultarInformacionAcademicaUseCase` con método `consultar(String codigoPeriodo): InformacionAcademica` en `src/main/java/com/cienciayfe/secretaria/aplicacion/puerto/entrada/ConsultarInformacionAcademicaUseCase.java`
- [X] T034 [US2] Implementar `ConsultarInformacionAcademicaService`: período con info → `DISPONIBLE`; período sin info → `SIN_INFORMACION`; período inexistente → `PeriodoNoHabilitadoException` en `src/main/java/com/cienciayfe/secretaria/aplicacion/servicio/ConsultarInformacionAcademicaService.java`
- [X] T035 [US2] Agregar handler GET `consultarInformacionAcademica` a `InformacionAcademicaController` retornando `ResponseEntity<FuenteCentralResponse>` 200 en `src/main/java/com/cienciayfe/secretaria/adaptadores/entrada/rest/InformacionAcademicaController.java`

**Checkpoint**: HU1 y HU2 completamente funcionales — `GET /periodos/{codigo}/informacion-academica`
opera correctamente. Verificar con `curl` de quickstart.md §6.2.

---

## Fase 5: Pulimiento y Validación Final

**Propósito**: Verificar umbrales de cobertura, calidad estática y validación completa de todos
los escenarios del quickstart.md.

- [ ] T036 [P] Ejecutar `./gradlew test jacocoTestCoverageVerification` y confirmar que JaCoCo pasa con cobertura > 80% por clase y ≥ 80% global; corregir cualquier clase que no alcance el umbral
- [ ] T037 [P] Revisar y corregir violaciones de Checkstyle y SpotBugs ejecutando `./gradlew checkstyleMain spotbugsMain`
- [ ] T038 [P] Validar manualmente todos los escenarios del `quickstart.md §7`: HU1-Esc.1 (`2025-II` nuevo → 201), HU1-Esc.2 (reemplazo → 201), HU1-Esc.3 (CSV inválido → 400), HU2-Esc.1 (`DISPONIBLE`), HU2-Esc.2 (`SIN_INFORMACION`), caso borde archivo > 10 MB (→ 413), caso borde período inexistente (→ 404), cobertura JaCoCo OK; incluir header `X-Usuario-Responsable: secretaria01` en todos los curl POST

---

## Dependencias y Orden de Ejecución

### Dependencias entre Fases

- **Fase 1 (Configuración)**: Sin dependencias — puede comenzar inmediatamente
- **Fase 2 (Fundacional)**: Requiere Fase 1 completa — bloquea ambas historias
- **Fase 3 (HU1)**: Requiere Fase 2 completa; T022 debe preceder a T023–T025; T024 y T025 requieren T009 y T010
- **Fase 4 (HU2)**: Requiere Fase 2 completa; T032 requiere T028
- **Fase 5 (Pulimiento)**: Requiere Fases 3 y 4 completas

### Oportunidades de Paralelismo

- **Fase 1**: T003/T004 en paralelo · T009/T010 en paralelo
- **Fase 2**: (T011,T012) ∥ (T013,T014) ∥ (T015,T016) ∥ (T017,T018) ∥ (T019,T020)
- **Fase 3 pruebas**: T023, T024, T025 en paralelo (tras T022)
- **Fase 4 pruebas**: T030, T031, T032 en paralelo (T032 espera T028)
- **Fase 5**: T036, T037, T038 en paralelo

---

## Notas

- **[P]** = archivos distintos, sin dependencias pendientes — pueden ejecutarse en paralelo
- **[US1] / [US2]** = vincula la tarea a la historia de usuario para trazabilidad
- **T022**: debe ejecutarse antes de las pruebas de HU1; el stub generado incluirá `@RequestHeader` automáticamente
- **db/schema.sql + db/data.sql**: fuente de verdad del esquema y datos semilla; usados vía `spring.sql.init` en dev y vía `@Sql` en tests con Testcontainers
- Principio IV (API First): T005, T006 y T022 DEBEN completarse antes de implementar el controlador
- Principio II (BDD): cada prueba DEBE tener `@DisplayName` con Dado/Cuando/Entonces
- Principio I (Arquitectura Limpia): `dominio/` y `aplicacion/` sin imports de Spring/JPA; `GlobalExceptionHandler` en `adaptadores/entrada/rest/`
- TDD: las pruebas DEBEN fallar antes de que el código de producción exista
- **Build**: Gradle (`./gradlew`) en lugar de Maven (`mvn`)
