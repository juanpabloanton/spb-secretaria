package com.cienciayfe.secretaria.dominio.modelo;

import java.time.LocalDateTime;
import java.util.UUID;

public record InformacionAcademica(
        UUID id,
        UUID periodoAcademicoId,
        byte[] contenido,
        String nombreArchivo,
        long tamanioBytes,
        EstadoInformacion estado,
        LocalDateTime fechaCarga,
        String usuarioResponsable) {

    public InformacionAcademica {
        contenido = contenido == null ? null : contenido.clone();
    }

    @Override
    public byte[] contenido() {
        return contenido == null ? null : contenido.clone();
    }

    public enum EstadoInformacion { DISPONIBLE, SIN_INFORMACION }
}
