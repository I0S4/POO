package com.grupo2.servidor.core;

import java.io.*;
import java.net.Socket;
import com.grupo2.servidor.comandos.ComandoFactory;

public class HiloCliente implements Runnable {
    private final Socket socket;
    private BufferedReader entrada;

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String linea;
            // Bucle optimizado de 3 líneas gracias al patrón Command
            while ((linea = entrada.readLine()) != null) {
                final String json = linea;
                ComandoFactory.obtenerComando(json).ejecutar(json, socket);
            }
        } catch (Exception e) {
            System.out.println("Conexión finalizada de forma ordenada.");
        }
    }
}