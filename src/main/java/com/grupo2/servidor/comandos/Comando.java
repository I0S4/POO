package com.grupo2.servidor.comandos;

import java.net.Socket;

public interface Comando {
    void ejecutar(String json, Socket socket);
}