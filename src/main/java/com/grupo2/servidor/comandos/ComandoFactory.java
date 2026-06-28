package com.grupo2.servidor.comandos;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ComandoFactory {
    public static Comando obtenerComando(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String tipo = obj.get("tipo").getAsString();

        return switch (tipo) {
            case "LOGIN" -> new ComandoLogin();
            case "CHAT" -> new ComandoChat();
            case "SUBIR" -> new ComandoSubir();
            case "KICK" -> new ComandoExpulsar();
            default -> (j, s) -> System.out.println("Comando desconocido.");
        };
    }
}