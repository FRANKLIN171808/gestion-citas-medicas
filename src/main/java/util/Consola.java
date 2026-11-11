package util;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades para mostrar la salida de consola de forma ordenada: cuadros de título y tablas alineadas.
 * Usa solo caracteres ASCII para que se vea igual en IntelliJ, CMD o PowerShell.
 */
public final class Consola {

    private static final int ANCHO = 64;

    private Consola() {
    }

    /** Cuadro grande para el nombre del centro. */
    public static String banner(String texto) {
        String borde = "+" + "=".repeat(ANCHO - 2) + "+";
        return borde + "\n" + centrar(texto) + "\n" + borde;
    }

    /** Título de una sección: una línea con el texto y una raya debajo. */
    public static void seccion(String texto) {
        System.out.println();
        System.out.println("  " + texto.toUpperCase());
        System.out.println("  " + "-".repeat(Math.min(ANCHO - 4, Math.max(texto.length(), 20))));
    }

    public static void ok(String mensaje) {
        System.out.println("  [OK] " + mensaje);
    }

    public static void aviso(String mensaje) {
        System.out.println("  [!]  " + mensaje);
    }

    public static void info(String mensaje) {
        System.out.println("  " + mensaje);
    }

    /** Imprime una tabla con bordes y columnas alineadas al texto más largo de cada una. */
    public static void tabla(List<String> encabezados, List<List<String>> filas) {
        int n = encabezados.size();
        int[] ancho = new int[n];
        for (int i = 0; i < n; i++) {
            ancho[i] = encabezados.get(i).length();
        }
        for (List<String> fila : filas) {
            for (int i = 0; i < n; i++) {
                ancho[i] = Math.max(ancho[i], fila.get(i).length());
            }
        }
        StringBuilder sep = new StringBuilder("  +");
        for (int a : ancho) {
            sep.append("-".repeat(a + 2)).append('+');
        }
        System.out.println(sep);
        System.out.println(linea(encabezados, ancho));
        System.out.println(sep);
        for (List<String> fila : filas) {
            System.out.println(linea(fila, ancho));
        }
        System.out.println(sep);
    }

    /** Atajo para armar una fila a partir de valores sueltos. */
    public static List<String> fila(Object... valores) {
        List<String> fila = new ArrayList<>();
        for (Object v : valores) {
            fila.add(String.valueOf(v));
        }
        return fila;
    }

    private static String linea(List<String> celdas, int[] ancho) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < ancho.length; i++) {
            sb.append(' ').append(String.format("%-" + ancho[i] + "s", celdas.get(i))).append(" |");
        }
        return sb.toString();
    }

    private static String centrar(String texto) {
        int espacio = Math.max(0, ANCHO - 2 - texto.length());
        int izq = espacio / 2;
        return "|" + " ".repeat(izq) + texto + " ".repeat(espacio - izq) + "|";
    }
}
