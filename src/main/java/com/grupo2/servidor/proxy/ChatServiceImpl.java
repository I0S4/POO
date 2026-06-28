package com.grupo2.servidor.proxy;

import com.grupo2.servidor.conexion.DatabaseConnection;
import java.sql.PreparedStatement;

public class ChatServiceImpl implements ChatService {
    @Override
    public void guardarMensajeEnBD(String sala, String usuario, String contenido) {
        String sql = "INSERT INTO Mensajes (IdSala, IdUsuario, Contenido) VALUES (?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(sala));
            ps.setInt(2, Integer.parseInt(usuario));
            ps.setString(3, contenido);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}