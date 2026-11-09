package compras;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa un producto de la compra pendiente de un cliente.
 *
 * El precio se guarda con BigDecimal para que los cálculos con dinero
 * sean exactos (con double, 0.10 * 3 no da exactamente 0.30).
 * El ID lo genera el programa y no se puede modificar.
 */
public class Producto {

    private final int id;
    private String nombre;
    private BigDecimal precio;
    private int cantidad;

    /**
     * @param id identificador del producto dentro de la compra
     * @param nombre nombre del producto
     * @param precio precio unitario (mayor que cero)
     * @param cantidad cantidad (entero positivo)
     */
    public Producto(int id, String nombre, BigDecimal precio, int cantidad) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio.setScale(2, RoundingMode.HALF_UP);
        this.cantidad = cantidad;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Guarda el precio siempre con dos decimales (por ejemplo, 1.2 -> 1.20).
     *
     * @param precio precio unitario
     */
    public void setPrecio(BigDecimal precio) {
        this.precio = precio.setScale(2, RoundingMode.HALF_UP);
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Calcula el importe de la línea: precio × cantidad.
     *
     * @return el importe con dos decimales
     */
    public BigDecimal getImporte() {
        return precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Devuelve el precio como texto para guardarlo en el XML:
     * siempre con punto decimal y dos decimales (por ejemplo, "1.20").
     *
     * @return el precio en formato XML
     */
    public String getPrecioXML() {
        return precio.toPlainString();
    }
}
