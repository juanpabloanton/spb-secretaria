# Quickstart: Centralizar Información Académica

**Funcionalidad**: US-01 · Centralizar Información Académica
**Rama**: `001-centralizar-info-academica`

## Prerrequisitos

- Java 17 instalado (`java -version`)
- Maven 3.9+ instalado (`mvn -version`)
- Docker disponible (para Testcontainers en pruebas de integración)
- PostgreSQL 15 ejecutándose localmente en `localhost:5432` (o usar Docker Compose)

## 1. Configurar la base de datos local

```bash
# Opción A: PostgreSQL con Docker
docker run -d \
  --name secretaria-db \
  -e POSTGRES_DB=secretaria \
  -e POSTGRES_USER=secretaria \
  -e POSTGRES_PASSWORD=secretaria \
  -p 5432:5432 \
  postgres:15-alpine

# Opción B: Base de datos existente — actualizar application.yml
```

## 2. Configurar `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/secretaria
    username: secretaria
    password: secretaria
  jpa:
    hibernate:
      ddl-auto: validate
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

## 3. Generar stubs desde el contrato OpenAPI

```bash
# Genera código en target/generated-sources/openapi/
mvn generate-sources
```

Los stubs generados implementan las interfaces del contrato `secretaria-api-v1.yaml`.
**No modificar** los archivos en `target/generated-sources/openapi/`.

## 4. Compilar y ejecutar pruebas

```bash
# Ejecutar todas las pruebas con reporte de cobertura JaCoCo
mvn verify

# Ver reporte HTML de cobertura
open target/site/jacoco/index.html
```

La compilación fallará si la cobertura está por debajo de los umbrales configurados
(> 80% por clase · ≥ 80% global).

## 5. Levantar el servicio

```bash
mvn spring-boot:run
```

El servicio queda disponible en `http://localhost:8080/api/v1`.

## 6. Verificar los endpoints manualmente

### Cargar información académica

```bash
curl -X POST http://localhost:8080/api/v1/periodos/2025-II/informacion-academica \
  -F "archivo=@datos_academicos_2025II.csv" \
  -H "Content-Type: multipart/form-data" \
  -H "X-Usuario-Responsable: secretaria01"
```

**Ejemplo de archivo CSV válido** (`datos_academicos_2025II.csv`):
```
codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
EST001;García;López;Ana María;18.5;PROMOVIDO
EST002;Torres;Ruiz;Carlos;12.0;PROMOVIDO
EST003;Mendoza;Silva;Lucía;8.5;REPROBADO
EST004;Flores;Castro;Diego;20.0;ABANDERADO
```

**Respuesta esperada (201 Created)**:
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

**Respuesta esperada (200 OK)**:
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

## 7. Validación de la historia de usuario

Para confirmar que la historia US-01 está completamente implementada:

- [ ] **HU1 — Escenario 1**: Cargar archivo válido → respuesta 201 con metadatos
- [ ] **HU1 — Escenario 2**: Cargar archivo para período ya cargado → 201, reemplaza la versión anterior
- [ ] **HU1 — Escenario 3**: Cargar archivo inválido → 400 con código y mensaje descriptivo
- [ ] **HU2 — Escenario 1**: Consultar período con información → 200, estado `DISPONIBLE`
- [ ] **HU2 — Escenario 2**: Consultar período sin información → 200, estado `SIN_INFORMACION`
- [ ] **CE-001**: Medir tiempo total de carga del Escenario 1 con `time curl -X POST ... -F archivo=@ ... -H "X-Usuario-Responsable: secretaria01"` — debe completarse en < 3 min
- [ ] **CE-003**: Medir tiempo de respuesta de consulta con `curl -w "%{time_total}\n" -o /dev/null -s http://localhost:8080/api/v1/periodos/2025-II/informacion-academica` — debe ser < 5 s
- [ ] **Caso borde**: Archivo > 10 MB → 413
- [ ] **Caso borde**: Período inexistente → 404
- [ ] **Cobertura JaCoCo**: `mvn verify` pasa sin errores de umbral

## Estructura de paquetes de referencia

```
com.maestriasoft.secretaria
├── dominio            ← Sin imports de Spring/JPA
├── aplicacion         ← Sin imports de Spring/JPA
├── adaptadores        ← Spring MVC, JPA Repositories
└── infraestructura    ← Configuración Spring
```
