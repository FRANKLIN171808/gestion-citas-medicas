package modelo;

import pagos.MetodoPago;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cita médica: une a un paciente, un médico y un consultorio en una fecha y hora.
 * Su ciclo de vida es PROGRAMADA -> (pago en caja, si el paciente no está exonerado) -> CONFIRMADA -> ATENDIDA,
 * o CANCELADA mientras no haya sido atendida.
 */
public class Cita implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private final LocalDateTime fechaHora;
    private final String motivo;
    private final double costo;
    private EstadoCita estado;
    private boolean pagada;
    private String metodoPago = "";
    private final Paciente paciente;
    private final Medico medico;
    private final Consultorio consultorio;

    /** Cita rápida: sin consultorio asignado y con motivo por defecto. */
    public Cita(Paciente paciente, Medico medico, LocalDateTime fechaHora) {
        this(paciente, medico, null, fechaHora, "Consulta general");
    }

    public Cita(Paciente paciente, Medico medico, Consultorio consultorio,
                LocalDateTime fechaHora, String motivo) {
        if (paciente == null || medico == null || fechaHora == null) {
            throw new IllegalArgumentException("Paciente, médico y fecha son obligatorios");
        }
        this.paciente = paciente;
        this.medico = medico;
        this.consultorio = consultorio;
        this.fechaHora = fechaHora;
        this.motivo = (motivo == null || motivo.isBlank()) ? "Consulta general" : motivo.trim();
        // Los pacientes exonerados (SIS) no pagan; los demás pagan la tarifa de la especialidad.
        this.costo = paciente.estaExonerado() ? 0 : medico.getEspecialidad().getTarifa();
        this.estado = EstadoCita.PROGRAMADA;
        marcarCubiertaSiCorresponde();
    }

    /**
     * Reconstruye una cita ya existente (por ejemplo, leída de la base de datos),
     * respetando su estado y su pago.
     */
    public static Cita reconstruir(int id, Paciente paciente, Medico medico, Consultorio consultorio,
                                   LocalDateTime fechaHora, String motivo, EstadoCita estado,
                                   boolean pagada, String metodoPago) {
        Cita c = new Cita(paciente, medico, consultorio, fechaHora, motivo);
        c.id = id;
        c.estado = estado;
        c.pagada = pagada;
        c.metodoPago = metodoPago == null ? "" : metodoPago;
        return c;
    }

    public void confirmar() {
        if (estado != EstadoCita.PROGRAMADA) {
            throw new IllegalStateException("Solo se puede confirmar una cita PROGRAMADA (estado actual: " + estado + ")");
        }
        if (requierePago() && !pagada) {
            throw new IllegalStateException("Primero debe pagar la cita en caja (S/ " + String.format("%.2f", costo)
                    + "); luego se confirma");
        }
        estado = EstadoCita.CONFIRMADA;
    }

    public void cancelar() {
        if (estado == EstadoCita.ATENDIDA || estado == EstadoCita.CANCELADA) {
            throw new IllegalStateException("No se puede cancelar una cita " + estado);
        }
        estado = EstadoCita.CANCELADA;
    }

    public void atender() {
        if (estado != EstadoCita.CONFIRMADA) {
            throw new IllegalStateException("Solo se puede atender una cita CONFIRMADA (estado actual: " + estado + ")");
        }
        estado = EstadoCita.ATENDIDA;
        marcarCubiertaSiCorresponde();
    }

    /** Cita sin costo (SIS): queda saldada sin cobrar nada. */
    private void marcarCubiertaSiCorresponde() {
        if (!requierePago()) {
            pagada = true;
            metodoPago = "Cubierta por " + paciente.getSeguro();
        }
    }

    /** true si la cita se cobra (paciente sin seguro). */
    public final boolean requierePago() {
        return costo > 0;
    }

    /**
     * Paga la cita con el método indicado (polimorfismo con la interfaz MetodoPago).
     *
     * @return true si el pago fue aceptado
     */
    public boolean pagar(MetodoPago metodo) {
        if (metodo == null) {
            throw new IllegalArgumentException("Debe indicar un método de pago");
        }
        if (estado == EstadoCita.CANCELADA) {
            throw new IllegalStateException("No se puede pagar una cita CANCELADA");
        }
        if (!requierePago()) {
            throw new IllegalStateException("Esta cita no se cobra: el paciente está cubierto por " + paciente.getSeguro());
        }
        if (pagada) {
            throw new IllegalStateException("La cita ya fue pagada");
        }
        if (metodo.procesarPago(costo)) {
            pagada = true;
            metodoPago = metodo.getNombre();
            return true;
        }
        return false;
    }

    public int getId() {
        return id;
    }

    /** Solo el CentroSalud (mismo paquete) asigna el identificador. */
    void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getMotivo() {
        return motivo;
    }

    public double getCosto() {
        return costo;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public boolean isPagada() {
        return pagada;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public Medico getMedico() {
        return medico;
    }

    public Consultorio getConsultorio() {
        return consultorio;
    }

    /** Texto corto del cobro: "S/ 80.00", "Sin costo (SIS)", etc. */
    public String descripcionCobro() {
        return requierePago() ? String.format("S/ %.2f", costo) : "Sin costo (" + paciente.getSeguro() + ")";
    }

    @Override
    public String toString() {
        return String.format("Cita #%d | %s | %s | %s | %s | %s | %s %s",
                id, fechaHora.format(FORMATO), paciente.getNombreCompleto(),
                medico.getNombreCompleto(), consultorio == null ? "sin consultorio" : "C" + consultorio.getNumero(),
                estado, descripcionCobro(),
                !requierePago() ? "" : pagada ? "(pagada: " + metodoPago + ")" : "(pendiente de pago)");
    }
}
