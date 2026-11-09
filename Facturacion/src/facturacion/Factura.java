package facturacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cálculo de la factura de una compra: subtotal, descuento y total.
 *
 * Todos los importes se calculan con BigDecimal y dos decimales, para
 * que los resultados con dinero sean exactos.
 *
 * Regla del enunciado: si el subtotal es igual o superior a 50 €, se
 * aplica un descuento del 10 %. El descuento se redondea a dos
 * decimales con redondeo comercial (HALF_UP: 5,005 -> 5,01).
 */
public class Factura {

    /** Subtotal mínimo para aplicar el descuento: 50,00 €. */
    public static final BigDecimal LIMITE_DESCUENTO = new BigDecimal("50.00");

    /** Porcentaje de descuento: 10 % (0,10). */
    public static final BigDecimal PORCENTAJE_DESCUENTO = new BigDecimal("0.10");

    private final BigDecimal subtotal;
    private final BigDecimal descuento;
    private final BigDecimal total;

    /**
     * Calcula la factura de una lista de productos.
     *
     * @param productos productos de la compra
     */
    public Factura(List<Producto> productos) {
        // 1. Subtotal: suma de los importes (precio × cantidad) de cada línea
        BigDecimal suma = BigDecimal.ZERO;
        for (Producto producto : productos) {
            suma = suma.add(producto.getImporte());
        }
        this.subtotal = suma.setScale(2, RoundingMode.HALF_UP);

        // 2. Descuento del 10 % solo si el subtotal es >= 50,00 €
        if (subtotal.compareTo(LIMITE_DESCUENTO) >= 0) {
            this.descuento = subtotal.multiply(PORCENTAJE_DESCUENTO).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.descuento = BigDecimal.ZERO.setScale(2);
        }

        // 3. Total a pagar
        this.total = subtotal.subtract(descuento);
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public BigDecimal getTotal() {
        return total;
    }

    /**
     * @return true si se aplica el descuento del 10 %
     */
    public boolean tieneDescuento() {
        return descuento.compareTo(BigDecimal.ZERO) > 0;
    }
}
