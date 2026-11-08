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

        System.out.print("Introduce la contraseña de la BD: ");
        String pass = sc.nextLine().trim();

        // Bloque try-with-resources para asegurar el cierre de la conexion
        try (Connection c = DriverManager.getConnection(URL, USER, pass)) {
            System.out.println("[INFO] ¡Conectado con éxito a la base de datos Banco!");

            String rol = null;
            String dniValido = "";

            // --- BUCLE DE LOGIN ---
            while (rol == null) {
                System.out.println("\n--- ACCESO AL BANCO ---");
                System.out.print("Introduce tu DNI: ");
                String dni = sc.nextLine().trim();

                System.out.print("Introduce tu PIN: ");
                String pin = sc.nextLine().trim();

                rol = BancoMetodos.autenticarUsuario(c, dni, pin);

                if (rol == null) {
                    System.out.println("[ERROR] DNI o PIN incorrectos. Vuelve a intentarlo.");
                } else {
                    dniValido = dni;
                }
            }

            // Obtenemos el nombre usando la funcion almacenada de la BD
            String nombre = BancoMetodos.obtenerNombre(c, dniValido);
            System.out.println("\n[EXITO] Login correcto. ¡Hola, " + nombre + "!");

            // --- REDIRECCION DE MENUS SEGUN ROL ---
            if ("E".equalsIgnoreCase(rol)) {
                menuEmpleado(sc, c, nombre, dniValido);
            } else if ("C".equalsIgnoreCase(rol)) {
                menuCliente(sc, c, nombre, dniValido);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Fallo critico en la base de datos: " + e.getMessage());
        }

        System.out.println("\n¡Gracias por usar la aplicación del Banco!");
        sc.close();
    }

    /* =========================================================
     *                    MENÚS PRINCIPALES
     * ========================================================= */

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
                    System.out.println("[INFO] Cerrando sesión de cliente...");
                    salir = true;
                    break;
                default:
                    System.out.println("[ERROR] Opción no válida.");
            }
        }
    }

    private static void menuEmpleado(Scanner sc, Connection c, String nombre, String dni) {
        boolean salir = false;
        while (!salir) {
            System.out.println("\n==================================");
            System.out.println("    MENÚ EMPLEADO - " + nombre.toUpperCase());
            System.out.println("==================================");
            System.out.println("1. Consultar saldo (Uso propio)");
            System.out.println("2. Realizar transferencia (Uso propio)");
            System.out.println("3. Rellenar cajero");
            System.out.println("4. Gestión de clientes");
            System.out.println("5. Gestión de cuentas");
            System.out.println("6. Cambiar PIN (Uso propio)");
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
                    procesarRellenarCajero(sc, c);
                    break;
                case "4":
                    procesarGestionClientes(sc, c);
                    break;
                case "5":
                    procesarGestionCuentas(sc, c);
                    break;
                case "6":
                    solicitarCambioPin(sc, c, dni);
                    break;
                case "0":
                    System.out.println("[INFO] Cerrando sesión de empleado...");
                    salir = true;
                    break;
                default:
                    System.out.println("[ERROR] Opción no válida.");
            }
        }
    }

    /* =========================================================
     *               MÉTODOS PROCESADORES (VISTA)
     * ========================================================= */

    private static void solicitarCambioPin(Scanner sc, Connection c, String dni) {
        System.out.print("\nIntroduce tu nuevo PIN (4 dígitos): ");
        String nuevoPin = sc.nextLine().trim();

        if (nuevoPin.isEmpty()) {
            System.out.println("[ERROR] El PIN no puede estar vacío.");
        } else {
            if (BancoMetodos.cambiarPin(c, dni, nuevoPin)) {
                System.out.println("[EXITO] PIN actualizado correctamente.");
            } else {
                System.out.println("[ERROR] No se pudo actualizar el PIN.");
            }
        }
    }

    private static void mostrarSaldoUsuario(Connection c, String dni) {
        List<String[]> cuentas = BancoMetodos.obtenerCuentasYSaldo(c, dni);

        if (cuentas.isEmpty()) {
            System.out.println("\n[INFO] No se encontraron cuentas asociadas a este DNI.");
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
            System.out.printf(" - Cuenta Nº: %s | Saldo: %d €%n", numCuenta, saldo);
        }

        System.out.println("----------------------------------");
        System.out.printf("  SALDO TOTAL ACUMULADO: %d €%n", total);
        System.out.println("==================================");
    }

    private static void procesarTransferencia(Scanner sc, Connection c, String dni, boolean esEmpleado) {
        try {
            System.out.print("\nIntroduce la cuenta de origen (Nº): ");
            int origen = Integer.parseInt(sc.nextLine().trim());

            // Si es cliente, comprobamos por seguridad que la cuenta sea suya
            if (!esEmpleado && !BancoMetodos.esTitularDeCuenta(c, dni, origen)) {
                System.out.println("[ERROR] Esa cuenta no te pertenece o no existe.");
                return;
            }

            System.out.print("Introduce la cuenta de destino (Nº): ");
            int destino = Integer.parseInt(sc.nextLine().trim());

            if (origen == destino) {
                System.out.println("[ERROR] La cuenta origen y destino deben ser distintas.");
                return;
            }

            System.out.print("Introduce la cantidad a transferir (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0) {
                System.out.println("[ERROR] La cantidad debe ser positiva.");
                return;
            }

            if (BancoMetodos.realizarTransferencia(c, origen, destino, cantidad)) {
                System.out.println("[EXITO] Transferencia realizada correctamente.");
            } else {
                System.out.println("[ERROR] Operación fallida. Revisa el saldo o la existencia de la cuenta destino.");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato incorrecto. Se espera un número entero.");
        }
    }

    private static void procesarIngreso(Scanner sc, Connection c, String dni) {
        try {
            System.out.print("\nIntroduce el ID del cajero (ej. 1): ");
            int idCajero = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Introduce la cuenta de destino (Nº): ");
            int numCuenta = Integer.parseInt(sc.nextLine().trim());

            if (!BancoMetodos.esTitularDeCuenta(c, dni, numCuenta)) {
                System.out.println("[ERROR] Esa cuenta no te pertenece o no existe.");
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
                System.out.println("[ERROR] La cantidad de billetes no puede ser negativa.");
                return;
            }

            if (BancoMetodos.ingresarEnCajero(c, idCajero, numCuenta, b5, b10, b20, b50)) {
                int total = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
                System.out.printf("[EXITO] Ingreso de %d € contabilizado en cuenta y cajero.%n", total);
            } else {
                System.out.println("[ERROR] Fallo al procesar el ingreso (cajero o cuenta no validos).");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }

    private static void procesarRetiro(Scanner sc, Connection c, String dni) {
        try {
            System.out.print("\nIntroduce el ID del cajero en el que te encuentras (ej. 1): ");
            int idCajero = Integer.parseInt(sc.nextLine().trim());

            System.out.print("Introduce el número de tu cuenta: ");
            int numCuenta = Integer.parseInt(sc.nextLine().trim());

            if (!BancoMetodos.esTitularDeCuenta(c, dni, numCuenta)) {
                System.out.println("[ERROR] Cuenta no válida o no autorizada.");
                return;
            }

            System.out.print("Introduce la cantidad a retirar (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0 || cantidad % 5 != 0) {
                System.out.println("[ERROR] La cantidad debe ser mayor que 0 y múltiplo de 5 €.");
                return;
            }

            int[] billetes = BancoMetodos.retirarDeCajero(c, idCajero, numCuenta, cantidad);

            if (billetes != null) {
                System.out.println("[EXITO] Operación completada.");
                System.out.printf(" >> Billetes dispensados: [50€: %d] | [20€: %d] | [10€: %d] | [5€: %d]%n",
                        billetes[0], billetes[1], billetes[2], billetes[3]);
            } else {
                System.out.println("[ERROR] Retiro denegado. Posibles causas: Saldo insuficiente o el cajero no tiene el desglose de billetes necesario.");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }

    private static void procesarRellenarCajero(Scanner sc, Connection c) {
        try {
            System.out.print("\nIntroduce el ID del cajero a rellenar (1 al 4): ");
            int idCajero = Integer.parseInt(sc.nextLine().trim());

            System.out.println("Introduce el stock de billetes a añadir:");
            System.out.print("Billetes de 5 €: ");
            int b5 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 10 €: ");
            int b10 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 20 €: ");
            int b20 = Integer.parseInt(sc.nextLine().trim());
            System.out.print("Billetes de 50 €: ");
            int b50 = Integer.parseInt(sc.nextLine().trim());

            if (b5 < 0 || b10 < 0 || b20 < 0 || b50 < 0) {
                System.out.println("[ERROR] La cantidad no puede ser negativa.");
                return;
            }

            if (BancoMetodos.rellenarCajero(c, idCajero, b5, b10, b20, b50)) {
                int totalAnadido = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
                System.out.printf("[EXITO] Cajero ID %d actualizado (+%d € totales).%n", idCajero, totalAnadido);
            } else {
                System.out.println("[ERROR] El cajero introducido no existe o error de conexión.");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }

    private static void procesarGestionClientes(Scanner sc, Connection c) {
        System.out.println("\n--- GESTIÓN DE CLIENTES ---");
        System.out.println("1. Añadir cliente");
        System.out.println("2. Eliminar cliente");
        System.out.print("Selecciona una opción: ");
        String subOp = sc.nextLine().trim();

        if ("1".equals(subOp)) {
            System.out.print("DNI del nuevo cliente: ");
            String dni = sc.nextLine().trim();

            if (BancoMetodos.existeCliente(c, dni)) {
                System.out.println("[ERROR] Ya existe un cliente registrado con ese DNI.");
                return;
            }

            System.out.print("Nombre completo: ");
            String nombre = sc.nextLine().trim();

            System.out.print("PIN (4 dígitos): ");
            String pin = sc.nextLine().trim();

            if (BancoMetodos.crearClienteConCuenta(c, dni, nombre, pin)) {
                System.out.println("[EXITO] Cliente creado y cuenta vinculada automáticamente.");
            } else {
                System.out.println("[ERROR] Fallo al insertar el registro del cliente.");
            }

        } else if ("2".equals(subOp)) {
            System.out.print("DNI del cliente a eliminar: ");
            String dni = sc.nextLine().trim();

            if (!BancoMetodos.existeCliente(c, dni)) {
                System.out.println("[ERROR] No hay registros con ese DNI en Clientes.");
                return;
            }

            if (BancoMetodos.eliminarCliente(c, dni)) {
                System.out.println("[EXITO] Cliente eliminado. (Sus cuentas desaparecen por cascada).");
            } else {
                System.out.println("[ERROR] Imposible eliminar el cliente.");
            }
        } else {
            System.out.println("[ERROR] Opción no válida.");
        }
    }

    private static void procesarGestionCuentas(Scanner sc, Connection c) {
        System.out.println("\n--- GESTIÓN DE CUENTAS ---");
        System.out.println("1. Añadir cuenta independiente");
        System.out.println("2. Eliminar cuenta");
        System.out.print("Selecciona una opción: ");
        String subOp = sc.nextLine().trim();

        try {
            if ("1".equals(subOp)) {
                System.out.print("DNI del titular: ");
                String dni = sc.nextLine().trim();

                if (!BancoMetodos.existeCliente(c, dni)) {
                    System.out.println("[ERROR] El cliente titular no existe en la base de datos.");
                    return;
                }

                System.out.print("Saldo inicial (€): ");
                int saldo = Integer.parseInt(sc.nextLine().trim());

                if (saldo < 0) {
                    System.out.println("[ERROR] El saldo inicial no puede ser menor a 0.");
                    return;
                }

                if (BancoMetodos.crearCuenta(c, dni, saldo)) {
                    System.out.println("[EXITO] Nueva cuenta operativa.");
                } else {
                    System.out.println("[ERROR] Fallo interno al crear cuenta.");
                }

            } else if ("2".equals(subOp)) {
                System.out.print("Número de cuenta a eliminar: ");
                int numCuenta = Integer.parseInt(sc.nextLine().trim());

                if (!BancoMetodos.existeCuenta(c, numCuenta)) {
                    System.out.println("[ERROR] La cuenta especificada no existe.");
                    return;
                }

                if (BancoMetodos.eliminarCuenta(c, numCuenta)) {
                    System.out.println("[EXITO] Cuenta eliminada del sistema.");
                } else {
                    System.out.println("[ERROR] No se pudo procesar la baja de la cuenta.");
                }
            } else {
                System.out.println("[ERROR] Opción no válida.");
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }
}