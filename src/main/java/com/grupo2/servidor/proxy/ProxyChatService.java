package com.grupo2.servidor.proxy;

public class ProxyChatService implements ChatService {
    private final ChatServiceImpl servicioReal = new ChatServiceImpl();

    @Override
    public void guardarMensajeEnBD(String sala, String usuario, String contenido) {
        // Verificación de seguridad perimetral en memoria RAM
        if (contenido == null || contenido.trim().isEmpty()) {
            System.out.println("Proxy bloqueó un mensaje vacío sospechoso.");
            return;
        }
        // Si pasa los filtros de seguridad, invoca el guardado físico real en XAMPP
        servicioReal.guardarMensajeEnBD(sala, usuario, contenido);
    }
}