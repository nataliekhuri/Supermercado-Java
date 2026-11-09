package supermercado;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ejecuta las aplicaciones Clientes, Compras y Facturacion como
 * procesos independientes del sistema operativo.
 *
 * Supermercado espera a que termine cada proceso y comprueba su
 * código de salida antes de volver al menú principal.
 */
public class LanzadorProcesos {

    /**
     * Ejecuta el JAR de una aplicación y espera a que termine.
     *
     * @param nombreAplicacion nombre de la aplicación y de su JAR
     *                         (Clientes, Compras o Facturacion)
     */
    public static void ejecutarAplicacion(String nombreAplicacion) {

        // 1. Comprobaciones previas: raíz del proyecto, JAR y Java
        Path raiz = Rutas.obtenerCarpetaRaiz();
        if (raiz == null) {
            System.out.println("ERROR: no se encuentra la carpeta 'datos' del proyecto.");
            return;
        }

        Path jar = Rutas.obtenerRutaJar(raiz, nombreAplicacion);
        if (!Files.isRegularFile(jar)) {
            System.out.println("ERROR: no se encuentra el archivo " + jar);
            return;
        }

        Path java = Rutas.obtenerEjecutableJava();
        if (!Files.isRegularFile(java)) {
            System.out.println("ERROR: no se encuentra el ejecutable de Java: " + java);
            return;
        }

        // 2. Preparar el proceso: equivale a escribir "java -jar <ruta del JAR>"
        ProcessBuilder constructor = new ProcessBuilder(java.toString(), "-jar", jar.toString());
        constructor.directory(jar.getParent().toFile()); // se ejecuta en su propia carpeta
        constructor.inheritIO();                         // comparte la consola con Supermercado

        System.out.println("Abriendo " + nombreAplicacion + "...");

        // 3. Iniciar el proceso y esperar a que termine
        try {
            Process proceso = constructor.start();
            int codigoSalida = proceso.waitFor();

            if (codigoSalida == 0) {
                System.out.println(nombreAplicacion + " se ha cerrado correctamente.");
            } else {
                System.out.println("AVISO: " + nombreAplicacion
                        + " ha terminado con un error (código de salida " + codigoSalida + ").");
            }
        } catch (IOException e) {
            System.out.println("ERROR: no se ha podido iniciar " + nombreAplicacion + ": " + e.getMessage());
        } catch (InterruptedException e) {
            // Se restaura el estado de interrupción del hilo (buena práctica)
            Thread.currentThread().interrupt();
            System.out.println("ERROR: se ha interrumpido la espera de " + nombreAplicacion + ".");
        }
    }
}
