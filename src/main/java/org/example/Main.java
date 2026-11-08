package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String URL = "jdbc:postgresql://localhost:5433/banco";
    private static final String USER = "postgres";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // 0. Contraseña base de datos del usuario
        System.out.print("Introduce la contraseña de la BD: ");
        String PASS = sc.nextLine().trim();

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
                menuEmpleado(sc, c, nombre, dniValido);
            } else if ("C".equalsIgnoreCase(rol)) {
                menuCliente(sc, c, nombre, dniValido);
            }

        } catch (SQLException e) {
            System.err.println("Error en la base de datos: " + e.getMessage());
        }

        System.out.println("\n¡Gracias por usar la aplicación del Banco!");
        sc.close();
    }

    // Menú de consola para Cliente
    private static void menuCliente(Scanner sc, Connection c, String nombre, String dni) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n==================================");
            System.out.println("     MENÚ CLIENTE - " + nombre.toUpperCase());
            System.out.println("==================================");
            System.out.println("1. Consultar saldo");
            System.out.println("2. Ingresar en cajero");
            System.out.println("3. Retirar efectivo");
            System.out.println("4. Realizar transferencia");
            System.out.println("5. Cambiar PIN");
            System.out.println("0. Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            String opcion = sc.nextLine().trim();

            switch (opcion) {
                case "1":
                    mostrarSaldoUsuario(c, dni);
                    break;
                case "2":
                    procesarIngreso(sc, c, dni);
                    break;
                case "3":
                    procesarRetiro(sc, c, dni);
                    break;
                case "4":
                    procesarTransferencia(sc, c, dni, false);
                    break;
                case "5":
                    solicitarCambioPin(sc, c, dni);
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
    private static void menuEmpleado(Scanner sc, Connection c, String nombre, String dni) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n==================================");
            System.out.println("    MENÚ EMPLEADO - " + nombre.toUpperCase());
            System.out.println("==================================");
            System.out.println("1. Consultar saldo");
            System.out.println("2. Realizar transferencia");
            System.out.println("3. Gestionar cajero");
            System.out.println("4. Gestón de clientes");
            System.out.println("5. Gestión de cuentas");
            System.out.println("6. Cambiar PIN");
            System.out.println("0. Cerrar sesión");
            System.out.print("Selecciona una opción: ");

            String opcion = sc.nextLine().trim();

            switch (opcion) {
                case "1":
                    mostrarSaldoUsuario(c, dni);
                    break;
                case "2":
                    procesarTransferencia(sc, c, dni, true);
                    break;
                case "3":
                    System.out.println("-> Opción en desarrollo...");
                    break;
                case "4":
                    System.out.println("-> Opción en desarrollo...");
                    break;
                case "5":
                    System.out.println("-> Opción en desarrollo...");
                    break;
                case "6":
                    solicitarCambioPin(sc, c, dni);
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
    // Interfaz para cambiar PIN
    private static void solicitarCambioPin(Scanner sc, Connection c, String dni) {
        System.out.print("\nIntroduce tu nuevo PIN: ");
        String nuevoPin = sc.nextLine().trim();

        if (nuevoPin.isEmpty()) {
            System.out.println("El PIN no puede estar vacío.");
        } else {
            boolean exito = BancoMetodos.cambiarPin(c, dni, nuevoPin);
            if (exito) {
                System.out.println("PIN actualizado correctamente.");
            } else {
                System.out.println("No se pudo actualizar el PIN.");
            }
        }
    }

    private static void mostrarSaldoUsuario(Connection c, String dni) {
        List<String[]> cuentas = BancoMetodos.obtenerCuentasYSaldo(c, dni);

        if (cuentas.isEmpty()) {
            System.out.println("\nNo se encontraron cuentas asociadas a este DNI.");
            return;
        }

        System.out.println("\n==================================");
        System.out.println("       TUS CUENTAS Y SALDOS       ");
        System.out.println("==================================");

        int total = 0;
        for (String[] cuenta : cuentas) {
            String numCuenta = cuenta[0];
            int saldo = Integer.parseInt(cuenta[1]);
            total += saldo;

            System.out.printf("🔹 Cuenta Nº: %s | Saldo: %d €%n", numCuenta, saldo);
        }

        System.out.println("----------------------------------");
        System.out.printf("  SALDO TOTAL ACUMULADO: %d €%n", total);
        System.out.println("==================================");
    }

    private static void procesarTransferencia(Scanner sc, Connection c, String dni, boolean esEmpleado) {
        try {
            System.out.print("\nIntroduce la cuenta de origen (Nº): ");
            int origen = Integer.parseInt(sc.nextLine().trim());

            // Si es cliente, verificamos que la cuenta le pertenezca
            if (!esEmpleado && !BancoMetodos.esTitularDeCuenta(c, dni, origen)) {
                System.out.println("Esa cuenta no te pertenece o no existe.");
                return;
            }

            System.out.print("Introduce la cuenta de destino (Nº): ");
            int destino = Integer.parseInt(sc.nextLine().trim());

            // Validar que origen y destino no sean la misma cuenta
            if (origen == destino) {
                System.out.println("La cuenta de origen y la de destino no pueden ser la misma.");
                return;
            }

            System.out.print("Introduce la cantidad a transferir (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0) {
                System.out.println("La cantidad debe ser mayor que 0.");
                return;
            }

            // Llamada al método atómico de BancoMetodos
            if (BancoMetodos.realizarTransferencia(c, origen, destino, cantidad)) {
                System.out.println("Transferencia realizada con éxito.");
            } else {
                System.out.println("Error al realizar la transferencia. Revisa el saldo o que la cuenta destino exista.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto. Debes introducir un número entero.");
        }
    }

    private static void procesarIngreso(Scanner sc, Connection c, String dni) {
        try {
            System.out.print("\nIntroduce el ID del cajero (ej. 1): ");
            int idCajero = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Introduce la cuenta de destino (Nº): ");
            int numCuenta = Integer.parseInt(sc.nextLine().trim());

            if (!BancoMetodos.esTitularDeCuenta(c, dni, numCuenta)) {
                System.out.println("Esa cuenta no te pertenece o no existe.");
                return;
            }

            System.out.println("Introduce el número de billetes a ingresar:");
            System.out.print("Billetes de 5 €: ");
            int b5 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 10 €: ");
            int b10 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 20 €: ");
            int b20 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 50 €: ");
            int b50 = Integer.parseInt(sc.nextLine().trim());

            if (b5 < 0 || b10 < 0 || b20 < 0 || b50 < 0) {
                System.out.println("La cantidad de billetes no puede ser negativa.");
                return;
            }

            if (BancoMetodos.ingresarEnCajero(c, idCajero, numCuenta, b5, b10, b20, b50)) {
                int total = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
                System.out.printf("Ingreso de %d € realizado con éxito.%n", total);
            } else {
                System.out.println("Error al procesar el ingreso en el cajero.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto. Debes introducir un número entero.");
        }
    }

    private static void procesarRetiro(Scanner sc, Connection c, String dni) {
        try {
            System.out.print("\nIntroduce el ID del cajero (ej. 1): ");
            int idCajero = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Introduce el número de cuenta: ");
            int numCuenta = Integer.parseInt(sc.nextLine().trim());

            if (!BancoMetodos.esTitularDeCuenta(c, dni, numCuenta)) {
                System.out.println("Esa cuenta no te pertenece o no existe.");
                return;
            }

            System.out.print("Introduce la cantidad a retirar (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0 || cantidad % 5 != 0) {
                System.out.println("La cantidad debe ser mayor que 0 y múltiplo de 5 €.");
                return;
            }

            int[] billetes = BancoMetodos.retirarDeCajero(c, idCajero, numCuenta, cantidad);

            if (billetes != null) {
                System.out.println("Retire su dinero. Operación completada con éxito.");
                System.out.printf("Billetes entregados -> 50€: %d | 20€: %d | 10€: %d | 5€: %d%n",
                        billetes[0], billetes[1], billetes[2], billetes[3]);
            } else {
                System.out.println("No se pudo realizar el retiro (saldo insuficiente en la cuenta o billetes insuficientes en el cajero).");
            }

        } catch (NumberFormatException e) {
            System.out.println("Formato incorrecto. Debes introducir un número entero.");
        }
    }
}
