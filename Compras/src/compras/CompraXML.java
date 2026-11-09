package compras;

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
 * Acceso a los archivos XML desde la aplicación Compras, mediante DOM.
 *
 * - clientes.xml solo se lee (Compras nunca lo modifica).
 * - compras.xml se lee y se actualiza al guardar la compra.
 */
public class CompraXML {

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
     * Solo se leen los productos de la compra de ese cliente;
     * las compras de los demás clientes no se tocan.
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

    // ==================== GUARDAR COMPRA ====================

    /**
     * Guarda en compras.xml la compra de un cliente.
     *
     * - Si el cliente ya tenía una compra, se sustituye en el mismo sitio
     *   (sin crear duplicados).
     * - Si no la tenía, se añade al final.
     * - Si la lista de productos está vacía, se elimina su compra.
     * - Las compras de los demás clientes no se modifican.
     *
     * Si compras.xml está mal formado, se lanza una excepción y el
     * archivo no se sobrescribe.
     *
     * @param archivoCompras ruta de compras.xml
     * @param idCliente ID del cliente
     * @param productos productos de la compra
     * @throws Exception si el XML no se puede leer o escribir
     */
    public static void guardarCompra(Path archivoCompras, int idCliente, List<Producto> productos)
            throws Exception {

        // 1. Cargar el documento actual del disco
        Document documento = XMLUtil.cargarDocumento(archivoCompras, "compras");
        Element raiz = documento.getDocumentElement();
        List<Element> comprasAnteriores = buscarComprasDeCliente(documento, idCliente);

        // Si no hay nada que guardar ni que borrar, no se escribe el archivo
        if (comprasAnteriores.isEmpty() && productos.isEmpty()) {
            return;
        }

        // 2. Construir la nueva compra con DOM (solo si tiene productos)
        Element nuevaCompra = null;
        if (!productos.isEmpty()) {
            nuevaCompra = crearElementoCompra(documento, idCliente, productos);
        }

        // 3. Sustituir, añadir o eliminar la compra del cliente
        if (comprasAnteriores.isEmpty()) {
            raiz.appendChild(nuevaCompra);
        } else {
            Element primera = comprasAnteriores.get(0);
            if (nuevaCompra != null) {
                primera.getParentNode().replaceChild(nuevaCompra, primera);
            } else {
                primera.getParentNode().removeChild(primera);
            }
            // Si por error hubiera más de una compra del cliente, se eliminan
            for (int i = 1; i < comprasAnteriores.size(); i++) {
                Element duplicada = comprasAnteriores.get(i);
                duplicada.getParentNode().removeChild(duplicada);
            }
        }

        // 4. Guardar de forma segura (.tmp + Files.move)
        XMLUtil.guardarDocumento(documento, archivoCompras);
    }

    /**
     * Crea con DOM el elemento compra de un cliente:
     * compra clienteId="..." con un producto por cada elemento de la lista.
     *
     * @param documento documento de compras.xml
     * @param idCliente ID del cliente
     * @param productos productos de la compra
     * @return el elemento compra creado (todavía no está añadido al documento)
     */
    private static Element crearElementoCompra(Document documento, int idCliente, List<Producto> productos) {
        Element compra = documento.createElement("compra");
        compra.setAttribute("clienteId", String.valueOf(idCliente));

        for (Producto producto : productos) {
            Element elementoProducto = documento.createElement("producto");
            elementoProducto.setAttribute("id", String.valueOf(producto.getId()));

            agregarHijo(documento, elementoProducto, "nombre", producto.getNombre());
            agregarHijo(documento, elementoProducto, "precio", producto.getPrecioXML());
            agregarHijo(documento, elementoProducto, "cantidad", String.valueOf(producto.getCantidad()));

            compra.appendChild(elementoProducto);
        }
        return compra;
    }

    /**
     * Crea un elemento hijo con texto y lo añade al elemento padre.
     * Ejemplo: nombre con el texto "Leche".
     */
    private static void agregarHijo(Document documento, Element padre, String etiqueta, String texto) {
        Element hijo = documento.createElement(etiqueta);
        hijo.setTextContent(texto);
        padre.appendChild(hijo);
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
