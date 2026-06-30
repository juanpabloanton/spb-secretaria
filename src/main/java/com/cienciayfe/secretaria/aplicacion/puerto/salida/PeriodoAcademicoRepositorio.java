package com.cienciayfe.secretaria.aplicacion.puerto.salida;

import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import java.util.Optional;

public interface PeriodoAcademicoRepositorio {
    Optional<PeriodoAcademico> findByCodigo(String codigo);
}
