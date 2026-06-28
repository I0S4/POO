package com.grupo2.cliente.fachada;

import com.google.gson.Gson;
import com.grupo2.cliente.conexion.ConexionManager;
import com.grupo2.compartido.models.MensajeDTO;
import java.io.*;
import java.net.Socket;

public class FachadaTransferencia {
    private static final int BLOCK_SIZE = 4096;

    public static void subirArchivo(File archivo, String sala, String usuario) throws Exception {
        String ipServidor = ConexionManager.getInstancia().getSocket().getInetAddress().getHostAddress();
        
        try (Socket socketDatos = new Socket(ipServidor, 8090);
             PrintWriter salidaTexto = new PrintWriter(socketDatos.getOutputStream(), true);
             FileInputStream fis = new FileInputStream(archivo);
             OutputStream osBinario = socketDatos.getOutputStream()) {

            // REEMPLAZO: Usamos MensajeDTO pasándole el tamaño como texto en el contenido
            MensajeDTO dto = new MensajeDTO("SUBIR", sala, archivo.getName(), String.valueOf(archivo.length()));
            salidaTexto.println(new Gson().toJson(dto));

            byte[] buffer = new byte[BLOCK_SIZE];
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                osBinario.write(buffer, 0, bytesLeidos);
            }
            osBinario.flush();
        }
    }

    public static void descargarArchivo(String idArchivo, String nombreOriginal, String sala) throws Exception {
        String ipServidor = ConexionManager.getInstancia().getSocket().getInetAddress().getHostAddress();
        
        try (Socket socketDatos = new Socket(ipServidor, 8090);
             PrintWriter salidaTexto = new PrintWriter(socketDatos.getOutputStream(), true);
             InputStream isBinario = socketDatos.getInputStream()) {
             
            // REEMPLAZO: Petición de descarga mapeada en MensajeDTO
            MensajeDTO dto = new MensajeDTO("DESCARGAR_ARCHIVO", sala, idArchivo, nombreOriginal);
            salidaTexto.println(new Gson().toJson(dto));
            
            File carpeta = new File("cliente/descargas/sala_" + sala);
            if (!carpeta.exists()) carpeta.mkdirs();
            
            try (FileOutputStream fos = new FileOutputStream(new File(carpeta, nombreOriginal))) {
                byte[] buffer = new byte[BLOCK_SIZE];
                int bytesLeidos;
                while ((bytesLeidos = isBinario.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesLeidos);
                }
                fos.flush();
            }
        }
    }
}
