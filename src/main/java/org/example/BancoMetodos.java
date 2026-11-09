package org.example;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BancoMetodos {

    /* ==========================================
     *        CONSULTAS GENERALES / LOGIN
     * ========================================== */

    public static String autenticarUsuario(Connection c, String dni, String pin) {
        String sql = "SELECT rol FROM Clientes WHERE dni = ? AND pin = ? " +
                "UNION " +
                "SELECT rol FROM Empleados WHERE dni = ? AND pin = ?";

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, pin);
            ps.setString(3, dni);
            ps.setString(4, pin);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rol");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error SQL en auth: " + e.getMessage());
        }
        return null;
    }

    public static String obtenerNombre(Connection c, String dni) {
        // Llamamos a la funcion almacenada obtener_nombre_usuario(dni)
        String sql = "{ ? = call obtener_nombre_usuario(?) }";

        try (CallableStatement cs = c.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.VARCHAR);
            cs.setString(2, dni);
            cs.execute();

            String nombre = cs.getString(1);
            return nombre != null ? nombre : "Usuario";
        } catch (SQLException e) {
            System.err.println("Error al obtener nombre: " + e.getMessage());
        }
        return "Usuario";
    }

    public static boolean cambiarPin(Connection con, String dni, String nuevoPin) {
        String sqlCliente = "UPDATE Clientes SET pin = ? WHERE dni = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sqlCliente)) {
            pstmt.setString(1, nuevoPin);
            pstmt.setString(2, dni);
            int filasAfectadas = pstmt.executeUpdate();

            // Si el update da 0 filas, el usuario es empleado
            if (filasAfectadas == 0) {
                String sqlEmpleado = "UPDATE Empleados SET pin = ? WHERE dni = ?";
                try (PreparedStatement pstmtEmp = con.prepareStatement(sqlEmpleado)) {
                    pstmtEmp.setString(1, nuevoPin);
                    pstmtEmp.setString(2, dni);
                    filasAfectadas = pstmtEmp.executeUpdate();
                }
            }
            return filasAfectadas > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<String[]> obtenerCuentasYSaldo(Connection con, String dni) {
        List<String[]> listaCuentas = new ArrayList<>();
        String sql = "SELECT numero, saldo FROM Cuentas WHERE dni_titular = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, dni);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String numCuenta = rs.getString("numero");
                    String saldo = String.valueOf(rs.getInt("saldo"));
                    listaCuentas.add(new String[]{numCuenta, saldo});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error SQL en saldos: " + e.getMessage());
        }
        return listaCuentas;
    }

    public static List<Integer> obtenerIdsCajeros(Connection con) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM Cajeros ORDER BY id";

        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            System.err.println("Error SQL al obtener cajeros: " + e.getMessage());
        }
        return ids;
    }

    /* ==========================================
     *    OPERACIONES BANCARIAS (TRANSACCIONES)
     * ========================================== */

    public static boolean esTitularDeCuenta(Connection con, String dni, int numCuenta) {
        String sql = "SELECT COUNT(*) FROM Cuentas WHERE numero = ? AND dni_titular = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, numCuenta);
            ps.setString(2, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean realizarTransferencia(Connection con, int cuentaOrigen, int cuentaDestino, int cantidad) {
        if (cantidad <= 0) return false;

        String sqlRestar = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        String sqlSumar = "UPDATE Cuentas SET saldo = saldo + ? WHERE numero = ?";

        try {
            con.setAutoCommit(false); // Inicio Transaccion

            // 1. Restar cuenta origen
            try (PreparedStatement psRestar = con.prepareStatement(sqlRestar)) {
                psRestar.setInt(1, cantidad);
                psRestar.setInt(2, cuentaOrigen);
                psRestar.setInt(3, cantidad);
                if (psRestar.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            // 2. Sumar cuenta destino
            try (PreparedStatement psSumar = con.prepareStatement(sqlSumar)) {
                psSumar.setInt(1, cantidad);
                psSumar.setInt(2, cuentaDestino);
                if (psSumar.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static boolean ingresarEnCajero(Connection con, int idCajero, int numCuenta, int b5, int b10, int b20, int b50) {
        int totalIngreso = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
        if (totalIngreso <= 0) return false;

        String sqlCuenta = "UPDATE Cuentas SET saldo = saldo + ? WHERE numero = ?";
        String sqlCajero = "UPDATE Cajeros SET billetes5 = billetes5 + ?, billetes10 = billetes10 + ?, billetes20 = billetes20 + ?, billetes50 = billetes50 + ? WHERE id = ?";

        try {
            con.setAutoCommit(false);

            try (PreparedStatement psC = con.prepareStatement(sqlCuenta)) {
                psC.setInt(1, totalIngreso);
                psC.setInt(2, numCuenta);
                if (psC.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            try (PreparedStatement psK = con.prepareStatement(sqlCajero)) {
                psK.setInt(1, b5);
                psK.setInt(2, b10);
                psK.setInt(3, b20);
                psK.setInt(4, b50);
                psK.setInt(5, idCajero);
                if (psK.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static int[] retirarDeCajero(Connection con, int idCajero, int numCuenta, int cantidad) {
        if (cantidad <= 0 || cantidad % 5 != 0) return null;

        String sqlCajeroSelect = "SELECT billetes5, billetes10, billetes20, billetes50 FROM Cajeros WHERE id = ?";
        String sqlCuentaRestar = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        String sqlCajeroRestar = "UPDATE Cajeros SET billetes5 = billetes5 - ?, billetes10 = billetes10 - ?, billetes20 = billetes20 - ?, billetes50 = billetes50 - ? WHERE id = ?";

        try {
            con.setAutoCommit(false);

            int b5 = 0, b10 = 0, b20 = 0, b50 = 0;

            // 1. Leemos el stock de billetes del cajero
            try (PreparedStatement psSel = con.prepareStatement(sqlCajeroSelect)) {
                psSel.setInt(1, idCajero);
                try (ResultSet rs = psSel.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return null;
                    }
                    int stock5 = rs.getInt("billetes5");
                    int stock10 = rs.getInt("billetes10");
                    int stock20 = rs.getInt("billetes20");
                    int stock50 = rs.getInt("billetes50");

                    // Buscamos la combinacion con el menor numero de billetes.
                    // Probamos cada cantidad posible de billetes de 50 y completamos
                    // el resto con billetes de 20, despues de 10 y por ultimo de 5.
                    int menorNumeroBilletes = Integer.MAX_VALUE;

                    for (int posible50 = 0; posible50 <= Math.min(stock50, cantidad / 50); posible50++) {
                        int resto = cantidad - posible50 * 50;

                        int posible20 = Math.min(stock20, resto / 20);
                        resto -= posible20 * 20;

                        int posible10 = Math.min(stock10, resto / 10);
                        resto -= posible10 * 10;

                        int posible5 = resto / 5;

                        // No hay bastantes billetes de 5 para completar la cantidad
                        if (posible5 > stock5) {
                            continue;
                        }

                        int numeroBilletes = posible50 + posible20 + posible10 + posible5;

                        if (numeroBilletes < menorNumeroBilletes) {
                            menorNumeroBilletes = numeroBilletes;
                            b50 = posible50;
                            b20 = posible20;
                            b10 = posible10;
                            b5 = posible5;
                        }
                    }

                    // Si no se ha encontrado ninguna combinacion, no se puede retirar
                    if (menorNumeroBilletes == Integer.MAX_VALUE) {
                        con.rollback();
                        return null;
                    }
                }
            }

            // 2. Restar saldo cuenta (Validando que la cuenta tenga el dinero)
            try (PreparedStatement psC = con.prepareStatement(sqlCuentaRestar)) {
                psC.setInt(1, cantidad);
                psC.setInt(2, numCuenta);
                psC.setInt(3, cantidad);
                if (psC.executeUpdate() == 0) {
                    con.rollback();
                    return null;
                }
            }

            // 3. Quitar el stock del cajero
            try (PreparedStatement psK = con.prepareStatement(sqlCajeroRestar)) {
                psK.setInt(1, b5);
                psK.setInt(2, b10);
                psK.setInt(3, b20);
                psK.setInt(4, b50);
                psK.setInt(5, idCajero);
                if (psK.executeUpdate() == 0) {
                    con.rollback();
                    return null;
                }
            }

            con.commit();
            return new int[]{b50, b20, b10, b5};

        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            return null;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static boolean rellenarCajero(Connection con, int idCajero, int b5, int b10, int b20, int b50) {
        if (b5 < 0 || b10 < 0 || b20 < 0 || b50 < 0) return false;

        String sql = "UPDATE Cajeros SET billetes5 = billetes5 + ?, billetes10 = billetes10 + ?, " +
                "billetes20 = billetes20 + ?, billetes50 = billetes50 + ? WHERE id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, b5);
            ps.setInt(2, b10);
            ps.setInt(3, b20);
            ps.setInt(4, b50);
            ps.setInt(5, idCajero);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    /* ==========================================
     *           GESTIÓN CLIENTES Y CUENTAS
     * ========================================== */

    public static boolean existeCliente(Connection con, String dni) {
        String sql = "SELECT COUNT(*) FROM Clientes WHERE dni = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean crearClienteConCuenta(Connection con, String dni, String nombre, String pin) {
        String sqlCliente = "INSERT INTO Clientes (dni, nombre, pin, rol) VALUES (?, ?, ?, 'C')";
        String sqlCuenta = "INSERT INTO Cuentas (dni_titular, saldo) VALUES (?, 0)";

        try {
            con.setAutoCommit(false); // Al crear un cliente se genera su cuenta automaticamente

            try (PreparedStatement ps1 = con.prepareStatement(sqlCliente)) {
                ps1.setString(1, dni);
                ps1.setString(2, nombre);
                ps1.setString(3, pin);
                if (ps1.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps2 = con.prepareStatement(sqlCuenta)) {
                ps2.setString(1, dni);
                if (ps2.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            try { con.rollback(); } catch (SQLException ignored) {}
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static boolean eliminarCliente(Connection con, String dni) {
        // La BD tiene configurado ON DELETE CASCADE, así que las cuentas se borran solas
        String sql = "DELETE FROM Clientes WHERE dni = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean existeCuenta(Connection con, int numeroCuenta) {
        String sql = "SELECT COUNT(*) FROM Cuentas WHERE numero = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, numeroCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean crearCuenta(Connection con, String dniTitular, int saldoInicial) {
        String sql = "INSERT INTO Cuentas (dni_titular, saldo) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dniTitular);
            ps.setInt(2, saldoInicial);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean eliminarCuenta(Connection con, int numeroCuenta) {
        String sql = "DELETE FROM Cuentas WHERE numero = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, numeroCuenta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}