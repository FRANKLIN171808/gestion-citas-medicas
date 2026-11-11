package dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz genérica para las operaciones CRUD contra la base de datos.
 *
 * @param <T> tipo de objeto que se persiste
 */
public interface DAO<T> {

    void insertar(T objeto) throws SQLException;

    Optional<T> buscarPorId(int id) throws SQLException;

    List<T> listar() throws SQLException;

    void actualizar(T objeto) throws SQLException;

    void eliminar(int id) throws SQLException;
}
