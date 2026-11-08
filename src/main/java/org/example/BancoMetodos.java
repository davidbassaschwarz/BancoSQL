package org.example;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        String sql = "SELECT nombre FROM Clientes WHERE dni = ? " +
                "UNION " +
                "SELECT nombre FROM Empleados WHERE dni = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dni);
            ps.setString(2, dni);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
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

        String sqlCajeroSelect = "SELECT billetes5, billetes10, billetes20, billetes50 FROM Cajeros WHERE id = ? FOR UPDATE";
        String sqlCuentaRestar = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        String sqlCajeroRestar = "UPDATE Cajeros SET billetes5 = billetes5 - ?, billetes10 = billetes10 - ?, billetes20 = billetes20 - ?, billetes50 = billetes50 - ? WHERE id = ?";

        try {
            con.setAutoCommit(false);

            int b5, b10, b20, b50;
            int restante = cantidad;

            // 1. Bloqueamos filas y miramos el stock de billetes (FOR UPDATE previene condiciones de carrera)
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

                    // Algoritmo voraz: coger siempre el billete mas grande disponible
                    int usar50 = Math.min(restante / 50, stock50);
                    restante -= usar50 * 50;
                    b50 = usar50;

                    int usar20 = Math.min(restante / 20, stock20);
                    restante -= usar20 * 20;
                    b20 = usar20;

                    int usar10 = Math.min(restante / 10, stock10);
                    restante -= usar10 * 10;
                    b10 = usar10;

                    int usar5 = Math.min(restante / 5, stock5);
                    restante -= usar5 * 5;
                    b5 = usar5;

                    // Si queda restante, significa que no habia billetes de 5 o 10 para llegar a la cifra
                    if (restante > 0) {
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
            con.setAutoCommit(false); // El PDF pide que al crear un cliente se genere la cuenta autom.

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