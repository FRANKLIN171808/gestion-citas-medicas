package modelo;

import java.text.Normalizer;

/**
 * Seguros que puede presentar un paciente en una posta del MINSA y cómo se cobra cada uno.
 * <ul>
 *   <li>SIS gratuito, emprendedor y microempresas: el Estado asume el costo, no se cobra.</li>
 *   <li>SIS independiente (semicontributivo): no se cobra solo si el paciente está al día en sus aportes.</li>
 *   <li>EsSalud, EPS/privado, FF.AA. y sin seguro: la posta no es parte de su red, se cobra como particular.</li>
 * </ul>
 */
public enum TipoSeguro {
    SIS_GRATUITO("SIS Gratuito", Cobro.GRATUITO),
    SIS_EMPRENDEDOR("SIS Emprendedor (NRUS)", Cobro.GRATUITO),
    SIS_MICROEMPRESAS("SIS Microempresas", Cobro.GRATUITO),
    SIS_INDEPENDIENTE("SIS Independiente", Cobro.SEGUN_APORTES),
    ESSALUD("EsSalud", Cobro.PARTICULAR),
    EPS("EPS / seguro privado", Cobro.PARTICULAR),
    FFAA_PNP("FF.AA. / PNP", Cobro.PARTICULAR),
    SIN_SEGURO("Sin seguro (particular)", Cobro.PARTICULAR);

    /** Cómo se cobra la atención con este seguro. */
    public enum Cobro { GRATUITO, SEGUN_APORTES, PARTICULAR }

    private final String nombre;
    private final Cobro cobro;

    TipoSeguro(String nombre, Cobro cobro) {
        this.nombre = nombre;
        this.cobro = cobro;
    }

    public String getNombre() {
        return nombre;
    }

    public Cobro getCobro() {
        return cobro;
    }

    public boolean esSis() {
        return name().startsWith("SIS_");
    }

    /** true si hay que validar que el paciente esté al día en sus aportes. */
    public boolean requiereAportes() {
        return cobro == Cobro.SEGUN_APORTES;
    }

    /**
     * Interpreta el texto guardado (nombre del enum, nombre visible o valores antiguos como "SIS" o "EsSalud").
     * Si no lo reconoce, lo trata como particular: es la opción que no regala atenciones por error.
     */
    public static TipoSeguro desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return SIN_SEGURO;
        }
        String clave = clave(texto);
        for (TipoSeguro t : values()) {
            if (clave(t.name()).equals(clave) || clave(t.nombre).equals(clave)) {
                return t;
            }
        }
        return switch (clave) {
            case "SIS" -> SIS_GRATUITO;                       // valor antiguo: "SIS" a secas
            case "ESSALUD" -> ESSALUD;
            case "EPS", "PRIVADO" -> EPS;
            case "FFAA", "PNP", "FFAA_PNP", "SANIDAD" -> FFAA_PNP;
            default -> SIN_SEGURO;
        };
    }

    private static String clave(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    @Override
    public String toString() {
        return nombre;
    }
}
