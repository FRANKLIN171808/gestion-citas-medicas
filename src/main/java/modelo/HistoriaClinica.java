package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Historia clínica de un paciente: el registro de todas sus citas en el centro.
 */
public class HistoriaClinica implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String numero;
    private final List<Cita> atenciones = new ArrayList<>();

    public HistoriaClinica(String dniPaciente) {
        this.numero = "HC-" + dniPaciente;
    }

    public void agregarAtencion(Cita cita) {
        atenciones.add(cita);
    }

    /** Citas del paciente, de la más reciente registrada a la más antigua (Sequenced Collection). */
    public SequencedCollection<Cita> ultimasAtenciones() {
        return List.copyOf(atenciones).reversed();
    }

    public String getNumero() {
        return numero;
    }

    public List<Cita> getAtenciones() {
        return Collections.unmodifiableList(atenciones);
    }
}
