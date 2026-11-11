package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Crea las conexiones JDBC a SQL Server.
 * <p>
 * Los datos de conexión se toman de las variables de entorno DB_URL, DB_USER y DB_PASSWORD;
 * si no existen, se usan los valores por defecto de un SQL Server local (autenticación de SQL Server).
 */
public class ConexionBD {

    private final String url;
    private final String usuario;
    private final String clave;

    public ConexionBD() {
        this(valor("DB_URL", "jdbc:sqlserver://localhost:1433;databaseName=citas_medicas;encrypt=true;trustServerCertificate=true"),
                valor("DB_USER", "sa"),
                valor("DB_PASSWORD", ""));
    }

    public ConexionBD(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    private static String valor(String variable, String porDefecto) {
        String v = System.getenv(variable);
        return (v == null || v.isBlank()) ? porDefecto : v;
    }

    public Connection getConexion() throws SQLException {
        DriverManager.setLoginTimeout(4); // si no hay servidor, el programa no se queda esperando
        return DriverManager.getConnection(url, usuario, clave);
    }

    /** Comprueba si se puede abrir una conexión. */
    public boolean probar() {
        try (Connection cn = getConexion()) {
            return cn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Intenta conectar y devuelve null si todo salió bien, o el motivo real del fallo. */
    public String diagnosticar() {
        try (Connection cn = getConexion()) {
            return cn.isValid(3) ? null : "La conexión se abrió pero no respondió.";
        } catch (SQLException e) {
            Throwable causa = e;
            while (causa.getCause() != null && causa.getCause() != causa) {
                causa = causa.getCause();
            }
            String detalle = e.getMessage() == null ? e.toString() : e.getMessage();
            if (causa != e && causa.getMessage() != null) {
                detalle += " | Causa: " + causa.getMessage();
            }
            return detalle;
        }
    }

    public String getUsuario() {
        return usuario;
    }

    public String getUrl() {
        return url;
    }
}
