package com.grupo2.servidor.comandos;

import com.google.gson.Gson;
import com.grupo2.compartido.models.ArchivoDTO;
import com.grupo2.servidor.conexion.DatabaseConnection;
import java.io.*;
import java.net.Socket;
import java.sql.PreparedStatement;

public class ComandoSubir implements Comando {
    private static final int BLOCK_SIZE = 4096; // Bloques físicos de 4KB coincidiendo con el cliente

    @Override
    public void ejecutar(String json, Socket socket) {
        try {
            // 1. Extraer metadata del casillero postal del cliente
            ArchivoDTO dto = new Gson().fromJson(json, ArchivoDTO.class);
            
            // 2. Crear la carpeta física en el almacenamiento del servidor para esa sala
            File directorioSala = new File("servidor/descargas/sala_" + dto.getSala());
            if (!directorioSala.exists()) directorioSala.mkdirs();
            
            File archivoDestino = new File(directorioSala, dto.getNombre());
            System.out.println("Recibiendo archivo binario por bloques: " + dto.getNombre() + " (" + dto.getTamano() + " bytes)");

            // 3. Conmutación a streams binarios directos del hardware de red
            try (InputStream is = socket.getInputStream();
                 FileOutputStream fos = new FileOutputStream(archivoDestino)) {
                
                byte[] buffer = new byte[BLOCK_SIZE];
                long bytesRestantes = dto.getTamano();
                int bytesLeidos;

                // El bucle lee ráfagas controladas de 4KB para no saturar memoria
                while (bytesRestantes > 0 && (bytesLeidos = is.read(buffer, 0, (int) Math.min(buffer.length, bytesRestantes))) != -1) {
                    fos.write(buffer, 0, bytesLeidos);
                    bytesRestantes -= bytesLeidos;
                }
                fos.flush();
            }

            // 4. Guardar la metadata de localización física en la tabla ArchivosCompartidos de XAMPP
            String sql = "INSERT INTO ArchivosCompartidos (IdSala, IdUsuario, NombreArchivo, RutaArchivo, TamanioBytes, Extension) VALUES (?, 1, ?, ?, ?, ?)";
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(dto.getSala()));
                ps.setString(2, dto.getNombre());
                ps.setString(3, archivoDestino.getAbsolutePath());
                ps.setLong(4, dto.getTamano());
                ps.setString(5, dto.getExtension());
                ps.executeUpdate();
            }
            
            System.out.println("Archivo almacenado en disco y registrado en phpMyAdmin con éxito.");
            
        } catch (Exception e) {
            System.err.println("Error crítico en la transferencia de hardware: " + e.getMessage());
        }
    }
}