package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    private static final String URL = "jdbc:postgresql://localhost:5433/banco";
    private static final String USER = "postgres";
    private static final String PASS = "postgres123";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("¡Conectado con éxito a la base de datos Banco!");

            String rol = null;
            String dniValido = "";

            // 1. LOGIN (Interfaz en el Main)
            while (rol == null) {
                System.out.println("\n--- ACCESO AL BANCO ---");
                System.out.print("Introduce tu DNI: ");
                String dni = sc.nextLine().trim();

                System.out.print("Introduce tu PIN: ");
                String pin = sc.nextLine().trim();

                // El método SOLO consulta a la BD y devuelve 'E', 'C' o null
                rol = BancoMetodos.autenticarUsuario(c, dni, pin);

                if (rol == null) {
                    System.out.println("DNI o PIN incorrectos. Vuelve a intentarlo.");
                } else {
                    dniValido = dni;
                }
            }

            String nombre = BancoMetodos.obtenerNombre(c, dniValido);
            System.out.println("\nLogin correcto. ¡Hola, " + nombre + "!");

            // 2. REDIRECCIÓN A MENÚS EN EL MAIN
            if ("E".equalsIgnoreCase(rol)) {
                menuEmpleado(sc, c, nombre);
            } else if ("C".equalsIgnoreCase(rol)) {
                menuCliente(sc, c, nombre);
            }

        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }

        System.out.println("\n¡Gracias por usar la aplicación del Banco!");
        sc.close();
    }

    // Menú de consola para Cliente
    private static void menuCliente(Scanner sc, Connection c, String nombre) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n==================================");
            System.out.println("     MENÚ CLIENTE - " + nombre.toUpperCase());
            System.out.println("==================================");
            System.out.println("1. Consultar saldo");
            System.out.println("2. Ingresar dinero");
            System.out.println("3. Sacar dinero");
            System.out.println("4. Transferencia");
            System.out.println("5. Ver últimos movimientos");
            System.out.println("0. Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            String opcion = sc.nextLine().trim();

            switch (opcion) {
                case "1":
                case "2":
                case "3":
                case "4":
                case "5":
                    System.out.println("-> Opción en desarrollo...");
                    break;
                case "0":
                    System.out.println("Cerrando sesión de cliente...");
                    salir = true;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    // Menú de consola para Empleado
    private static void menuEmpleado(Scanner sc, Connection c, String nombre) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n==================================");
            System.out.println("    MENÚ EMPLEADO - " + nombre.toUpperCase());
            System.out.println("==================================");
            System.out.println("1. Listar todos los clientes");
            System.out.println("2. Alta de nuevo cliente");
            System.out.println("3. Buscar cliente por DNI");
            System.out.println("4. Ver total de depósitos del banco");
            System.out.println("0. Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            String opcion = sc.nextLine().trim();

            switch (opcion) {
                case "1":
                case "2":
                case "3":
                case "4":
                    System.out.println("-> Opción en desarrollo...");
                    break;
                case "0":
                    System.out.println("Cerrando sesión de empleado...");
                    salir = true;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }
}
