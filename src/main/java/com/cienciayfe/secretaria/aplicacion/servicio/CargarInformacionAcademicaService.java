package com.cienciayfe.secretaria.aplicacion.servicio;

import com.cienciayfe.secretaria.aplicacion.puerto.entrada.CargarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.InformacionAcademicaRepositorio;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.PeriodoAcademicoRepositorio;
import com.cienciayfe.secretaria.dominio.excepcion.ArchivoInvalidoException;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CargarInformacionAcademicaService implements CargarInformacionAcademicaUseCase {

    private static final String[] ENCABEZADOS_ESPERADOS = {
        "codigo_estudiante", "apellido_paterno", "apellido_materno",
        "nombres", "calificacion_final", "condicion"
    };
    private static final Set<String> CONDICIONES_VALIDAS = Set.of("PROMOVIDO", "REPROBADO", "ABANDERADO");
    private static final int INDICE_NOMBRES = 3;
    private static final int INDICE_CALIFICACION = 4;
    private static final int INDICE_CONDICION = 5;
    private static final double CALIFICACION_MAXIMA = 20.0;

    private final PeriodoAcademicoRepositorio periodoRepositorio;
    private final InformacionAcademicaRepositorio informacionRepositorio;

    @Override
    public InformacionAcademica cargar(String codigoPeriodo, byte[] contenido, String nombreArchivo, String usuarioResponsable) {
        PeriodoAcademico periodo = periodoRepositorio.findByCodigo(codigoPeriodo)
                .orElseThrow(() -> new PeriodoNoHabilitadoException(codigoPeriodo,
                        "El período '" + codigoPeriodo + "' no existe en el sistema"));

        if (!periodo.estaHabilitado()) {
            throw new PeriodoNoHabilitadoException(codigoPeriodo,
                    "El período '" + codigoPeriodo + "' está cerrado y no acepta cargas de información");
        }

        if (contenido == null || contenido.length == 0) {
            throw new ArchivoInvalidoException("ARCHIVO_VACIO", "El archivo no puede estar vacío");
        }

        List<String> lineas = parsearLineas(contenido);
        validarEncabezados(lineas.get(0));

        if (lineas.size() < 2) {
            throw new ArchivoInvalidoException("ARCHIVO_VACIO", "El archivo debe contener al menos una fila de datos");
        }

        for (int i = 1; i < lineas.size(); i++) {
            validarFila(lineas.get(i), i + 1);
        }

        InformacionAcademica nueva = new InformacionAcademica(null, periodo.id(), contenido,
                nombreArchivo, contenido.length, EstadoInformacion.DISPONIBLE,
                LocalDateTime.now(), usuarioResponsable);

        return informacionRepositorio.save(nueva);
    }

    private List<String> parsearLineas(byte[] contenido) {
        String texto = new String(contenido, StandardCharsets.UTF_8);
        return Arrays.stream(texto.split("\r?\n"))
                .filter(l -> !l.isBlank())
                .toList();
    }

    private void validarEncabezados(String primeraLinea) {
        String[] columnas = primeraLinea.split(";", -1);
        String[] esperados = ENCABEZADOS_ESPERADOS;
        if (columnas.length != esperados.length) {
            throw new ArchivoInvalidoException("ENCABEZADOS_INVALIDOS", encabezadoEsperadoMsg());
        }
        for (int i = 0; i < esperados.length; i++) {
            if (!esperados[i].equals(columnas[i].trim())) {
                throw new ArchivoInvalidoException("ENCABEZADOS_INVALIDOS", encabezadoEsperadoMsg());
            }
        }
    }

    private void validarFila(String linea, int numeroFila) {
        String[] cols = linea.split(";", -1);
        if (cols.length != ENCABEZADOS_ESPERADOS.length) {
            throw new ArchivoInvalidoException("FORMATO_INVALIDO",
                    "Fila " + numeroFila + ": número de columnas incorrecto");
        }
        if (cols[0].isBlank()) {
            throw new ArchivoInvalidoException("CAMPO_REQUERIDO", "Fila " + numeroFila + ": codigo_estudiante es obligatorio");
        }
        if (cols[1].isBlank()) {
            throw new ArchivoInvalidoException("CAMPO_REQUERIDO", "Fila " + numeroFila + ": apellido_paterno es obligatorio");
        }
        if (cols[INDICE_NOMBRES].isBlank()) {
            throw new ArchivoInvalidoException("CAMPO_REQUERIDO", "Fila " + numeroFila + ": nombres es obligatorio");
        }
        validarCalificacion(cols[INDICE_CALIFICACION].trim(), numeroFila);
        validarCondicion(cols[INDICE_CONDICION].trim(), numeroFila);
    }

    private void validarCalificacion(String valor, int fila) {
        try {
            double cal = Double.parseDouble(valor);
            if (cal < 0.0 || cal > CALIFICACION_MAXIMA) {
                throw new ArchivoInvalidoException("CALIFICACION_INVALIDA",
                        "Fila " + fila + ": calificacion_final '" + valor + "' no es válida. Debe ser un número entre 0.0 y 20.0");
            }
        } catch (NumberFormatException e) {
            throw new ArchivoInvalidoException("CALIFICACION_INVALIDA",
                    "Fila " + fila + ": calificacion_final '" + valor + "' no es válida. Debe ser un número entre 0.0 y 20.0");
        }
    }

    private void validarCondicion(String valor, int fila) {
        if (!CONDICIONES_VALIDAS.contains(valor)) {
            throw new ArchivoInvalidoException("CONDICION_INVALIDA",
                    "Fila " + fila + ": condicion '" + valor + "' no es válida. Valores permitidos: PROMOVIDO, REPROBADO, ABANDERADO");
        }
    }

    private String encabezadoEsperadoMsg() {
        return "Los encabezados del archivo no coinciden con el formato esperado. Esperado: "
                + String.join(";", ENCABEZADOS_ESPERADOS);
    }
}
