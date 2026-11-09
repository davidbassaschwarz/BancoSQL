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

            // Obtenemos el nombre del usuario
            String nombre = BancoMetodos.obtenerNombre(c, dniValido);
            System.out.println("\n[EXITO] Login correcto. ¡Hola, " + nombre + "!");

            // --- REDIRECCION DE MENUS SEGUN ROL ---
            if ("E".equalsIgnoreCase(rol)) {
                menuEmpleado(sc, c, nombre, dniValido);
            } else if ("C".equalsIgnoreCase(rol)) {
                menuCliente(sc, c, nombre, dniValido);
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] No se pudo conectar a la base de datos: " + e.getMessage());
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
                case "1" ->
                    mostrarSaldoUsuario(c, dni);
                case "2" ->
                    procesarIngreso(sc, c, dni);
                case "3" ->
                    procesarRetiro(sc, c, dni);
                case "4" ->
                    procesarTransferencia(sc, c, dni, false);
                case "5" ->
                    solicitarCambioPin(sc, c, dni);
                case "0" -> {
                    System.out.println("[INFO] Cerrando sesión de cliente...");
                    salir = true;
                    break;
                }
                default ->
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
            System.out.println("1. Consultar saldo");
            System.out.println("2. Realizar transferencia");
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

        if (!esPinValido(nuevoPin)) {
            System.out.println("[ERROR] El PIN debe tener exactamente 4 dígitos.");
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
            System.out.println("\n[INFO] No tienes cuentas asociadas.");
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
            System.out.print("\nIntroduce la cuenta de destino (Nº): ");
            int destino = Integer.parseInt(sc.nextLine().trim());

            if (!BancoMetodos.existeCuenta(c, destino)) {
                System.out.println("[ERROR] La cuenta de destino no existe.");
                return;
            }

            int origen;

            if (!esEmpleado) {
                // Un cliente solo puede transferir desde una de sus cuentas
                List<String[]> cuentas = BancoMetodos.obtenerCuentasYSaldo(c, dni);

                if (cuentas.isEmpty()) {
                    System.out.println("[ERROR] No tienes cuentas asociadas.");
                    return;
                }

                if (cuentas.size() == 1) {
                    // Solo tiene una cuenta: se usa automaticamente
                    origen = Integer.parseInt(cuentas.get(0)[0]);
                    System.out.println("[INFO] Cuenta de origen: " + origen);
                } else {
                    System.out.print("Introduce tu cuenta de origen (Nº): ");
                    origen = Integer.parseInt(sc.nextLine().trim());

                    if (!BancoMetodos.esTitularDeCuenta(c, dni, origen)) {
                        System.out.println("[ERROR] Esa cuenta no te pertenece.");
                        return;
                    }
                }
            } else {
                System.out.print("Introduce la cuenta de origen (Nº): ");
                origen = Integer.parseInt(sc.nextLine().trim());

                if (!BancoMetodos.existeCuenta(c, origen)) {
                    System.out.println("[ERROR] La cuenta de origen no existe.");
                    return;
                }
            }

            if (origen == destino) {
                System.out.println("[ERROR] La cuenta de origen y destino deben ser distintas.");
                return;
            }

            System.out.print("Introduce la cantidad a transferir (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0) {
                System.out.println("[ERROR] La cantidad debe ser mayor que 0.");
                return;
            }

            if (BancoMetodos.realizarTransferencia(c, origen, destino, cantidad)) {
                System.out.println("[EXITO] Transferencia realizada correctamente.");
            } else {
                System.out.println("[ERROR] Saldo insuficiente en la cuenta de origen.");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato incorrecto. Se espera un número entero.");
        }
    }

    private static void procesarIngreso(Scanner sc, Connection c, String dni) {
        try {
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

            if (b5 + b10 + b20 + b50 == 0) {
                System.out.println("[ERROR] Debes ingresar al menos un billete.");
                return;
            }

            // En un ingreso siempre se pregunta la cuenta, aunque solo tenga una.
            String[] cuenta = seleccionarCuentaCliente(sc, c, dni, true);

            if (cuenta == null) {
                return;
            }

            int numCuenta = Integer.parseInt(cuenta[0]);

            List<Integer> cajeros = BancoMetodos.obtenerIdsCajeros(c);

            if (cajeros.isEmpty()) {
                System.out.println("[ERROR] No hay cajeros disponibles.");
                return;
            }

            int idCajero = cajeros.get(0);

            if (BancoMetodos.ingresarEnCajero(c, idCajero, numCuenta, b5, b10, b20, b50)) {
                int total = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
                System.out.printf("[EXITO] Ingreso de %d € contabilizado en cuenta y cajero.%n", total);
            } else {
                System.out.println("[ERROR] No se pudo realizar el ingreso.");
            }

        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }

    private static void procesarRetiro(Scanner sc, Connection c, String dni) {
        try {
            System.out.print("\nIntroduce la cantidad a retirar (€): ");
            int cantidad = Integer.parseInt(sc.nextLine().trim());

            if (cantidad <= 0 || cantidad % 5 != 0) {
                System.out.println("[ERROR] La cantidad debe ser mayor que 0 y múltiplo de 5 €.");
                return;
            }

            String[] cuenta = seleccionarCuentaCliente(sc, c, dni, false);

            if (cuenta == null) {
                return;
            }

            int numCuenta = Integer.parseInt(cuenta[0]);
            int saldo = Integer.parseInt(cuenta[1]);

            if (saldo < cantidad) {
                System.out.println("[ERROR] Saldo insuficiente.");
                return;
            }

            // Probamos los cajeros disponibles hasta encontrar uno con billetes suficientes
            List<Integer> cajeros = BancoMetodos.obtenerIdsCajeros(c);

            for (int idCajero : cajeros) {
                int[] billetes = BancoMetodos.retirarDeCajero(c, idCajero, numCuenta, cantidad);

                if (billetes != null) {
                    System.out.println("[EXITO] Operación completada.");
                    System.out.printf(" >> Billetes dispensados: [50€: %d] | [20€: %d] | [10€: %d] | [5€: %d]%n",
                            billetes[0], billetes[1], billetes[2], billetes[3]);
                    return;
                }
            }

            System.out.println("[ERROR] Ningún cajero tiene billetes suficientes para esa cantidad.");

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

            if (b5 + b10 + b20 + b50 == 0) {
                System.out.println("[ERROR] Debes añadir al menos un billete.");
                return;
            }

            if (BancoMetodos.rellenarCajero(c, idCajero, b5, b10, b20, b50)) {
                int totalAnadido = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
                System.out.printf("[EXITO] Cajero ID %d actualizado (+%d € totales).%n", idCajero, totalAnadido);
            } else {
                System.out.println("[ERROR] El cajero no existe.");
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

            if (dni.isEmpty()) {
                System.out.println("[ERROR] El DNI no puede estar vacío.");
                return;
            }

            if (!esDniValido(dni)) {
                System.out.println("[ERROR] El DNI debe tener 8 números y una letra.");
                return;
            }

            if (BancoMetodos.existeCliente(c, dni)) {
                System.out.println("[ERROR] Ya existe un cliente registrado con ese DNI.");
                return;
            }

            System.out.print("Nombre completo: ");
            String nombre = sc.nextLine().trim();

            if (nombre.isEmpty()) {
                System.out.println("[ERROR] El nombre no puede estar vacío.");
                return;
            }

            System.out.print("PIN (4 dígitos): ");
            String pin = sc.nextLine().trim();

            if (!esPinValido(pin)) {
                System.out.println("[ERROR] El PIN debe tener exactamente 4 dígitos.");
                return;
            }

            if (BancoMetodos.crearClienteConCuenta(c, dni, nombre, pin)) {
                System.out.println("[EXITO] Cliente creado y cuenta vinculada automáticamente.");
            } else {
                System.out.println("[ERROR] No se pudo crear el cliente.");
            }

        } else if ("2".equals(subOp)) {
            System.out.print("DNI del cliente a eliminar: ");
            String dni = sc.nextLine().trim();

            if (!BancoMetodos.existeCliente(c, dni)) {
                System.out.println("[ERROR] No existe ningún cliente con ese DNI.");
                return;
            }

            if (BancoMetodos.eliminarCliente(c, dni)) {
                System.out.println("[EXITO] Cliente eliminado. (Sus cuentas desaparecen por cascada).");
            } else {
                System.out.println("[ERROR] No se pudo eliminar el cliente.");
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
                    System.out.println("[ERROR] El cliente titular no existe.");
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
                    System.out.println("[ERROR] No se pudo crear la cuenta.");
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
                    System.out.println("[ERROR] No se pudo eliminar la cuenta.");
                }
            } else {
                System.out.println("[ERROR] Opción no válida.");
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Formato numérico incorrecto.");
        }
    }

    /* =========================================================
     *               MÉTODOS AUXILIARES
     * ========================================================= */

    /**
     * Devuelve la cuenta seleccionada del cliente como {numero, saldo}.
     * Si preguntarSiempre es false y el cliente tiene una sola cuenta,
     * se usa automaticamente; si no, se le pide cual quiere.
     * Devuelve null si no se puede seleccionar.
     */
    private static String[] seleccionarCuentaCliente(Scanner sc, Connection c, String dni, boolean preguntarSiempre) {
        List<String[]> cuentas = BancoMetodos.obtenerCuentasYSaldo(c, dni);

        if (cuentas.isEmpty()) {
            System.out.println("[ERROR] No tienes cuentas asociadas.");
            return null;
        }

        if (!preguntarSiempre && cuentas.size() == 1) {
            String[] cuenta = cuentas.get(0);
            System.out.println("[INFO] Cuenta seleccionada: " + cuenta[0]);
            return cuenta;
        }

        System.out.print("Introduce el número de tu cuenta: ");
        int numCuenta = Integer.parseInt(sc.nextLine().trim());

        for (String[] cuenta : cuentas) {
            if (Integer.parseInt(cuenta[0]) == numCuenta) {
                return cuenta;
            }
        }

        System.out.println("[ERROR] Esa cuenta no te pertenece.");
        return null;
    }

    /**
     * Comprueba que el PIN tenga exactamente 4 dígitos.
     */
    private static boolean esPinValido(String pin) {
        if (pin == null || pin.length() != 4) {
            return false;
        }

        for (int i = 0; i < pin.length(); i++) {
            if (!Character.isDigit(pin.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Comprueba el formato básico del DNI: 8 números y una letra.
     */
    private static boolean esDniValido(String dni) {
        if (dni == null || dni.length() != 9) {
            return false;
        }

        for (int i = 0; i < 8; i++) {
            if (!Character.isDigit(dni.charAt(i))) {
                return false;
            }
        }

        return Character.isLetter(dni.charAt(8));
    }
}
