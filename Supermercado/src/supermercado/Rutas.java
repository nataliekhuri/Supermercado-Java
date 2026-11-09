package supermercado;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

/**
 * Localiza las rutas que necesita Supermercado: la carpeta raíz del
 * proyecto, los JAR de las otras aplicaciones y el ejecutable de Java.
 *
 * Las rutas se calculan a partir de la ubicación del propio programa,
 * por eso funcionan desde cualquier directorio y también desde NetBeans.
 */
public class Rutas {

    /** Nombre de la carpeta común que identifica la raíz del proyecto. */
    private static final String CARPETA_DATOS = "datos";

    /**
     * Devuelve la ubicación del código que se está ejecutando:
     * el archivo JAR o, desde NetBeans, la carpeta build/classes.
     *
     * @return la ruta del código, o null si no se puede obtener
     */
    public static Path obtenerUbicacionCodigo() {
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
     * Busca la carpeta raíz del proyecto: sube por los directorios padres
     * hasta encontrar uno que contenga la carpeta "datos".
     *
     * @return la carpeta raíz, o null si no se encuentra
     */
    public static Path obtenerCarpetaRaiz() {
        Path actual = obtenerUbicacionCodigo();
        while (actual != null) {
            if (Files.isDirectory(actual.resolve(CARPETA_DATOS))) {
                return actual;
            }
            actual = actual.getParent();
        }
        return null;
    }

    /**
     * Construye la ruta del JAR de una aplicación.
     * Ejemplo: raiz\Clientes\Clientes.jar
     *
     * @param raiz carpeta raíz del proyecto
     * @param nombreAplicacion nombre de la aplicación (Clientes, Compras...)
     * @return la ruta del archivo JAR
     */
    public static Path obtenerRutaJar(Path raiz, String nombreAplicacion) {
        return raiz.resolve(nombreAplicacion).resolve(nombreAplicacion + ".jar");
    }

    /**
     * Devuelve la ruta del ejecutable de Java que está ejecutando este
     * programa, sin depender de la variable PATH del sistema.
     *
     * @return la ruta de java.exe (Windows) o java (otros sistemas)
     */
    public static Path obtenerEjecutableJava() {
        String sistema = System.getProperty("os.name").toLowerCase();
        String ejecutable = sistema.contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", ejecutable);
    }
}
