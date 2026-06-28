package com.grupo2.servidor.comandos;

import com.google.gson.Gson;
import com.grupo2.compartido.models.MensajeDTO;
import com.grupo2.servidor.conexion.DatabaseConnection;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ComandoLogin implements Comando {
    @Override
    public void ejecutar(String json, Socket socket) {
        try {
            MensajeDTO dto = new Gson().fromJson(json, MensajeDTO.class);
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

            // Consulta directa a la tabla Usuarios creada en tablas.sql
            String sql = "SELECT * FROM Usuarios WHERE Correo = ? AND PasswordHash = ? AND Activo = 1";
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, dto.getUsuario()); // El correo viaja en el campo usuario
                ps.setString(2, dto.getContenido()); // El hash/password viaja en el contenido
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Login exitoso en base de datos para: " + dto.getUsuario());
                        salida.println(new Gson().toJson(new MensajeDTO("LOGIN_OK", "", dto.getUsuario(), "Aceptado")));
                    } else {
                        System.out.println("Intento de login fallido para: " + dto.getUsuario());
                        salida.println(new Gson().toJson(new MensajeDTO("LOGIN_FAIL", "", "", "Credenciales incorrectas")));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error procesando el ComandoLogin: " + e.getMessage());
        }
    }
}