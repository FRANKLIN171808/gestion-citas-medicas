package pagos;

/**
 * Pago en efectivo: se acepta solo si el monto entregado cubre el costo.
 */
public class PagoEfectivo implements MetodoPago {

    private final double montoEntregado;

    public PagoEfectivo(double montoEntregado) {
        if (montoEntregado <= 0) {
            throw new IllegalArgumentException("El monto entregado debe ser mayor que cero");
        }
        this.montoEntregado = montoEntregado;
    }

    @Override
    public boolean procesarPago(double monto) {
        return monto > 0 && montoEntregado >= monto;
    }

    public double calcularVuelto(double monto) {
        return Math.max(0, montoEntregado - monto);
    }

    @Override
    public String getNombre() {
        return "Efectivo";
    }
}
