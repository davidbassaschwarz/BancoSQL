# Respuestas — Tarea 2.1

## ¿Qué ventajas e inconvenientes tiene utilizar conectores?

### Ventajas

* **Independencia de la base de datos.** El código Java es prácticamente el mismo usando PostgreSQL, MySQL u Oracle. Solo cambia la URL de conexión y el driver JAR (salvo diferencias de SQL propias de cada SGBD).
* **Seguridad contra la inyección SQL.** Al usar `PreparedStatement`, los parámetros se tratan como datos y no como código, evitando que se cuele SQL malicioso.
* **Control total de las transacciones.** Permite manejar `commit` y `rollback` a mano para asegurar que operaciones como las transferencias se hagan enteras o no se hagan.
* **Llamadas a funciones del servidor.** Permite ejecutar procedimientos y funciones almacenadas de la base de datos fácilmente con `CallableStatement`.

### Inconvenientes

* **Mucho código repetitivo.** Hay que abrir y cerrar conexiones, controlar las `SQLException` y mapear los `ResultSet` a mano, lo que hace el código más largo.
* **Errores solo en ejecución.** Como las consultas SQL son cadenas de texto en Java, si te equivocas en el nombre de una columna el IDE no te avisa al compilar; el fallo aparece al ejecutar.
* **Gestión manual de relaciones.** A diferencia de un ORM (como Hibernate), la base de datos no convierte los datos a objetos automáticamente: hay que hacer los `JOIN` y la lógica a mano.
