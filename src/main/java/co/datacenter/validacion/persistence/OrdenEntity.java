package co.datacenter.validacion.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orden")
public class OrdenEntity {

    @Id
    @Column(name = "id_orden", length = 36)
    private String idOrden;

    @Column(nullable = false, unique = true, length = 40)
    private String referencia;

    @Column(name = "documento_cliente", nullable = false, length = 32)
    private String documentoCliente;

    @Column(name = "nombre_cliente", nullable = false, length = 120)
    private String nombreCliente;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @Column(name = "id_pago", length = 36)
    private String idPago;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Lob
    @Column(name = "respuesta_json")
    private String respuestaJson;

    @Column(nullable = false)
    private long version;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
}
