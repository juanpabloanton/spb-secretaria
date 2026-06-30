package com.cienciayfe.secretaria.adaptadores.salida.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.InformacionAcademicaEntity;
import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.PeriodoAcademicoEntity;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico.EstadoPeriodo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositorioAdaptersTest {

    @Test
    void periodoBuscaYConvierteEntidad() {
        PeriodoAcademicoJpaRepositorio jpa = mock(PeriodoAcademicoJpaRepositorio.class);
        PeriodoAcademicoEntity entity = periodoEntity();
        when(jpa.findByCodigo("2025-II")).thenReturn(Optional.of(entity));
        PeriodoAcademicoRepositorioAdapter adapter = new PeriodoAcademicoRepositorioAdapter(jpa);

        Optional<PeriodoAcademico> resultado = adapter.findByCodigo("2025-II");

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().codigo()).isEqualTo("2025-II");
        assertThat(resultado.orElseThrow().estado()).isEqualTo(EstadoPeriodo.HABILITADO);
        assertThat(adapter.findByCodigo("inexistente")).isEmpty();
    }

    @Test
    void informacionCreaNuevaEntidad() {
        InformacionAcademicaJpaRepositorio jpa = mock(InformacionAcademicaJpaRepositorio.class);
        InformacionAcademica dominio = informacion();
        when(jpa.findByPeriodoAcademicoId(dominio.periodoAcademicoId()))
                .thenReturn(Optional.empty());
        when(jpa.save(any())).thenAnswer(invocacion -> {
            InformacionAcademicaEntity entity = invocacion.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        InformacionAcademicaRepositorioAdapter adapter = new InformacionAcademicaRepositorioAdapter(jpa);

        InformacionAcademica resultado = adapter.save(dominio);

        assertThat(resultado.nombreArchivo()).isEqualTo("datos.csv");
        assertThat(resultado.estado()).isEqualTo(EstadoInformacion.DISPONIBLE);
    }

    @Test
    void informacionActualizaYConsultaEntidadExistente() {
        InformacionAcademicaJpaRepositorio jpa = mock(InformacionAcademicaJpaRepositorio.class);
        InformacionAcademica dominio = informacion();
        InformacionAcademicaEntity existente = informacionEntity(dominio);
        existente.setNombreArchivo("anterior.csv");
        when(jpa.findByPeriodoAcademicoId(dominio.periodoAcademicoId()))
                .thenReturn(Optional.of(existente));
        when(jpa.save(existente)).thenReturn(existente);
        InformacionAcademicaRepositorioAdapter adapter = new InformacionAcademicaRepositorioAdapter(jpa);

        InformacionAcademica resultado = adapter.save(dominio);
        Optional<InformacionAcademica> consulta =
                adapter.findByPeriodoAcademicoId(dominio.periodoAcademicoId());

        assertThat(resultado.nombreArchivo()).isEqualTo("datos.csv");
        assertThat(consulta).isPresent();
        assertThat(consulta.orElseThrow().usuarioResponsable()).isEqualTo("usuario");
    }

    private PeriodoAcademicoEntity periodoEntity() {
        PeriodoAcademicoEntity entity = new PeriodoAcademicoEntity();
        entity.setId(UUID.randomUUID());
        entity.setCodigo("2025-II");
        entity.setNombre("Segundo semestre");
        entity.setEstado(EstadoPeriodo.HABILITADO);
        entity.setFechaInicio(LocalDate.parse("2025-08-01"));
        entity.setFechaFin(LocalDate.parse("2025-12-20"));
        return entity;
    }

    private InformacionAcademica informacion() {
        byte[] contenido = {1, 2};
        return new InformacionAcademica(null, UUID.randomUUID(), contenido, "datos.csv",
                contenido.length, EstadoInformacion.DISPONIBLE, LocalDateTime.now(), "usuario");
    }

    private InformacionAcademicaEntity informacionEntity(InformacionAcademica dominio) {
        InformacionAcademicaEntity entity = new InformacionAcademicaEntity();
        entity.setId(UUID.randomUUID());
        entity.setPeriodoAcademicoId(dominio.periodoAcademicoId());
        entity.setContenido(dominio.contenido());
        entity.setNombreArchivo(dominio.nombreArchivo());
        entity.setTamanioBytes(dominio.tamanioBytes());
        entity.setEstado(dominio.estado());
        entity.setFechaCarga(dominio.fechaCarga());
        entity.setUsuarioResponsable(dominio.usuarioResponsable());
        return entity;
    }
}
