package facturacion;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Operaciones generales con archivos XML mediante la API DOM:
 * cargar (creando el archivo si no existe) y guardar de forma segura.
 */
public class XMLUtil {

    /**
     * Carga un documento XML. Si el archivo no existe, lo crea
     * con su elemento raíz vacío.
     *
     * Si el archivo existe pero está mal formado o su raíz no es la
     * esperada, lanza una excepción y NO modifica el archivo.
     *
     * @param archivo ruta del archivo XML
     * @param nombreRaiz nombre del elemento raíz (clientes o compras)
     * @return el documento cargado
     */
    public static Document cargarDocumento(Path archivo, String nombreRaiz)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {

        DocumentBuilder constructor = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        // Los errores se lanzan como excepción, sin que el parser los imprima en consola
        constructor.setErrorHandler(new DefaultHandler());

        // Si no existe, se crea con su elemento raíz
        if (!Files.exists(archivo)) {
            Document nuevo = constructor.newDocument();
            nuevo.appendChild(nuevo.createElement(nombreRaiz));
            guardarDocumento(nuevo, archivo);
            return nuevo;
        }

        // Si existe, se lee y se comprueba la raíz
        Document documento = constructor.parse(archivo.toFile());
        String raizEncontrada = documento.getDocumentElement().getTagName();
        if (!raizEncontrada.equals(nombreRaiz)) {
            throw new SAXException("El archivo " + archivo.getFileName()
                    + " tiene la raíz <" + raizEncontrada + "> en lugar de <" + nombreRaiz + ">.");
        }

        limpiarEspacios(documento.getDocumentElement());
        return documento;
    }

    /**
     * Guarda un documento XML de forma segura: primero lo escribe en un
     * archivo temporal (.tmp) y después sustituye el archivo original.
     * Así el XML nunca queda escrito a medias.
     *
     * @param documento documento que se va a guardar
     * @param archivo ruta del archivo XML
     */
    public static void guardarDocumento(Document documento, Path archivo)
            throws TransformerException, IOException {

        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");

        // Configuración del Transformer: UTF-8 y sangría de 4 espacios
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        // Salto de línea entre la cabecera <?xml ...?> y el elemento raíz
        transformer.setOutputProperty("http://www.oracle.com/xml/is-standalone", "yes");
        documento.setXmlStandalone(true); // evita standalone="no" en la cabecera

        try {
            // 1. Escribir el documento en el archivo temporal
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                transformer.transform(new DOMSource(documento), new StreamResult(salida));
            }

            // 2. Sustituir el archivo original por el temporal
            try {
                Files.move(temporal, archivo,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Si algo ha fallado, no se deja el archivo temporal
            Files.deleteIfExists(temporal);
        }
    }

    /**
     * Elimina los nodos de texto que solo contienen espacios o saltos
     * de línea. Sin esto, cada vez que se guarda el XML aparecen
     * líneas en blanco adicionales.
     *
     * @param nodo nodo desde el que se empieza a limpiar
     */
    public static void limpiarEspacios(Node nodo) {
        NodeList hijos = nodo.getChildNodes();
        // Se recorre de atrás hacia delante porque se eliminan nodos
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.TEXT_NODE && hijo.getTextContent().trim().isEmpty()) {
                nodo.removeChild(hijo);
            } else if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                limpiarEspacios(hijo);
            }
        }
    }
}
