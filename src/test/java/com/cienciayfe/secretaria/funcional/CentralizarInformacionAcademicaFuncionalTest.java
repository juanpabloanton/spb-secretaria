package com.cienciayfe.secretaria.funcional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Centralizar Información Académica — Funcional")
class CentralizarInformacionAcademicaFuncionalTest {

    private static final String ENDPOINT = "/api/v1/periodos/{periodo}/informacion-academica";
    private static final String HEADER = "X-Usuario-Responsable";
    private static final byte[] CSV_VALIDO = """
            codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
            EST001;García;López;Ana María;18.5;PROMOVIDO
            EST002;Torres;Ruiz;Carlos;12.0;PROMOVIDO
            EST003;Mendoza;Silva;Lucía;8.5;REPROBADO
            EST004;Flores;Castro;Diego;20.0;ABANDERADO
            """.getBytes();
    private static final int TAMANIO_ARCHIVO_DEMASIADO_GRANDE_BYTES = 11 * 1024 * 1024;

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS periodo_academico (
                id           UUID        DEFAULT RANDOM_UUID() NOT NULL,
                codigo       VARCHAR(10) NOT NULL UNIQUE,
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

    private RequestSpecification requestConAuth() {
        return given().port(port).auth().preemptive().basic("secretaria", "test-secretaria-password");
    }

    @Test
    @DisplayName("Dado CSV válido con header, Cuando POST a 2025-II, Entonces 201 y usuarioResponsable coincide")
    void dadoCsvValidoCuandoPostEntonces201ConUsuario() {
        requestConAuth()
            .header(HEADER, "secretaria01")
            .multiPart("archivo", "datos_academicos_2025II.csv", CSV_VALIDO, "text/plain")
        .when()
            .post(ENDPOINT, "2025-II")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("usuarioResponsable", equalTo("secretaria01"))
            .body("nombreArchivo", equalTo("datos_academicos_2025II.csv"))
            .body("tamanioBytes", notNullValue());
    }

    @Test
    @DisplayName("Dado período con carga previa, Cuando POST con CSV nuevo, Entonces 201 y reemplaza")
    void dadoCargaPreviaCuandoPostNuevoCsvEntonces201Reemplaza() {
        byte[] segundoArchivo = ("codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion\n"
                + "EST999;Nuevo;Apellido;Nombre Nuevo;15.0;PROMOVIDO\n").getBytes();

        requestConAuth().header(HEADER, "secretaria01")
            .multiPart("archivo", "primera.csv", CSV_VALIDO, "text/plain")
        .when().post(ENDPOINT, "2025-II").then().statusCode(HttpStatus.CREATED.value());

        requestConAuth().header(HEADER, "secretaria02")
            .multiPart("archivo", "segunda.csv", segundoArchivo, "text/plain")
        .when()
            .post(ENDPOINT, "2025-II")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("usuarioResponsable", equalTo("secretaria02"))
            .body("nombreArchivo", equalTo("segunda.csv"));
    }

    @Test
    @DisplayName("Dado período 2025-II con información, Cuando GET, Entonces 200 DISPONIBLE")
    void dadoPeriodoConInformacionCuandoGetEntonces200Disponible() {
        requestConAuth().header(HEADER, "secretaria01")
            .multiPart("archivo", "datos.csv", CSV_VALIDO, "text/plain")
        .when().post(ENDPOINT, "2025-II").then().statusCode(HttpStatus.CREATED.value());

        requestConAuth()
        .when()
            .get(ENDPOINT, "2025-II")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("estado", equalTo("DISPONIBLE"))
            .body("usuarioResponsable", equalTo("secretaria01"));
    }

    @Test
    @DisplayName("Dado período 2025-I sin información, Cuando GET, Entonces 200 SIN_INFORMACION")
    void dadoPeriodoSinInformacionCuandoGetEntonces200SinInformacion() {
        requestConAuth()
        .when()
            .get(ENDPOINT, "2025-I")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("estado", equalTo("SIN_INFORMACION"));
    }

    // ── Caso borde: tamaño máximo excedido ──────────────────────────────────
    @Test
    @DisplayName("Dado archivo que excede el tamaño máximo permitido, Cuando POST, Entonces 413 sin guardar datos")
    void dadoArchivoDemasiadoGrandeCuandoPostEntonces413() {
        byte[] archivoGrande = new byte[TAMANIO_ARCHIVO_DEMASIADO_GRANDE_BYTES];

        requestConAuth().header(HEADER, "secretaria01")
            .multiPart("archivo", "grande.csv", archivoGrande, "text/plain")
        .when()
            .post(ENDPOINT, "2025-II")
        .then()
            .statusCode(HttpStatus.PAYLOAD_TOO_LARGE.value())
            .body("codigo", equalTo("ARCHIVO_DEMASIADO_GRANDE"));

        requestConAuth()
        .when()
            .get(ENDPOINT, "2025-II")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("estado", equalTo("SIN_INFORMACION"));
    }
}
