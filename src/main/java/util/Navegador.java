package util;

import java.awt.Desktop;
import java.nio.file.Path;

/**
 * Abre un archivo en el navegador predeterminado. Si el equipo no lo permite
 * (sin entorno gráfico, por ejemplo), simplemente no hace nada: el archivo ya quedó guardado.
 */
public final class Navegador {

    private Navegador() {
    }

    /** @return true si se pudo abrir el archivo. */
    public static boolean abrir(Path archivo) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(archivo.toUri());
                return true;
            }
        } catch (Exception | java.awt.AWTError e) {
            // sin navegador disponible: se ignora
        }
        return false;
    }
}
