# 🏦 Sistema Bancario JDBC

Proyecto de consola en **Java** y **PostgreSQL** para Acceso a Datos (2º DAM).

## Tecnologías
* Java 27 | PostgreSQL | JDBC | Maven

## Estructura
* **`Main.java`**: Menús, `Scanner` y ciclo de la aplicación.
* **`BancoMetodos.java`**: Consultas SQL (`PreparedStatement`, `CallableStatement` y `ResultSet`).
* **`Banco.sql`**: Script de creación de la BD y función almacenada `obtener_nombre_usuario`.
* **`RESPUESTAS.md`**: Respuesta razonada sobre ventajas e inconvenientes de los conectores.

## Inicio rápido
1. Ejecuta `Banco.sql` en PostgreSQL.
2. Ajusta la conexión en `Main.java`.
3. Compila y ejecuta `Main.java`.
