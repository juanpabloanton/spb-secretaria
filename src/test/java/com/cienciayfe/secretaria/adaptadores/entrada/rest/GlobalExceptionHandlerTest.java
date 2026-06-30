package com.cienciayfe.secretaria.adaptadores.entrada.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.ErrorResponse;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejaArchivoInvalido() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleArchivoInvalido(
                new ArchivoInvalidoException("ARCHIVO_INVALIDO", "archivo incorrecto"));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo("ARCHIVO_INVALIDO");
        assertThat(respuesta.getBody().getMensaje()).isEqualTo("archivo incorrecto");
    }

    @Test
    void manejaPeriodoNoHabilitado() {
        PeriodoNoHabilitadoException excepcion =
                new PeriodoNoHabilitadoException("2025-I", "periodo cerrado");
        ResponseEntity<ErrorResponse> respuesta = handler.handlePeriodoNoHabilitado(excepcion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo("PERIODO_NO_HABILITADO");
        assertThat(excepcion.getCodigoPeriodo()).isEqualTo("2025-I");
    }

    @Test
    void manejaArchivoDemasiadoGrande() {
        ResponseEntity<ErrorResponse> respuesta = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(Long.MAX_VALUE));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getCodigo()).isEqualTo("ARCHIVO_DEMASIADO_GRANDE");
    }
}
