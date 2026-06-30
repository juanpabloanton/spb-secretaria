package com.cienciayfe.secretaria.adaptadores.salida.persistencia.entidad;

import com.cienciayfe.secretaria.dominio.modelo.InformacionAcademica.EstadoInformacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "informacion_academica")
@Data
@NoArgsConstructor
public class InformacionAcademicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "periodo_academico_id", nullable = false, unique = true)
    private UUID periodoAcademicoId;

    @Column(nullable = false)
    private byte[] contenido;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tamanio_bytes", nullable = false)
    private long tamanioBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInformacion estado;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Column(name = "usuario_responsable", nullable = false, length = 100)
    private String usuarioResponsable;
}
