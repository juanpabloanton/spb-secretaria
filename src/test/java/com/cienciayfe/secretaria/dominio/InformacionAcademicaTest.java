package com.cienciayfe.secretaria.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica;
import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InformacionAcademicaTest {

    @Test
    void protegeElContenidoMutable() {
        byte[] original = {1, 2};
        InformacionAcademica informacion = new InformacionAcademica(UUID.randomUUID(),
                UUID.randomUUID(), original, "datos.csv", original.length,
                EstadoInformacion.DISPONIBLE, LocalDateTime.now(), "usuario");

        original[0] = 2;
        byte[] obtenido = informacion.contenido();
        obtenido[0] = 2;

        assertThat(informacion.contenido()[0]).isEqualTo((byte) 1);
    }

    @Test
    void admiteContenidoNuloParaEstadoSinInformacion() {
        InformacionAcademica informacion = new InformacionAcademica(null, UUID.randomUUID(),
                null, null, 0, EstadoInformacion.SIN_INFORMACION, null, null);

        assertThat(informacion.contenido()).isNull();
    }
}
