package com.cienciayfe.secretaria.adaptadores.entrada.rest;

import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.ErrorResponse;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleArchivoInvalido(ArchivoInvalidoException ex) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo(ex.getCodigo());
        error.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PeriodoNoHabilitadoException.class)
    public ResponseEntity<ErrorResponse> handlePeriodoNoHabilitado(PeriodoNoHabilitadoException ex) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo("PERIODO_NO_HABILITADO");
        error.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        ErrorResponse error = new ErrorResponse();
        error.setCodigo("ARCHIVO_DEMASIADO_GRANDE");
        error.setMensaje("El archivo supera el tamaño máximo permitido de 10 MB");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }
}
