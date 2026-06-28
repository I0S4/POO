package com.grupo2.servidor.comandos;

import java.net.Socket;

public class ComandoExpulsar implements Comando {
    @Override
    public void ejecutar(String json, Socket socket) {
        // Lógica exclusiva de moderación del Host:
        // 1. Desconecta el socket del alumno rebelde
        // 2. Hace broadcast de actualización al panel de participantes
    }
}