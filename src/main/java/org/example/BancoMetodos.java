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

    // Ingresar dinero en una cuenta
    public static boolean ingresarDinero(Connection con, int numCuenta, int cantidad) {
        if (cantidad <= 0) return false;

        String sql = "UPDATE Cuentas SET saldo = saldo + ? WHERE numero = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, numCuenta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al ingresar dinero: " + e.getMessage());
            return false;
        }
    }

    // Retirar dinero de una cuenta (valida saldo suficiente en SQL)
    public static boolean retirarDinero(Connection con, int numCuenta, int cantidad) {
        if (cantidad <= 0) return false;

        String sql = "UPDATE Cuentas SET saldo = saldo - ? WHERE numero = ? AND saldo >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, numCuenta);
            ps.setInt(3, cantidad);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al retirar dinero: " + e.getMessage());
            return false;
        }
    }
}
