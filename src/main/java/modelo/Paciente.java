package modelo;

import java.io.Serial;
import java.time.LocalDate;

/**
 * Paciente del centro de salud. Tiene una historia clínica propia (composición).
 */
public final class Paciente extends Persona {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TipoSeguro tipoSeguro;
    private boolean aportesAlDia;
    private String grupoSanguineo = "";
    private final HistoriaClinica historia;

    public Paciente(String dni, String nombres, String apellidos, LocalDate fechaNacimiento,
                    TipoSeguro tipoSeguro, boolean aportesAlDia) {
        super(dni, nombres, apellidos, fechaNacimiento);
        this.tipoSeguro = tipoSeguro == null ? TipoSeguro.SIN_SEGURO : tipoSeguro;
        this.aportesAlDia = aportesAlDia;
        this.historia = new HistoriaClinica(getDni());
    }

    /** Paciente sin seguro o con un seguro que no exige aportes. */
    public Paciente(String dni, String nombres, String apellidos, LocalDate fechaNacimiento, TipoSeguro tipoSeguro) {
        this(dni, nombres, apellidos, fechaNacimiento, tipoSeguro, false);
    }

    @Override
    public String getRol() {
        return "Paciente";
    }

    public TipoSeguro getTipoSeguro() {
        return tipoSeguro;
    }

    /** Nombre del seguro tal como se muestra en pantalla y reportes. */
    public String getSeguro() {
        return tipoSeguro.getNombre();
    }

    public boolean isAportesAlDia() {
        return aportesAlDia;
    }

    public void setAportesAlDia(boolean aportesAlDia) {
        this.aportesAlDia = aportesAlDia;
    }

    /**
     * true si la posta no le cobra: SIS gratuito/emprendedor/microempresas, o SIS independiente con
     * aportes al día. EsSalud, EPS, FF.AA. y sin seguro pagan como particulares.
     */
    public boolean estaExonerado() {
        return switch (tipoSeguro.getCobro()) {
            case GRATUITO -> true;
            case SEGUN_APORTES -> aportesAlDia;
            case PARTICULAR -> false;
        };
    }

    public String getGrupoSanguineo() {
        return grupoSanguineo;
    }

    public void setGrupoSanguineo(String grupoSanguineo) {
        this.grupoSanguineo = grupoSanguineo == null ? "" : grupoSanguineo.trim().toUpperCase();
    }

    public HistoriaClinica getHistoria() {
        return historia;
    }
}
