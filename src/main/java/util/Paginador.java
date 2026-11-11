package util;

import java.util.List;

/**
 * Clase genérica que divide cualquier lista en páginas.
 *
 * @param <T> tipo de los elementos a paginar
 */
public class Paginador<T> {

    private final List<T> elementos;
    private final int tamanoPagina;

    public Paginador(List<T> elementos, int tamanoPagina) {
        if (elementos == null) {
            throw new IllegalArgumentException("La lista no puede ser nula");
        }
        if (tamanoPagina <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser mayor que cero");
        }
        this.elementos = List.copyOf(elementos);
        this.tamanoPagina = tamanoPagina;
    }

    public int totalPaginas() {
        return (int) Math.ceil(elementos.size() / (double) tamanoPagina);
    }

    /**
     * Devuelve los elementos de una página.
     *
     * @param numeroPagina número de página, empezando en 1
     */
    public List<T> getPagina(int numeroPagina) {
        if (numeroPagina < 1 || numeroPagina > Math.max(1, totalPaginas())) {
            throw new IllegalArgumentException("Página fuera de rango: " + numeroPagina);
        }
        int desde = (numeroPagina - 1) * tamanoPagina;
        int hasta = Math.min(desde + tamanoPagina, elementos.size());
        return elementos.subList(desde, hasta);
    }

    public int getTotalElementos() {
        return elementos.size();
    }
}
