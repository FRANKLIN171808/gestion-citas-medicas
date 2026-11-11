package reportes;

import modelo.CentroSalud;
import modelo.Medico;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reporte HTML para recepción: muestra, por cada día de atención (lunes a sábado), qué médicos
 * tienen turnos libres y a qué hora, junto con los datos de cada médico.
 * Un turno está libre según la misma regla que usa la programación de citas
 * ({@link Medico#estaDisponible}), y los turnos que ya pasaron no se muestran.
 */
public class ReporteDisponibilidad {

    private static final Locale ES = Locale.of("es", "PE");
    private static final DateTimeFormatter DIA_LARGO = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE d/M", ES);
    private static final DateTimeFormatter RANGO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter GENERADO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private enum Turno { LIBRE, OCUPADO, PASADO }

    private final CentroSalud centro;
    private final List<LocalDate> fechas = new ArrayList<>();
    private final LocalDateTime ahora;

    /**
     * @param desde primer día a considerar (si es domingo, se empieza el lunes)
     * @param dias  cuántos días de atención mostrar; los domingos se saltan
     */
    public ReporteDisponibilidad(CentroSalud centro, LocalDate desde, int dias, LocalDateTime ahora) {
        if (dias < 1) {
            throw new IllegalArgumentException("Debe mostrar al menos un día");
        }
        this.centro = centro;
        this.ahora = ahora;
        for (LocalDate f = desde; fechas.size() < dias; f = f.plusDays(1)) {
            if (Medico.esDiaDeAtencion(f)) {
                fechas.add(f);
            }
        }
    }

    public void exportar(Path archivo) throws IOException {
        Files.writeString(archivo, generar(), StandardCharsets.UTF_8);
    }

    public String generar() {
        List<Medico> medicos = centro.getMedicos();

        int libres = 0;
        int ocupados = 0;
        for (Medico m : medicos) {
            for (LocalDate f : fechas) {
                for (LocalTime t : m.turnosDelDia()) {
                    switch (estado(m, f, t)) {
                        case LIBRE -> libres++;
                        case OCUPADO -> ocupados++;
                        default -> { /* pasado: no cuenta */ }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder(16_000);
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Disponibilidad de médicos</title>\n<style>\n").append(CSS).append("</style>\n</head>\n<body>\n");

        sb.append("<header><h1>Disponibilidad de médicos</h1><p>")
                .append(Reporte.escapar(centro.getNombre())).append(" · del ")
                .append(RANGO.format(fechas.get(0))).append(" al ").append(RANGO.format(fechas.get(fechas.size() - 1)))
                .append("</p></header>\n<main>\n");

        sb.append("<section class=\"tarjetas\">")
                .append(tarjeta("Médicos", String.valueOf(medicos.size())))
                .append(tarjeta("Días de atención", String.valueOf(fechas.size())))
                .append(tarjeta("Turnos libres", String.valueOf(libres)))
                .append(tarjeta("Turnos ocupados", String.valueOf(ocupados)))
                .append("</section>\n");

        sb.append("<div class=\"barra\"><label>Especialidad: <select id=\"filtro\">")
                .append("<option value=\"\">Todas</option>");
        medicos.stream().map(m -> m.getEspecialidad().getNombre()).distinct().sorted()
                .forEach(e -> sb.append("<option>").append(Reporte.escapar(e)).append("</option>"));
        sb.append("</select></label></div>\n");

        sb.append("<p class=\"leyenda\"><span class=\"chip libre\">09:00</span> libre &nbsp; ")
                .append("<span class=\"chip ocupado\">09:00</span> ocupado &nbsp; ")
                .append("Atención de lunes a sábado, de ")
                .append(HORA.format(Medico.INICIO_ATENCION)).append(" a ").append(HORA.format(Medico.FIN_ATENCION))
                .append(" · turnos de ").append(Medico.MINUTOS_POR_TURNO)
                .append(" min · los turnos que ya pasaron no se muestran.</p>\n");

        if (medicos.isEmpty()) {
            sb.append("<p class=\"vacio\">No hay médicos registrados.</p>\n");
        } else {
            resumen(sb, medicos);
            for (LocalDate f : fechas) {
                dia(sb, medicos, f);
            }
            directorio(sb, medicos);
        }

        sb.append("</main>\n<footer>Generado el ").append(GENERADO.format(ahora)).append("</footer>\n")
                .append("<script>")
                .append("document.getElementById('filtro').addEventListener('change',function(){")
                .append("var v=this.value;document.querySelectorAll('[data-esp]').forEach(function(e){")
                .append("e.style.display=(!v||e.dataset.esp===v)?'':'none';});});")
                .append("</script>\n</body>\n</html>\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------ secciones

    /** Matriz médico x día con la cantidad de turnos libres: sirve para ver "qué días hay cupo". */
    private void resumen(StringBuilder sb, List<Medico> medicos) {
        sb.append("<h2>Resumen: turnos libres por día</h2>\n<div class=\"tabla\"><table>\n<tr><th>Médico</th>");
        fechas.forEach(f -> sb.append("<th class=\"c\">").append(DIA_CORTO.format(f)).append("</th>"));
        sb.append("</tr>\n");
        for (Medico m : medicos) {
            sb.append("<tr data-esp=\"").append(Reporte.escapar(m.getEspecialidad().getNombre())).append("\"><td>")
                    .append("<strong>").append(Reporte.escapar(m.getNombreCompleto())).append("</strong><br><small>")
                    .append(Reporte.escapar(m.getEspecialidad().getNombre())).append("</small></td>");
            for (LocalDate f : fechas) {
                long n = m.turnosDelDia().stream().filter(t -> estado(m, f, t) == Turno.LIBRE).count();
                sb.append("<td class=\"c ").append(n == 0 ? "sin" : "con").append("\">")
                        .append(n == 0 ? "Sin cupo" : n == 1 ? "1 libre" : n + " libres").append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table></div>\n");
    }

    private void dia(StringBuilder sb, List<Medico> medicos, LocalDate fecha) {
        String titulo = DIA_LARGO.format(fecha);
        sb.append("<section class=\"dia\"><h2>").append(Character.toUpperCase(titulo.charAt(0)))
                .append(titulo.substring(1)).append("</h2>\n<div class=\"tabla\"><table>\n")
                .append("<tr><th>Médico</th><th>Teléfono</th><th>Turnos</th></tr>\n");
        for (Medico m : medicos) {
            sb.append("<tr data-esp=\"").append(Reporte.escapar(m.getEspecialidad().getNombre())).append("\"><td>")
                    .append("<strong>").append(Reporte.escapar(m.getNombreCompleto())).append("</strong><br><small>")
                    .append(Reporte.escapar(m.getEspecialidad().getNombre())).append("</small></td><td>")
                    .append(dato(m.getTelefono())).append("</td><td class=\"chips\">");
            boolean alguno = false;
            for (LocalTime t : m.turnosDelDia()) {
                Turno e = estado(m, fecha, t);
                if (e == Turno.PASADO) {
                    continue;
                }
                alguno = true;
                sb.append("<span class=\"chip ").append(e == Turno.LIBRE ? "libre" : "ocupado").append("\">")
                        .append(HORA.format(t)).append("</span>");
            }
            if (!alguno) {
                sb.append("<span class=\"nada\">Sin turnos restantes</span>");
            }
            sb.append("</td></tr>\n");
        }
        sb.append("</table></div></section>\n");
    }

    private void directorio(StringBuilder sb, List<Medico> medicos) {
        sb.append("<section class=\"dia\"><h2>Datos de los médicos</h2>\n<div class=\"tabla\"><table>\n")
                .append("<tr><th>Médico</th><th>Especialidad</th><th>CMP</th><th>RNE</th><th>DNI</th><th>Teléfono</th>")
                .append("<th>Correo</th><th class=\"num\">Tarifa sin seguro</th></tr>\n");
        for (Medico m : medicos) {
            sb.append("<tr data-esp=\"").append(Reporte.escapar(m.getEspecialidad().getNombre())).append("\"><td>")
                    .append(Reporte.escapar(m.getNombreCompleto())).append("</td><td>")
                    .append(Reporte.escapar(m.getEspecialidad().getNombre())).append("</td><td>")
                    .append(Reporte.escapar(m.getCmpFormateado())).append("</td><td>")
                    .append(m.tieneRne() ? Reporte.escapar(m.getRneFormateado()) : "—").append("</td><td>")
                    .append(Reporte.escapar(m.getDni())).append("</td><td>")
                    .append(dato(m.getTelefono())).append("</td><td>")
                    .append(dato(m.getEmail())).append("</td><td class=\"num\">S/ ")
                    .append("%.2f".formatted(m.getEspecialidad().getTarifa())).append("</td></tr>\n");
        }
        sb.append("</table></div>\n<p class=\"nota\">Posta del MINSA: los pacientes con SIS no pagan; con EsSalud, EPS, FF.AA. o sin seguro se cobra la tarifa como particular.</p></section>\n");
    }

    // ------------------------------------------------------------------ ayudas

    private Turno estado(Medico m, LocalDate fecha, LocalTime hora) {
        LocalDateTime momento = fecha.atTime(hora);
        if (momento.isBefore(ahora)) {
            return Turno.PASADO;
        }
        return m.estaDisponible(momento) ? Turno.LIBRE : Turno.OCUPADO;
    }

    private static String tarjeta(String etiqueta, String valor) {
        return "<div class=\"tarjeta\"><span>" + etiqueta + "</span><strong>" + valor + "</strong></div>";
    }

    private static String dato(String texto) {
        return texto == null || texto.isBlank() ? "—" : Reporte.escapar(texto);
    }

    private static final String CSS = """
            :root{--azul:#1e6fb8;--fondo:#f3f6fa;--texto:#1f2937;--borde:#dbe3ec}
            *{box-sizing:border-box}
            body{margin:0;font-family:"Segoe UI",Roboto,Arial,sans-serif;background:var(--fondo);color:var(--texto)}
            header{background:linear-gradient(135deg,#1e6fb8,#2a9d8f);color:#fff;padding:28px 40px 52px}
            header h1{margin:0;font-size:26px;font-weight:600}
            header p{margin:6px 0 0;opacity:.9;font-size:14px}
            main{max-width:1100px;margin:-24px auto 40px;padding:0 20px}
            h2{font-size:18px;margin:28px 0 10px;color:#1b4b7a}
            .tarjetas{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:14px;margin-bottom:18px}
            .tarjeta{background:#fff;border-radius:12px;padding:16px 18px;box-shadow:0 2px 8px rgba(15,40,80,.08)}
            .tarjeta span{display:block;font-size:12px;text-transform:uppercase;letter-spacing:.05em;color:#6b7280}
            .tarjeta strong{display:block;margin-top:4px;font-size:24px;color:var(--azul)}
            .barra{display:flex;gap:12px;align-items:center;margin:8px 0}
            .barra select{font:inherit;padding:7px 12px;border-radius:8px;border:1px solid var(--borde);background:#fff}
            .leyenda{font-size:13px;color:#4b5563}
            .tabla{background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(15,40,80,.08);overflow-x:auto}
            table{width:100%;border-collapse:collapse;font-size:14px}
            th{background:#eaf1f9;color:#1b4b7a;text-align:left;padding:11px 14px;font-weight:600;white-space:nowrap}
            td{padding:10px 14px;border-top:1px solid var(--borde);vertical-align:top}
            small{color:#6b7280}
            .c{text-align:center}.num{text-align:right}
            td.con{color:#15803d;font-weight:600}td.sin{color:#9ca3af}
            .chips{line-height:2.1}
            .chip{display:inline-block;margin:0 4px 0 0;padding:1px 9px;border-radius:999px;font-size:12px;font-variant-numeric:tabular-nums}
            .chip.libre{background:#dcfce7;color:#15803d;font-weight:600}
            .chip.ocupado{background:#e5e7eb;color:#6b7280;text-decoration:line-through}
            .nada,.vacio,.nota{color:#6b7280;font-size:13px}
            footer{text-align:center;color:#6b7280;font-size:12px;padding-bottom:30px}
            """;
}
