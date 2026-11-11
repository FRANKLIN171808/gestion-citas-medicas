package reportes;

import modelo.CentroSalud;
import modelo.Cita;
import modelo.EstadoCita;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Comprobante de una cita en HTML, para consultarlo en el navegador.
 */
public class ComprobanteCita {

    private static final Locale ES = Locale.of("es", "PE");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CentroSalud centro;

    public ComprobanteCita(CentroSalud centro) {
        this.centro = centro;
    }

    public void exportar(Cita cita, Path archivo) throws IOException {
        Files.writeString(archivo, generar(cita), StandardCharsets.UTF_8);
    }

    public String generar(Cita c) {
        String dia = DIA.format(c.getFechaHora());
        dia = Character.toUpperCase(dia.charAt(0)) + dia.substring(1);
        String consultorio = c.getConsultorio() == null ? "Por asignar"
                : "Consultorio " + c.getConsultorio().getNumero() + " · piso " + c.getConsultorio().getPiso();
        String pago = !c.requierePago() ? "Sin costo: exonerada (" + c.getPaciente().getSeguro() + ")"
                : "S/ %.2f".formatted(c.getCosto()) + (c.isPagada() ? " · pagada (" + c.getMetodoPago() + ")" : " · pago pendiente");
        boolean ordenPago = c.requierePago() && !c.isPagada() && c.getEstado() != EstadoCita.CANCELADA;
        String titulo = "Cita #" + c.getId() + " - " + c.getPaciente().getNombreCompleto();

        StringBuilder sb = new StringBuilder(6_000);
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>")
                .append(Reporte.escapar(titulo)).append("</title>\n<style>\n").append(CSS).append("</style>\n</head>\n<body>\n")
                .append("<article class=\"hoja\">\n")
                .append("<header><div><h1>").append(Reporte.escapar(centro.getNombre())).append("</h1><p>")
                .append(Reporte.escapar(centro.getDireccion())).append("</p></div>")
                .append("<div class=\"num\"><span>Comprobante de cita</span><strong>N° ")
                .append("%05d".formatted(c.getId())).append("</strong></div></header>\n")
                .append("<div class=\"fecha\"><strong>").append(Reporte.escapar(dia)).append("</strong>")
                .append("<span>").append(HORA.format(c.getFechaHora())).append(" h</span>")
                .append("<em class=\"estado ").append(c.getEstado().name()).append("\">").append(c.getEstado()).append("</em></div>\n")
                .append("<dl>\n")
                .append(fila("Paciente", c.getPaciente().getNombreCompleto() + " (DNI " + c.getPaciente().getDni() + ")"))
                .append(fila("Seguro", c.getPaciente().getSeguro()
                        + (c.getPaciente().getTipoSeguro().requiereAportes()
                        ? (c.getPaciente().isAportesAlDia() ? " · aportes al día" : " · aportes pendientes") : "")))
                .append(fila("Médico", c.getMedico().getNombreCompleto() + " · " + c.getMedico().getCmpFormateado()))
                .append(fila("Especialidad", c.getMedico().getEspecialidad().getNombre()
                        + (c.getMedico().tieneRne() ? " · " + c.getMedico().getRneFormateado() : "")))
                .append(fila("Lugar", consultorio))
                .append(fila("Motivo", c.getMotivo()))
                .append(fila("Costo", pago))
                .append("</dl>\n");

        if (ordenPago) {
            sb.append("<section class=\"orden\"><strong>Orden de pago: S/ ").append("%.2f".formatted(c.getCosto()))
                    .append("</strong><span>Cancele en caja antes de que se confirme la cita.</span></section>\n");
        }
        sb.append("<section class=\"indicaciones\"><h2>Indicaciones</h2><ul>")
                .append("<li>Llegue 15 minutos antes de la hora indicada y presente su DNI.</li>");
        if (ordenPago) {
            sb.append("<li>El pago se realiza en caja (efectivo o tarjeta); la cita se confirma una vez pagada.</li>");
        } else if (c.getEstado() == EstadoCita.PROGRAMADA) {
            sb.append("<li>Esta cita aún debe ser confirmada por el centro de salud.</li>");
        }
        sb.append("<li>Si no puede asistir, avise con anticipación para liberar el horario.</li></ul></section>\n")
                .append("<footer>Documento informativo · no es comprobante de pago</footer>\n</article>\n</body>\n</html>\n");
        return sb.toString();
    }

    private static String fila(String etiqueta, String valor) {
        return "<div><dt>" + etiqueta + "</dt><dd>" + Reporte.escapar(valor) + "</dd></div>\n";
    }

    private static final String CSS = """
            *{box-sizing:border-box}
            body{margin:0;padding:24px 12px;background:#eef2f7;font-family:"Segoe UI",Roboto,Arial,sans-serif;color:#1f2937}
            .hoja{max-width:560px;margin:0 auto;background:#fff;border-radius:12px;padding:28px 32px;box-shadow:0 2px 10px rgba(15,40,80,.1)}
            header{display:flex;justify-content:space-between;gap:16px;border-bottom:3px solid #1e6fb8;padding-bottom:14px}
            h1{margin:0;font-size:20px;color:#1b4b7a}
            header p{margin:4px 0 0;font-size:13px;color:#6b7280}
            .num{text-align:right}.num span{display:block;font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:#6b7280}
            .num strong{font-size:20px;color:#1e6fb8;white-space:nowrap}
            .fecha{display:flex;align-items:baseline;gap:14px;margin:18px 0 6px;flex-wrap:wrap}
            .fecha strong{font-size:18px}.fecha span{font-size:22px;font-weight:600;color:#1e6fb8}
            .estado{margin-left:auto;font-style:normal;font-size:12px;font-weight:600;padding:3px 12px;border-radius:999px;background:#e5e7eb;color:#374151}
            .estado.CONFIRMADA{background:#dbeafe;color:#1d4ed8}.estado.ATENDIDA{background:#dcfce7;color:#15803d}
            .estado.CANCELADA{background:#fee2e2;color:#b91c1c}
            dl{margin:10px 0 0}dl div{display:flex;gap:12px;padding:8px 0;border-bottom:1px solid #e5e9f0;font-size:14px}
            dt{width:110px;flex:none;color:#6b7280}dd{margin:0;font-weight:500}
            .orden{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-top:16px;padding:10px 16px;border-radius:8px;background:#fef3c7;color:#92400e;font-size:13px}
            .orden strong{font-size:16px;white-space:nowrap}
            .indicaciones{margin-top:18px;background:#f3f6fa;border-radius:8px;padding:10px 16px;font-size:13px}
            .indicaciones h2{margin:0 0 4px;font-size:13px;text-transform:uppercase;letter-spacing:.05em;color:#1b4b7a}
            .indicaciones ul{margin:0;padding-left:18px}.indicaciones li{margin:3px 0}
            footer{margin-top:16px;text-align:center;font-size:11px;color:#9ca3af}
            """;
}
