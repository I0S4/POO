package com.grupo2.cliente.conexion;

import java.io.*;
import java.net.Socket;

public class ConexionManager {
    private static ConexionManager instancia;
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;

    private ConexionManager() {}

    public static synchronized ConexionManager getInstancia() {
        if (instancia == null) instancia = new ConexionManager();
        return instancia;
    }

    public void conectar(String ip, int puerto) throws IOException {
        if (socket == null || socket.isClosed()) {
            this.socket = new Socket(ip, puerto);
            this.salida = new PrintWriter(socket.getOutputStream(), true);
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
    }

    public void enviarTexto(String json) {
        if (salida != null) salida.println(json);
    }

    public Socket getSocket() { return socket; }
    public BufferedReader getEntrada() { return entrada; }
}