package com.cienciayfe.secretaria.adaptadores.salida.persistencia;

import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.PeriodoAcademicoEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PeriodoAcademicoJpaRepositorio extends JpaRepository<PeriodoAcademicoEntity, UUID> {
    Optional<PeriodoAcademicoEntity> findByCodigo(String codigo);
}
