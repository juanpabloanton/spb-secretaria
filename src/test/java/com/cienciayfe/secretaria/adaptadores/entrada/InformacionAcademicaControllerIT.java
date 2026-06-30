package com.cienciayfe.secretaria.adaptadores.entrada;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("InformacionAcademicaController — Integración")
class InformacionAcademicaControllerIT {

    // MockMvc uses paths relative to context-path, so no /api/v1 prefix here
    private static final String ENDPOINT = "/periodos/{periodo}/informacion-academica";
    private static final String HEADER = "X-Usuario-Responsable";
    private static final String CSV_VALIDO = """
            codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
            EST001;García;López;Ana María;18.5;PROMOVIDO
            EST002;Torres;Ruiz;Carlos;12.0;PROMOVIDO
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS periodo_academico (
                id           UUID         DEFAULT RANDOM_UUID() NOT NULL,
                codigo       VARCHAR(10)  NOT NULL UNIQUE,
                nombre       VARCHAR(100) NOT NULL,
                estado       VARCHAR(20)  NOT NULL CHECK (estado IN ('HABILITADO', 'CERRADO')),
                fecha_inicio DATE         NOT NULL,
                fecha_fin    DATE         NOT NULL,
                PRIMARY KEY (id),
                CONSTRAINT chk_fechas CHECK (fecha_fin > fecha_inicio)
            )""");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS informacion_academica (
                id                   UUID         DEFAULT RANDOM_UUID() NOT NULL,
                periodo_academico_id UUID         NOT NULL UNIQUE REFERENCES periodo_academico(id),
                contenido            BYTEA        NOT NULL,
                nombre_archivo       VARCHAR(255) NOT NULL,
                tamanio_bytes        BIGINT       NOT NULL,
                estado               VARCHAR(20)  NOT NULL DEFAULT 'DISPONIBLE',
                fecha_carga          TIMESTAMP    NOT NULL,
                usuario_responsable  VARCHAR(100) NOT NULL,
                PRIMARY KEY (id)
            )""");
        jdbc.update("DELETE FROM informacion_academica");
        jdbc.update("DELETE FROM periodo_academico");
        jdbc.update("""
            INSERT INTO periodo_academico (id, codigo, nombre, estado, fecha_inicio, fecha_fin) VALUES
              (RANDOM_UUID(), '2024-I',  'Primer Semestre 2024',  'CERRADO',    '2024-03-01', '2024-07-31'),
              (RANDOM_UUID(), '2024-II', 'Segundo Semestre 2024', 'CERRADO',    '2024-08-01', '2024-12-20'),
              (RANDOM_UUID(), '2025-I',  'Primer Semestre 2025',  'HABILITADO', '2025-03-01', '2025-07-31'),
              (RANDOM_UUID(), '2025-II', 'Segundo Semestre 2025', 'HABILITADO', '2025-08-01', '2025-12-20')
            """);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM informacion_academica");
        jdbc.update("DELETE FROM periodo_academico");
    }

    // ── HU1 Carga ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Dado CSV válido para 2025-II, Cuando POST con header, Entonces 201 con metadatos")
    void dadoCsvValidoCuandoPostEntonces201() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv",
                MediaType.TEXT_PLAIN_VALUE, CSV_VALIDO.getBytes());

        mockMvc.perform(multipart(ENDPOINT, "2025-II")
                .file(archivo)
                .header(HEADER, "secretaria01")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreArchivo").value("datos.csv"))
                .andExpect(jsonPath("$.usuarioResponsable").value("secretaria01"));
    }

    @Test
    @DisplayName("Dado CSV con encabezados incorrectos, Cuando POST, Entonces 400")
    void dadoCsvInvalidoCuandoPostEntonces400() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "malo.csv",
                MediaType.TEXT_PLAIN_VALUE, "encabezado_malo;otro\nval1;val2\n".getBytes());

        mockMvc.perform(multipart(ENDPOINT, "2025-II")
                .file(archivo)
                .header(HEADER, "secretaria01")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENCABEZADOS_INVALIDOS"));
    }

    @Test
    @DisplayName("Dado período CERRADO, Cuando POST, Entonces 404")
    void dadoPeriodoCerradoCuandoPostEntonces404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv",
                MediaType.TEXT_PLAIN_VALUE, CSV_VALIDO.getBytes());

        mockMvc.perform(multipart(ENDPOINT, "2024-I")
                .file(archivo)
                .header(HEADER, "secretaria01")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Dado período inexistente, Cuando POST, Entonces 404")
    void dadoPeriodoInexistenteCuandoPostEntonces404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv",
                MediaType.TEXT_PLAIN_VALUE, CSV_VALIDO.getBytes());

        mockMvc.perform(multipart(ENDPOINT, "9999-X")
                .file(archivo)
                .header(HEADER, "secretaria01")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isNotFound());
    }

    // ── HU2 Consulta ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Dado período 2025-I sin información, Cuando GET, Entonces 200 con SIN_INFORMACION")
    void dadoPeriodoSinInformacionCuandoGetEntonces200SinInformacion() throws Exception {
        mockMvc.perform(get(ENDPOINT, "2025-I")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SIN_INFORMACION"));
    }

    @Test
    @DisplayName("Dado período inexistente, Cuando GET, Entonces 404")
    void dadoPeriodoInexistenteCuandoGetEntonces404() throws Exception {
        mockMvc.perform(get(ENDPOINT, "9999-X")
                .with(user("secretaria").roles("SECRETARIA")))
                .andExpect(status().isNotFound());
    }
}
