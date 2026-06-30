package com.cienciayfe.secretaria.aplicacion.puerto.entrada;

import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;

public interface ConsultarInformacionAcademicaUseCase {
    InformacionAcademica consultar(String codigoPeriodo);
}
