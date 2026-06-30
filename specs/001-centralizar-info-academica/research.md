# Investigación: Centralizar Información Académica

**Generado**: 2026-06-28 | **Funcionalidad**: US-01 · Centralizar Información Académica

## Decisiones Técnicas

---

### 1. Stack tecnológico

**Decisión**: Java 17 + Spring Boot 3.x + Maven

**Justificación**: El nombre del repositorio `spb-secretaria` indica Spring Boot (SPB). Java 17
es la versión LTS vigente soportada por Spring Boot 3.x. Maven es el estándar del ecosistema
y el `openapi-generator-maven-plugin` ofrece soporte oficial para Maven.

**Alternativas consideradas**:
- Gradle: válido, pero Maven tiene mejor soporte documentado para openapi-generator en proyectos Java educativos.
- Spring Boot 2.x: descartado por ser EOL; Spring Boot 3.x requiere Java 17+.

---

### 2. Almacenamiento del archivo académico

**Decisión**: El contenido del archivo se persiste como BLOB en PostgreSQL (columna `bytea`),
referenciado por la entidad `InformacionAcademica`.

**Justificación**: Simplifica la atomicidad de la operación (la transacción BD garantiza que
o se guarda todo o nada), elimina dependencias de sistema de archivos del servidor, y facilita
las pruebas con Testcontainers. Dado el volumen esperado (200-2 000 estudiantes, archivos
< 5 MB), el impacto de rendimiento es despreciable.

**Alternativas consideradas**:
- Sistema de archivos local: descartado por riesgo de inconsistencia si falla la transacción.
- Almacenamiento en la nube (S3, GCS): descartado por YAGNI; complejidad no justificada para
  este alcance.

---

### 3. Formato del archivo de datos académicos

**Decisión**: CSV con delimitador punto y coma (`;`), codificación UTF-8, primera fila de encabezados.

**Columnas mínimas**:
```
codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
```
Donde `condicion` toma valores: `PROMOVIDO`, `REPROBADO`, `ABANDERADO`.

**Justificación**: CSV es el formato de exportación nativo de las planillas de cálculo usadas
por secretarías escolares. El punto y coma evita conflictos con comas en nombres propios.
UTF-8 garantiza soporte de tildes y caracteres especiales del español.

**Alternativas consideradas**:
- Excel (.xlsx): descartado por dependencia de librería externa (Apache POI) — YAGNI para v1.
- JSON: descartado por no ser el formato natural de trabajo de la secretaria.

---

### 4. Estrategia de pruebas de integración (base de datos)

**Decisión**: Testcontainers con imagen oficial `postgres:15-alpine` para pruebas de integración.

**Justificación**: Garantiza que las pruebas se ejecutan contra el mismo motor de base de datos
que producción, eliminando divergencias entre H2 (en memoria) y PostgreSQL (producción).
La constitución (Principio II) exige base de datos real o contenedorizada en integración.

**Alternativas consideradas**:
- H2 en memoria: descartado por divergencias de dialectos SQL con PostgreSQL.
- Base de datos compartida: descartado por acoplar el entorno de pruebas al estado de producción.

---

### 5. Pruebas funcionales (API)

**Decisión**: RestAssured con `@SpringBootTest(webEnvironment = RANDOM_PORT)`.

**Justificación**: RestAssured integra nativamente con Spring Boot, soporta multipart/form-data
para pruebas de carga de archivo, y tiene API fluida compatible con BDD (given/when/then).

**Alternativas consideradas**:
- Karate: válido pero introduce DSL adicional; RestAssured es más familiar en ecosistema Java.
- MockMvc: descartado para pruebas funcionales (no prueba el stack HTTP completo).

---

### 6. Manejo del archivo en la API

**Decisión**: `multipart/form-data` con campo `archivo` (tipo `binary`). Tamaño máximo: 10 MB
(configurable en `application.yml` vía `spring.servlet.multipart.max-file-size`).

**Justificación**: El estándar HTTP para subida de archivos es multipart/form-data.
Spring Boot lo maneja nativamente con `MultipartFile`. 10 MB es suficiente para archivos
CSV de hasta ~100 000 filas.

---

### 7. Validación del CSV

**Decisión**: La validación ocurre en la capa de Aplicación, dentro del Caso de Uso
`CargarInformacionAcademicaService`. El Controlador solo parsea el multipart; la lógica
de negocio de validación pertenece al dominio de aplicación.

**Reglas de validación**:
1. El archivo no debe estar vacío.
2. La primera fila debe contener exactamente los encabezados esperados.
3. Cada fila posterior debe tener el número correcto de columnas.
4. `calificacion_final` debe ser numérico entre 0 y 20.
5. `condicion` debe ser uno de los valores permitidos.

**Justificación**: Ubica la lógica en la capa correcta según Arquitectura Limpia (Principio I).
El Controlador solo convierte la petición HTTP; no valida reglas de negocio.
