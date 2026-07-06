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
        byte[] contenidoAnterior = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);
        LocalDateTime fechaCargaAnterior = LocalDateTime.now().minusDays(1);
        InformacionAcademica anterior = new InformacionAcademica(UUID.randomUUID(),
                periodoHabilitado.id(), contenidoAnterior, "anterior.csv", contenidoAnterior.length,
                EstadoInformacion.DISPONIBLE, fechaCargaAnterior, "secretaria01");

        byte[] contenido = CSV_VALIDO.getBytes(StandardCharsets.UTF_8);
        LocalDateTime fechaCargaNueva = LocalDateTime.now();
        InformacionAcademica actualizada = new InformacionAcademica(UUID.randomUUID(),
                periodoHabilitado.id(), contenido, "nuevo.csv", contenido.length,
                EstadoInformacion.DISPONIBLE, fechaCargaNueva, "secretaria02");

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));
        when(informacionRepositorio.save(any())).thenReturn(actualizada);

        InformacionAcademica resultado = service.cargar("2025-II", contenido, "nuevo.csv", "secretaria02");

        assertThat(resultado.nombreArchivo()).isEqualTo("nuevo.csv");
        assertThat(resultado.usuarioResponsable()).isEqualTo("secretaria02");
        assertThat(resultado.fechaCarga())
                .as("la fechaCarga debe actualizarse y no conservar la del registro reemplazado")
                .isNotEqualTo(anterior.fechaCarga())
                .isEqualTo(fechaCargaNueva);
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
    @DisplayName("Dado fila con número de columnas incorrecto, Cuando se carga, Entonces lanza FORMATO_INVALIDO")
    void dadoFilaConNumeroColumnasIncorrectoCuandoSeCargaEntoncesLanzaExcepcionFormatoInvalido() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;García;López;Ana María;18.5
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("FORMATO_INVALIDO");
    }

    @Test
    @DisplayName("Dado fila con apellido_paterno vacío, Cuando se carga, Entonces lanza CAMPO_REQUERIDO")
    void dadoFilaConApellidoPaternoVacioCuandoSeCargaEntoncesLanzaExcepcionCampoRequerido() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;;López;Ana María;18.5;PROMOVIDO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CAMPO_REQUERIDO");
    }

    @Test
    @DisplayName("Dado fila con nombres vacío, Cuando se carga, Entonces lanza CAMPO_REQUERIDO")
    void dadoFilaConNombresVacioCuandoSeCargaEntoncesLanzaExcepcionCampoRequerido() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;García;López;;18.5;PROMOVIDO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CAMPO_REQUERIDO");
    }

    @Test
    @DisplayName("Dado codigo_estudiante vacío, Cuando se carga, Entonces lanza CAMPO_REQUERIDO")
    void dadoFilaConCodigoEstudianteVacioCuandoSeCargaEntoncesLanzaExcepcionCampoRequerido() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                ;García;López;Ana María;18.5;PROMOVIDO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CAMPO_REQUERIDO");
    }

    @Test
    @DisplayName("Dado calificacion_final no numérica, Cuando se carga, Entonces lanza CALIFICACION_INVALIDA")
    void dadoCalificacionNoNumericaCuandoSeCargaEntoncesLanzaExcepcionCalificacionInvalida() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;García;López;Ana María;abc;PROMOVIDO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CALIFICACION_INVALIDA");
    }

    @Test
    @DisplayName("Dado calificacion_final fuera de rango, Cuando se carga, Entonces lanza CALIFICACION_INVALIDA")
    void dadoCalificacionFueraDeRangoCuandoSeCargaEntoncesLanzaExcepcionCalificacionInvalida() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;García;López;Ana María;20.5;PROMOVIDO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CALIFICACION_INVALIDA");
    }

    @Test
    @DisplayName("Dado condicion inválida, Cuando se carga, Entonces lanza CONDICION_INVALIDA")
    void dadoCondicionInvalidaCuandoSeCargaEntoncesLanzaExcepcionCondicionInvalida() {
        byte[] contenido = ("""
                codigo_estudiante;apellido_paterno;apellido_materno;nombres;calificacion_final;condicion
                EST001;García;López;Ana María;18.5;DESAPROBADO
                """).getBytes(StandardCharsets.UTF_8);
        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));

        assertThatThrownBy(() -> service.cargar("2025-II", contenido, "malo.csv", "secretaria01"))
                .isInstanceOf(ArchivoInvalidoException.class)
                .extracting("codigo").isEqualTo("CONDICION_INVALIDA");
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
