package com.grupo2.servidor.proxy;

public interface ChatService {
    void guardarMensajeEnBD(String sala, String usuario, String contenido);
}