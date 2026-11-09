package clientes;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

/**
 * Aplicación Clientes: permite registrar, eliminar y consultar
 * los clientes del supermercado, guardados en clientes.xml.
 *
 * Se ejecuta como un programa independiente. Al elegir "Volver al
 * supermercado" termina y Supermercado recupera el control.
 *
 * @author natal
 */
public class Clientes {

    // Opciones del menú
    private static final int OPCION_ALTA = 1;
    private static final int OPCION_BAJA = 2;
    private static final int OPCION_LISTAR = 3;
    private static final int OPCION_VOLVER = 4;

    // Atributo opcional de la raíz que guarda el último ID asignado
    private static final String ATRIBUTO_ULTIMO_ID = "ultimoId";

    // Lector del teclado, compartido por todo el programa.
    // Usa la codificación real de la consola para leer bien las tildes.
    private static final Scanner teclado = new Scanner(System.in, obtenerCodificacionTeclado());

    // Rutas de los XML (se calculan al iniciar)
    private static Path archivoClientes;
    private static Path archivoCompras;

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
            opcion = leerOpcion();

            switch (opcion) {
                case OPCION_ALTA:
                    darDeAlta();
                    break;
                case OPCION_BAJA:
                    darDeBaja();
                    break;
                case OPCION_LISTAR:
                    listarClientes();
                    break;
                case OPCION_VOLVER:
                    System.out.println("Volviendo al supermercado...");
                    break;
            }
        } while (opcion != OPCION_VOLVER);
    }

    // ==================== MENÚ Y ENTRADA DE DATOS ====================

    /**
     * Devuelve la codificación con la que llega el texto del teclado.
     *
     * En la consola de Windows el teclado suele enviar el texto con la
     * página de códigos de la consola (por ejemplo, cp850), no en UTF-8.
     * Si se leyera con otra codificación, nombres como "María" se
     * guardarían mal en el XML.
     *
     * @return la codificación de la entrada estándar
     */
    private static Charset obtenerCodificacionTeclado() {
        // stdin.encoding existe desde Java 25; stdout.encoding, desde Java 19
        String[] propiedades = {"stdin.encoding", "stdout.encoding"};
        for (String propiedad : propiedades) {
            String valor = System.getProperty(propiedad);
            if (valor != null) {
                try {
                    return Charset.forName(valor);
                } catch (IllegalArgumentException e) {
                    // Codificación desconocida: se prueba la siguiente opción
                }
            }
        }
        return Charset.defaultCharset();
    }

    /**
     * Muestra el menú de Clientes.
     */
    private static void mostrarMenu() {
        System.out.println();
        System.out.println("========== CLIENTES ==========");
        System.out.println();
        System.out.println("1. Alta de cliente");
        System.out.println("2. Baja de cliente");
        System.out.println("3. Listar clientes");
        System.out.println("4. Volver al supermercado");
        System.out.println();
    }

    /**
     * Pide una opción al usuario hasta que escriba un número válido
     * entre 1 y 4. Controla las entradas vacías, las letras y los
     * números fuera de rango.
     *
     * @return la opción elegida
     */
    private static int leerOpcion() {
        while (true) {
            System.out.print("Seleccione una opción: ");

            // Si la entrada se cierra (por ejemplo, con Ctrl+Z), volvemos
            if (!teclado.hasNextLine()) {
                System.out.println();
                return OPCION_VOLVER;
            }

            String texto = teclado.nextLine().trim();

            if (texto.isEmpty()) {
                System.out.println("ERROR: debe escribir una opción.");
                continue;
            }

            try {
                int opcion = Integer.parseInt(texto);
                if (opcion >= OPCION_ALTA && opcion <= OPCION_VOLVER) {
                    return opcion;
                }
                System.out.println("ERROR: la opción debe ser un número entre 1 y 4.");
            } catch (NumberFormatException e) {
                System.out.println("ERROR: \"" + texto + "\" no es un número válido.");
            }
        }
    }

    /**
     * Pide un texto que no esté vacío. Si el usuario escribe solo
     * espacios o nada, muestra un error y lo vuelve a pedir.
     *
     * @param mensaje texto que se muestra al usuario
     * @return el texto escrito (sin espacios al principio ni al final),
     *         o null si la entrada se ha cerrado
     */
    private static String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (!teclado.hasNextLine()) {
                System.out.println();
                return null;
            }
            String texto = teclado.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("ERROR: el nombre no puede estar vacío.");
        }
    }

    /**
     * Pide un número entero positivo (mayor que cero). Si el usuario
     * escribe algo vacío, texto o un número no positivo, muestra un
     * error y lo vuelve a pedir.
     *
     * @param mensaje texto que se muestra al usuario
     * @return el número escrito, o -1 si la entrada se ha cerrado
     */
    private static int leerIdPositivo(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (!teclado.hasNextLine()) {
                System.out.println();
                return -1;
            }
            String texto = teclado.nextLine().trim();

            if (texto.isEmpty()) {
                System.out.println("ERROR: debe escribir un ID.");
                continue;
            }
            try {
                int numero = Integer.parseInt(texto);
                if (numero > 0) {
                    return numero;
                }
                System.out.println("ERROR: el ID debe ser un número entero positivo.");
            } catch (NumberFormatException e) {
                System.out.println("ERROR: \"" + texto + "\" no es un número entero válido.");
            }
        }
    }

    /**
     * Pide una confirmación S/N. Si la respuesta no es válida,
     * la vuelve a pedir.
     *
     * @param mensaje pregunta que se muestra al usuario
     * @return true si responde S; false si responde N o se cierra la entrada
     */
    private static boolean confirmar(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (!teclado.hasNextLine()) {
                System.out.println();
                return false;
            }
            String respuesta = teclado.nextLine().trim();
            if (respuesta.equalsIgnoreCase("S")) {
                return true;
            }
            if (respuesta.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("ERROR: responda S (sí) o N (no).");
        }
    }

    // ==================== ALTA DE CLIENTE ====================

    /**
     * Registra un cliente nuevo en clientes.xml. Solo se pide el nombre;
     * el ID se genera automáticamente y no se puede elegir.
     */
    private static void darDeAlta() {
        String nombre = leerTexto("Nombre del cliente: ");
        if (nombre == null) {
            return; // la entrada se ha cerrado
        }

        try {
            Document documento = XMLUtil.cargarDocumento(archivoClientes, "clientes");
            Element raiz = documento.getDocumentElement();
            int nuevoId = calcularSiguienteId(documento);

            // Crear <cliente id="..."><nombre>...</nombre></cliente> con DOM
            Element cliente = documento.createElement("cliente");
            cliente.setAttribute("id", String.valueOf(nuevoId));

            Element elementoNombre = documento.createElement("nombre");
            elementoNombre.setTextContent(nombre);
            cliente.appendChild(elementoNombre);

            raiz.appendChild(cliente);

            // Guardar el último ID usado para que nunca se reutilice
            raiz.setAttribute(ATRIBUTO_ULTIMO_ID, String.valueOf(nuevoId));

            XMLUtil.guardarDocumento(documento, archivoClientes);

            System.out.println("Cliente registrado correctamente.");
            System.out.println("ID: " + nuevoId + " | Nombre: " + nombre);
        } catch (Exception e) {
            System.out.println("ERROR: no se ha podido registrar el cliente. "
                    + mensajeError(e, "clientes.xml"));
        }
    }

    /**
     * Calcula el siguiente ID: el mayor valor entre el atributo
     * "ultimoId" de la raíz (si existe) y el ID mayor de los clientes
     * registrados, más uno. Así un ID nunca se reutiliza, aunque se
     * haya eliminado el cliente que lo tenía.
     *
     * @param documento documento de clientes.xml
     * @return el ID para el nuevo cliente
     * @throws Exception si algún ID guardado no es válido
     */
    private static int calcularSiguienteId(Document documento) throws Exception {
        int mayor = 0;

        // 1. Último ID guardado en la raíz (atributo opcional)
        String ultimoId = documento.getDocumentElement().getAttribute(ATRIBUTO_ULTIMO_ID);
        if (!ultimoId.isEmpty()) {
            try {
                mayor = Integer.parseInt(ultimoId.trim());
            } catch (NumberFormatException e) {
                throw new Exception("El atributo ultimoId (\"" + ultimoId + "\") no es válido.");
            }
        }

        // 2. ID mayor entre los clientes registrados
        NodeList nodos = documento.getElementsByTagName("cliente");
        for (int i = 0; i < nodos.getLength(); i++) {
            int id = leerId((Element) nodos.item(i));
            if (id > mayor) {
                mayor = id;
            }
        }

        return mayor + 1;
    }

    // ==================== BAJA DE CLIENTE ====================

    /**
     * Elimina un cliente de clientes.xml y, si tiene una compra
     * pendiente, también la elimina de compras.xml.
     *
     * Primero se hacen todos los cambios en memoria. Después se guarda
     * compras.xml y, por último, clientes.xml. Con este orden nunca
     * queda una compra de un cliente que ya no existe.
     */
    private static void darDeBaja() {

        // 1. Cargar los clientes y mostrarlos
        Document documentoClientes;
        List<Cliente> clientes;
        try {
            documentoClientes = XMLUtil.cargarDocumento(archivoClientes, "clientes");
            clientes = obtenerClientes(documentoClientes);
        } catch (Exception e) {
            System.out.println("ERROR: " + mensajeError(e, "clientes.xml"));
            return;
        }

        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados.");
            return;
        }
        mostrarClientes(clientes);
        System.out.println();

        // 2. Pedir el ID y comprobar que el cliente existe
        int id = leerIdPositivo("ID del cliente que desea eliminar: ");
        if (id == -1) {
            return; // la entrada se ha cerrado
        }

        Element cliente;
        try {
            cliente = buscarCliente(documentoClientes, id);
        } catch (Exception e) {
            System.out.println("ERROR: " + mensajeError(e, "clientes.xml"));
            return;
        }
        if (cliente == null) {
            System.out.println("ERROR: no existe ningún cliente con ID " + id + ".");
            return;
        }
        String nombre = cliente.getElementsByTagName("nombre").item(0).getTextContent();

        // 3. Comprobar si tiene una compra pendiente en compras.xml
        Document documentoCompras;
        List<Element> comprasCliente;
        try {
            documentoCompras = XMLUtil.cargarDocumento(archivoCompras, "compras");
            comprasCliente = buscarComprasDeCliente(documentoCompras, id);
        } catch (Exception e) {
            System.out.println("ERROR: " + mensajeError(e, "compras.xml"));
            System.out.println("No se ha eliminado ningún dato.");
            return;
        }

        int numeroProductos = 0;
        for (Element compra : comprasCliente) {
            numeroProductos += compra.getElementsByTagName("producto").getLength();
        }

        // 4. Informar y pedir confirmación
        System.out.println("Cliente seleccionado -> ID: " + id + " | Nombre: " + nombre);
        boolean confirmado;
        if (comprasCliente.isEmpty()) {
            System.out.println("El cliente no tiene compras pendientes.");
            confirmado = confirmar("¿Desea eliminar este cliente? (S/N): ");
        } else {
            System.out.println("AVISO: el cliente tiene una compra pendiente con "
                    + numeroProductos + " producto(s).");
            System.out.println("Si continúa, se eliminarán el cliente y su compra pendiente.");
            confirmado = confirmar("¿Desea eliminar el cliente y su compra pendiente? (S/N): ");
        }

        if (!confirmado) {
            System.out.println("Operación cancelada. No se ha modificado ningún dato.");
            return;
        }

        // 5. Hacer los cambios en memoria
        Element raizClientes = documentoClientes.getDocumentElement();
        try {
            // El contador conserva el ID mayor usado hasta ahora, para que
            // el ID eliminado no se vuelva a asignar (nunca disminuye)
            int ultimoId = calcularSiguienteId(documentoClientes) - 1;
            raizClientes.setAttribute(ATRIBUTO_ULTIMO_ID, String.valueOf(ultimoId));
        } catch (Exception e) {
            System.out.println("ERROR: " + mensajeError(e, "clientes.xml"));
            System.out.println("No se ha eliminado ningún dato.");
            return;
        }

        for (Element compra : comprasCliente) {
            compra.getParentNode().removeChild(compra);
        }
        cliente.getParentNode().removeChild(cliente);

        // 6. Guardar: primero compras.xml (solo si ha cambiado)...
        if (!comprasCliente.isEmpty()) {
            try {
                XMLUtil.guardarDocumento(documentoCompras, archivoCompras);
            } catch (Exception e) {
                System.out.println("ERROR: no se ha podido actualizar compras.xml. "
                        + mensajeError(e, "compras.xml"));
                System.out.println("No se ha eliminado ningún dato.");
                return;
            }
        }

        // ...y después clientes.xml
        try {
            XMLUtil.guardarDocumento(documentoClientes, archivoClientes);
        } catch (Exception e) {
            System.out.println("ERROR: no se ha podido actualizar clientes.xml. "
                    + mensajeError(e, "clientes.xml"));
            if (comprasCliente.isEmpty()) {
                System.out.println("No se ha eliminado ningún dato.");
            } else {
                System.out.println("La compra pendiente se ha eliminado, pero el cliente sigue registrado.");
                System.out.println("Puede intentar la baja de nuevo.");
            }
            return;
        }

        System.out.println("Cliente eliminado correctamente: ID " + id + " | Nombre: " + nombre);
        if (!comprasCliente.isEmpty()) {
            System.out.println("También se ha eliminado su compra pendiente.");
        }
    }

    /**
     * Busca un cliente por su ID.
     *
     * @param documento documento de clientes.xml
     * @param id ID buscado
     * @return el elemento cliente, o null si no existe
     * @throws Exception si algún ID guardado no es válido
     */
    private static Element buscarCliente(Document documento, int id) throws Exception {
        NodeList nodos = documento.getElementsByTagName("cliente");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element cliente = (Element) nodos.item(i);
            if (leerId(cliente) == id) {
                return cliente;
            }
        }
        return null;
    }

    /**
     * Busca las compras pendientes de un cliente en compras.xml.
     * Normalmente habrá como máximo una.
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

    // ==================== LISTAR CLIENTES ====================

    /**
     * Muestra todos los clientes registrados con su ID y nombre.
     * Si no hay clientes, muestra un mensaje informativo.
     */
    private static void listarClientes() {
        List<Cliente> clientes;
        try {
            Document documento = XMLUtil.cargarDocumento(archivoClientes, "clientes");
            clientes = obtenerClientes(documento);
        } catch (Exception e) {
            System.out.println("ERROR: " + mensajeError(e, "clientes.xml"));
            return;
        }

        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados.");
            return;
        }
        mostrarClientes(clientes);
    }

    /**
     * Muestra una lista de clientes con el formato del enunciado.
     *
     * @param clientes clientes que se van a mostrar
     */
    private static void mostrarClientes(List<Cliente> clientes) {
        System.out.println();
        System.out.println("====== CLIENTES REGISTRADOS ======");
        System.out.println();
        for (Cliente cliente : clientes) {
            System.out.println("ID: " + cliente.getId() + " | Nombre: " + cliente.getNombre());
        }
    }

    /**
     * Convierte los elementos cliente de clientes.xml en una lista.
     *
     * @param documento documento de clientes.xml
     * @return lista de clientes (vacía si no hay ninguno)
     * @throws Exception si algún cliente tiene datos no válidos
     */
    private static List<Cliente> obtenerClientes(Document documento) throws Exception {
        NodeList nodos = documento.getElementsByTagName("cliente");

        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            Element elemento = (Element) nodos.item(i);
            int id = leerId(elemento);

            NodeList nombres = elemento.getElementsByTagName("nombre");
            if (nombres.getLength() == 0) {
                throw new Exception("En clientes.xml, el cliente con ID " + id + " no tiene nombre.");
            }
            String nombre = nombres.item(0).getTextContent();

            clientes.add(new Cliente(id, nombre));
        }
        return clientes;
    }

    // ==================== UTILIDADES ====================

    /**
     * Lee el atributo "id" de un elemento cliente.
     *
     * @param cliente elemento cliente
     * @return el ID como número
     * @throws Exception si el ID no es un número válido
     */
    private static int leerId(Element cliente) throws Exception {
        String textoId = cliente.getAttribute("id");
        try {
            return Integer.parseInt(textoId.trim());
        } catch (NumberFormatException e) {
            throw new Exception("En clientes.xml, el ID \"" + textoId + "\" no es válido.");
        }
    }

    /**
     * Convierte una excepción en un mensaje claro en español.
     *
     * @param e excepción producida
     * @param nombreArchivo archivo con el que se estaba trabajando
     * @return mensaje para mostrar al usuario
     */
    private static String mensajeError(Exception e, String nombreArchivo) {
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
