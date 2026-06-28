package com.grupo2.cliente.estado;

public class EstadoLobby implements EstadoCliente {
    @Override
    public void procesarMensaje(String json) {
        // En el lobby solo procesa si fue aceptado o rechazado de la sala
    }
}