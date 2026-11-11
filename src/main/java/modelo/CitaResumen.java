package modelo;

import java.time.LocalDateTime;

/**
 * Vista de solo lectura de una cita (record class).
 *
 * @param idCita    identificador de la cita
 * @param paciente  nombre completo del paciente
 * @param medico    nombre completo del médico
 * @param fechaHora fecha y hora de la cita
 * @param estado    estado actual de la cita
 */
public record CitaResumen(int idCita, String paciente, String medico,
                          LocalDateTime fechaHora, EstadoCita estado) {

    /** Construye el resumen a partir de una cita completa. */
    public static CitaResumen de(Cita c) {
        return new CitaResumen(c.getId(),
                c.getPaciente().getNombreCompleto(),
                c.getMedico().getNombreCompleto(),
                c.getFechaHora(),
                c.getEstado());
    }
}
