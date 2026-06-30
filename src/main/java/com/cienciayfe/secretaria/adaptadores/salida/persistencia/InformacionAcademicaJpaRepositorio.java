package com.cienciayfe.secretaria.adaptadores.salida.persistencia;

import com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad.InformacionAcademicaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InformacionAcademicaJpaRepositorio extends JpaRepository<InformacionAcademicaEntity, UUID> {
    Optional<InformacionAcademicaEntity> findByPeriodoAcademicoId(UUID periodoAcademicoId);
}
