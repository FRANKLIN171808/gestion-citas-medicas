package dao;

import modelo.Cita;
import modelo.CitaResumen;
import modelo.Consultorio;
import modelo.Especialidad;
import modelo.EstadoCita;
import modelo.Medico;
import modelo.Paciente;
import modelo.TipoSeguro;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Operaciones CRUD de las citas en SQL Server (JDBC).
 * Al insertar una cita también guarda su paciente (alta o actualización). El médico y el consultorio ya
 * existen en la base (personal predeterminado); solo se insertan si faltan, por ejemplo cuando el sistema
 * trabajó con el personal de respaldo.
 */
public class CitaDAO implements DAO<Cita> {

    private static final String SELECT_BASE = """
            SELECT c.id, c.fecha_hora, c.motivo, c.estado, c.pagada, c.metodo_pago,
                   p.dni AS p_dni, p.nombres AS p_nombres, p.apellidos AS p_apellidos,
                   p.fecha_nacimiento AS p_nac, p.seguro AS p_seguro, p.aportes_al_dia AS p_aportes,
                   m.dni AS m_dni, m.nombres AS m_nombres, m.apellidos AS m_apellidos,
                   m.fecha_nacimiento AS m_nac, m.cmp AS m_cmp, m.rne AS m_rne, m.especialidad AS m_esp,
                   k.numero AS k_numero, k.piso AS k_piso
            FROM cita c
            JOIN paciente p ON p.dni = c.dni_paciente
            JOIN medico m ON m.dni = c.dni_medico
            LEFT JOIN consultorio k ON k.numero = c.num_consultorio
            """;

    private final ConexionBD conexion;

    public CitaDAO(ConexionBD conexion) {
        this.conexion = conexion;
    }

    // ------------------------------------------------------------------ CREATE

    @Override
    public void insertar(Cita cita) throws SQLException {
        try (Connection cn = conexion.getConexion()) {
            cn.setAutoCommit(false);
            try {
                guardarPaciente(cn, cita.getPaciente());
                guardarMedico(cn, cita.getMedico());
                if (cita.getConsultorio() != null) {
                    guardarConsultorio(cn, cita.getConsultorio());
                }
                String sql = "INSERT INTO cita (id, fecha_hora, motivo, costo, estado, pagada, metodo_pago,"
                        + " dni_paciente, dni_medico, num_consultorio) VALUES (?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setInt(1, cita.getId());
                    ps.setTimestamp(2, Timestamp.valueOf(cita.getFechaHora()));
                    ps.setString(3, cita.getMotivo());
                    ps.setDouble(4, cita.getCosto());
                    ps.setString(5, cita.getEstado().name());
                    ps.setBoolean(6, cita.isPagada());
                    ps.setString(7, cita.getMetodoPago());
                    ps.setString(8, cita.getPaciente().getDni());
                    ps.setString(9, cita.getMedico().getDni());
                    if (cita.getConsultorio() == null) {
                        ps.setNull(10, Types.INTEGER);
                    } else {
                        ps.setInt(10, cita.getConsultorio().getNumero());
                    }
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    // ------------------------------------------------------------------ READ

    @Override
    public Optional<Cita> buscarPorId(int id) throws SQLException {
        try (Connection cn = conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " WHERE c.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cita> lista = leer(rs);
                return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
            }
        }
    }

    @Override
    public List<Cita> listar() throws SQLException {
        try (Connection cn = conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " ORDER BY c.fecha_hora");
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        }
    }

    /** Lista las citas como records de solo lectura. */
    public List<CitaResumen> resumen() throws SQLException {
        return listar().stream().map(CitaResumen::de).toList();
    }

    // ------------------------------------------------------------------ UPDATE

    @Override
    public void actualizar(Cita cita) throws SQLException {
        String sql = "UPDATE cita SET fecha_hora = ?, motivo = ?, costo = ?, estado = ?, pagada = ?,"
                + " metodo_pago = ? WHERE id = ?";
        try (Connection cn = conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cita.getFechaHora()));
            ps.setString(2, cita.getMotivo());
            ps.setDouble(3, cita.getCosto());
            ps.setString(4, cita.getEstado().name());
            ps.setBoolean(5, cita.isPagada());
            ps.setString(6, cita.getMetodoPago());
            ps.setInt(7, cita.getId());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ DELETE

    @Override
    public void eliminar(int id) throws SQLException {
        try (Connection cn = conexion.getConexion();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM cita WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private List<Cita> leer(ResultSet rs) throws SQLException {
        Map<String, Paciente> pacientes = new HashMap<>();
        Map<String, Medico> medicos = new HashMap<>();
        Map<Integer, Consultorio> consultorios = new HashMap<>();
        List<Cita> citas = new ArrayList<>();

        while (rs.next()) {
            String dniPaciente = rs.getString("p_dni");
            Paciente paciente = pacientes.get(dniPaciente);
            if (paciente == null) {
                paciente = new Paciente(dniPaciente, rs.getString("p_nombres"), rs.getString("p_apellidos"),
                        rs.getDate("p_nac").toLocalDate(), TipoSeguro.desdeTexto(rs.getString("p_seguro")),
                        rs.getBoolean("p_aportes"));
                pacientes.put(dniPaciente, paciente);
            }

            String dniMedico = rs.getString("m_dni");
            Medico medico = medicos.get(dniMedico);
            if (medico == null) {
                medico = new Medico(dniMedico, rs.getString("m_nombres"), rs.getString("m_apellidos"),
                        rs.getDate("m_nac").toLocalDate(), rs.getString("m_cmp"), rs.getString("m_rne"),
                        Especialidad.valueOf(rs.getString("m_esp").trim()));
                medicos.put(dniMedico, medico);
            }

            Consultorio consultorio = null;
            if (rs.getObject("k_numero") != null) {
                int numero = rs.getInt("k_numero");
                consultorio = consultorios.get(numero);
                if (consultorio == null) {
                    consultorio = new Consultorio(numero, rs.getInt("k_piso"));
                    consultorios.put(numero, consultorio);
                }
            }

            citas.add(Cita.reconstruir(rs.getInt("id"), paciente, medico, consultorio,
                    rs.getTimestamp("fecha_hora").toLocalDateTime(), rs.getString("motivo"),
                    EstadoCita.valueOf(rs.getString("estado").trim()), rs.getBoolean("pagada"),
                    rs.getString("metodo_pago")));
        }
        return citas;
    }

    // En SQL Server no existe ON DUPLICATE KEY: el paciente se intenta UPDATE y, si no existía, se hace INSERT.

    private void guardarPaciente(Connection cn, Paciente p) throws SQLException {
        String update = "UPDATE paciente SET nombres = ?, apellidos = ?, telefono = ?, email = ?, seguro = ?,"
                + " aportes_al_dia = ?, grupo_sanguineo = ? WHERE dni = ?";
        try (PreparedStatement ps = cn.prepareStatement(update)) {
            ps.setString(1, p.getNombres());
            ps.setString(2, p.getApellidos());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getTipoSeguro().name());
            ps.setBoolean(6, p.isAportesAlDia());
            ps.setString(7, p.getGrupoSanguineo());
            ps.setString(8, p.getDni());
            if (ps.executeUpdate() > 0) {
                return;
            }
        }
        String insert = "INSERT INTO paciente (dni, nombres, apellidos, fecha_nacimiento, telefono, email,"
                + " seguro, aportes_al_dia, grupo_sanguineo) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(insert)) {
            ps.setString(1, p.getDni());
            ps.setString(2, p.getNombres());
            ps.setString(3, p.getApellidos());
            ps.setDate(4, Date.valueOf(p.getFechaNacimiento()));
            ps.setString(5, p.getTelefono());
            ps.setString(6, p.getEmail());
            ps.setString(7, p.getTipoSeguro().name());
            ps.setBoolean(8, p.isAportesAlDia());
            ps.setString(9, p.getGrupoSanguineo());
            ps.executeUpdate();
        }
    }

    /** El médico es personal predeterminado: solo se inserta si no está en la base. */
    private void guardarMedico(Connection cn, Medico m) throws SQLException {
        if (existe(cn, "SELECT 1 FROM medico WHERE dni = ?", m.getDni())) {
            return;
        }
        String insert = "INSERT INTO medico (dni, nombres, apellidos, fecha_nacimiento, telefono, email,"
                + " cmp, rne, especialidad) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(insert)) {
            ps.setString(1, m.getDni());
            ps.setString(2, m.getNombres());
            ps.setString(3, m.getApellidos());
            ps.setDate(4, Date.valueOf(m.getFechaNacimiento()));
            ps.setString(5, m.getTelefono());
            ps.setString(6, m.getEmail());
            ps.setString(7, m.getCmp());
            ps.setString(8, m.getRne());
            ps.setString(9, m.getEspecialidad().name());
            ps.executeUpdate();
        }
    }

    private static boolean existe(Connection cn, String sql, String valor) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** El consultorio es predeterminado: solo se inserta si no está en la base. */
    private void guardarConsultorio(Connection cn, Consultorio c) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM consultorio WHERE numero = ?")) {
            ps.setInt(1, c.getNumero());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = cn.prepareStatement("INSERT INTO consultorio (numero, piso) VALUES (?,?)")) {
            ps.setInt(1, c.getNumero());
            ps.setInt(2, c.getPiso());
            ps.executeUpdate();
        }
    }
}
