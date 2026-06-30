package com.cienciayfe.secretaria.dominio.excepcion;

public class PeriodoNoHabilitadoException extends RuntimeException {

    private final String codigoPeriodo;

    public PeriodoNoHabilitadoException(String codigoPeriodo, String mensaje) {
        super(mensaje);
        this.codigoPeriodo = codigoPeriodo;
    }

    public String getCodigoPeriodo() {
        return codigoPeriodo;
    }
}
