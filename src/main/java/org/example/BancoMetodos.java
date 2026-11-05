package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
}
