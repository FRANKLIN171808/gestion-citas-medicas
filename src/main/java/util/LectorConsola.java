package util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Lee datos desde la consola con validaciones: repite la pregunta hasta que el
 * usuario escribe un valor correcto.
 */
public class LectorConsola {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Scanner scanner;

    public LectorConsola() {
        this(new Scanner(System.in, StandardCharsets.UTF_8));
    }

    public LectorConsola(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Lee una línea. Si la entrada se cierra (Ctrl+D / fin de archivo), el Scanner lanza
     * NoSuchElementException y la dejamos subir hasta Main para terminar el programa.
     */
    private String leerLinea(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }

    public int leerEntero(String mensaje, int min, int max) {
        while (true) {
            String linea = leerLinea(mensaje);
            try {
                int valor = Integer.parseInt(linea);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.println("  Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Valor inválido: escriba un número entero.");
            }
        }
    }

    public double leerDecimal(String mensaje, double min) {
        while (true) {
            String linea = leerLinea(mensaje).replace(',', '.');
            try {
                double valor = Double.parseDouble(linea);
                if (valor >= min) {
                    return valor;
                }
                System.out.println("  El valor debe ser como mínimo " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Valor inválido: escriba un número (ej. 50.5).");
            }
        }
    }

    public String leerTexto(String mensaje) {
        while (true) {
            String linea = leerLinea(mensaje);
            if (!linea.isEmpty()) {
                return linea;
            }
            System.out.println("  Este dato es obligatorio.");
        }
    }

    public String leerTextoOpcional(String mensaje) {
        return leerLinea(mensaje);
    }

    public String leerDni(String mensaje) {
        while (true) {
            String linea = leerLinea(mensaje);
            if (linea.matches("\\d{8}")) {
                return linea;
            }
            System.out.println("  El DNI debe tener exactamente 8 dígitos.");
        }
    }

    /** Lee un texto formado solo por dígitos y de la longitud exacta indicada (ej. número de tarjeta). */
    public String leerDigitos(String mensaje, int cantidad) {
        while (true) {
            String linea = leerLinea(mensaje).replace(" ", "");
            if (linea.matches("\\d{" + cantidad + "}")) {
                return linea;
            }
            System.out.println("  Debe tener exactamente " + cantidad + " dígitos.");
        }
    }

    public LocalDate leerFecha(String mensaje) {
        while (true) {
            String linea = leerLinea(mensaje + " (dd/MM/aaaa): ");
            try {
                return LocalDate.parse(linea, FECHA);
            } catch (DateTimeParseException e) {
                System.out.println("  Fecha inválida. Use el formato dd/MM/aaaa, por ejemplo 25/03/1990.");
            }
        }
    }

    public LocalDateTime leerFechaHora(String mensaje) {
        while (true) {
            String linea = leerLinea(mensaje + " (dd/MM/aaaa HH:mm): ");
            try {
                return LocalDateTime.parse(linea, FECHA_HORA);
            } catch (DateTimeParseException e) {
                System.out.println("  Fecha y hora inválidas. Use el formato dd/MM/aaaa HH:mm, por ejemplo 15/11/2026 09:30.");
            }
        }
    }

    public boolean leerConfirmacion(String mensaje) {
        while (true) {
            String linea = leerLinea(mensaje + " (s/n): ").toLowerCase();
            if (linea.equals("s") || linea.equals("si") || linea.equals("sí")) {
                return true;
            }
            if (linea.equals("n") || linea.equals("no")) {
                return false;
            }
            System.out.println("  Responda s o n.");
        }
    }
}
