package supermercado;

import java.util.Scanner;

/**
 * Aplicación principal del sistema Supermercado.
 *
 * Muestra el menú general y abre las aplicaciones Clientes, Compras
 * y Facturacion como procesos independientes (ver LanzadorProcesos).
 *
 * @author natal
 */
public class Supermercado {

    // Opciones del menú principal
    private static final int OPCION_CLIENTES = 1;
    private static final int OPCION_COMPRAS = 2;
    private static final int OPCION_FACTURACION = 3;
    private static final int OPCION_SALIR = 4;

    // Lector del teclado, compartido por todo el programa
    private static final Scanner teclado = new Scanner(System.in);

    public static void main(String[] args) {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerOpcion();

            switch (opcion) {
                case OPCION_CLIENTES:
                    LanzadorProcesos.ejecutarAplicacion("Clientes");
                    break;
                case OPCION_COMPRAS:
                    LanzadorProcesos.ejecutarAplicacion("Compras");
                    break;
                case OPCION_FACTURACION:
                    LanzadorProcesos.ejecutarAplicacion("Facturacion");
                    break;
                case OPCION_SALIR:
                    System.out.println("Saliendo de Supermercado. ¡Hasta pronto!");
                    break;
            }
        } while (opcion != OPCION_SALIR);
    }

    /**
     * Muestra el menú principal.
     */
    private static void mostrarMenu() {
        System.out.println();
        System.out.println("========== SUPERMERCADO ==========");
        System.out.println();
        System.out.println("1. Abrir Clientes");
        System.out.println("2. Abrir Compras");
        System.out.println("3. Abrir Facturación");
        System.out.println("4. Salir");
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

            // Si la entrada se cierra (por ejemplo, con Ctrl+Z), salimos
            if (!teclado.hasNextLine()) {
                System.out.println();
                return OPCION_SALIR;
            }

            String texto = teclado.nextLine().trim();

            if (texto.isEmpty()) {
                System.out.println("ERROR: debe escribir una opción.");
                continue;
            }

            try {
                int opcion = Integer.parseInt(texto);
                if (opcion >= OPCION_CLIENTES && opcion <= OPCION_SALIR) {
                    return opcion;
                }
                System.out.println("ERROR: la opción debe ser un número entre 1 y 4.");
            } catch (NumberFormatException e) {
                System.out.println("ERROR: \"" + texto + "\" no es un número válido.");
            }
        }
    }
}
