package facturacion;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;

/**
 * Lectura y validación de los datos que escribe el usuario por teclado,
 * y formato de los importes en euros.
 *
 * Versión reducida de la clase Consola de Compras: Facturación solo
 * necesita leer opciones de menú, IDs y confirmaciones S/N.
 *
 * Todos los métodos de lectura vuelven a pedir el dato si no es válido,
 * de modo que el programa nunca termina por una entrada incorrecta.
 */
public class Consola {

    // Formato español para mostrar importes: 1,20 €
    private static final Locale ESPANOL = Locale.forLanguageTag("es-ES");

    // Lector del teclado. Usa la codificación real de la consola para leer bien las tildes.
    private static final Scanner teclado = new Scanner(System.in, obtenerCodificacionTeclado());

    // ==================== CODIFICACIÓN DEL TECLADO ====================

    /**
     * Devuelve la codificación con la que llega el texto del teclado.
     *
     * En la consola de Windows el teclado suele enviar el texto con la
     * página de códigos de la consola (por ejemplo, cp850), no en UTF-8.
     * Si se leyera con otra codificación, las tildes se leerían mal.
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
     * Lee una línea del teclado.
     *
     * @return la línea sin espacios al principio ni al final,
     *         o null si la entrada se ha cerrado (por ejemplo, con Ctrl+Z)
     */
    private static String leerLinea() {
        if (!teclado.hasNextLine()) {
            System.out.println();
            return null;
        }
        return teclado.nextLine().trim();
    }

    // ==================== LECTURA DE DATOS ====================

    /**
     * Pide una opción de menú hasta que sea un número entre min y max.
     *
     * @param min opción más baja
     * @param max opción más alta (la de volver o salir)
     * @return la opción elegida; si la entrada se cierra, devuelve max
     */
    public static int leerOpcion(int min, int max) {
        while (true) {
            System.out.print("Seleccione una opción: ");
            String texto = leerLinea();
            if (texto == null) {
                return max;
            }
            if (texto.isEmpty()) {
                System.out.println("ERROR: debe escribir una opción.");
                continue;
            }
            try {
                int opcion = Integer.parseInt(texto);
                if (opcion >= min && opcion <= max) {
                    return opcion;
                }
                System.out.println("ERROR: la opción debe ser un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("ERROR: \"" + texto + "\" no es un número válido.");
            }
        }
    }

    /**
     * Pide un número entero positivo (mayor que cero), por ejemplo un ID.
     *
     * @param mensaje texto que se muestra al usuario
     * @return el número, o -1 si la entrada se ha cerrado
     */
    public static int leerEnteroPositivo(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerLinea();
            if (texto == null) {
                return -1;
            }
            try {
                return convertirEnteroPositivo(texto);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Pide una confirmación S/N. Si la respuesta no es válida, la vuelve a pedir.
     *
     * @param mensaje pregunta que se muestra al usuario
     * @return true si responde S; false si responde N o se cierra la entrada
     */
    public static boolean confirmar(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String respuesta = leerLinea();
            if (respuesta == null) {
                return false;
            }
            if (respuesta.equalsIgnoreCase("S")) {
                return true;
            }
            if (respuesta.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("ERROR: responda S (sí) o N (no).");
        }
    }

    // ==================== CONVERSIÓN Y VALIDACIÓN ====================

    /**
     * Convierte un texto en un número entero positivo.
     *
     * @param texto texto escrito por el usuario
     * @return el número
     * @throws IllegalArgumentException con un mensaje en español si no es válido
     */
    public static int convertirEnteroPositivo(String texto) {
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("debe escribir un número.");
        }
        int numero;
        try {
            numero = Integer.parseInt(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + limpio + "\" no es un número entero válido.");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("debe ser un número entero mayor que cero.");
        }
        return numero;
    }

    // ==================== FORMATO ====================

    /**
     * Da formato español a un importe: dos decimales, coma decimal
     * y el símbolo del euro (por ejemplo, 1,20 €).
     *
     * @param importe importe que se va a mostrar
     * @return el importe con formato
     */
    public static String formatearEuros(BigDecimal importe) {
        return String.format(ESPANOL, "%.2f €", importe);
    }
}
