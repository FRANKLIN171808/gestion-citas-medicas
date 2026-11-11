package pagos;

/**
 * Contrato que cumple cualquier forma de pago aceptada por el centro de salud.
 */
public interface MetodoPago {

    /**
     * Intenta cobrar el monto indicado.
     *
     * @return true si el pago fue aceptado
     */
    boolean procesarPago(double monto);

    String getNombre();
}
