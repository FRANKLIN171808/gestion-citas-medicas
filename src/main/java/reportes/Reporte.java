package reportes;

import modelo.Cita;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Clase abstracta que define la plantilla para exportar citas.
 * {@link #generar(List)} es el método plantilla: fija el orden (encabezado, filas, pie)
 * y cada subclase decide el formato concreto.
 */
public abstract class Reporte {

    protected final String titulo;

    protected Reporte(String titulo) {
        this.titulo = titulo;
    }

    /** Método plantilla: no se puede sobreescribir. */
    public final String generar(List<Cita> citas) {
        StringBuilder sb = new StringBuilder();
        sb.append(encabezado(citas));
        citas.forEach(c -> sb.append(formatearFila(c)));
        sb.append(pie(citas));
        return sb.toString();
    }

    public void exportar(List<Cita> citas, Path archivo) throws IOException {
        Files.writeString(archivo, generar(citas), StandardCharsets.UTF_8);
    }

    protected abstract String encabezado(List<Cita> citas);

    protected abstract String formatearFila(Cita cita);

    protected abstract String pie(List<Cita> citas);

    /** Escapa los caracteres especiales de HTML y XML. */
    protected static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
