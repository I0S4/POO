package com.grupo2.servidor.core;

import com.google.gson.Gson;
import com.grupo2.compartido.models.MensajeDTO;
import com.grupo2.servidor.comandos.Comando;
import com.grupo2.servidor.comandos.ComandoFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class HiloCliente implements Runnable {
    private final Socket socket;

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linea;
            // El bucle lee líneas de comandos limpias separadas por \n
            while ((linea = entrada.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                try {
                    MensajeDTO dto = new Gson().fromJson(linea, MensajeDTO.class);
                    Comando comando = ComandoFactory.obtenerComando(dto.getTipo());
                    
                    if (comando != null) {
                        comando.ejecutar(linea, socket);
                    } else {
                        System.out.println("Comando desconocido: " + dto.getTipo());
                    }
                } catch (Exception jsonEx) {
                    // Si llega a quedar un residuo binario, se limpia aquí sin romper el bucle
                    System.err.println("Descarte de residuo de buffer en red.");
                }
            }
        } catch (Exception e) {
            System.err.println("Conexión de cliente cerrada: " + e.getMessage());
        } finally {
            try { 
                if (!socket.isClosed()) socket.close(); 
            } catch (Exception e) { 
                // Ignorar fallos menores al limpiar el socket físico
            }
        }
    }
}