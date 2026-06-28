package com.grupo2.servidor.conexion;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static Connection conexion;

    public static synchronized Connection getConnection() throws Exception {
        if (conexion == null || conexion.isClosed()) {
            String url = "jdbc:mysql://localhost:3306/prototipo_zoom";
            conexion = DriverManager.getConnection(url, "root", ""); // Acceso a XAMPP local
        }
        return conexion;
    }
}