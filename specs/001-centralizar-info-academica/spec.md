# Especificación de Funcionalidad: Centralizar Información Académica

**Rama de funcionalidad**: `001-centralizar-info-academica`

**Creado**: 2026-06-28

**Estado**: Borrador

**Épica**: E-01 | **Historia**: US-01 | **Puntos**: 5

**Entrada**: Descripción del usuario: "Como secretaria académica, quiero cargar la información académica en una única fuente para el período seleccionado, para preparar cuadros finales, promociones y abanderados sin trabajar con archivos dispersos."

## Escenarios de Usuario y Pruebas *(obligatorio)*

<!--
  ALINEACIÓN CON LA CONSTITUCIÓN (Principio II — Estrategia de Pruebas con BDD):
  Todos los escenarios expresados en notación BDD: Dado / Cuando / Entonces.
  Cada escenario se mapea a prueba Unitaria + Integración + Funcional.
-->

### Historia de Usuario 1 — Carga de Información Académica para un Período (Prioridad: P1)

La secretaria académica selecciona un período lectivo y carga un archivo con la información
académica de todos los estudiantes de ese período. Una vez cargado, el archivo queda
registrado como la fuente central de información oficial para ese período, reemplazando
cualquier versión anterior si existiera.

**Por qué esta prioridad**: Es el flujo central que habilita todas las demás funcionalidades
del sistema (cuadros finales, promociones y abanderados). Sin la carga, no hay información
centralizada que procesar.

**Prueba independiente**: Puede verificarse cargando un archivo válido para un período nuevo
y confirmando que el sistema lo registra y lo reporta como fuente vigente.

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

---

### Historia de Usuario 2 — Consulta de la Fuente Central de un Período (Prioridad: P2)

La secretaria puede consultar en cualquier momento qué información académica está registrada
como fuente central para un período dado, visualizando su estado, fecha de carga y
la posibilidad de revisarla antes de generar cuadros finales o listados de promoción.

**Por qué esta prioridad**: Permite a la secretaria verificar que la información centralizada
es correcta antes de usarla para procesos críticos como cuadros finales y abanderados.

**Prueba independiente**: Puede verificarse consultando un período con información ya cargada
y confirmando que se muestra la versión vigente con sus metadatos.

**Escenarios de Aceptación**:

1. **Dado** que existe información académica cargada para el período 2025-II,
   **Cuando** la secretaria consulta el período 2025-II,
   **Entonces** el sistema muestra la versión vigente de la información con la fecha de carga
   y el estado "Disponible para revisión".

2. **Dado** que no existe información cargada para el período 2025-I,
   **Cuando** la secretaria consulta el período 2025-I,
   **Entonces** el sistema informa que no hay fuente central registrada para ese período
   y sugiere realizar una carga.

---

### Casos Borde

- ¿Qué ocurre si el archivo supera el tamaño máximo permitido?
  → El sistema informa el límite de tamaño y rechaza la carga sin guardar datos parciales.
- ¿Qué ocurre si se pierde la conexión durante la carga?
  → La carga se considera fallida; la fuente central anterior permanece intacta.
- ¿Qué ocurre si se selecciona un período que no existe en el sistema?
  → El sistema impide la selección y muestra solo los períodos habilitados.

## Requisitos *(obligatorio)*

### Requisitos Funcionales

- **RF-001**: El sistema DEBE permitir a la secretaria académica seleccionar un período
  académico habilitado antes de realizar la carga de información.
- **RF-002**: El sistema DEBE permitir cargar un archivo de datos académicos para el
  período seleccionado.
- **RF-003**: El sistema DEBE registrar el archivo cargado como la fuente central oficial
  de información académica para ese período.
- **RF-004**: El sistema DEBE reemplazar la fuente central existente de un período cuando
  se carga una versión nueva, conservando los metadatos de la actualización
  (fecha, hora, usuario que cargó).
- **RF-005**: El sistema DEBE validar el formato y la integridad del archivo antes de
  aceptarlo como fuente central.
- **RF-006**: El sistema DEBE rechazar archivos con formato inválido o datos incompletos,
  informando el motivo específico del rechazo.
- **RF-007**: El sistema DEBE permitir a la secretaria consultar la fuente central vigente
  de cualquier período habilitado.
- **RF-008**: El sistema DEBE mostrar el estado de la fuente central (sin información /
  disponible para revisión) y los metadatos de la última carga.

### Entidades Clave

- **Período Académico**: Representa un ciclo lectivo identificado (ej.: 2025-I, 2025-II).
  Atributos clave: código, nombre, estado (habilitado/cerrado).
- **Información Académica Centralizada**: El conjunto de datos académicos de todos los
  estudiantes para un período dado, registrado como fuente oficial única.
  Atributos clave: período asociado, fecha de carga, usuario responsable, estado,
  contenido de los datos.
- **Secretaria Académica**: Usuaria del sistema con permisos para cargar y consultar
  la información académica centralizada.

## Criterios de Éxito *(obligatorio)*

### Resultados Medibles

- **CE-001**: La secretaria puede completar la carga de información académica para un
  período en menos de 3 minutos desde que selecciona el período hasta recibir la
  confirmación de éxito.
- **CE-002**: El 100% de los datos del archivo cargado quedan disponibles para revisión
  inmediatamente después de recibir la confirmación de carga exitosa.
- **CE-003**: La secretaria puede consultar el estado de la fuente central de cualquier
  período en menos de 5 segundos.
- **CE-004**: La tasa de errores en cargas con archivos válidos es menor al 1%
  (los rechazos de archivos inválidos no cuentan como error del sistema).
- **CE-005**: La secretaria elimina por completo la necesidad de consultar archivos
  dispersos para preparar cuadros finales, promociones y abanderados del período cargado.

## Supuestos

- La secretaria académica ya está autenticada en el sistema antes de acceder a esta funcionalidad.
- El sistema ya cuenta con una lista de períodos académicos habilitados previamente configurada.
- El formato del archivo de datos académicos es predefinido, documentado y conocido por la
  secretaria; el formato exacto se definirá en la fase de planificación.
- Cada período académico tiene exactamente una fuente central de información activa a la vez.
- El sistema conserva solo la versión más reciente como fuente central; el historial de versiones
  anteriores queda fuera del alcance de esta historia.
- La secretaria tiene permisos exclusivos para cargar y actualizar la fuente central;
  otros roles solo pueden consultar.
