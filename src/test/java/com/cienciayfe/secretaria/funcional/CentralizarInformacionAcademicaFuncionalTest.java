package com.cienciayfe.secretaria.funcional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Sql(scripts = {"classpath:db/schema.sql", "classpath:db/data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db/cleanup.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
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

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    int port;

    private RequestSpecification request() {
        return given().port(port);
    }

    @Test
    @DisplayName("Dado CSV válido con header, Cuando POST a 2025-II, Entonces 201 y usuarioResponsable coincide")
    void dadoCsvValidoCuandoPostEntonces201ConUsuario() {
        request()
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
        byte[] primerArchivo = CSV_VALIDO;
        byte[] segundoArchivo = ("codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion\n"
                + "EST999;Nuevo;Apellido;Nombre Nuevo;15.0;PROMOVIDO\n").getBytes();

        request().header(HEADER, "secretaria01")
            .multiPart("archivo", "primera.csv", primerArchivo, "text/plain")
        .when().post(ENDPOINT, "2025-II").then().statusCode(HttpStatus.CREATED.value());

        request().header(HEADER, "secretaria02")
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
        request().header(HEADER, "secretaria01")
            .multiPart("archivo", "datos.csv", CSV_VALIDO, "text/plain")
        .when().post(ENDPOINT, "2025-II").then().statusCode(HttpStatus.CREATED.value());

        request()
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
        request()
        .when()
            .get(ENDPOINT, "2025-I")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("estado", equalTo("SIN_INFORMACION"));
    }
}
