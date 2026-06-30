package com.cienciayfe.secretaria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataAccessResourceFailureException;

import com.cienciayfe.secretaria.aplicacion.puerto.salida.InformacionAcademicaRepositorio;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.PeriodoAcademicoRepositorio;
import com.cienciayfe.secretaria.aplicacion.servicio.CargarInformacionAcademicaService;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico.EstadoPeriodo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CargarInformacionAcademicaService")
class CargarInformacionAcademicaServiceTest {

    private static final String CSV_VALIDO = """
            codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
            EST001;García;López;Ana María;18.5;PROMOVIDO
            """;

    @Mock private PeriodoAcademicoRepositorio periodoRepositorio;
    @Mock private InformacionAcademicaRepositorio informacionRepositorio;
    @InjectMocks private CargarInformacionAcademicaService service;

    private PeriodoAcademico periodoHabilitado;

    @BeforeEach
    void setUp() {
        periodoHabilitado = new PeriodoAcademico(UUID.randomUUID(), "2025-II",
                "Segundo Semestre 2025", EstadoPeriodo.HABILITADO,
                LocalDate.parse("2025-08-01"), LocalDate.parse("2025-12-20"));
    }

    @Test
    @DisplayName("Dado un CSV válido para período habilitado, Cuando se carga, Entonces se registra exitosamente")
    void dadoCsvValidoCuandoSeCargaEntoncesSeRegistra() {
        byte[] contenido = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);
        InformacionAcademica guardada = new InformacionAcademica(UUID.randomUUID(),
                periodoHabilitado.id(), contenido, "datos.csv", contenido.length,
                EstadoInformacion.DISPONIBLE, LocalDateTime.now(), "secretaria01");

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));
        when(informacionRepositorio.save(any())).thenReturn(guardada);

        InformacionAcademica resultado = service.cargar("2025-II", contenido, "datos.csv", "secretaria01");

        assertThat(resultado.estado()).isEqualTo(EstadoInformacion.DISPONIBLE);
        assertThat(resultado.nombreArchivo()).isEqualTo("datos.csv");
        assertThat(resultado.usuarioResponsable()).isEqualTo("secretaria01");
    }

    @Test
    @DisplayName("Dado período con carga previa, Cuando se carga nuevo CSV, Entonces reemplaza la fuente anterior")
    void dadoPeriodoConCargaPreviaCuandoSeCargaNuevoEntoncesReemplaza() {
        byte[] contenido = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);
        InformacionAcademica actualizada = new InformacionAcademica(UUID.randomUUID(),
                periodoHabilitado.id(), contenido, "nuevo.csv", contenido.length,
                EstadoInformacion.DISPONIBLE, LocalDateTime.now(), "secretaria02");

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));
        when(informacionRepositorio.save(any())).thenReturn(actualizada);

        InformacionAcademica resultado = service.cargar("2025-II", contenido, "nuevo.csv", "secretaria02");

        assertThat(resultado.nombreArchivo()).isEqualTo("nuevo.csv");
        assertThat(resultado.usuarioResponsable()).isEqualTo("secretaria02");
    }

    @Test
    @DisplayName("Dado CSV con encabezados inválidos, Cuando se carga, Entonces lanza ArchivoInvalidoException")
    void dadoCsvEncabezadosInvalidosCuandoSeCargaEntoncesLanzaExcepcion() {
        byte[] contenido = "encabezado_malo;otro\nval1;val2\n".getBytes(StandardCharsets.UTF_8);

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .hasMessageContaining("encabezados");
    }

    @Test
    @DisplayName("Dado archivo vacío, Cuando se carga, Entonces lanza ArchivoInvalidoException con ARCHIVO_VACIO")
    void dadoArchivoVacioCuandoSeCargaEntoncesLanzaExcepcionVacio() {
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", new byte[0], "vacio.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("ARCHIVO_VACIO");
    }

    @Test
    void dadoCsvSinFilasEntoncesLanzaExcepcionVacio() {
        byte[] contenido = ("codigo_estudiante;apellido_paterno;apellido_materno;"
                + "nombres;calificacion_final;condicion\n").getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "vacio.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("ARCHIVO_VACIO");
    }

    @Test
    @DisplayName("Dado período CERRADO, Cuando se intenta cargar, Entonces lanza PeriodoNoHabilitadoException")
    void dadoPeriodoCerradoCuandoSeCargaEntoncesLanzaExcepcion() {
        PeriodoAcademico periodoCerrado = new PeriodoAcademico(UUID.randomUUID(), "2024-I",
                "Primer Semestre 2024", EstadoPeriodo.CERRADO,
                LocalDate.parse("2024-03-01"), LocalDate.parse("2024-07-31"));
        byte[] contenido = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);

        when(periodoRepositorio.findByCodigo("2024-I")).thenReturn(Optional.of(periodoCerrado));

        assertThatThrownBy(() -> service.cargar("2024-I", contenido, "datos.csv", "secretaria01"))
                .isInstanceOf(PeriodoNoHabilitadoException.class)
                .hasMessageContaining("cerrado");
    }

    @Test
    void dadoPeriodoInexistenteEntoncesLanzaExcepcion() {
        when(periodoRepositorio.findByCodigo("9999-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cargar("9999-X", new byte[] {1}, "datos.csv", "usuario"))
                .isInstanceOf(PeriodoNoHabilitadoException.class)
                .satisfies(error -> assertThat(((PeriodoNoHabilitadoException) error).getCodigoPeriodo())
                        .isEqualTo("9999-X"));
    }

    @Test
    @DisplayName("Dado fallo de conexión durante el guardado, Cuando se carga, Entonces la excepción se propaga")
    void dadoFalloConexionDuranteGuardadoCuandoSeCargaEntoncesExcepcionPropaga() {
        byte[] contenido = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));
        when(informacionRepositorio.save(any()))
                .thenThrow(new DataAccessResourceFailureException("Conexión perdida con la base de datos"));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "datos.csv", "secretaria01"))
                .isInstanceOf(DataAccessResourceFailureException.class)
                .hasMessageContaining("Conexión perdida");
    }
}
