package com.cienciayfe.secretaria.adaptadores.entrada.rest;

import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.InformacionAcademicaApi;
import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.CargaResponse;
import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.FuenteCentralResponse;
import com.cienciayfe.secretaria.aplicacion.puerto.entrada.CargarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.aplicacion.puerto.entrada.ConsultarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class InformacionAcademicaController implements InformacionAcademicaApi {

    private final CargarInformacionAcademicaUseCase cargarUseCase;
    private final ConsultarInformacionAcademicaUseCase consultarUseCase;

    @Override
    public ResponseEntity<CargaResponse> cargarInformacionAcademica(
            String codigoPeriodo,
            String xUsuarioResponsable,
            MultipartFile archivo) {

        if (xUsuarioResponsable == null || xUsuarioResponsable.isBlank()) {
            throw new ArchivoInvalidoException("HEADER_REQUERIDO",
                    "El header 'X-Usuario-Responsable' es obligatorio");
        }

        String contentType = archivo.getContentType();
        String nombreOriginal = archivo.getOriginalFilename();
        boolean contentTypeValido = contentType != null
                && (contentType.startsWith("text/csv") || contentType.startsWith("text/plain"));
        boolean extensionValida = nombreOriginal != null
                && nombreOriginal.toLowerCase(Locale.ROOT).endsWith(".csv");
        if (!contentTypeValido || !extensionValida) {
            throw new ArchivoInvalidoException("TIPO_INVALIDO",
                    "El archivo debe ser un CSV (text/csv o text/plain) con extensión .csv");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ArchivoInvalidoException("ERROR_LECTURA", "No se pudo leer el archivo cargado");
        }

        InformacionAcademica resultado = cargarUseCase.cargar(
                codigoPeriodo, contenido, archivo.getOriginalFilename(), xUsuarioResponsable);

        CargaResponse response = new CargaResponse();
        response.setCodigoPeriodo(codigoPeriodo);
        response.setNombreArchivo(resultado.nombreArchivo());
        response.setTamanioBytes(resultado.tamanioBytes());
        response.setFechaCarga(resultado.fechaCarga().atOffset(ZoneOffset.UTC));
        response.setUsuarioResponsable(resultado.usuarioResponsable());
        response.setMensaje("Información académica del período " + codigoPeriodo
                + " registrada exitosamente como fuente central.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<FuenteCentralResponse> consultarInformacionAcademica(String codigoPeriodo) {
        InformacionAcademica resultado = consultarUseCase.consultar(codigoPeriodo);

        FuenteCentralResponse response = new FuenteCentralResponse();
        response.setCodigoPeriodo(codigoPeriodo);

        if (resultado.estado() == EstadoInformacion.SIN_INFORMACION) {
            response.setEstado(FuenteCentralResponse.EstadoEnum.SIN_INFORMACION);
            response.setMensaje("No existe fuente central para este período. Realice una carga de información.");
        } else {
            response.setEstado(FuenteCentralResponse.EstadoEnum.DISPONIBLE);
            response.setNombreArchivo(resultado.nombreArchivo());
            response.setTamanioBytes(resultado.tamanioBytes());
            response.setFechaCarga(resultado.fechaCarga().atOffset(ZoneOffset.UTC));
            response.setUsuarioResponsable(resultado.usuarioResponsable());
            response.setMensaje("Disponible para revisión");
        }

        return ResponseEntity.ok(response);
    }
}
