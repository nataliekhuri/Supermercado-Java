package facturacion;

import java.nio.file.Path;
import java.util.List;

/**
 * Aplicación Facturación: permite consultar la compra pendiente de un
 * cliente y generar su factura por consola.
 *
 * Los datos se leen siempre del disco (clientes.xml y compras.xml),
 * para que la factura refleje exactamente lo que está guardado.
 *
 * Se ejecuta como un programa independiente. Al elegir "Volver al
 * supermercado" termina y Supermercado recupera el control.
 *
 * @author natal
 */
public class Facturacion {

    // Opciones del menú
    private static final int OPCION_SELECCIONAR = 1;
    private static final int OPCION_VER = 2;
    private static final int OPCION_FACTURA = 3;
    private static final int OPCION_VOLVER = 4;

    // Tiempo de espera tras el aviso de "ningún cliente seleccionado"
    private static final int ESPERA_SIN_CLIENTE_MS = 3000;

    // Rutas de los XML (se calculan al iniciar)
    private static Path archivoClientes;
    private static Path archivoCompras;

    // Cliente seleccionado (null = ninguno)
    private static Cliente clienteActual = null;

    public static void main(String[] args) {
        // Localizar la carpeta común "datos"
        Path carpetaDatos = Rutas.obtenerCarpetaDatos();
        if (carpetaDatos == null) {
            System.out.println("ERROR: no se encuentra la carpeta 'datos' del proyecto.");
            System.exit(1); // Supermercado verá un código de salida distinto de 0
        }
        archivoClientes = Rutas.obtenerArchivoClientes(carpetaDatos);
        archivoCompras = Rutas.obtenerArchivoCompras(carpetaDatos);

        int opcion;
        do {
            mostrarMenu();
            opcion = Consola.leerOpcion(OPCION_SELECCIONAR, OPCION_VOLVER);

            switch (opcion) {
                case OPCION_SELECCIONAR:
                    seleccionarCliente();
                    break;
                case OPCION_VER:
                    if (hayClienteSeleccionado()) {
                        verCompraPendiente();
                    }
                    break;
                case OPCION_FACTURA:
                    if (hayClienteSeleccionado()) {
                        generarFactura();
                    }
                    break;
                case OPCION_VOLVER:
                    System.out.println("Volviendo al supermercado...");
                    break;
            }
        } while (opcion != OPCION_VOLVER);
    }

    // ==================== MENÚ ====================

    /**
     * Muestra el menú de Facturación (igual que en el enunciado).
     */
    private static void mostrarMenu() {
        System.out.println();
        System.out.println("========== FACTURACIÓN ==========");
        System.out.println();
        System.out.println("1. Seleccionar cliente");
        System.out.println("2. Ver compra pendiente");
        System.out.println("3. Generar factura");
        System.out.println("4. Volver al supermercado");
        System.out.println();
    }

    /**
     * Comprueba si hay un cliente seleccionado. Si no lo hay, muestra
     * un aviso y espera unos tres segundos (igual que en Compras).
     *
     * @return true si hay un cliente seleccionado
     */
    private static boolean hayClienteSeleccionado() {
        if (clienteActual != null) {
            return true;
        }
        System.out.println("ERROR: No hay ningún cliente seleccionado.");
        System.out.println("Debe seleccionar primero la opción 1.");
        System.out.println("Seleccionar cliente.");
        try {
            Thread.sleep(ESPERA_SIN_CLIENTE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // se restaura la interrupción (buena práctica)
        }
        return false;
    }

    // ==================== SELECCIONAR CLIENTE ====================

    /**
     * Muestra los clientes de clientes.xml y pide el ID de uno de ellos.
     * También comprueba en compras.xml si tiene una compra pendiente.
     *
     * Si algo falla, se mantiene el cliente que estaba seleccionado.
     */
    private static void seleccionarCliente() {
        // 1. Leer y mostrar los clientes registrados
        List<Cliente> clientes;
        try {
            clientes = FacturaXML.cargarClientes(archivoClientes);
        } catch (Exception e) {
            System.out.println("ERROR: " + FacturaXML.mensajeError(e, "clientes.xml"));
            return;
        }

        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados. Debe darlos de alta en la aplicación Clientes.");
            return;
        }

        System.out.println();
        System.out.println("====== CLIENTES REGISTRADOS ======");
        System.out.println();
        for (Cliente cliente : clientes) {
            System.out.println("ID: " + cliente.getId() + " | Nombre: " + cliente.getNombre());
        }
        System.out.println();

        // 2. Pedir el ID y comprobar que el cliente existe
        int id = Consola.leerEnteroPositivo("ID del cliente: ");
        if (id == -1) {
            return; // la entrada se ha cerrado
        }

        Cliente elegido = null;
        for (Cliente cliente : clientes) {
            if (cliente.getId() == id) {
                elegido = cliente;
            }
        }
        if (elegido == null) {
            System.out.println("ERROR: no existe ningún cliente con ID " + id + ".");
            return;
        }

        // 3. Comprobar su compra pendiente en compras.xml
        List<Producto> productos;
        try {
            productos = FacturaXML.cargarProductos(archivoCompras, id);
        } catch (Exception e) {
            System.out.println("ERROR: " + FacturaXML.mensajeError(e, "compras.xml"));
            System.out.println("No se ha cambiado el cliente seleccionado.");
            return;
        }

        clienteActual = elegido;
        System.out.println("Cliente seleccionado: ID " + elegido.getId() + " - " + elegido.getNombre());
        if (productos.isEmpty()) {
            System.out.println("El cliente no tiene productos pendientes.");
        } else {
            System.out.println("Tiene una compra pendiente con " + productos.size() + " producto(s).");
        }
    }

    // ==================== GENERAR FACTURA ====================

    /**
     * Genera la factura de la compra pendiente del cliente seleccionado
     * y pide confirmación.
     *
     * - Si se cancela, la compra sigue pendiente y no se modifica ningún XML.
     * - Si se confirma, se elimina la compra de compras.xml (solo si la
     *   operación se completa correctamente). El cliente no se elimina.
     */
    private static void generarFactura() {
        // 1. Leer la compra pendiente del disco
        List<Producto> productos;
        try {
            productos = FacturaXML.cargarProductos(archivoCompras, clienteActual.getId());
        } catch (Exception e) {
            System.out.println("ERROR: " + FacturaXML.mensajeError(e, "compras.xml"));
            return;
        }

        if (productos.isEmpty()) {
            System.out.println("El cliente " + clienteActual.getNombre()
                    + " no tiene productos pendientes. No se puede generar la factura.");
            return;
        }

        // 2. Calcular y mostrar la factura
        Factura factura = new Factura(productos);
        mostrarFactura(factura, productos);

        // 3. Pedir confirmación
        System.out.println();
        if (!Consola.confirmar("¿Desea confirmar la facturación? (S/N): ")) {
            System.out.println("Facturación cancelada. La compra sigue pendiente.");
            return;
        }

        // 4. Eliminar la compra pendiente de compras.xml
        try {
            int eliminadas = FacturaXML.eliminarCompra(archivoCompras, clienteActual.getId());
            if (eliminadas == 0) {
                System.out.println("AVISO: la compra ya no estaba pendiente en compras.xml.");
            } else {
                System.out.println("Factura confirmada. Se ha eliminado la compra pendiente de "
                        + clienteActual.getNombre() + ".");
            }
        } catch (Exception e) {
            System.out.println("ERROR: no se ha podido finalizar la facturación. "
                    + FacturaXML.mensajeError(e, "compras.xml"));
            System.out.println("La compra sigue pendiente.");
        }
    }

    /**
     * Muestra la factura con el formato del enunciado: datos del cliente,
     * líneas de productos, subtotal, descuento y total a pagar.
     *
     * @param factura importes calculados
     * @param productos productos de la compra
     */
    private static void mostrarFactura(Factura factura, List<Producto> productos) {
        // Columnas como en el ejemplo del enunciado. La columna "Producto"
        // mide 15 caracteres y crece si algún nombre es más largo.
        int anchoNombre = 15;
        for (Producto producto : productos) {
            anchoNombre = Math.max(anchoNombre, producto.getNombre().length() + 1);
        }
        String formatoCabecera = "%-" + anchoNombre + "s%-5s%9s%10s%n";
        String formatoLinea = "%-" + anchoNombre + "s%3s%11s%11s%n";
        String formatoResumen = "%-" + (anchoNombre + 11) + "s%12s%n";
        String separador = "-".repeat(anchoNombre + 24);

        System.out.println();
        System.out.println("========== FACTURA ==========");
        System.out.println();
        System.out.println("Cliente: " + clienteActual.getNombre());
        System.out.println("ID cliente: " + clienteActual.getId());
        System.out.println();
        System.out.printf(formatoCabecera, "Producto", "Cant.", "Precio", "Importe");
        System.out.println();
        for (Producto producto : productos) {
            System.out.printf(formatoLinea,
                    producto.getNombre(),
                    producto.getCantidad(),
                    Consola.formatearEuros(producto.getPrecio()),
                    Consola.formatearEuros(producto.getImporte()));
        }
        System.out.println();
        System.out.println(separador);
        System.out.printf(formatoResumen, "Subtotal:", Consola.formatearEuros(factura.getSubtotal()));
        String etiquetaDescuento = factura.tieneDescuento() ? "Descuento (10 %):" : "Descuento:";
        System.out.printf(formatoResumen, etiquetaDescuento, Consola.formatearEuros(factura.getDescuento()));
        System.out.printf(formatoResumen, "TOTAL A PAGAR:", Consola.formatearEuros(factura.getTotal()));
        System.out.println(separador);
    }

    // ==================== VER COMPRA PENDIENTE ====================

    /**
     * Muestra los productos pendientes del cliente seleccionado,
     * leídos en este momento de compras.xml.
     */
    private static void verCompraPendiente() {
        List<Producto> productos;
        try {
            productos = FacturaXML.cargarProductos(archivoCompras, clienteActual.getId());
        } catch (Exception e) {
            System.out.println("ERROR: " + FacturaXML.mensajeError(e, "compras.xml"));
            return;
        }

        if (productos.isEmpty()) {
            System.out.println("El cliente " + clienteActual.getNombre() + " no tiene productos pendientes.");
            return;
        }

        System.out.println();
        System.out.println("====== COMPRA PENDIENTE DE " + clienteActual.getNombre().toUpperCase() + " ======");
        System.out.println();

        // La columna "Producto" se ajusta al nombre más largo
        int anchoNombre = "Producto".length();
        for (Producto producto : productos) {
            anchoNombre = Math.max(anchoNombre, producto.getNombre().length());
        }
        String formato = "%-4s %-" + anchoNombre + "s %10s %9s %11s%n";

        System.out.printf(formato, "ID", "Producto", "Precio", "Cantidad", "Importe");
        System.out.println();
        for (Producto producto : productos) {
            System.out.printf(formato,
                    producto.getId(),
                    producto.getNombre(),
                    Consola.formatearEuros(producto.getPrecio()),
                    producto.getCantidad(),
                    Consola.formatearEuros(producto.getImporte()));
        }
    }
}
