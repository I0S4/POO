package com.grupo2.servidor.core;

import java.net.ServerSocket;
import java.net.Socket;

public class MainServidor {
    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(8090)) {
            System.out.println("Servidor centralizado corriendo en puerto 8090...");
            while (true) {
                Socket cliente = server.accept();
                new Thread(new HiloCliente(cliente)).start(); // Multi-hilo concurrente
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}