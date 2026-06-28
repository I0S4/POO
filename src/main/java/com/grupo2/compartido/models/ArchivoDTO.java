package com.grupo2.compartido.models;
import lombok.Data;

@Data
public class ArchivoDTO {
    private String sala;
    private String nombre;
    private long tamano;
    private String extension;
}