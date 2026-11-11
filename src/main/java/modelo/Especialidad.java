package modelo;

/**
 * Especialidades médicas disponibles y la tarifa base de una consulta.
 */
public enum Especialidad {
    MEDICINA_GENERAL("Medicina General", 10.0),
    ODONTOLOGIA("Odontología", 12.0),
    ENFERMERIA("Enfermería", 12.0),
    OBSTETRICIA("Obstetricia", 15.0),
    EMERGENCIA("Emergencias", 15.0);

    private final String nombre;
    private final double tarifa;

    Especialidad(String nombre, double tarifa) {
        this.nombre = nombre;
        this.tarifa = tarifa;
    }

    public String getNombre() {
        return nombre;
    }

    public double getTarifa() {
        return tarifa;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
