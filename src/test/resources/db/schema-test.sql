CREATE TABLE IF NOT EXISTS periodo_academico (
    id               UUID         DEFAULT RANDOM_UUID() NOT NULL,
    codigo           VARCHAR(10)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    estado           VARCHAR(20)  NOT NULL CHECK (estado IN ('HABILITADO', 'CERRADO')),
    fecha_inicio     DATE         NOT NULL,
    fecha_fin        DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio)
);

CREATE TABLE IF NOT EXISTS informacion_academica (
    id                      UUID         DEFAULT RANDOM_UUID() NOT NULL,
    periodo_academico_id    UUID         NOT NULL UNIQUE REFERENCES periodo_academico(id),
    contenido               BYTEA        NOT NULL,
    nombre_archivo          VARCHAR(255) NOT NULL,
    tamanio_bytes           BIGINT       NOT NULL,
    estado                  VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
    fecha_carga             TIMESTAMP    NOT NULL,
    usuario_responsable     VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);
