package com.cienciayfe.secretaria.aplicacion.servicio;

import com.cienciayfe.secretaria.aplicacion.puerto.entrada.ConsultarInformacionAcademicaUseCase;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.InformacionAcademicaRepositorio;
import com.cienciayfe.secretaria.aplicacion.puerto.salida.PeriodoAcademicoRepositorio;
import com.cienciayfe.secretaria.dominio.excepcion.PeriodoNoHabilitadoException;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import com.cienciayfe.secretaria.dominio.modelo.PeriodoAcademico;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConsultarInformacionAcademicaService implements ConsultarInformacionAcademicaUseCase {

    private final PeriodoAcademicoRepositorio periodoRepositorio;
    private final InformacionAcademicaRepositorio informacionRepositorio;

    @Override
    public InformacionAcademica consultar(String codigoPeriodo) {
        PeriodoAcademico periodo = periodoRepositorio.findByCodigo(codigoPeriodo)
                .orElseThrow(() -> new PeriodoNoHabilitadoException(codigoPeriodo,
                        "El período '" + codigoPeriodo + "' no existe en el sistema"));

        Optional<InformacionAcademica> existente = informacionRepositorio.findByPeriodoAcademicoId(periodo.id());

        return existente.orElseGet(() -> new InformacionAcademica(
                null, periodo.id(), null, null, 0,
                EstadoInformacion.SIN_INFORMACION, null, null));
    }
}
