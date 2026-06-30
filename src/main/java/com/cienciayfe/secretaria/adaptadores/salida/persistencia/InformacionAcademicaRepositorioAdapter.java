package com.cienciayfe.secretaria.adaptadores.salida.persistencia;

import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.InformacionAcademicaEntity;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.InformacionAcademicaRepositorio;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class InformacionAcademicaRepositorioAdapter implements InformacionAcademicaRepositorio {

    private final InformacionAcademicaJpaRepositorio jpa;

    @Override
    public InformacionAcademica save(InformacionAcademica dominio) {
        InformacionAcademicaEntity entity = jpa
                .findByPeriodoAcademicoId(dominio.periodoAcademicoId())
                .map(existing -> actualizarEntity(existing, dominio))
                .orElseGet(() -> toEntity(dominio));
        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<InformacionAcademica> findByPeriodoAcademicoId(UUID periodoAcademicoId) {
        return jpa.findByPeriodoAcademicoId(periodoAcademicoId).map(this::toDomain);
    }

    private InformacionAcademicaEntity actualizarEntity(InformacionAcademicaEntity e, InformacionAcademica d) {
        e.setContenido(d.contenido());
        e.setNombreArchivo(d.nombreArchivo());
        e.setTamanioBytes(d.tamanioBytes());
        e.setFechaCarga(d.fechaCarga());
        e.setUsuarioResponsable(d.usuarioResponsable());
        return e;
    }

    private InformacionAcademicaEntity toEntity(InformacionAcademica d) {
        InformacionAcademicaEntity e = new InformacionAcademicaEntity();
        e.setPeriodoAcademicoId(d.periodoAcademicoId());
        e.setContenido(d.contenido());
        e.setNombreArchivo(d.nombreArchivo());
        e.setTamanioBytes(d.tamanioBytes());
        e.setEstado(EstadoInformacion.DISPONIBLE);
        e.setFechaCarga(d.fechaCarga());
        e.setUsuarioResponsable(d.usuarioResponsable());
        return e;
    }

    private InformacionAcademica toDomain(InformacionAcademicaEntity e) {
        return new InformacionAcademica(e.getId(), e.getPeriodoAcademicoId(), e.getContenido(),
                e.getNombreArchivo(), e.getTamanioBytes(), e.getEstado(),
                e.getFechaCarga(), e.getUsuarioResponsable());
    }
}
