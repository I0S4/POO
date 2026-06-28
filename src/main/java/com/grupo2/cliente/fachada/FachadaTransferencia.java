package com.grupo2.cliente.fachada;

import com.google.gson.Gson;
import com.grupo2.cliente.conexion.ConexionManager;
import com.grupo2.compartido.models.ArchivoDTO;

import java.io.*;

public class FachadaTransferencia {
    private static final int BLOCK_SIZE = 4096; // Bloques físicos de 4KB

    public static void subirArchivo(File archivo, String idSala, String usuario) throws Exception {
        // 1. Enviar metadata via JSON de Control
        ArchivoDTO dto = new ArchivoDTO();
        dto.setSala(idSala);
        dto.setNombre(archivo.getName());
        dto.setTamano(archivo.length());
        ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));

        // 2. Conmutación de Hardware: Flujo Binario Crudo
        try (FileInputStream fis = new FileInputStream(archivo);
             OutputStream os = ConexionManager.getInstancia().getSocket().getOutputStream()) {
            
            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesLeidos);
            }
            os.flush();
        }
    }
}