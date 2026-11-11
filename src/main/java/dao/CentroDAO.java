package dao;

import modelo.CentroSalud;
import modelo.Cita;
import modelo.Consultorio;
import modelo.Especialidad;
import modelo.EstadoCita;
import modelo.Medico;
import modelo.Paciente;
import modelo.Recepcionista;
import modelo.TipoSeguro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga el centro completo desde SQL Server al iniciar: el personal (recepcionistas, médicos y
 * consultorios, que ya están guardados en la base), los pacientes y las citas.
 * Las filas con datos inválidos no detienen la carga: se omiten y se informan en las advertencias.
 */
public class CentroDAO {

    private final ConexionBD conexion;
    private final List<String> advertencias = new ArrayList<>();

    public CentroDAO(ConexionBD conexion) {
        this.conexion = conexion;
    }

    /** Problemas encontrados en la última carga (filas omitidas y su motivo). */
    public List<String> getAdvertencias() {
        return advertencias;
    }

    public void cargarEn(CentroSalud centro) throws SQLException {
        advertencias.clear();
        try (Connection cn = conexion.getConexion()) {
            cargarConsultorios(cn, centro);
            cargarRecepcionistas(cn, centro);
            cargarMedicos(cn, centro);
            cargarPacientes(cn, centro);
            cargarCitas(cn, centro);
        }
    }

    private void cargarConsultorios(Connection cn, CentroSalud centro) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT numero, piso FROM consultorio ORDER BY numero");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                centro.agregarConsultorio(rs.getInt("numero"), rs.getInt("piso"));
            }
        }
    }

    private void cargarRecepcionistas(Connection cn, CentroSalud centro) throws SQLException {
        String sql = "SELECT dni, nombres, apellidos, fecha_nacimiento, telefono, email, turno"
                + " FROM recepcionista ORDER BY apellidos, nombres";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String dni = rs.getString("dni");
                try {
                    Recepcionista r = new Recepcionista(dni, rs.getString("nombres"), rs.getString("apellidos"),
                            rs.getDate("fecha_nacimiento").toLocalDate(), rs.getString("turno"));
                    r.setTelefono(rs.getString("telefono"));
                    r.setEmail(rs.getString("email"));
                    centro.registrarRecepcionista(r);
                } catch (RuntimeException e) {
                    advertencias.add("Recepcionista " + dni + " omitida: " + e.getMessage());
                }
            }
        }
    }

    private void cargarMedicos(Connection cn, CentroSalud centro) throws SQLException {
        String sql = "SELECT dni, nombres, apellidos, fecha_nacimiento, telefono, email, cmp, rne, especialidad"
                + " FROM medico ORDER BY apellidos, nombres";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String dni = rs.getString("dni");
                try {
                    Medico m = new Medico(dni, rs.getString("nombres"), rs.getString("apellidos"),
                            rs.getDate("fecha_nacimiento").toLocalDate(), rs.getString("cmp"), rs.getString("rne"),
                            Especialidad.valueOf(rs.getString("especialidad").trim()));
                    m.setTelefono(rs.getString("telefono"));
                    m.setEmail(rs.getString("email"));
                    centro.registrarMedico(m);
                } catch (RuntimeException e) {
                    advertencias.add("Médico " + dni + " omitido: " + e.getMessage());
                }
            }
        }
    }

    private void cargarPacientes(Connection cn, CentroSalud centro) throws SQLException {
        String sql = "SELECT dni, nombres, apellidos, fecha_nacimiento, telefono, email, seguro, aportes_al_dia,"
                + " grupo_sanguineo FROM paciente ORDER BY apellidos, nombres";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String dni = rs.getString("dni");
                try {
                    Paciente p = new Paciente(dni, rs.getString("nombres"), rs.getString("apellidos"),
                            rs.getDate("fecha_nacimiento").toLocalDate(),
                            TipoSeguro.desdeTexto(rs.getString("seguro")), rs.getBoolean("aportes_al_dia"));
                    p.setTelefono(rs.getString("telefono"));
                    p.setEmail(rs.getString("email"));
                    p.setGrupoSanguineo(rs.getString("grupo_sanguineo"));
                    centro.registrarPaciente(p);
                } catch (RuntimeException e) {
                    advertencias.add("Paciente " + dni + " omitido: " + e.getMessage());
                }
            }
        }
    }

    /** Las citas se enlazan con los médicos, pacientes y consultorios ya cargados en el centro. */
    private void cargarCitas(Connection cn, CentroSalud centro) throws SQLException {
        String sql = "SELECT id, fecha_hora, motivo, estado, pagada, metodo_pago, dni_paciente, dni_medico,"
                + " num_consultorio FROM cita ORDER BY id";
        int mayorId = 0;
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                mayorId = Math.max(mayorId, id);
                try {
                    Paciente paciente = centro.buscarPaciente(rs.getString("dni_paciente"))
                            .orElseThrow(() -> new IllegalStateException("su paciente no se pudo cargar"));
                    Medico medico = centro.buscarMedico(rs.getString("dni_medico"))
                            .orElseThrow(() -> new IllegalStateException("su médico no se pudo cargar"));
                    Consultorio consultorio = rs.getObject("num_consultorio") == null ? null
                            : centro.buscarConsultorio(rs.getInt("num_consultorio")).orElse(null);
                    centro.restaurarCita(Cita.reconstruir(id, paciente, medico, consultorio,
                            rs.getTimestamp("fecha_hora").toLocalDateTime(), rs.getString("motivo"),
                            EstadoCita.valueOf(rs.getString("estado").trim()), rs.getBoolean("pagada"),
                            rs.getString("metodo_pago")));
                } catch (RuntimeException e) {
                    advertencias.add("Cita #" + id + " omitida: " + e.getMessage());
                }
            }
        }
        centro.reservarIdsHasta(mayorId);
    }
}
