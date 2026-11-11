package reportes;

import modelo.Cita;
import modelo.EstadoCita;

import java.util.List;

/**
 * Exporta las citas como una página HTML con resumen y tabla con estilo.
 */
public class ReporteHTML extends Reporte {

    public ReporteHTML(String titulo) {
        super(titulo);
    }

    @Override
    protected String encabezado(List<Cita> citas) {
        long atendidas = citas.stream().filter(c -> c.getEstado() == EstadoCita.ATENDIDA).count();
        long cubiertas = citas.stream().filter(c -> !c.requierePago()).count();
        double cobrado = citas.stream().filter(c -> c.requierePago() && c.isPagada()).mapToDouble(Cita::getCosto).sum();
        double pendiente = citas.stream().filter(c -> c.requierePago() && !c.isPagada()
                && c.getEstado() != EstadoCita.CANCELADA).mapToDouble(Cita::getCosto).sum();
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%1$s</title>
                <style>
                  :root{--azul:#1e6fb8;--fondo:#f3f6fa;--texto:#1f2937;--borde:#dbe3ec}
                  *{box-sizing:border-box}
                  body{margin:0;font-family:"Segoe UI",Roboto,Arial,sans-serif;background:var(--fondo);color:var(--texto)}
                  header{background:linear-gradient(135deg,#1e6fb8,#2a9d8f);color:#fff;padding:28px 40px 52px}
                  header h1{margin:0;font-size:26px;font-weight:600}
                  header p{margin:6px 0 0;opacity:.85;font-size:14px}
                  main{max-width:1100px;margin:-24px auto 40px;padding:0 20px}
                  .tarjetas{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:14px;margin-bottom:22px}
                  .tarjeta{background:#fff;border-radius:12px;padding:16px 18px;box-shadow:0 2px 8px rgba(15,40,80,.08)}
                  .tarjeta span{display:block;font-size:12px;text-transform:uppercase;letter-spacing:.05em;color:#6b7280}
                  .tarjeta strong{display:block;margin-top:4px;font-size:24px;color:var(--azul)}
                  .tabla{background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(15,40,80,.08);overflow-x:auto}
                  table{width:100%%;border-collapse:collapse;font-size:14px}
                  th{background:#eaf1f9;color:#1b4b7a;text-align:left;padding:12px 14px;font-weight:600;white-space:nowrap}
                  td{padding:11px 14px;border-top:1px solid var(--borde)}
                  tr:hover td{background:#f8fbff}
                  .num{text-align:right;font-variant-numeric:tabular-nums}
                  .badge{display:inline-block;padding:3px 10px;border-radius:999px;font-size:12px;font-weight:600}
                  .PROGRAMADA{background:#e5e7eb;color:#374151}.CONFIRMADA{background:#dbeafe;color:#1d4ed8}
                  .ATENDIDA{background:#dcfce7;color:#15803d}.CANCELADA{background:#fee2e2;color:#b91c1c}
                  .cubierta{background:#ccfbf1;color:#0f766e}.pagada{background:#dcfce7;color:#15803d}
                  .pendiente{background:#fef3c7;color:#b45309}
                  footer{text-align:center;color:#6b7280;font-size:12px;padding-bottom:30px}
                </style>
                </head>
                <body>
                <header><h1>%1$s</h1><p>Los pacientes con seguro no pagan; solo se cobra a quienes no tienen seguro.</p></header>
                <main>
                <section class="tarjetas">
                  <div class="tarjeta"><span>Total de citas</span><strong>%2$d</strong></div>
                  <div class="tarjeta"><span>Atendidas</span><strong>%3$d</strong></div>
                  <div class="tarjeta"><span>Cubiertas por seguro</span><strong>%4$d</strong></div>
                  <div class="tarjeta"><span>Cobrado</span><strong>S/ %5$.2f</strong></div>
                  <div class="tarjeta"><span>Por cobrar</span><strong>S/ %6$.2f</strong></div>
                </section>
                <div class="tabla">
                <table>
                <tr><th>ID</th><th>Fecha</th><th>Paciente</th><th>Seguro</th><th>Médico</th><th>Estado</th><th class="num">Costo</th><th>Pago</th></tr>
                """.formatted(escapar(titulo), citas.size(), atendidas, cubiertas, cobrado, pendiente);
    }

    @Override
    protected String formatearFila(Cita c) {
        String claseEstado = c.getEstado().name();
        String costo = c.requierePago() ? "S/ %.2f".formatted(c.getCosto()) : "—";
        String pago = !c.requierePago() ? "<span class=\"badge cubierta\">Cubierta por seguro</span>"
                : c.isPagada() ? "<span class=\"badge pagada\">Pagada</span>"
                : "<span class=\"badge pendiente\">Pendiente</span>";
        return ("<tr><td>%d</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>"
                + "<td><span class=\"badge %s\">%s</span></td><td class=\"num\">%s</td><td>%s</td></tr>%n").formatted(
                c.getId(), c.getFechaHora().format(Cita.FORMATO),
                escapar(c.getPaciente().getNombreCompleto()),
                escapar(c.getPaciente().getSeguro()),
                escapar(c.getMedico().getNombreCompleto()),
                claseEstado, c.getEstado(), costo, pago);
    }

    @Override
    protected String pie(List<Cita> citas) {
        return "</table>\n</div>\n</main>\n<footer>Reporte generado automáticamente</footer>\n</body>\n</html>\n";
    }
}
