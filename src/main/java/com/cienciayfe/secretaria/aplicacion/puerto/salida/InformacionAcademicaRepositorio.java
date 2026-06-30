package com.cienciayfe.secretaria.aplicacion.puerto.salida;

import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import java.util.Optional;
import java.util.UUID;

public interface InformacionAcademicaRepositorio {
    InformacionAcademica save(InformacionAcademica informacion);
    Optional<InformacionAcademica> findByPeriodoAcademicoId(UUID periodoAcademicoId);
}
