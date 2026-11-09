========================================================================
 PROYECTO INTERMODULAR: SUPERMERCADO
 Programación de Servicios y Procesos + Acceso a Datos
 Autora: Natalie Khuri
========================================================================

1. CONTENIDO DE LA ENTREGA
--------------------------
Supermercado-Java/
  Supermercado/   Supermercado.jar + src/supermercado/*.java  (aplicación principal)
  Clientes/       Clientes.jar     + src/clientes/*.java
  Compras/        Compras.jar      + src/compras/*.java
  Facturacion/    Facturacion.jar  + src/facturacion/*.java
  datos/          clientes.xml y compras.xml (carpeta común)
  README.txt      este archivo

Cada carpeta de aplicación es además un proyecto de Apache NetBeans
(Java with Ant), con su build.xml y su carpeta nbproject.


2. REQUISITOS
-------------
- Java Runtime 21 o superior. No se necesitan librerías externas.


3. CÓMO EJECUTAR EL PROYECTO
----------------------------
El punto de entrada es Supermercado.jar. Desde una consola (CMD):

    java -jar Supermercado\Supermercado.jar

(ejecutado desde la carpeta principal del proyecto; también funciona
indicando la ruta completa del JAR desde cualquier otra carpeta).

Si el comando "java" no está en el PATH, se puede usar la ruta completa
del ejecutable, por ejemplo:

    "C:\Program Files\Apache NetBeans\jdk\bin\java.exe" -jar Supermercado\Supermercado.jar

Desde el menú de Supermercado se abren las otras tres aplicaciones.
Clientes.jar, Compras.jar y Facturacion.jar también pueden ejecutarse
por separado con "java -jar".

Se recomienda ejecutar el proyecto desde CMD: la ventana Output de
NetBeans no siempre permite escribir en los procesos secundarios.


4. CÓMO SE COMUNICAN LOS PROCESOS (PSP)
---------------------------------------
Supermercado ejecuta cada aplicación como un proceso real del sistema
operativo (otra máquina virtual de Java), nunca llamando a sus métodos:

  - ProcessBuilder prepara el comando:  <java> -jar <ruta del JAR>
  - <java> es el mismo ejecutable que está usando Supermercado
    (propiedad java.home), por lo que no depende del PATH.
  - inheritIO() hace que el proceso hijo use la misma consola: el usuario
    lee y escribe directamente en la aplicación abierta.
  - waitFor() deja a Supermercado esperando hasta que el hijo termina.
  - Se comprueba el código de salida: 0 = cierre correcto; otro valor =
    se muestra un aviso. Si no se encuentra el JAR o no se puede iniciar
    el proceso, se informa del error y se vuelve al menú.

Los JAR se localizan a partir de la ubicación del propio Supermercado.jar,
así que el proyecto funciona desde cualquier directorio de trabajo.


5. CÓMO SE COMPARTEN LOS DATOS (ACCESO A DATOS)
-----------------------------------------------
Las aplicaciones comparten los archivos datos/clientes.xml y
datos/compras.xml, que se leen y escriben exclusivamente con la API DOM
(DocumentBuilder, Document, Element, NodeList y Transformer).

  - Cada aplicación localiza la carpeta "datos" subiendo por las carpetas
    desde la ubicación de su propio JAR (o de build/classes en NetBeans).
  - Si un XML no existe, se crea con su elemento raíz.
  - Si un XML está mal formado, se informa del error y NO se sobrescribe.
  - Para no dejar nunca un XML escrito a medias, cada archivo se guarda
    primero en un archivo temporal (.tmp) que después sustituye al original.
  - Codificación UTF-8; los precios se guardan con punto decimal (1.20) y
    se muestran en formato español (1,20 €). Los importes se calculan con
    BigDecimal.

Uso de cada archivo:
  - Clientes:    lee y modifica clientes.xml. Al dar de baja a un cliente
                 elimina también su compra pendiente de compras.xml
                 (primero compras.xml y después clientes.xml, para que nunca
                 quede una compra de un cliente inexistente).
  - Compras:     lee clientes.xml; lee y modifica compras.xml. Los cambios se
                 hacen en memoria y solo se guardan con "Guardar compra".
  - Facturación: lee clientes.xml; al confirmar una factura elimina la compra
                 de ese cliente de compras.xml. Nunca modifica clientes.xml.

Nota sobre clientes.xml: el elemento raíz puede llevar el atributo
opcional ultimoId (por ejemplo <clientes ultimoId="3">). Guarda el último
ID asignado para que un ID eliminado no se reutilice nunca. Si no existe,
el programa funciona igualmente usando el ID mayor registrado.


6. RECOMPILAR (OPCIONAL)
------------------------
No es necesario: los JAR entregados ya son ejecutables. Para recompilar,
abrir cada carpeta como proyecto en Apache NetBeans y usar "Clean and
Build". El build.xml de cada proyecto copia automáticamente el JAR
generado a la carpeta de la aplicación.
