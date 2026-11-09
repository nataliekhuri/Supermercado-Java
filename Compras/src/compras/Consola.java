package compras;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Scanner;

/**
 * Lectura y validación de los datos que escribe el usuario por teclado,
 * y formato de los importes en euros.
 *
 * Todos los métodos de lectura vuelven a pedir el dato si no es válido,
 * de modo que el programa nunca termina por una entrada incorrecta.
 */
public class Consola {

    // Formato de precios: dígitos y, opcionalmente, coma o punto con 1 o 2 decimales
    private static final String FORMATO_PRECIO = "\\d+([.,]\\d{1,2})?";

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
     * Si se leyera con otra codificación, nombres como "Piña" se
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
     * Pide un texto que no esté vacío.
     *
     * @param mensaje texto que se muestra al usuario
     * @param campo nombre del dato, para el mensaje de error (por ejemplo, "el nombre")
     * @return el texto escrito, o null si la entrada se ha cerrado
     */
    public static String leerTexto(String mensaje, String campo) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerLinea();
            if (texto == null || !texto.isEmpty()) {
                return texto;
            }
            System.out.println("ERROR: " + campo + " no puede estar vacío.");
        }
    }

    /**
     * Pide un número entero positivo (mayor que cero).
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
     * Pide un precio mayor que cero con un máximo de dos decimales.
     * Acepta coma o punto como separador decimal.
     *
     * @param mensaje texto que se muestra al usuario
     * @return el precio, o null si la entrada se ha cerrado
     */
    public static BigDecimal leerPrecio(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerLinea();
            if (texto == null) {
                return null;
            }
            try {
                return convertirPrecio(texto);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    // ==================== LECTURA OPCIONAL (Enter = conservar) ====================

    /**
     * Pide un texto nuevo. Si el usuario pulsa Enter sin escribir nada,
     * se conserva el valor actual.
     *
     * @param mensaje texto que se muestra al usuario
     * @param actual valor actual
     * @return el texto nuevo o el actual; null si la entrada se ha cerrado
     */
    public static String leerTextoOpcional(String mensaje, String actual) {
        System.out.print(mensaje);
        String texto = leerLinea();
        if (texto == null) {
            return null;
        }
        return texto.isEmpty() ? actual : texto;
    }

    /**
     * Pide un precio nuevo. Si el usuario pulsa Enter, se conserva el
     * precio actual. Si escribe algo no válido, lo vuelve a pedir.
     *
     * @param mensaje texto que se muestra al usuario
     * @param actual precio actual
     * @return el precio nuevo o el actual; null si la entrada se ha cerrado
     */
    public static BigDecimal leerPrecioOpcional(String mensaje, BigDecimal actual) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerLinea();
            if (texto == null) {
                return null;
            }
            if (texto.isEmpty()) {
                return actual;
            }
            try {
                return convertirPrecio(texto);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    /**
     * Pide un entero positivo nuevo. Si el usuario pulsa Enter, se
     * conserva el valor actual. Si escribe algo no válido, lo vuelve a pedir.
     *
     * @param mensaje texto que se muestra al usuario
     * @param actual valor actual
     * @return el número nuevo o el actual; -1 si la entrada se ha cerrado
     */
    public static int leerEnteroPositivoOpcional(String mensaje, int actual) {
        while (true) {
            System.out.print(mensaje);
            String texto = leerLinea();
            if (texto == null) {
                return -1;
            }
            if (texto.isEmpty()) {
                return actual;
            }
            try {
                return convertirEnteroPositivo(texto);
            } catch (IllegalArgumentException e) {
                System.out.println("ERROR: " + e.getMessage());
            }
        }
    }

    // ==================== CONFIRMACIONES ====================

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

    /**
     * Pregunta qué hacer con los cambios sin guardar:
     * G (guardar), D (descartar) o C (cancelar).
     *
     * @param mensaje pregunta que se muestra al usuario
     * @return 'G', 'D' o 'C'; si la entrada se cierra, devuelve 'D'
     *         (se descartan los cambios: nunca se guarda sin permiso)
     */
    public static char leerGuardarDescartarCancelar(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String respuesta = leerLinea();
            if (respuesta == null) {
                return 'D';
            }
            if (respuesta.equalsIgnoreCase("G")) {
                return 'G';
            }
            if (respuesta.equalsIgnoreCase("D")) {
                return 'D';
            }
            if (respuesta.equalsIgnoreCase("C")) {
                return 'C';
            }
            System.out.println("ERROR: responda G (guardar), D (descartar) o C (cancelar).");
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

    /**
     * Convierte un texto en un precio. Acepta coma o punto como separador
     * decimal y un máximo de dos decimales. El precio debe ser mayor que cero.
     *
     * @param texto texto escrito por el usuario (por ejemplo, "1,20" o "1.2")
     * @return el precio como BigDecimal
     * @throws IllegalArgumentException con un mensaje en español si no es válido
     */
    public static BigDecimal convertirPrecio(String texto) {
        String limpio = texto.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("debe escribir un precio.");
        }
        if (!limpio.matches(FORMATO_PRECIO)) {
            throw new IllegalArgumentException("\"" + limpio
                    + "\" no es un precio válido (use números con un máximo de dos decimales, por ejemplo 1,20).");
        }
        BigDecimal precio = new BigDecimal(limpio.replace(',', '.'));
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("el precio debe ser mayor que cero.");
        }
        return precio;
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
