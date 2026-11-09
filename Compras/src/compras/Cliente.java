package compras;

/**
 * Representa un cliente del supermercado.
 *
 * El ID lo genera el programa y no se puede modificar.
 */
public class Cliente {

    private final int id;
    private final String nombre;

    /**
     * @param id identificador único del cliente
     * @param nombre nombre del cliente
     */
    public Cliente(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
