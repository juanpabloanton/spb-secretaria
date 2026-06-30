package com.cienciayfe.secretaria.aplicacion.puerto.entrada;

import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;

public interface CargarInformacionAcademicaUseCase {
    InformacionAcademica cargar(String codigoPeriodo, byte[] contenido, String nombreArchivo, String usuarioResponsable);
}
