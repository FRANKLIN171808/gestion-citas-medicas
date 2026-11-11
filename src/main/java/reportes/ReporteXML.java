package reportes;

import modelo.Cita;

import java.util.List;

/**
 * Exporta las citas como un documento XML.
 */
public class ReporteXML extends Reporte {

    public ReporteXML(String titulo) {
        super(titulo);
    }

    @Override
    protected String encabezado(List<Cita> citas) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<citas titulo=\"" + escapar(titulo) + "\">\n";
    }

    @Override
    protected String formatearFila(Cita c) {
        return """
                  <cita id="%d">
                    <fecha>%s</fecha>
                    <paciente>%s</paciente>
                    <medico>%s</medico>
                    <estado>%s</estado>
                    <costo>%.2f</costo>
                    <seguro>%s</seguro>
                    <pagada>%s</pagada>
                  </cita>
                """.formatted(c.getId(), c.getFechaHora().format(Cita.FORMATO),
                escapar(c.getPaciente().getNombreCompleto()),
                escapar(c.getMedico().getNombreCompleto()),
                c.getEstado(), c.getCosto(), escapar(c.getPaciente().getSeguro()), c.isPagada());
    }

    @Override
    protected String pie(List<Cita> citas) {
        return "</citas>\n";
    }
}
