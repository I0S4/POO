package com.grupo2.servidor.comandos;

import com.google.gson.Gson;
import com.grupo2.compartido.models.ArchivoDTO;
import com.grupo2.servidor.conexion.DatabaseConnection;
import java.io.*;
import java.net.Socket;
import java.sql.PreparedStatement;

public class ComandoSubir implements Comando {
    private static final int BLOCK_SIZE = 4096;

    @Override
    public void ejecutar(String json, Socket socket) {
        try {
            ArchivoDTO dto = new Gson().fromJson(json, ArchivoDTO.class);
            
            File directorioSala = new File("servidor/descargas/sala_" + dto.getSala());
            if (!directorioSala.exists()) directorioSala.mkdirs();
            
            File archivoDestino = new File(directorioSala, dto.getNombre());
            System.out.println("Recibiendo archivo binario por bloques: " + dto.getNombre());

            // CORRECCIÓN: No cerramos el socket.getInputStream() en el try, solo el FileOutputStream
            InputStream is = socket.getInputStream(); 
            try (FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                
                byte[] buffer = new byte[BLOCK_SIZE];
                long bytesRestantes = dto.getTamano();
                int bytesLeidos;

                while (bytesRestantes > 0 && (bytesLeidos = is.read(buffer, 0, (int) Math.min(buffer.length, bytesRestantes))) != -1) {
                    fos.write(buffer, 0, bytesLeidos);
                    bytesRestantes -= bytesLeidos;
                }
                fos.flush();
            } // Aquí SOLO se cierra fos, manteniendo la conexión de red activa.

            // Guardar metadata en la base de datos
            String sql = "INSERT INTO ArchivosCompartidos (IdSala, IdUsuario, NombreArchivo, RutaArchivo, TamanioBytes, Extension) VALUES (?, 1, ?, ?, ?, ?)";
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(dto.getSala()));
                ps.setString(2, dto.getNombre());
                ps.setString(3, archivoDestino.getAbsolutePath());
                ps.setLong(4, dto.getTamano());
                ps.setString(5, dto.getExtension());
                ps.executeUpdate();
            }
            
            System.out.println("Archivo guardado con éxito. El socket permanece abierto.");
            
        } catch (Exception e) {
            System.err.println("Error en la transferencia: " + e.getMessage());
        }
    }
}