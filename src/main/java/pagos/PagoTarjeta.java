package pagos;

/**
 * Pago con tarjeta de crédito o débito.
 */
public class PagoTarjeta implements MetodoPago {

    private final String numeroTarjeta;
    private final String titular;

    public PagoTarjeta(String numeroTarjeta, String titular) {
        if (numeroTarjeta == null || !numeroTarjeta.matches("\\d{16}")) {
            throw new IllegalArgumentException("El número de tarjeta debe tener 16 dígitos");
        }
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("El titular es obligatorio");
        }
        this.numeroTarjeta = numeroTarjeta;
        this.titular = titular.trim();
    }

    @Override
    public boolean procesarPago(double monto) {
        return monto > 0;
    }

    @Override
    public String getNombre() {
        return "Tarjeta ****" + numeroTarjeta.substring(12) + " (" + titular + ")";
    }
}
