package modelo;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Recepcionista: es quien registra las citas de los pacientes.
 */
public final class Recepcionista extends Persona {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String turno;

    public Recepcionista(String dni, String nombres, String apellidos,
                         LocalDate fechaNacimiento, String turno) {
        super(dni, nombres, apellidos, fechaNacimiento);
        this.turno = (turno == null || turno.isBlank()) ? "Mañana" : turno.trim();
    }

    @Override
    public String getRol() {
        return "Recepcionista";
    }

    public String getTurno() {
        return turno;
    }

    /**
     * Crea una cita validando la fecha y la disponibilidad del médico.
     *
     * @throws IllegalArgumentException si algún dato falta o la fecha ya pasó
     * @throws IllegalStateException    si el médico no está disponible a esa hora
     */
    public Cita registrarCita(Paciente paciente, Medico medico, Consultorio consultorio,
                              LocalDateTime fechaHora, String motivo) {
        if (paciente == null || medico == null || consultorio == null || fechaHora == null) {
            throw new IllegalArgumentException("Faltan datos para registrar la cita");
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No se puede programar una cita en el pasado");
        }
        if (!medico.estaDisponible(fechaHora)) {
            throw new IllegalStateException("El médico no está disponible en ese horario "
                    + "(atiende de 08:00 a 18:00 y no puede tener dos citas a la misma hora)");
        }
        return new Cita(paciente, medico, consultorio, fechaHora, motivo);
    }
}
