package compras;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Localiza la carpeta común "datos" y los archivos XML del proyecto.
 *
 * La ruta se calcula a partir de la ubicación del propio programa,
 * por eso funciona desde cualquier directorio y también desde NetBeans.
 */
public class Rutas {

    /** Nombre de la carpeta común donde están los XML. */
    private static final String CARPETA_DATOS = "datos";

    /**
     * Devuelve la ubicación del código que se está ejecutando:
     * el archivo JAR o, desde NetBeans, la carpeta build/classes.
     *
     * @return la ruta del código, o null si no se puede obtener
     */
    private static Path obtenerUbicacionCodigo() {
        try {
            CodeSource origen = Rutas.class.getProtectionDomain().getCodeSource();
            if (origen == null) {
                return null;
            }
            return Path.of(origen.getLocation().toURI()).toAbsolutePath();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Busca la carpeta "datos": sube por los directorios padres
     * hasta encontrar uno que la contenga.
     *
     * @return la ruta de la carpeta datos, o null si no se encuentra
     */
    public static Path obtenerCarpetaDatos() {
        Path actual = obtenerUbicacionCodigo();
        while (actual != null) {
            Path datos = actual.resolve(CARPETA_DATOS);
            if (Files.isDirectory(datos)) {
                return datos;
            }
            actual = actual.getParent();
        }
        return null;
    }

    /**
     * @param carpetaDatos carpeta datos del proyecto
     * @return la ruta de clientes.xml
     */
    public static Path obtenerArchivoClientes(Path carpetaDatos) {
        return carpetaDatos.resolve("clientes.xml");
    }

    /**
     * @param carpetaDatos carpeta datos del proyecto
     * @return la ruta de compras.xml
     */
    public static Path obtenerArchivoCompras(Path carpetaDatos) {
        return carpetaDatos.resolve("compras.xml");
    }
}
