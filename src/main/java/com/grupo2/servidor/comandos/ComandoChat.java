package com.grupo2.servidor.comandos;

import com.google.gson.Gson;
import com.grupo2.compartido.models.MensajeDTO;
import com.grupo2.servidor.proxy.ProxyChatService;
import java.net.Socket;

public class ComandoChat implements Comando {
    private final ProxyChatService proxyChat = new ProxyChatService();

    @Override
    public void ejecutar(String json, Socket socket) {
        try {
            MensajeDTO dto = new Gson().fromJson(json, MensajeDTO.class);
            
            // 1. Pasar por el escudo de seguridad Proxy antes de tocar XAMPP
            proxyChat.guardarMensajeEnBD(dto.getSala(), dto.getUsuario(), dto.getContenido());
            
            // 2. Retransmisión (Broadcast) del mensaje a la sala mediante el patrón Observer
            System.out.println("Mensaje de chat distribuido en la sala " + dto.getSala() + " por " + dto.getUsuario());
            
            // Nota: Aquí se invocaría el Gestor de Salas en RAM para replicar el JSON 
            // a todos los sockets conectados que pertenezcan al idSala.
        } catch (Exception e) {
            System.err.println("Error procesando el ComandoChat: " + e.getMessage());
        }
    }
}