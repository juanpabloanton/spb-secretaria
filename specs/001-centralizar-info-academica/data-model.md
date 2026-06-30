# Modelo de Datos: Centralizar Información Académica

**Generado**: 2026-06-28 | **Funcionalidad**: US-01

## Entidades del Dominio

### PeriodoAcademico

Representa un ciclo lectivo en el que la institución imparte clases.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | UUID | PK, NOT NULL | Identificador único del período |
| `codigo` | String (10) | UNIQUE, NOT NULL | Código del período (ej.: `2025-II`) |
| `nombre` | String (100) | NOT NULL | Nombre descriptivo (ej.: `Segundo Semestre 2025`) |
| `estado` | Enum | NOT NULL | `HABILITADO` o `CERRADO` |
| `fechaInicio` | LocalDate | NOT NULL | Fecha de inicio del período |
| `fechaFin` | LocalDate | NOT NULL | Fecha de fin del período |

**Reglas de negocio**:
- Solo los períodos con estado `HABILITADO` pueden recibir cargas de información académica.
- `fechaFin` debe ser posterior a `fechaInicio`.

---

### InformacionAcademica

Fuente central de datos académicos de todos los estudiantes para un período específico.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | UUID | PK, NOT NULL | Identificador único del registro |
| `periodoAcademicoId` | UUID | FK → PeriodoAcademico, NOT NULL, UNIQUE | Un período tiene exactamente una fuente central |
| `contenido` | byte[] (BLOB) | NOT NULL | Contenido binario del archivo CSV cargado |
| `nombreArchivo` | String (255) | NOT NULL | Nombre original del archivo |
| `tamanioBytes` | Long | NOT NULL | Tamaño del archivo en bytes |
| `estado` | Enum | NOT NULL | `DISPONIBLE` |
| `fechaCarga` | LocalDateTime | NOT NULL | Fecha y hora de la última carga |
| `usuarioResponsable` | String (100) | NOT NULL | Usuario que realizó la última carga |

**Reglas de negocio**:
- Solo puede existir un registro por período (`periodoAcademicoId` es UNIQUE).
- Si ya existe un registro para el período, se reemplaza completamente (upsert).
- El estado siempre es `DISPONIBLE` una vez que el archivo fue aceptado.
- `fechaCarga` y `usuarioResponsable` se actualizan en cada reemplazo.

---

## Diagrama de Relaciones

```
PeriodoAcademico (1) ←——— (0..1) InformacionAcademica
     |
     | Un período tiene como máximo una fuente central
     | de información académica en estado DISPONIBLE
```

---

## Estructura del CSV de Datos Académicos

El archivo aceptado como fuente central debe tener la siguiente estructura:

```
codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
```

| Columna | Tipo | Restricciones |
|---------|------|---------------|
| `codigo_estudiante` | String | NOT NULL, NOT EMPTY |
| `apellido_paterno` | String | NOT NULL, NOT EMPTY |
| `apellido_materno` | String | Puede estar vacío |
| `nombres` | String | NOT NULL, NOT EMPTY |
| `calificacion_final` | Decimal | 0.0 – 20.0 |
| `condicion` | String | `PROMOVIDO`, `REPROBADO` o `ABANDERADO` |

**Reglas de validación del archivo**:
1. Codificación: UTF-8.
2. Delimitador: punto y coma (`;`).
3. Primera fila: encabezados exactos (sin espacios adicionales).
4. Mínimo una fila de datos (no puede estar vacío).
5. Ningún campo obligatorio puede estar en blanco.
6. `calificacion_final` debe ser un número decimal válido entre 0 y 20.
7. `condicion` debe ser exactamente uno de los tres valores permitidos.

---

## Transiciones de Estado

### InformacionAcademica

```
[Sin información para el período]
         |
         | Carga exitosa de archivo
         ↓
     DISPONIBLE ←──── Reemplazo exitoso (nueva carga del período)
```

No existe estado intermedio: la operación de carga es atómica. Si falla por cualquier
razón, el estado anterior (sin información o DISPONIBLE con datos previos) se preserva.

---

## Esquema de Base de Datos (PostgreSQL)

```sql
CREATE TABLE periodo_academico (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo           VARCHAR(10)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    estado           VARCHAR(20)  NOT NULL CHECK (estado IN ('HABILITADO', 'CERRADO')),
    fecha_inicio     DATE         NOT NULL,
    fecha_fin        DATE         NOT NULL,
    CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE informacion_academica (
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
