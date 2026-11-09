package compras;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplicación Compras: permite seleccionar un cliente y gestionar los
 * productos de su compra pendiente, guardada en compras.xml.
 *
 * Los productos se modifican en memoria y solo se escriben en el XML
 * al elegir "Guardar compra".
 *
 * Se ejecuta como un programa independiente. Al elegir "Volver al
 * supermercado" termina y Supermercado recupera el control.
 *
 * @author natal
 */
public class Compras {

    // Opciones del menú
    private static final int OPCION_SELECCIONAR = 1;
    private static final int OPCION_ANADIR = 2;
    private static final int OPCION_VER = 3;
    private static final int OPCION_MODIFICAR = 4;
    private static final int OPCION_ELIMINAR = 5;
    private static final int OPCION_GUARDAR = 6;
    private static final int OPCION_VOLVER = 7;

    // Tiempo de espera tras el aviso de "ningún cliente seleccionado"
    private static final int ESPERA_SIN_CLIENTE_MS = 3000;

    // Rutas de los XML (se calculan al iniciar)
    private static Path archivoClientes;
    private static Path archivoCompras;

    // Estado de la compra que se está editando
    private static Cliente clienteActual = null;                  // null = ningún cliente
    private static List<Producto> productos = new ArrayList<>();  // productos en memoria
    private static boolean cambiosSinGuardar = false;

    public static void main(String[] args) {
        // Localizar la carpeta común "datos"
        Path carpetaDatos = Rutas.obtenerCarpetaDatos();
        if (carpetaDatos == null) {
            System.out.println("ERROR: no se encuentra la carpeta 'datos' del proyecto.");
            System.exit(1); // Supermercado verá un código de salida distinto de 0
        }
        archivoClientes = Rutas.obtenerArchivoClientes(carpetaDatos);
        archivoCompras = Rutas.obtenerArchivoCompras(carpetaDatos);

        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            int opcion = Consola.leerOpcion(OPCION_SELECCIONAR, OPCION_VOLVER);

            switch (opcion) {
                case OPCION_SELECCIONAR:
                    seleccionarCliente();
                    break;
                case OPCION_ANADIR:
                    if (hayClienteSeleccionado()) {
                        anadirProducto();
                    }
                    break;
                case OPCION_VER:
                    if (hayClienteSeleccionado()) {
                        verProductos();
                    }
                    break;
                case OPCION_MODIFICAR:
                    if (hayClienteSeleccionado()) {
                        modificarProducto();
                    }
                    break;
                case OPCION_ELIMINAR:
                    if (hayClienteSeleccionado()) {
                        eliminarProducto();
                    }
                    break;
                case OPCION_GUARDAR:
                    if (hayClienteSeleccionado()) {
                        if (cambiosSinGuardar) {
                            guardarCompra();
                        } else {
                            System.out.println("No hay cambios pendientes de guardar.");
                        }
                    }
                    break;
                case OPCION_VOLVER:
                    // Solo se sale si no quedan cambios pendientes
                    // (o el usuario decide guardarlos o descartarlos)
                    if (resolverCambiosPendientes()) {
                        System.out.println("Volviendo al supermercado...");
                        salir = true;
                    }
                    break;
            }
        }
    }

    // ==================== MENÚ ====================

    /**
     * Muestra el menú de Compras con el cliente actual.
     */
    private static void mostrarMenu() {
        System.out.println();
        System.out.println("========== COMPRAS ==========");
        System.out.println();
        if (clienteActual == null) {
            System.out.println("Cliente actual: Ninguno");
        } else {
            System.out.println("Cliente actual: ID " + clienteActual.getId() + " - " + clienteActual.getNombre());
        }
        System.out.println();
        System.out.println("1. Seleccionar cliente");
        System.out.println("2. Añadir producto");
        System.out.println("3. Ver productos");
        System.out.println("4. Modificar producto");
        System.out.println("5. Eliminar producto");
        System.out.println("6. Guardar compra");
        System.out.println("7. Volver al supermercado");
        System.out.println();
    }

    /**
     * Comprueba si hay un cliente seleccionado. Si no lo hay, muestra
     * el aviso del enunciado y espera unos tres segundos.
     *
     * @return true si hay un cliente seleccionado
     */
    private static boolean hayClienteSeleccionado() {
        if (clienteActual == null) {
            avisarSinCliente();
            return false;
        }
        return true;
    }

    /**
     * Muestra el aviso del enunciado cuando no hay ningún cliente
     * seleccionado, espera unos tres segundos y vuelve al menú.
     */
    private static void avisarSinCliente() {
        System.out.println("ERROR: No hay ningún cliente seleccionado.");
        System.out.println("Debe seleccionar primero la opción 1.");
        System.out.println("Seleccionar cliente.");
        try {
            Thread.sleep(ESPERA_SIN_CLIENTE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // se restaura la interrupción (buena práctica)
        }
    }

    // ==================== SELECCIONAR CLIENTE ====================

    /**
     * Muestra los clientes de clientes.xml, pide el ID de uno de ellos
     * y carga sus productos pendientes de compras.xml.
     *
     * Si algo falla, se mantiene el cliente que estaba seleccionado.
     */
    private static void seleccionarCliente() {
        // 1. Leer y mostrar los clientes registrados
        List<Cliente> clientes;
        try {
            clientes = CompraXML.cargarClientes(archivoClientes);
        } catch (Exception e) {
            System.out.println("ERROR: " + CompraXML.mensajeError(e, "clientes.xml"));
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

        // 3. Comprobar que se pueden cargar sus productos de compras.xml.
        //    Si no se puede, no se cambia nada (ni se pregunta por los cambios).
        List<Producto> productosCargados;
        try {
            productosCargados = CompraXML.cargarProductos(archivoCompras, id);
        } catch (Exception e) {
            System.out.println("ERROR: " + CompraXML.mensajeError(e, "compras.xml"));
            System.out.println("No se ha cambiado el cliente seleccionado.");
            return;
        }

        // 4. Si hay cambios sin guardar, preguntar qué hacer con ellos
        boolean habiaCambios = cambiosSinGuardar;
        if (!resolverCambiosPendientes()) {
            return; // cancelado o error al guardar: se mantiene el cliente actual
        }

        // Si se acaba de guardar, el disco ha cambiado: se vuelve a leer
        if (habiaCambios) {
            try {
                productosCargados = CompraXML.cargarProductos(archivoCompras, id);
            } catch (Exception e) {
                System.out.println("ERROR: " + CompraXML.mensajeError(e, "compras.xml"));
                clienteActual = null;
                productos = new ArrayList<>();
                cambiosSinGuardar = false;
                System.out.println("No hay ningún cliente seleccionado.");
                return;
            }
        }

        // 5. Actualizar el estado
        clienteActual = elegido;
        productos = productosCargados;
        cambiosSinGuardar = false;

        System.out.println("Cliente seleccionado: ID " + elegido.getId() + " - " + elegido.getNombre());
        if (productos.isEmpty()) {
            System.out.println("El cliente no tiene productos pendientes.");
        } else {
            System.out.println("Se han cargado " + productos.size() + " producto(s) pendiente(s).");
        }
    }

    // ==================== GUARDAR Y CAMBIOS PENDIENTES ====================

    /**
     * Guarda en compras.xml la compra del cliente actual.
     * Si falla, muestra el error y los cambios se conservan en memoria.
     *
     * @return true si se ha guardado correctamente
     */
    private static boolean guardarCompra() {
        try {
            CompraXML.guardarCompra(archivoCompras, clienteActual.getId(), productos);
        } catch (Exception e) {
            System.out.println("ERROR: no se ha podido guardar la compra. "
                    + CompraXML.mensajeError(e, "compras.xml"));
            System.out.println("Los cambios se conservan en memoria; puede intentarlo de nuevo.");
            return false;
        }

        cambiosSinGuardar = false;
        if (productos.isEmpty()) {
            System.out.println("La compra de " + clienteActual.getNombre()
                    + " no tiene productos: se ha eliminado su compra pendiente.");
        } else {
            BigDecimal total = BigDecimal.ZERO;
            for (Producto producto : productos) {
                total = total.add(producto.getImporte());
            }
            System.out.println("Compra guardada correctamente: " + productos.size()
                    + " producto(s), total provisional " + Consola.formatearEuros(total) + ".");
        }
        return true;
    }

    /**
     * Si hay cambios sin guardar, pregunta si se quieren guardar,
     * descartar o cancelar la acción (cambiar de cliente o salir).
     *
     * @return true si se puede continuar con la acción; false si se
     *         cancela o si el guardado falla
     */
    private static boolean resolverCambiosPendientes() {
        if (!cambiosSinGuardar) {
            return true;
        }

        System.out.println("AVISO: hay cambios sin guardar en la compra de " + clienteActual.getNombre() + ".");
        System.out.println("G. Guardar D. Descartar C. Cancelar");
        char respuesta = Consola.leerGuardarDescartarCancelar("Seleccione una opción (G/D/C): ");

        switch (respuesta) {
            case 'G':
                return guardarCompra(); // solo se continúa si se ha guardado bien
            case 'D':
                cambiosSinGuardar = false;
                System.out.println("Cambios descartados. No se ha modificado compras.xml.");
                return true;
            default: // 'C'
                System.out.println("Acción cancelada. Se conservan el cliente y los cambios.");
                return false;
        }
    }

    // ==================== AÑADIR PRODUCTO ====================

    /**
     * Pide los datos de un producto y lo añade a la compra en memoria.
     * El ID se genera automáticamente. Los cambios no se escriben en
     * compras.xml hasta que se elige "Guardar compra".
     */
    private static void anadirProducto() {
        String nombre = Consola.leerTexto("Nombre del producto: ", "el nombre del producto");
        if (nombre == null) {
            return; // la entrada se ha cerrado
        }
        BigDecimal precio = Consola.leerPrecio("Precio unitario: ");
        if (precio == null) {
            return;
        }
        int cantidad = Consola.leerEnteroPositivo("Cantidad: ");
        if (cantidad == -1) {
            return;
        }

        Producto producto = new Producto(siguienteIdProducto(), nombre, precio, cantidad);
        productos.add(producto);
        cambiosSinGuardar = true;

        System.out.println("Producto añadido: " + describirProducto(producto));
        System.out.println("Recuerde guardar la compra (opción 6) para conservar los cambios.");
    }

    /**
     * Calcula el ID del siguiente producto: el mayor ID de la compra
     * actual más uno (1 si la compra está vacía). Los IDs son únicos
     * dentro de la compra de cada cliente.
     *
     * @return el ID para el nuevo producto
     */
    private static int siguienteIdProducto() {
        int mayor = 0;
        for (Producto producto : productos) {
            if (producto.getId() > mayor) {
                mayor = producto.getId();
            }
        }
        return mayor + 1;
    }

    // ==================== MODIFICAR PRODUCTO ====================

    /**
     * Modifica el nombre, el precio o la cantidad de un producto de la
     * compra en memoria. Pulsando Enter se conserva el valor actual.
     * El ID del producto no se puede modificar.
     */
    private static void modificarProducto() {
        Producto producto = elegirProducto("ID del producto que desea modificar: ");
        if (producto == null) {
            return;
        }

        System.out.println("Pulse Enter para conservar el valor actual.");
        String nombre = Consola.leerTextoOpcional(
                "Nombre [" + producto.getNombre() + "]: ", producto.getNombre());
        if (nombre == null) {
            return; // la entrada se ha cerrado: no se cambia nada
        }
        BigDecimal precio = Consola.leerPrecioOpcional(
                "Precio unitario [" + Consola.formatearEuros(producto.getPrecio()) + "]: ", producto.getPrecio());
        if (precio == null) {
            return;
        }
        int cantidad = Consola.leerEnteroPositivoOpcional(
                "Cantidad [" + producto.getCantidad() + "]: ", producto.getCantidad());
        if (cantidad == -1) {
            return;
        }

        // Solo se marca como cambiado si algún valor es distinto
        // (compareTo compara el valor: 1.2 y 1.20 son el mismo precio)
        boolean hayCambios = !nombre.equals(producto.getNombre())
                || precio.compareTo(producto.getPrecio()) != 0
                || cantidad != producto.getCantidad();

        if (!hayCambios) {
            System.out.println("No se ha realizado ningún cambio.");
            return;
        }

        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setCantidad(cantidad);
        cambiosSinGuardar = true;

        System.out.println("Producto modificado: " + describirProducto(producto));
        System.out.println("Recuerde guardar la compra (opción 6) para conservar los cambios.");
    }

    // ==================== ELIMINAR PRODUCTO ====================

    /**
     * Elimina un producto de la compra en memoria, después de pedir
     * confirmación. Los demás productos conservan su ID.
     */
    private static void eliminarProducto() {
        Producto producto = elegirProducto("ID del producto que desea eliminar: ");
        if (producto == null) {
            return;
        }

        System.out.println("Producto seleccionado: " + describirProducto(producto));
        if (!Consola.confirmar("¿Desea eliminar este producto? (S/N): ")) {
            System.out.println("Eliminación cancelada. La compra no se ha modificado.");
            return;
        }

        productos.remove(producto);
        cambiosSinGuardar = true;

        System.out.println("Producto eliminado: ID " + producto.getId() + " | " + producto.getNombre());
        System.out.println("Recuerde guardar la compra (opción 6) para conservar los cambios.");
    }

    // ==================== UTILIDADES DE PRODUCTOS ====================

    /**
     * Muestra los productos de la compra, pide un ID y devuelve el
     * producto correspondiente.
     *
     * @param mensaje texto para pedir el ID
     * @return el producto elegido, o null si no hay productos, el ID no
     *         existe o la entrada se ha cerrado
     */
    private static Producto elegirProducto(String mensaje) {
        if (productos.isEmpty()) {
            System.out.println("La compra de " + clienteActual.getNombre() + " no tiene productos.");
            return null;
        }

        verProductos();
        System.out.println();

        int id = Consola.leerEnteroPositivo(mensaje);
        if (id == -1) {
            return null; // la entrada se ha cerrado
        }

        Producto producto = buscarProducto(id);
        if (producto == null) {
            System.out.println("ERROR: no existe ningún producto con ID " + id + " en la compra.");
        }
        return producto;
    }

    /**
     * Busca un producto de la compra actual por su ID.
     *
     * @param id ID del producto
     * @return el producto, o null si no existe
     */
    private static Producto buscarProducto(int id) {
        for (Producto producto : productos) {
            if (producto.getId() == id) {
                return producto;
            }
        }
        return null;
    }

    /**
     * Devuelve una descripción de una línea de un producto.
     * Ejemplo: ID 1 | Leche | 1,20 € x 2 = 2,40 €
     *
     * @param producto producto que se describe
     * @return el texto con los datos del producto
     */
    private static String describirProducto(Producto producto) {
        return "ID " + producto.getId() + " | " + producto.getNombre()
                + " | " + Consola.formatearEuros(producto.getPrecio()) + " x " + producto.getCantidad()
                + " = " + Consola.formatearEuros(producto.getImporte());
    }

    // ==================== VER PRODUCTOS ====================

    /**
     * Muestra los productos de la compra actual en forma de tabla,
     * con el importe de cada línea y el total provisional.
     */
    private static void verProductos() {
        if (productos.isEmpty()) {
            System.out.println("La compra de " + clienteActual.getNombre() + " no tiene productos.");
            return;
        }

        // La columna "Producto" se ajusta al nombre más largo
        int anchoNombre = "Producto".length();
        for (Producto producto : productos) {
            anchoNombre = Math.max(anchoNombre, producto.getNombre().length());
        }
        String formato = "%-4s %-" + anchoNombre + "s %10s %9s %11s%n";

        System.out.println();
        System.out.println("====== COMPRA DE " + clienteActual.getNombre().toUpperCase() + " ======");
        System.out.println();
        System.out.printf(formato, "ID", "Producto", "Precio", "Cantidad", "Importe");
        System.out.println();

        BigDecimal total = BigDecimal.ZERO;
        for (Producto producto : productos) {
            System.out.printf(formato,
                    producto.getId(),
                    producto.getNombre(),
                    Consola.formatearEuros(producto.getPrecio()),
                    producto.getCantidad(),
                    Consola.formatearEuros(producto.getImporte()));
            total = total.add(producto.getImporte());
        }

        System.out.println();
        System.out.println("Total provisional: " + Consola.formatearEuros(total));
        if (cambiosSinGuardar) {
            System.out.println("(Hay cambios sin guardar.)");
        }
    }
}
