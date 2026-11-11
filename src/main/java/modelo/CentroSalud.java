package modelo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Clase principal del dominio: agrupa consultorios, personal, pacientes y citas,
 * y ofrece las operaciones y consultas (con streams y lambdas) del sistema.
 */
public class CentroSalud implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final String direccion;
    private final List<Consultorio> consultorios = new ArrayList<>();
    private final Map<String, Paciente> pacientes = new LinkedHashMap<>();
    private final List<Medico> medicos = new ArrayList<>();
    private final List<Recepcionista> personal = new ArrayList<>();
    private final List<Cita> citas = new ArrayList<>();
    private int siguienteIdCita = 1;

    public CentroSalud(String nombre, String direccion) {
        this.nombre = nombre;
        this.direccion = direccion;
    }

    // ---------------------------------------------------------------- registros
    // Médicos, recepcionistas y consultorios vienen predeterminados (base de datos o PersonalPredeterminado):
    // estos métodos solo los usan quienes cargan ese personal al iniciar. El menú solo registra pacientes.

    public void registrarPaciente(Paciente paciente) {
        if (pacientes.containsKey(paciente.getDni())) {
            throw new IllegalArgumentException("Ya existe un paciente con DNI " + paciente.getDni());
        }
        pacientes.put(paciente.getDni(), paciente);
    }

    public void registrarMedico(Medico medico) {
        if (medicos.contains(medico)) {
            throw new IllegalArgumentException("Ya existe un médico con DNI " + medico.getDni());
        }
        medicos.add(medico);
    }

    public void registrarRecepcionista(Recepcionista recepcionista) {
        if (personal.contains(recepcionista)) {
            throw new IllegalArgumentException("Ya existe una recepcionista con DNI " + recepcionista.getDni());
        }
        personal.add(recepcionista);
    }

    public void agregarConsultorio(int numero, int piso) {
        Consultorio nuevo = new Consultorio(numero, piso);
        if (consultorios.contains(nuevo)) {
            throw new IllegalArgumentException("Ya existe el consultorio " + numero);
        }
        consultorios.add(nuevo);
    }

    // ---------------------------------------------------------------- búsquedas

    public Optional<Paciente> buscarPaciente(String dni) {
        return Optional.ofNullable(pacientes.get(dni));
    }

    public Optional<Medico> buscarMedico(String dni) {
        return medicos.stream().filter(m -> m.getDni().equals(dni)).findFirst();
    }

    public Optional<Consultorio> buscarConsultorio(int numero) {
        return consultorios.stream().filter(c -> c.getNumero() == numero).findFirst();
    }

    public Optional<Cita> buscarCita(int id) {
        return citas.stream().filter(c -> c.getId() == id).findFirst();
    }

    // ---------------------------------------------------------------- citas

    /**
     * Programa una cita validando que todos los involucrados existan en el centro
     * y que ni el consultorio ni el paciente tengan otra cita activa a esa hora.
     */
    public Cita programarCita(Recepcionista recepcionista, Paciente paciente, Medico medico,
                              Consultorio consultorio, LocalDateTime fechaHora, String motivo) {
        if (recepcionista == null || !personal.contains(recepcionista)) {
            throw new IllegalArgumentException("La recepcionista no está registrada");
        }
        if (paciente == null || !pacientes.containsKey(paciente.getDni())) {
            throw new IllegalArgumentException("El paciente no está registrado");
        }
        if (medico == null || !medicos.contains(medico)) {
            throw new IllegalArgumentException("El médico no está registrado");
        }
        if (consultorio == null || !consultorios.contains(consultorio)) {
            throw new IllegalArgumentException("El consultorio no está registrado");
        }
        boolean consultorioOcupado = citas.stream().anyMatch(c ->
                c.getEstado() != EstadoCita.CANCELADA
                        && consultorio.equals(c.getConsultorio())
                        && c.getFechaHora().equals(fechaHora));
        if (consultorioOcupado) {
            throw new IllegalStateException("El consultorio ya está ocupado a esa hora");
        }
        boolean pacienteOcupado = citas.stream().anyMatch(c ->
                c.getEstado() != EstadoCita.CANCELADA
                        && c.getPaciente().equals(paciente)
                        && c.getFechaHora().equals(fechaHora));
        if (pacienteOcupado) {
            throw new IllegalStateException("El paciente ya tiene una cita a esa hora");
        }

        Cita cita = recepcionista.registrarCita(paciente, medico, consultorio, fechaHora, motivo);
        cita.setId(siguienteIdCita++);
        citas.add(cita);
        medico.agregarCita(cita);
        paciente.getHistoria().agregarAtencion(cita);
        return cita;
    }

    /** Reincorpora una cita leída de la base de datos, enlazándola con su médico y su paciente. */
    public void restaurarCita(Cita cita) {
        citas.add(cita);
        cita.getMedico().agregarCita(cita);
        cita.getPaciente().getHistoria().agregarAtencion(cita);
        siguienteIdCita = Math.max(siguienteIdCita, cita.getId() + 1);
    }

    /** Evita repetir identificadores: las nuevas citas empezarán después de {@code mayorId}. */
    public void reservarIdsHasta(int mayorId) {
        siguienteIdCita = Math.max(siguienteIdCita, mayorId + 1);
    }

    // ---------------------------------------------------------------- consultas (streams y lambdas)

    /** Consultorios que no tienen una cita activa a esa fecha y hora. */
    public List<Consultorio> consultoriosLibres(LocalDateTime fechaHora) {
        return consultorios.stream()
                .filter(k -> citas.stream().noneMatch(c -> c.getEstado() != EstadoCita.CANCELADA
                        && k.equals(c.getConsultorio()) && c.getFechaHora().equals(fechaHora)))
                .toList();
    }

    /** Citas que todavía se pueden gestionar (ni atendidas ni canceladas), por fecha. */
    public List<Cita> citasActivas() {
        return citas.stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .sorted((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()))
                .toList();
    }

    /** Citas no canceladas que aún deben pagarse (paciente no exonerado). */
    public List<Cita> citasPendientesDePago() {
        return citas.stream()
                .filter(c -> c.getEstado() != EstadoCita.CANCELADA && c.requierePago() && !c.isPagada())
                .sorted((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()))
                .toList();
    }

    public List<Cita> citasPorMedico(Medico medico) {
        return citas.stream().filter(c -> c.getMedico().equals(medico)).toList();
    }

    public List<Cita> citasDelDia(LocalDate dia) {
        return citas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(dia))
                .sorted((a, b) -> a.getFechaHora().compareTo(b.getFechaHora()))
                .toList();
    }

    public List<Cita> citasOrdenadasPorFecha() {
        return citas.stream().sorted((a, b) -> a.getFechaHora().compareTo(b.getFechaHora())).toList();
    }

    public Map<EstadoCita, Long> citasPorEstado() {
        return citas.stream().collect(Collectors.groupingBy(
                Cita::getEstado, () -> new EnumMap<>(EstadoCita.class), Collectors.counting()));
    }

    public Map<String, Long> citasPorNombreDeMedico() {
        return citas.stream().collect(Collectors.groupingBy(
                c -> c.getMedico().getNombreCompleto(), LinkedHashMap::new, Collectors.counting()));
    }

    public double ingresosTotales() {
        return citas.stream().filter(c -> c.requierePago() && c.isPagada()).mapToDouble(Cita::getCosto).sum();
    }

    public Map<Especialidad, Double> ingresosPorEspecialidad() {
        return citas.stream().filter(c -> c.isPagada() && c.requierePago()).collect(Collectors.groupingBy(
                c -> c.getMedico().getEspecialidad(),
                () -> new EnumMap<>(Especialidad.class),
                Collectors.summingDouble(Cita::getCosto)));
    }

    public List<Paciente> pacientesMayoresDe(int edad) {
        return pacientes.values().stream().filter(p -> p.calcularEdad() > edad).toList();
    }

    public List<CitaResumen> resumenDeCitas() {
        return citas.stream().map(CitaResumen::de).toList();
    }

    // ---------------------------------------------------------------- persistencia (serialización)

    public void guardarEn(Path archivo) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(archivo))) {
            out.writeObject(this);
        }
    }

    public static CentroSalud cargarDe(Path archivo) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(archivo))) {
            return (CentroSalud) in.readObject();
        }
    }

    // ---------------------------------------------------------------- getters

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public Collection<Paciente> getPacientes() {
        return Collections.unmodifiableCollection(pacientes.values());
    }

    public List<Medico> getMedicos() {
        return Collections.unmodifiableList(medicos);
    }

    public List<Recepcionista> getPersonal() {
        return Collections.unmodifiableList(personal);
    }

    public List<Consultorio> getConsultorios() {
        return Collections.unmodifiableList(consultorios);
    }

    public List<Cita> getCitas() {
        return Collections.unmodifiableList(citas);
    }
}
