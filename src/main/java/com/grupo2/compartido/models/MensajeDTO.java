package com.grupo2.compartido.models;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class MensajeDTO {
    private String tipo; // LOGIN, CHAT, KICK, etc.
    private String sala;
    private String usuario;
    private String contenido;
}