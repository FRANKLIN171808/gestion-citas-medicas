package modelo;

import java.io.Serial;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Médico del centro de salud, con su especialidad y su agenda de citas.
 * Atiende de lunes a sábado, de 08:00 a 18:00, en turnos de {@value #MINUTOS_POR_TURNO} minutos.
 */
public final class Medico extends Persona {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Horario de atención: de 08:00 (incluida) a 18:00 (excluida). */
    public static final LocalTime INICIO_ATENCION = LocalTime.of(8, 0);
    public static final LocalTime FIN_ATENCION = LocalTime.of(18, 0);
    public static final int MINUTOS_POR_TURNO = 30;

    private final String cmp;
    private final String rne;
    private final Especialidad especialidad;
    private final List<Cita> agenda = new ArrayList<>();

    /**
     * @param cmp colegiatura del médico (obligatoria)
     * @param rne registro de especialista; puede venir vacío si el médico no es especialista
     */
    public Medico(String dni, String nombres, String apellidos, LocalDate fechaNacimiento,
                  String cmp, String rne, Especialidad especialidad) {
        super(dni, nombres, apellidos, fechaNacimiento);
        if (cmp == null || cmp.isBlank()) {
            throw new IllegalArgumentException("El CMP es obligatorio");
        }
        if (especialidad == null) {
            throw new IllegalArgumentException("La especialidad es obligatoria");
        }
        this.cmp = cmp.trim();
        this.rne = rne == null ? "" : rne.trim();
        this.especialidad = especialidad;
    }

    @Override
    public String getRol() {
        return "Médico (" + especialidad + ")";
    }

    public String getCmp() {
        return cmp;
    }

    public String getCmpFormateado() {
        return "CMP " + cmp;
    }

    public String getRne() {
        return rne;
    }

    public boolean tieneRne() {
        return !rne.isEmpty();
    }

    public String getRneFormateado() {
        return "RNE " + rne;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    // ------------------------------------------------------------------ horario y agenda

    /** El centro atiende de lunes a sábado. */
    public static boolean esDiaDeAtencion(LocalDate dia) {
        return dia.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** Horas de inicio de todos los turnos de un día de atención (08:00, 08:30 ... 17:30). */
    public List<LocalTime> turnosDelDia() {
        List<LocalTime> turnos = new ArrayList<>();
        for (LocalTime t = INICIO_ATENCION; t.isBefore(FIN_ATENCION); t = t.plusMinutes(MINUTOS_POR_TURNO)) {
            turnos.add(t);
        }
        return turnos;
    }

    /** Turnos de ese día que aún están libres y todavía no han pasado. */
    public List<LocalTime> turnosLibres(LocalDate dia, LocalDateTime ahora) {
        return turnosDelDia().stream()
                .filter(t -> dia.atTime(t).isAfter(ahora) && estaDisponible(dia.atTime(t)))
                .toList();
    }

    /**
     * Indica si la hora coincide con un turno de un día de atención y el médico
     * no tiene otra cita activa en ese momento.
     */
    public boolean estaDisponible(LocalDateTime fechaHora) {
        if (!esDiaDeAtencion(fechaHora.toLocalDate()) || !turnosDelDia().contains(fechaHora.toLocalTime())) {
            return false;
        }
        return agenda.stream().noneMatch(c ->
                c.getEstado() != EstadoCita.CANCELADA && c.getFechaHora().equals(fechaHora));
    }

    public void agregarCita(Cita cita) {
        agenda.add(cita);
    }

    public List<Cita> getAgenda() {
        return Collections.unmodifiableList(agenda);
    }
}
