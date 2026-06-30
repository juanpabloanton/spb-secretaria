package com.cienciayfe.secretaria.adaptadores.entrada;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:db/schema.sql", "classpath:db/data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@DisplayName("InformacionAcademicaController — Integración")
class InformacionAcademicaControllerIT {

    private static final String ENDPOINT = "/api/v1/periodos/{periodo}/informacion-academica";
    private static final String HEADER = "X-Usuario-Responsable";
    private static final String CSV_VALIDO = """
            codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
            EST001;García;López;Ana María;18.5;PROMOVIDO
            EST002;Torres;Ruiz;Carlos;12.0;PROMOVIDO
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;

    // ── HU1 Carga ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("Dado CSV válido para 2025-II, Cuando POST con header, Entonces 201 con metadatos")
    void dadoCsvValidoCuandoPostEntonces201() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv",
                MediaType.TEXT_PLAIN_VALUE, CSV_VALIDO.getBytes());

        mockMvc.perform(multipart(ENDPOINT, "2025-II")
                .file(archivo)
                .header(HEADER, "secretaria01"))
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
                .header(HEADER, "secretaria01"))
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
                .header(HEADER, "secretaria01"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Dado período inexistente, Cuando POST, Entonces 404")
    void dadoPeriodoInexistenteCuandoPostEntonces404() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv",
                MediaType.TEXT_PLAIN_VALUE, CSV_VALIDO.getBytes());

        mockMvc.perform(multipart(ENDPOINT, "9999-X")
                .file(archivo)
                .header(HEADER, "secretaria01"))
                .andExpect(status().isNotFound());
    }

    // ── HU2 Consulta ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Dado período 2025-I sin información, Cuando GET, Entonces 200 con SIN_INFORMACION")
    void dadoPeriodoSinInformacionCuandoGetEntonces200SinInformacion() throws Exception {
        mockMvc.perform(get(ENDPOINT, "2025-I"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SIN_INFORMACION"));
    }

    @Test
    @DisplayName("Dado período inexistente, Cuando GET, Entonces 404")
    void dadoPeriodoInexistenteCuandoGetEntonces404() throws Exception {
        mockMvc.perform(get(ENDPOINT, "9999-X"))
                .andExpect(status().isNotFound());
    }
}
