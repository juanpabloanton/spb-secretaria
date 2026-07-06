package com.cienciayfe.secretaria.adaptadores.entrada.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.CargaResponse;
import com.cienciayfe.secretaria.adaptadores.entrada.rest.generated.model.FuenteCentralResponse;
import com.cienciayfe.secretaria.aplicacion.puerto.entrada.CargarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.aplicacion.puerto.entrada.ConsultarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class InformacionAcademicaControllerTest {

    private CargarInformacionAcademicaUseCase cargarUseCase;
    private ConsultarInformacionAcademicaUseCase consultarUseCase;
    private InformacionAcademicaController controller;

    @BeforeEach
    void setUp() {
        cargarUseCase = mock(CargarInformacionAcademicaUseCase.class);
        consultarUseCase = mock(ConsultarInformacionAcademicaUseCase.class);
        controller = new InformacionAcademicaController(cargarUseCase, consultarUseCase);
    }

    @Test
    void cargarRetornaCreadoConMetadatos() {
        byte[] contenido = {1, 2};
        MockMultipartFile archivo = new MockMultipartFile("archivo", "datos.csv", "text/plain", contenido);
        InformacionAcademica informacion = informacion(EstadoInformacion.DISPONIBLE, contenido);
        when(cargarUseCase.cargar("2025-II", contenido, "datos.csv", "usuario"))
                .thenReturn(informacion);

        ResponseEntity<CargaResponse> respuesta = controller.cargarInformacionAcademica(
                "2025-II", "usuario", archivo);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getNombreArchivo()).isEqualTo("datos.csv");
        assertThat(respuesta.getBody().getUsuarioResponsable()).isEqualTo("usuario");
    }

    @Test
    void cargarPropagaErrorDeLectura() throws IOException {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.getContentType()).thenReturn("text/plain");
        when(archivo.getOriginalFilename()).thenReturn("datos.csv");
        when(archivo.getBytes()).thenThrow(new IOException("lectura"));

        assertThatThrownBy(() -> controller.cargarInformacionAcademica("2025-II", "usuario", archivo))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("ERROR_LECTURA");
    }

    @Test
    void consultarRetornaDisponible() {
        when(consultarUseCase.consultar("2025-II"))
                .thenReturn(informacion(EstadoInformacion.DISPONIBLE, new byte[] {1}));

        ResponseEntity<FuenteCentralResponse> respuesta =
                controller.consultarInformacionAcademica("2025-II");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getEstado())
                .isEqualTo(FuenteCentralResponse.EstadoEnum.DISPONIBLE);
        assertThat(respuesta.getBody().getNombreArchivo()).isEqualTo("datos.csv");
    }

    @Test
    void consultarRetornaSinInformacion() {
        when(consultarUseCase.consultar("2025-I"))
                .thenReturn(informacion(EstadoInformacion.SIN_INFORMACION, null));

        ResponseEntity<FuenteCentralResponse> respuesta =
                controller.consultarInformacionAcademica("2025-I");

        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getEstado())
                .isEqualTo(FuenteCentralResponse.EstadoEnum.SIN_INFORMACION);
        assertThat(respuesta.getBody().getNombreArchivo()).isNull();
    }

    private InformacionAcademica informacion(EstadoInformacion estado, byte[] contenido) {
        return new InformacionAcademica(UUID.randomUUID(), UUID.randomUUID(), contenido,
                estado == EstadoInformacion.DISPONIBLE ? "datos.csv" : null,
                contenido == null ? 0 : contenido.length, estado,
                estado == EstadoInformacion.DISPONIBLE ? LocalDateTime.now() : null,
                estado == EstadoInformacion.DISPONIBLE ? "usuario" : null);
    }
}
