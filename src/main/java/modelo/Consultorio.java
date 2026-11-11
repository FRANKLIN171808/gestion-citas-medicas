package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Consultorio físico donde se atienden las citas.
 */
public class Consultorio implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int numero;
    private final int piso;

    public Consultorio(int numero, int piso) {
        if (numero <= 0 || piso <= 0) {
            throw new IllegalArgumentException("El número y el piso deben ser mayores que cero");
        }
        this.numero = numero;
        this.piso = piso;
    }

    public int getNumero() {
        return numero;
    }

    public int getPiso() {
        return piso;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Consultorio c && c.numero == numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Consultorio " + numero + " (piso " + piso + ")";
    }
}
