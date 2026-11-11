package app;

import dao.CentroDAO;
import dao.ConexionBD;
import modelo.CentroSalud;
import util.Consola;
import util.LectorConsola;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.NoSuchElementException;

/**
 * Punto de entrada del sistema de gestión de citas médicas.
 */
public class Main {

    private static final String NOMBRE_CENTRO = "Posta Médica San Martín";
    private static final String DIRECCION_CENTRO = "Av. Principal 123";

    public static void main(String[] args) {
        // Permite mostrar tildes y eñes correctamente en la consola de IntelliJ.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        CentroSalud centro = iniciarCentro();
        MenuPrincipal menu = new MenuPrincipal(centro, new LectorConsola());
        try {
            menu.iniciar();
        } catch (NoSuchElementException e) {
            System.out.println("\nLa entrada de datos se cerró. Fin del programa.");
        }
    }

    /**
     * Arma el centro con su personal predeterminado: primero lo lee de SQL Server (junto con los pacientes y
     * citas ya guardados) y, si no hay conexión o la base no está cargada, usa {@link PersonalPredeterminado}.
     */
    private static CentroSalud iniciarCentro() {
        CentroDAO dao = new CentroDAO(new ConexionBD());
        CentroSalud desdeBD = new CentroSalud(NOMBRE_CENTRO, DIRECCION_CENTRO);
        String motivoRespaldo;
        try {
            dao.cargarEn(desdeBD);
            dao.getAdvertencias().forEach(Consola::aviso);
            if (!desdeBD.getMedicos().isEmpty() && !desdeBD.getPersonal().isEmpty()
                    && !desdeBD.getConsultorios().isEmpty()) {
                Consola.ok("Datos cargados desde SQL Server: " + desdeBD.getMedicos().size() + " médicos, "
                        + desdeBD.getConsultorios().size() + " consultorios, " + desdeBD.getPacientes().size()
                        + " pacientes, " + desdeBD.getCitas().size() + " citas.");
                return desdeBD;
            }
            motivoRespaldo = "la base no tiene el personal cargado (ejecute sql/schema.sql)";
        } catch (SQLException | RuntimeException e) {
            motivoRespaldo = "sin conexión a SQL Server";
        }
        Consola.aviso("Se usa el personal predeterminado de respaldo: " + motivoRespaldo + ".");
        CentroSalud respaldo = new CentroSalud(NOMBRE_CENTRO, DIRECCION_CENTRO);
        PersonalPredeterminado.cargarEn(respaldo);
        return respaldo;
    }
}
