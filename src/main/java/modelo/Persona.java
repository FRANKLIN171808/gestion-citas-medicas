package modelo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Clase abstracta base de todas las personas del sistema.
 * Es una clase sellada: solo Paciente, Medico y Recepcionista pueden heredar de ella.
 */
public abstract sealed class Persona implements Serializable
        permits Paciente, Medico, Recepcionista {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String dni;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    protected String telefono = "";
    protected String email = "";

    protected Persona(String dni, String nombres, String apellidos, LocalDate fechaNacimiento) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI es obligatorio");
        }
        if (nombres == null || nombres.isBlank() || apellidos == null || apellidos.isBlank()) {
            throw new IllegalArgumentException("Los nombres y apellidos son obligatorios");
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida");
        }
        this.dni = dni.trim();
        this.nombres = nombres.trim();
        this.apellidos = apellidos.trim();
        this.fechaNacimiento = fechaNacimiento;
    }

    /** Rol que cumple la persona dentro del centro de salud (método abstracto). */
    public abstract String getRol();

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public int calcularEdad() {
        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public String getDni() {
        return dni;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        if (nombres == null || nombres.isBlank()) {
            throw new IllegalArgumentException("Los nombres son obligatorios");
        }
        this.nombres = nombres.trim();
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        if (apellidos == null || apellidos.isBlank()) {
            throw new IllegalArgumentException("Los apellidos son obligatorios");
        }
        this.apellidos = apellidos.trim();
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono == null ? "" : telefono.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return dni.equals(((Persona) o).dni);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), dni);
    }

    @Override
    public String toString() {
        return getRol() + " [" + dni + "] " + getNombreCompleto();
    }
}
