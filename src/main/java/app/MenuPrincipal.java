package app;

import dao.CitaDAO;
import dao.ConexionBD;
import modelo.CentroSalud;
import modelo.Cita;
import modelo.CitaResumen;
import modelo.Consultorio;
import modelo.Especialidad;
import modelo.EstadoCita;
import modelo.HistoriaClinica;
import modelo.Medico;
import modelo.Paciente;
import modelo.Recepcionista;
import modelo.TipoSeguro;
import pagos.MetodoPago;
import pagos.PagoEfectivo;
import pagos.PagoTarjeta;
import reportes.ComprobanteCita;
import reportes.Reporte;
import reportes.ReporteDisponibilidad;
import reportes.ReporteHTML;
import reportes.ReporteXML;
import util.Consola;
import util.LectorConsola;
import util.Navegador;
import util.Paginador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Menú de consola de la recepción. Los médicos, consultorios y recepcionistas ya vienen cargados
 * (base de datos o personal predeterminado), así que aquí solo se eligen escribiendo su número.
 */
public class MenuPrincipal {

    private static final Path ARCHIVO_DATOS = Path.of("datos_centro.dat");
    private static final Path CARPETA_REPORTES = Path.of("reportes_generados");
    private static final int CITAS_POR_PAGINA = 5;
    private static final int DIAS_SUGERIDOS = 6;
    private static final int DIAS_DE_BUSQUEDA = 60;
    private static final int DIAS_REPORTE_DISPONIBILIDAD = 7;
    private static final Locale ES = Locale.of("es", "PE");
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEEE dd/MM", ES);

    /** Se lanza cuando el usuario escribe 0 en una lista para volver sin hacer nada. */
    private static final class OperacionCancelada extends RuntimeException {
        private static final long serialVersionUID = 1L;

        OperacionCancelada() {
            super("Operación cancelada");
        }
    }

    private CentroSalud centro;
    private Recepcionista recepcionista;
    private final LectorConsola lector;
    private final CitaDAO citaDAO = new CitaDAO(new ConexionBD());

    public MenuPrincipal(CentroSalud centro, LectorConsola lector) {
        this.centro = centro;
        this.lector = lector;
    }

    public void iniciar() {
        try {
            recepcionista = elegirRecepcionista();
        } catch (OperacionCancelada | IllegalStateException e) {
            System.out.println("\nNo hay recepcionista disponible. Fin del programa.");
            return;
        }
        int opcion;
        do {
            mostrarMenu();
            opcion = lector.leerEntero("Seleccione una opción: ", 0, 12);
            try {
                ejecutar(opcion);
            } catch (OperacionCancelada e) {
                Consola.info("Operación cancelada.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println();
                Consola.aviso("Error: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println();
                Consola.aviso("Error de base de datos: " + e.getMessage());
            } catch (IOException | ClassNotFoundException e) {
                System.out.println();
                Consola.aviso("Error de archivo: " + e.getMessage());
            }
        } while (opcion != 0);
        System.out.println("\nHasta pronto.");
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println(Consola.banner(centro.getNombre()));
        System.out.println("  Recepción: " + recepcionista.getNombreCompleto() + " (turno " + recepcionista.getTurno() + ")");
        System.out.println("""

                  PACIENTES Y CITAS                CONSULTAS
                    1. Registrar paciente            6. Listar citas
                    2. Programar cita                7. Médicos y consultorios
                    3. Cobrar cita (caja)            8. Historia clínica
                    4. Confirmar / cancelar /        9. Reportes y exportar
                       atender cita
                    5. Comprobante de una cita     DATOS
                                                    10. Guardar / cargar archivo
                                                    11. Base de datos SQL Server
                                                    12. Cargar datos de ejemplo

                    0. Salir
                """);
    }

    private void ejecutar(int opcion) throws SQLException, IOException, ClassNotFoundException {
        switch (opcion) {
            case 1 -> registrarPaciente();
            case 2 -> programarCita();
            case 3 -> pagarCita();
            case 4 -> cambiarEstadoCita();
            case 5 -> comprobante();
            case 6 -> listarCitas();
            case 7 -> menuMedicos();
            case 8 -> verHistoria();
            case 9 -> menuReportes();
            case 10 -> menuArchivo();
            case 11 -> menuBaseDatos();
            case 12 -> cargarDatosEjemplo();
            default -> { /* 0: salir */ }
        }
    }

    // ------------------------------------------------------------------ listas numeradas

    /**
     * Muestra una tabla numerada (1, 2, 3...) y devuelve el elemento cuyo número escribe el usuario.
     * Con 0 se cancela la operación.
     */
    private <T> T elegirDeLista(List<T> elementos, List<String> columnas, Function<T, List<String>> fila,
                                String pregunta) {
        List<String> encabezados = new ArrayList<>();
        encabezados.add("N°");
        encabezados.addAll(columnas);
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < elementos.size(); i++) {
            List<String> f = new ArrayList<>();
            f.add(String.valueOf(i + 1));
            f.addAll(fila.apply(elementos.get(i)));
            filas.add(f);
        }
        Consola.tabla(encabezados, filas);
        int n = lector.leerEntero(pregunta + " (0 = cancelar): ", 0, elementos.size());
        if (n == 0) {
            throw new OperacionCancelada();
        }
        return elementos.get(n - 1);
    }

    private Recepcionista elegirRecepcionista() {
        List<Recepcionista> personal = centro.getPersonal();
        if (personal.isEmpty()) {
            throw new IllegalStateException("No hay recepcionistas cargadas");
        }
        if (personal.size() == 1) {
            return personal.get(0);
        }
        Consola.seccion("¿Quién atiende la recepción?");
        return elegirDeLista(personal, List.of("Recepcionista", "Turno"),
                r -> Consola.fila(r.getNombreCompleto(), r.getTurno()), "Recepcionista");
    }

    private Medico elegirMedico() {
        Consola.info("Médicos:");
        return elegirDeLista(centro.getMedicos(), List.of("Médico", "Especialidad", "Registro", "Tarifa"),
                m -> Consola.fila(m.getNombreCompleto(), m.getEspecialidad().getNombre(),
                        m.getCmpFormateado() + (m.tieneRne() ? " / " + m.getRneFormateado() : ""),
                        String.format("S/ %.2f", m.getEspecialidad().getTarifa())),
                "Médico");
    }

    private Consultorio elegirConsultorio(LocalDateTime fechaHora) {
        List<Consultorio> libres = centro.consultoriosLibres(fechaHora);
        if (libres.isEmpty()) {
            throw new IllegalStateException("No hay consultorios libres a esa hora; elija otro turno");
        }
        Consola.info("Consultorios libres a las " + fechaHora.format(Cita.FORMATO) + ":");
        return elegirDeLista(libres, List.of("Consultorio", "Piso"),
                k -> Consola.fila("Consultorio " + k.getNumero(), k.getPiso()), "Consultorio");
    }

    private LocalDate elegirDia(Medico medico) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = hoy; dias.size() < DIAS_SUGERIDOS && d.isBefore(hoy.plusDays(DIAS_DE_BUSQUEDA)); d = d.plusDays(1)) {
            if (!medico.turnosLibres(d, ahora).isEmpty()) {
                dias.add(d);
            }
        }
        if (dias.isEmpty()) {
            throw new IllegalStateException(medico.getNombreCompleto() + " no tiene turnos libres en los próximos "
                    + DIAS_DE_BUSQUEDA + " días");
        }
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < dias.size(); i++) {
            filas.add(Consola.fila(i + 1, etiquetaDia(dias.get(i), hoy), medico.turnosLibres(dias.get(i), ahora).size()));
        }
        Consola.info("Días con turnos libres de " + medico.getNombreCompleto() + ":");
        Consola.tabla(List.of("N°", "Día", "Turnos libres"), filas);
        int otra = dias.size() + 1;
        Consola.info(otra + ". Escribir otra fecha");
        int opcion = lector.leerEntero("Día (0 = cancelar): ", 0, otra);
        if (opcion == 0) {
            throw new OperacionCancelada();
        }
        return opcion == otra ? lector.leerFecha("Fecha") : dias.get(opcion - 1);
    }

    private static String etiquetaDia(LocalDate dia, LocalDate hoy) {
        String texto = DIA_CORTO.format(dia);
        texto = Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
        if (dia.equals(hoy)) {
            return "Hoy - " + texto;
        }
        return dia.equals(hoy.plusDays(1)) ? "Mañana - " + texto : texto;
    }

    private LocalTime elegirTurno(Medico medico, LocalDate dia) {
        List<LocalTime> libres = medico.turnosLibres(dia, LocalDateTime.now());
        if (libres.isEmpty()) {
            throw new IllegalStateException("No hay turnos libres ese día (atiende de lunes a sábado, "
                    + "de 08:00 a 18:00, y no se programan horas que ya pasaron)");
        }
        Consola.info("Turnos libres (cada " + Medico.MINUTOS_POR_TURNO + " min):");
        StringBuilder fila = new StringBuilder("  ");
        for (int i = 0; i < libres.size(); i++) {
            fila.append(String.format("[%2d] %s   ", i + 1, libres.get(i)));
            if ((i + 1) % 5 == 0 || i == libres.size() - 1) {
                System.out.println(fila);
                fila = new StringBuilder("  ");
            }
        }
        int n = lector.leerEntero("Turno (0 = cancelar): ", 0, libres.size());
        if (n == 0) {
            throw new OperacionCancelada();
        }
        return libres.get(n - 1);
    }

    // ------------------------------------------------------------------ pacientes

    private static final List<String> COLUMNAS_PACIENTES = List.of("DNI", "Paciente", "Edad", "Seguro");

    private static List<String> filaPaciente(Paciente p) {
        return Consola.fila(p.getDni(), p.getNombreCompleto(), p.calcularEdad(), p.getSeguro());
    }

    private void registrarPaciente() {
        Consola.seccion("Registrar paciente");
        String dni = lector.leerDni("DNI (8 dígitos): ");
        if (centro.buscarPaciente(dni).isPresent()) {
            throw new IllegalArgumentException("Ya existe un paciente con DNI " + dni);
        }
        registrarPacienteConDni(dni);
    }

    private Paciente registrarPacienteConDni(String dni) {
        String nombres = lector.leerTexto("Nombres: ");
        String apellidos = lector.leerTexto("Apellidos: ");
        LocalDate nacimiento;
        while (true) {
            nacimiento = lector.leerFecha("Fecha de nacimiento");
            if (!nacimiento.isAfter(LocalDate.now())) {
                break;
            }
            System.out.println("  La fecha de nacimiento no puede ser futura.");
        }
        TipoSeguro seguro = elegirSeguro();
        boolean aportes = seguro.requiereAportes() && lector.leerConfirmacion("¿Está al día en sus aportes?");
        Paciente p = new Paciente(dni, nombres, apellidos, nacimiento, seguro, aportes);
        p.setTelefono(lector.leerTextoOpcional("Teléfono (opcional): "));
        p.setGrupoSanguineo(lector.leerTextoOpcional("Grupo sanguíneo (opcional): "));
        centro.registrarPaciente(p);
        Consola.ok("Paciente registrado: " + p);
        Consola.info(p.estaExonerado()
                ? "Cubierto por " + p.getSeguro() + ": sus citas no se cobran."
                : "Pagará sus citas según la tarifa de la especialidad.");
        return p;
    }

    private TipoSeguro elegirSeguro() {
        Consola.info("Seguro del paciente:");
        List<List<String>> filas = new ArrayList<>();
        TipoSeguro[] tipos = TipoSeguro.values();
        for (int i = 0; i < tipos.length; i++) {
            String cobro = switch (tipos[i].getCobro()) {
                case GRATUITO -> "No paga";
                case SEGUN_APORTES -> "No paga si está al día en sus aportes";
                case PARTICULAR -> "Paga la tarifa";
            };
            filas.add(Consola.fila(i + 1, tipos[i].getNombre(), cobro));
        }
        Consola.tabla(List.of("N°", "Seguro", "Cobro en la posta"), filas);
        return tipos[lector.leerEntero("Seguro: ", 1, tipos.length) - 1];
    }

    /** Pide el DNI; con Enter muestra la lista de pacientes para elegir por número. */
    private Paciente pedirPaciente(boolean ofrecerRegistro) {
        while (true) {
            String texto = lector.leerTextoOpcional("DNI del paciente (Enter = ver lista, 0 = cancelar): ");
            if (texto.equals("0")) {
                throw new OperacionCancelada();
            }
            if (texto.isEmpty()) {
                if (centro.getPacientes().isEmpty()) {
                    throw new IllegalStateException("Todavía no hay pacientes registrados (opción 1)");
                }
                return elegirDeLista(List.copyOf(centro.getPacientes()), COLUMNAS_PACIENTES,
                        MenuPrincipal::filaPaciente, "Paciente");
            }
            if (!texto.matches("\\d{8}")) {
                System.out.println("  El DNI debe tener exactamente 8 dígitos.");
                continue;
            }
            var encontrado = centro.buscarPaciente(texto);
            if (encontrado.isPresent()) {
                return encontrado.get();
            }
            if (ofrecerRegistro && lector.leerConfirmacion("No existe un paciente con ese DNI. ¿Registrarlo ahora?")) {
                return registrarPacienteConDni(texto);
            }
            if (!ofrecerRegistro) {
                System.out.println("  No existe un paciente con ese DNI.");
            }
        }
    }

    // ------------------------------------------------------------------ citas

    private void programarCita() {
        Consola.seccion("Programar cita");
        Paciente paciente = pedirPaciente(true);
        Medico medico = elegirMedico();
        LocalDate dia = elegirDia(medico);
        LocalTime hora = elegirTurno(medico, dia);
        LocalDateTime fechaHora = dia.atTime(hora);
        Consultorio consultorio = elegirConsultorio(fechaHora);
        String motivo = lector.leerTextoOpcional("Motivo (Enter = consulta general): ");

        Cita cita = centro.programarCita(recepcionista, paciente, medico, consultorio, fechaHora, motivo);
        Consola.ok("Cita #" + cita.getId() + " programada (" + cita.descripcionCobro() + ")");
        mostrarCitas(List.of(cita));
        if (cita.requierePago()) {
            Consola.info("El paciente debe pasar por caja (opción 3); al pagar, la cita queda confirmada.");
        } else {
            Consola.info("Paciente cubierto por " + paciente.getSeguro() + ": solo falta confirmar la cita (opción 4).");
        }
        if (lector.leerConfirmacion("¿Generar el comprobante de la cita?")) {
            generarComprobante(cita);
        }
    }

    private Cita elegirCita(List<Cita> citas, String vacio) {
        if (citas.isEmpty()) {
            throw new IllegalStateException(vacio);
        }
        mostrarCitas(citas);
        int id = lector.leerEntero("ID de la cita (0 = cancelar): ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            throw new OperacionCancelada();
        }
        return citas.stream().filter(c -> c.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La cita #" + id + " no está en la lista"));
    }

    private void cambiarEstadoCita() {
        Consola.seccion("Confirmar / cancelar / atender una cita");
        Cita cita = elegirCita(centro.citasActivas(), "No hay citas pendientes (programadas o confirmadas)");
        Consola.info("1. Confirmar   2. Cancelar   3. Atender");
        switch (lector.leerEntero("Acción: ", 1, 3)) {
            case 1 -> cita.confirmar();
            case 2 -> cita.cancelar();
            default -> cita.atender();
        }
        Consola.ok("Cita actualizada");
        mostrarCitas(List.of(cita));
    }

    private void pagarCita() {
        Consola.seccion("Caja: cobrar cita");
        Cita cita = elegirCita(centro.citasPendientesDePago(), "No hay citas pendientes de pago");
        System.out.printf("  Monto a pagar: S/ %.2f%n", cita.getCosto());
        Consola.info("1. Tarjeta   2. Efectivo");
        MetodoPago metodo;
        if (lector.leerEntero("Método de pago: ", 1, 2) == 1) {
            metodo = new PagoTarjeta(lector.leerDigitos("Número de tarjeta (16 dígitos): ", 16),
                    lector.leerTexto("Titular: "));
        } else {
            metodo = new PagoEfectivo(lector.leerDecimal("Monto entregado: ", 0.01));
        }
        if (cita.pagar(metodo)) {
            Consola.ok("Pago aceptado con " + metodo.getNombre() + ".");
            if (metodo instanceof PagoEfectivo efectivo) {
                System.out.printf("  Vuelto: S/ %.2f%n", efectivo.calcularVuelto(cita.getCosto()));
            }
            if (cita.getEstado() == EstadoCita.PROGRAMADA) {
                cita.confirmar();
                Consola.info("La cita #" + cita.getId() + " quedó CONFIRMADA.");
            }
        } else {
            Consola.aviso("Pago rechazado (monto insuficiente). La cita sigue pendiente de pago.");
        }
    }

    private void comprobante() throws IOException {
        Consola.seccion("Comprobante de una cita");
        Cita cita = elegirCita(centro.citasOrdenadasPorFecha(), "Todavía no hay citas registradas");
        generarComprobante(cita);
    }

    private void generarComprobante(Cita cita) {
        try {
            Files.createDirectories(CARPETA_REPORTES);
            Path archivo = CARPETA_REPORTES.resolve("comprobante_cita_%05d.html".formatted(cita.getId()));
            new ComprobanteCita(centro).exportar(cita, archivo);
            avisarArchivo(archivo);
        } catch (IOException e) {
            Consola.aviso("No se pudo guardar el comprobante: " + e.getMessage());
        }
    }

    /** Informa dónde quedó el archivo y trata de abrirlo en el navegador. */
    private static void avisarArchivo(Path archivo) {
        Consola.ok("Archivo generado en " + archivo.toAbsolutePath());
        if (Navegador.abrir(archivo.toAbsolutePath())) {
            Consola.info("Se abrió en el navegador.");
        }
    }

    private static final List<String> COLUMNAS_CITAS =
            List.of("ID", "Fecha", "Paciente", "Médico", "Cons.", "Estado", "Cobro", "Pago");

    private static List<String> filaCita(Cita c) {
        String pago = !c.requierePago() ? "Cubierta" : c.isPagada() ? "Pagada" : "Pendiente";
        return Consola.fila(c.getId(), c.getFechaHora().format(Cita.FORMATO),
                c.getPaciente().getNombreCompleto(), c.getMedico().getNombreCompleto(),
                c.getConsultorio() == null ? "-" : c.getConsultorio().getNumero(),
                c.getEstado(), c.descripcionCobro(), pago);
    }

    private static void mostrarCitas(List<Cita> citas) {
        Consola.tabla(COLUMNAS_CITAS, citas.stream().map(MenuPrincipal::filaCita).toList());
    }

    private void listarCitas() {
        Consola.seccion("Citas (ordenadas por fecha)");
        Paginador<Cita> paginador = new Paginador<>(centro.citasOrdenadasPorFecha(), CITAS_POR_PAGINA);
        if (paginador.getTotalElementos() == 0) {
            System.out.println("  No hay citas registradas.");
            return;
        }
        int pagina = 1;
        while (true) {
            System.out.printf("  Página %d de %d%n", pagina, paginador.totalPaginas());
            mostrarCitas(paginador.getPagina(pagina));
            if (pagina >= paginador.totalPaginas() || !lector.leerConfirmacion("¿Ver la siguiente página?")) {
                return;
            }
            pagina++;
        }
    }

    private void verHistoria() {
        Consola.seccion("Historia clínica");
        Paciente paciente = pedirPaciente(false);
        HistoriaClinica h = paciente.getHistoria();
        System.out.printf("  %s | %s | %d años | seguro: %s%n", h.getNumero(),
                paciente.getNombreCompleto(), paciente.calcularEdad(), paciente.getSeguro());
        if (h.getAtenciones().isEmpty()) {
            System.out.println("  Sin citas registradas.");
        } else {
            System.out.println("  Citas (la más reciente primero):");
            mostrarCitas(List.copyOf(h.ultimasAtenciones()));
        }
    }

    // ------------------------------------------------------------------ médicos y consultorios (solo consulta)

    private void menuMedicos() throws IOException {
        Consola.seccion("Médicos y consultorios");
        List<Medico> medicos = centro.getMedicos();
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < medicos.size(); i++) {
            Medico m = medicos.get(i);
            filas.add(Consola.fila(i + 1, m.getNombreCompleto(), m.getEspecialidad().getNombre(),
                    m.getCmpFormateado() + (m.tieneRne() ? " / " + m.getRneFormateado() : ""),
                    m.getTelefono().isEmpty() ? "-" : m.getTelefono(),
                    String.format("S/ %.2f", m.getEspecialidad().getTarifa())));
        }
        Consola.info("Médicos:");
        Consola.tabla(List.of("N°", "Médico", "Especialidad", "Registro", "Teléfono", "Tarifa"), filas);

        List<List<String>> consultorios = new ArrayList<>();
        List<Consultorio> lista = centro.getConsultorios();
        for (int i = 0; i < lista.size(); i++) {
            consultorios.add(Consola.fila(i + 1, "Consultorio " + lista.get(i).getNumero(), lista.get(i).getPiso()));
        }
        Consola.info("Consultorios:");
        Consola.tabla(List.of("N°", "Consultorio", "Piso"), consultorios);
        Consola.info("Atención de lunes a sábado, de 08:00 a 18:00, en turnos de " + Medico.MINUTOS_POR_TURNO + " minutos.");

        System.out.println("""

                  1. Ver turnos libres de un médico
                  2. Reporte de disponibilidad de todos (HTML)
                  0. Volver
                """);
        switch (lector.leerEntero("Opción: ", 0, 2)) {
            case 1 -> {
                Medico medico = elegirMedico();
                LocalDate dia = elegirDia(medico);
                List<LocalTime> libres = medico.turnosLibres(dia, LocalDateTime.now());
                System.out.println("  Turnos libres: " + String.join("  ", libres.stream().map(LocalTime::toString).toList()));
            }
            case 2 -> {
                Files.createDirectories(CARPETA_REPORTES);
                Path archivo = CARPETA_REPORTES.resolve("disponibilidad.html");
                new ReporteDisponibilidad(centro, LocalDate.now(), DIAS_REPORTE_DISPONIBILIDAD, LocalDateTime.now())
                        .exportar(archivo);
                avisarArchivo(archivo);
            }
            default -> { /* 0: volver */ }
        }
    }

    // ------------------------------------------------------------------ reportes y consultas

    private void menuReportes() throws IOException {
        System.out.println("""

                  REPORTES Y CONSULTAS
                  -----------------------
                  1. Cantidad de citas por estado
                  2. Cantidad de citas por médico
                  3. Ingresos (total y por especialidad)
                  4. Pacientes mayores de cierta edad
                  5. Citas de un día
                  6. Exportar citas a HTML
                  7. Exportar citas a XML
                  0. Volver
                """);
        int opcion = lector.leerEntero("Opción: ", 0, 7);
        switch (opcion) {
            case 1 -> centro.citasPorEstado().forEach((estado, n) ->
                    System.out.printf("  %-10s : %d%n", estado, n));
            case 2 -> centro.citasPorNombreDeMedico().forEach((medico, n) ->
                    System.out.printf("  %-25s : %d%n", medico, n));
            case 3 -> {
                Consola.info("Solo se cobra a pacientes sin SIS (o con SIS independiente sin aportes al día).");
                System.out.printf("  Ingresos totales: S/ %.2f%n", centro.ingresosTotales());
                Map<Especialidad, Double> porEsp = centro.ingresosPorEspecialidad();
                porEsp.forEach((esp, total) -> System.out.printf("  %-18s : S/ %.2f%n", esp, total));
            }
            case 4 -> {
                int edad = lector.leerEntero("Edad mínima (se muestran los mayores a ese valor): ", 0, 120);
                List<Paciente> lista = centro.pacientesMayoresDe(edad);
                if (lista.isEmpty()) {
                    System.out.println("  Ningún paciente cumple la condición.");
                }
                lista.forEach(p -> System.out.printf("  %s (%d años)%n", p, p.calcularEdad()));
            }
            case 5 -> {
                List<Cita> citas = centro.citasDelDia(lector.leerFecha("Día a consultar"));
                if (citas.isEmpty()) {
                    Consola.aviso("No hay citas ese día.");
                } else {
                    mostrarCitas(citas);
                }
            }
            case 6, 7 -> exportar(opcion == 6);
            default -> { /* 0: volver */ }
        }
    }

    /** Exporta todas las citas; el tipo de reporte se elige en tiempo de ejecución (polimorfismo). */
    private void exportar(boolean enHtml) throws IOException {
        String titulo = "Reporte de citas - " + centro.getNombre();
        Reporte reporte = enHtml ? new ReporteHTML(titulo) : new ReporteXML(titulo);
        Files.createDirectories(CARPETA_REPORTES);
        Path archivo = CARPETA_REPORTES.resolve(enHtml ? "citas.html" : "citas.xml");
        reporte.exportar(centro.citasOrdenadasPorFecha(), archivo);
        if (enHtml) {
            avisarArchivo(archivo);
        } else {
            Consola.ok("Archivo generado en " + archivo.toAbsolutePath());
        }
    }

    // ------------------------------------------------------------------ archivo y base de datos

    private void menuArchivo() throws IOException, ClassNotFoundException {
        Consola.info("1. Guardar datos en archivo   2. Cargar datos desde archivo");
        if (lector.leerEntero("Opción: ", 1, 2) == 1) {
            centro.guardarEn(ARCHIVO_DATOS);
            System.out.println("  Datos guardados en " + ARCHIVO_DATOS.toAbsolutePath());
        } else {
            if (!Files.exists(ARCHIVO_DATOS)) {
                throw new IllegalStateException("Todavía no existe el archivo " + ARCHIVO_DATOS);
            }
            centro = CentroSalud.cargarDe(ARCHIVO_DATOS);
            // La recepcionista de esta sesión es la misma persona en el centro cargado (se compara por DNI).
            recepcionista = centro.getPersonal().stream().filter(recepcionista::equals).findFirst()
                    .orElseGet(this::elegirRecepcionista);
            System.out.println("  Datos cargados: " + centro.getCitas().size() + " cita(s), "
                    + centro.getPacientes().size() + " paciente(s).");
        }
    }

    private void menuBaseDatos() throws SQLException {
        System.out.println("""

                  BASE DE DATOS SQL SERVER
                  -----------------------
                  1. Probar conexión
                  2. Guardar las citas en la base de datos
                  3. Ver las citas guardadas en la base de datos
                  4. Eliminar una cita de la base de datos
                  0. Volver
                """);
        switch (lector.leerEntero("Opción: ", 0, 4)) {
            case 1 -> {
                ConexionBD bd = new ConexionBD();
                Consola.info("URL: " + bd.getUrl());
                Consola.info("Usuario: " + bd.getUsuario());
                String problema = bd.diagnosticar();
                if (problema == null) {
                    Consola.ok("Conexión exitosa.");
                } else {
                    Consola.aviso("No se pudo conectar. Motivo real del driver:");
                    Consola.info(problema);
                }
            }
            case 2 -> {
                int nuevas = 0;
                int actualizadas = 0;
                for (Cita cita : centro.getCitas()) {
                    if (citaDAO.buscarPorId(cita.getId()).isPresent()) {
                        citaDAO.actualizar(cita);
                        actualizadas++;
                    } else {
                        citaDAO.insertar(cita);
                        nuevas++;
                    }
                }
                System.out.printf("  Citas insertadas: %d | actualizadas: %d%n", nuevas, actualizadas);
            }
            case 3 -> {
                List<CitaResumen> resumen = citaDAO.resumen();
                if (resumen.isEmpty()) {
                    System.out.println("  No hay citas en la base de datos.");
                }
                resumen.forEach(r -> System.out.printf("  #%d | %s | %s | %s | %s%n", r.idCita(), r.fechaHora().format(Cita.FORMATO),
                        r.paciente(), r.medico(), r.estado()));
            }
            case 4 -> {
                int id = lector.leerEntero("ID de la cita a eliminar: ", 1, Integer.MAX_VALUE);
                if (lector.leerConfirmacion("¿Seguro que desea eliminar la cita #" + id + " de la base de datos?")) {
                    citaDAO.eliminar(id);
                    System.out.println("  Cita eliminada de la base de datos.");
                }
            }
            default -> { /* 0: volver */ }
        }
    }

    // ------------------------------------------------------------------ datos de ejemplo

    /** Pacientes y citas de prueba para la demostración; el personal ya está cargado. */
    private void cargarDatosEjemplo() {
        if (!centro.getPacientes().isEmpty() || !centro.getCitas().isEmpty()) {
            throw new IllegalStateException("Ya hay pacientes o citas; use esta opción solo con el sistema vacío");
        }
        List<Medico> medicos = centro.getMedicos();
        List<Consultorio> consultorios = centro.getConsultorios();
        if (medicos.size() < 2 || consultorios.size() < 2) {
            throw new IllegalStateException("Se necesitan al menos 2 médicos y 2 consultorios cargados");
        }
        Paciente ana = new Paciente("10000001", "Ana", "Torres", LocalDate.of(1990, 5, 10), TipoSeguro.SIS_GRATUITO);
        Paciente luis = new Paciente("10000002", "Luis", "Pérez", LocalDate.of(1955, 11, 22), TipoSeguro.ESSALUD);
        Paciente rosa = new Paciente("10000003", "Rosa", "Quispe", LocalDate.of(1988, 3, 15), TipoSeguro.SIN_SEGURO);
        Paciente pedro = new Paciente("10000004", "Pedro", "Campos", LocalDate.of(1972, 7, 2), TipoSeguro.SIS_INDEPENDIENTE, true);
        List.of(ana, luis, rosa, pedro).forEach(centro::registrarPaciente);

        LocalDate dia = LocalDate.now().plusDays(1);
        while (!Medico.esDiaDeAtencion(dia)) {
            dia = dia.plusDays(1);
        }
        LocalDateTime nueve = dia.atTime(9, 0);
        Medico general = medicos.get(0);
        Medico dentista = medicos.get(1);
        centro.programarCita(recepcionista, ana, general, consultorios.get(0), nueve, "Control general");
        centro.programarCita(recepcionista, luis, dentista, consultorios.get(1), nueve.plusHours(1), "Dolor de muela");
        centro.programarCita(recepcionista, rosa, dentista, consultorios.get(1), nueve.plusHours(2), "Limpieza dental");
        centro.programarCita(recepcionista, pedro, general, consultorios.get(0), nueve.plusMinutes(30), "Control de presión");
        Consola.ok("Datos de ejemplo cargados: 4 pacientes (SIS gratuito, EsSalud, sin seguro y SIS independiente) y 4 citas.");
    }
}
