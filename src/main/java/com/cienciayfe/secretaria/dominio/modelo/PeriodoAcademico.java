package com.cienciayfe.secretaria.dominio.modelo;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoAcademico(
        UUID id,
        String codigo,
        String nombre,
        EstadoPeriodo estado,
        LocalDate fechaInicio,
        LocalDate fechaFin) {

    public enum EstadoPeriodo { HABILITADO, CERRADO }

    public boolean estaHabilitado() {
        return estado == EstadoPeriodo.HABILITADO;
    }
}
