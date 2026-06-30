package com.cienciayfe.secretaria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cienciayfe.secretaria.aplicacion.puerto.salida.InformacionAcademicaRepositorio;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.PeriodoAcademicoRepositorio;
import com.cienciayfe.secretaria.aplicacion.servicio.ConsultarInformacionAcademicaService;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico.EstadoPeriodo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsultarInformacionAcademicaService")
class ConsultarInformacionAcademicaServiceTest {

    @Mock private PeriodoAcademicoRepositorio periodoRepositorio;
    @Mock private InformacionAcademicaRepositorio informacionRepositorio;
    @InjectMocks private ConsultarInformacionAcademicaService service;

    private final PeriodoAcademico periodoHabilitado = new PeriodoAcademico(
            UUID.randomUUID(), "2025-II", "Segundo Semestre 2025", EstadoPeriodo.HABILITADO,
            LocalDate.parse("2025-08-01"), LocalDate.parse("2025-12-20"));

    @Test
    @DisplayName("Dado período con información cargada, Cuando se consulta, Entonces retorna estado DISPONIBLE con metadatos")
    void dadoPeriodoConInformacionCuandoSeConsultaEntoncesRetornaDisponible() {
        byte[] contenido = "datos".getBytes();
        InformacionAcademica info = new InformacionAcademica(UUID.randomUUID(),
                periodoHabilitado.id(), contenido, "datos.csv", contenido.length,
                EstadoInformacion.DISPONIBLE, LocalDateTime.now(), "secretaria01");

        when(periodoRepositorio.findByCodigo("2025-II")).thenReturn(Optional.of(periodoHabilitado));
        when(informacionRepositorio.findByPeriodoAcademicoId(periodoHabilitado.id()))
                .thenReturn(Optional.of(info));

        InformacionAcademica resultado = service.consultar("2025-II");

        assertThat(resultado.estado()).isEqualTo(EstadoInformacion.DISPONIBLE);
        assertThat(resultado.nombreArchivo()).isEqualTo("datos.csv");
        assertThat(resultado.usuarioResponsable()).isEqualTo("secretaria01");
    }

    @Test
    @DisplayName("Dado período habilitado sin información, Cuando se consulta, Entonces retorna estado SIN_INFORMACION")
    void dadoPeriodoSinInformacionCuandoSeConsultaEntoncesRetornaSinInformacion() {
        PeriodoAcademico periodo2025I = new PeriodoAcademico(UUID.randomUUID(), "2025-I",
                "Primer Semestre 2025", EstadoPeriodo.HABILITADO,
                LocalDate.parse("2025-03-01"), LocalDate.parse("2025-07-31"));

        when(periodoRepositorio.findByCodigo("2025-I")).thenReturn(Optional.of(periodo2025I));
        when(informacionRepositorio.findByPeriodoAcademicoId(any(UUID.class)))
                .thenReturn(Optional.empty());

        InformacionAcademica resultado = service.consultar("2025-I");

        assertThat(resultado.estado()).isEqualTo(EstadoInformacion.SIN_INFORMACION);
        assertThat(resultado.nombreArchivo()).isNull();
    }

    @Test
    @DisplayName("Dado período inexistente, Cuando se consulta, Entonces lanza PeriodoNoHabilitadoException")
    void dadoPeriodoInexistenteCuandoSeConsultaEntoncesLanzaExcepcion() {
        when(periodoRepositorio.findByCodigo("9999-X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultar("9999-X"))
                .isInstanceOf(PeriodoNoHabilitadoException.class)
                .hasMessageContaining("no existe");
    }
}
