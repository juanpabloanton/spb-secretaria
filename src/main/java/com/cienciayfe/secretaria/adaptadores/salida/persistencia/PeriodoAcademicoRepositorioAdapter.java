package com.cienciayfe.secretaria.adaptadores.salida.persistencia;

import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.PeriodoAcademicoEntity;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.PeriodoAcademicoRepositorio;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PeriodoAcademicoRepositorioAdapter implements PeriodoAcademicoRepositorio {

    private final PeriodoAcademicoJpaRepositorio jpa;

    @Override
    public Optional<PeriodoAcademico> findByCodigo(String codigo) {
        return jpa.findByCodigo(codigo).map(this::toDomain);
    }

    private PeriodoAcademico toDomain(PeriodoAcademicoEntity e) {
        return new PeriodoAcademico(e.getId(), e.getCodigo(), e.getNombre(),
                e.getEstado(), e.getFechaInicio(), e.getFechaFin());
    }
}
