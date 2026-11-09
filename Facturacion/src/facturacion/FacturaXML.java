package facturacion;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

/**
 * Acceso a los archivos XML desde la aplicación Facturación, mediante DOM.
 *
 * - clientes.xml solo se lee (Facturación nunca lo modifica).
 * - compras.xml se lee para mostrar la compra pendiente y generar la factura,
 *   y se actualiza al confirmar una factura (se elimina esa compra).
 */
public class FacturaXML {

    // ==================== LECTURA DE CLIENTES ====================

    /**
     * Lee todos los clientes registrados en clientes.xml.
     *
     * @param archivoClientes ruta de clientes.xml
     * @return lista de clientes (vacía si no hay ninguno)
     * @throws Exception si el XML no se puede leer o tiene datos no válidos
     */
    public static List<Cliente> cargarClientes(Path archivoClientes) throws Exception {
        Document documento = XMLUtil.cargarDocumento(archivoClientes, "clientes");
        NodeList nodos = documento.getElementsByTagName("cliente");

        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            Element elemento = (Element) nodos.item(i);
            int id = leerEntero(elemento.getAttribute("id"),
                    "En clientes.xml, el ID \"" + elemento.getAttribute("id") + "\" no es válido.");

            NodeList nombres = elemento.getElementsByTagName("nombre");
            if (nombres.getLength() == 0) {
                throw new Exception("En clientes.xml, el cliente con ID " + id + " no tiene nombre.");
            }
            clientes.add(new Cliente(id, nombres.item(0).getTextContent()));
        }
        return clientes;
    }

    // ==================== LECTURA DE PRODUCTOS ====================

    /**
     * Lee los productos pendientes de un cliente en compras.xml.
     * Solo se leen los productos de la compra de ese cliente.
     *
     * @param archivoCompras ruta de compras.xml
     * @param idCliente ID del cliente
     * @return lista de productos (vacía si el cliente no tiene compra pendiente)
     * @throws Exception si el XML no se puede leer o tiene datos no válidos
     */
    public static List<Producto> cargarProductos(Path archivoCompras, int idCliente) throws Exception {
        Document documento = XMLUtil.cargarDocumento(archivoCompras, "compras");
        List<Producto> productos = new ArrayList<>();

        for (Element compra : buscarComprasDeCliente(documento, idCliente)) {
            NodeList nodos = compra.getElementsByTagName("producto");
            for (int i = 0; i < nodos.getLength(); i++) {
                productos.add(leerProducto((Element) nodos.item(i), idCliente));
            }
        }
        return productos;
    }

    // ==================== ELIMINAR COMPRA (FACTURA CONFIRMADA) ====================

    /**
     * Elimina de compras.xml la compra pendiente de un cliente, después
     * de confirmar su factura. El cliente no se elimina de clientes.xml
     * y las compras de los demás clientes no se modifican.
     *
     * Si compras.xml está mal formado o no se puede escribir, se lanza
     * una excepción y el archivo queda como estaba.
     *
     * @param archivoCompras ruta de compras.xml
     * @param idCliente ID del cliente
     * @return número de compras eliminadas (0 si el cliente no tenía ninguna)
     * @throws Exception si el XML no se puede leer o escribir
     */
    public static int eliminarCompra(Path archivoCompras, int idCliente) throws Exception {
        Document documento = XMLUtil.cargarDocumento(archivoCompras, "compras");
        List<Element> compras = buscarComprasDeCliente(documento, idCliente);

        if (compras.isEmpty()) {
            return 0; // no hay nada que eliminar: no se escribe el archivo
        }

        for (Element compra : compras) {
            compra.getParentNode().removeChild(compra);
        }

        // Guardar de forma segura (.tmp + Files.move)
        XMLUtil.guardarDocumento(documento, archivoCompras);
        return compras.size();
    }

    /**
     * Busca las compras de un cliente (normalmente habrá como máximo una).
     *
     * @param documento documento de compras.xml
     * @param idCliente ID del cliente
     * @return lista con las compras de ese cliente (vacía si no tiene)
     */
    private static List<Element> buscarComprasDeCliente(Document documento, int idCliente) {
        List<Element> resultado = new ArrayList<>();
        NodeList compras = documento.getElementsByTagName("compra");
        for (int i = 0; i < compras.getLength(); i++) {
            Element compra = (Element) compras.item(i);
            if (compra.getAttribute("clienteId").trim().equals(String.valueOf(idCliente))) {
                resultado.add(compra);
            }
        }
        return resultado;
    }

    /**
     * Convierte un elemento producto del XML en un objeto Producto,
     * comprobando que sus datos son válidos.
     *
     * @param elemento elemento producto
     * @param idCliente ID del cliente (para los mensajes de error)
     * @return el producto leído
     * @throws Exception si algún dato no es válido
     */
    private static Producto leerProducto(Element elemento, int idCliente) throws Exception {
        String prefijo = "En compras.xml (cliente " + idCliente + "), ";

        int id = leerEntero(elemento.getAttribute("id"),
                prefijo + "el ID de producto \"" + elemento.getAttribute("id") + "\" no es válido.");

        String nombre = leerTextoHijo(elemento, "nombre", prefijo + "el producto " + id + " no tiene nombre.");

        String textoPrecio = leerTextoHijo(elemento, "precio", prefijo + "el producto " + id + " no tiene precio.");
        BigDecimal precio;
        try {
            precio = new BigDecimal(textoPrecio.trim());
        } catch (NumberFormatException e) {
            throw new Exception(prefijo + "el precio \"" + textoPrecio + "\" del producto " + id + " no es válido.");
        }
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new Exception(prefijo + "el precio del producto " + id + " debe ser mayor que cero.");
        }

        String textoCantidad = leerTextoHijo(elemento, "cantidad", prefijo + "el producto " + id + " no tiene cantidad.");
        int cantidad = leerEntero(textoCantidad,
                prefijo + "la cantidad \"" + textoCantidad + "\" del producto " + id + " no es válida.");

        return new Producto(id, nombre, precio, cantidad);
    }

    // ==================== UTILIDADES ====================

    /**
     * Lee el texto del primer hijo con un nombre de etiqueta.
     *
     * @throws Exception con el mensaje indicado si el hijo no existe
     */
    private static String leerTextoHijo(Element padre, String etiqueta, String mensajeError) throws Exception {
        NodeList hijos = padre.getElementsByTagName(etiqueta);
        if (hijos.getLength() == 0) {
            throw new Exception(mensajeError);
        }
        return hijos.item(0).getTextContent();
    }

    /**
     * Convierte un texto del XML en un entero positivo.
     *
     * @throws Exception con el mensaje indicado si no es un entero positivo
     */
    private static int leerEntero(String texto, String mensajeError) throws Exception {
        try {
            int numero = Integer.parseInt(texto.trim());
            if (numero > 0) {
                return numero;
            }
        } catch (NumberFormatException e) {
            // Se lanza el error de abajo
        }
        throw new Exception(mensajeError);
    }

    /**
     * Convierte una excepción en un mensaje claro en español.
     *
     * @param e excepción producida
     * @param nombreArchivo archivo con el que se estaba trabajando
     * @return mensaje para mostrar al usuario
     */
    public static String mensajeError(Exception e, String nombreArchivo) {
        if (e instanceof SAXParseException) {
            return nombreArchivo + " está dañado o no es un XML válido.";
        }
        if (e instanceof IOException) {
            return "No se puede acceder a " + nombreArchivo
                    + ": compruebe que no esté protegido contra escritura ni abierto en otro programa.";
        }
        return e.getMessage();
    }
}
