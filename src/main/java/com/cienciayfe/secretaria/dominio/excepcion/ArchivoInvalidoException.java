package com.cienciayfe.secretaria.dominio.excepcion;

public class ArchivoInvalidoException extends RuntimeException {

    private final String codigo;

    public ArchivoInvalidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
