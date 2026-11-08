package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BancoMetodos {

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
            System.err.println("Error al consultar credenciales: " + e.getMessage());
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
            System.err.println("Error al obtener el nombre: " + e.getMessage());
        }

        return "Usuario";
    }

    public static boolean cambiarPin(Connection con, String dni, String nuevoPin) {
        String sqlCliente = "UPDATE Clientes SET pin = ? WHERE dni = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sqlCliente)) {
            pstmt.setString(1, nuevoPin);
            pstmt.setString(2, dni);

            int filasAfectadas = pstmt.executeUpdate();

            // Si no era cliente, intentamos en la tabla Empleados
            if (filasAfectadas == 0) {
                String sqlEmpleado = "UPDATE Empleados SET pin = ? WHERE dni = ?";
                try (PreparedStatement pstmtEmp = con.prepareStatement(sqlEmpleado)) {
                    pstmtEmp.setString(1, nuevoPin);
                    pstmtEmp.setString(2, dni);
                    filasAfectadas = pstmtEmp.executeUpdate();
                }
            }

            // Si cambió al menos 1 fila, la operación fue un éxito
            return filasAfectadas > 0;

        } catch (SQLException e) {
            // Podrías relanzar la excepción o devolver false
            return false;
        }
    }

    public static List<String[]> obtenerCuentasYSaldo(Connection con, String dni) {
        List<String[]> listaCuentas = new ArrayList<>();

        // Consulta para obtener las cuentas y saldos del cliente mediante la tabla intermedia
        String sql = "SELECT numero, saldo FROM Cuentas WHERE dni_titular = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, dni);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String numCuenta = rs.getString("numero");
                    String saldo = String.valueOf(rs.getInt("saldo"));

                    // Guardamos cada cuenta como un par [num_cuenta, saldo]
                    listaCuentas.add(new String[]{numCuenta, saldo});
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar el saldo: " + e.getMessage());
        }

        return listaCuentas;
    }

    public static boolean realizarTransferencia(Connection con, int cuentaOrigen, int cuentaDestino, int cantidad) {
        if (cantidad <= 0) return false;

        String sqlRestar = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        String sqlSumar = "UPDATE Cuentas SET saldo = saldo + ? WHERE numero = ?";

        try {
            con.setAutoCommit(false);

            // 1. Restar de la cuenta origen
            try (PreparedStatement psRestar = con.prepareStatement(sqlRestar)) {
                psRestar.setInt(1, cantidad);
                psRestar.setInt(2, cuentaOrigen);
                psRestar.setInt(3, cantidad);

                if (psRestar.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            // 2. Sumar a la cuenta destino
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
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("Error en rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar autoCommit: " + e.getMessage());
            }
        }
    }

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

    // Ingresar dinero especificando número de billetes
    public static boolean ingresarEnCajero(Connection con, int idCajero, int numCuenta, int b5, int b10, int b20, int b50) {
        int totalIngreso = (b5 * 5) + (b10 * 10) + (b20 * 20) + (b50 * 50);
        if (totalIngreso <= 0) return false;

        String sqlCuenta = "UPDATE Cuentas SET saldo = saldo + ? WHERE numero = ?";
        String sqlCajero = "UPDATE Cajeros SET billetes5 = billetes5 + ?, billetes10 = billetes10 + ?, billetes20 = billetes20 + ?, billetes50 = billetes50 + ? WHERE id = ?";

        try {
            con.setAutoCommit(false);

            // 1. Aumentar saldo de la cuenta
            try (PreparedStatement psC = con.prepareStatement(sqlCuenta)) {
                psC.setInt(1, totalIngreso);
                psC.setInt(2, numCuenta);
                if (psC.executeUpdate() == 0) {
                    con.rollback();
                    return false;
                }
            }

            // 2. Incrementar stock de billetes en el cajero
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
            try { con.rollback(); } catch (SQLException ex) {}
            return false;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) {}
        }
    }

    // Retirar dinero entregando el menor número de billetes posible
    // Devuelve un array int[]{b50, b20, b10, b5} si tiene éxito, o null si falla
    public static int[] retirarDeCajero(Connection con, int idCajero, int numCuenta, int cantidad) {
        if (cantidad <= 0 || cantidad % 5 != 0) return null;

        String sqlCajeroSelect = "SELECT billetes5, billetes10, billetes20, billetes50 FROM Cajeros WHERE id = ? FOR UPDATE";
        String sqlCuentaRestar = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        String sqlCajeroRestar = "UPDATE Cajeros SET billetes5 = billetes5 - ?, billetes10 = billetes10 - ?, billetes20 = billetes20 - ?, billetes50 = billetes50 - ? WHERE id = ?";

        try {
            con.setAutoCommit(false);

            int b5 = 0, b10 = 0, b20 = 0, b50 = 0;
            int restante = cantidad;

            // 1. Consultar el stock actual de billetes en el cajero
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

                    // Algoritmo voraz (greedy) para minimizar billetes entregados
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

                    // Si no se pudo completar el desglose exacto con los billetes disponibles
                    if (restante > 0) {
                        con.rollback();
                        return null;
                    }
                }
            }

            // 2. Restar saldo de la cuenta
            try (PreparedStatement psC = con.prepareStatement(sqlCuentaRestar)) {
                psC.setInt(1, cantidad);
                psC.setInt(2, numCuenta);
                psC.setInt(3, cantidad);
                if (psC.executeUpdate() == 0) {
                    con.rollback();
                    return null;
                }
            }

            // 3. Restar stock del cajero
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
            try { con.rollback(); } catch (SQLException ex) {}
            return null;
        } finally {
            try { con.setAutoCommit(true); } catch (SQLException e) {}
        }
    }
}
