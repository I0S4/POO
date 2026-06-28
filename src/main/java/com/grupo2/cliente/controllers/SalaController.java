package com.grupo2.cliente.controllers;

import com.google.gson.Gson;
import com.grupo2.cliente.conexion.ConexionManager;
import com.grupo2.cliente.fachada.FachadaTransferencia;
import com.grupo2.cliente.views.SalaView;
import com.grupo2.compartido.models.MensajeDTO;
import javax.swing.*;
import java.io.File;

public class SalaController {
    private final SalaView view;
    private final String idSala;
    private final String usuario;

    public SalaController(SalaView view, String idSala, String usuario) {
        this.view = view;
        this.idSala = idSala;
        this.usuario = usuario;

        // Registro de acciones del UI
        this.view.btnEnviarChat.addActionListener(e -> transmitirMensajeTexto());
        this.view.btnArchivo.addActionListener(e -> dispararFachadaArchivo());
        this.view.btnSalir.addActionListener(e -> abandonarReunion());
        
        this.view.modeloLista.addElement(usuario + " (Tú)");
        this.view.setVisible(true);
    }

    private void transmitirMensajeTexto() {
        String texto = view.txtInputChat.getText().trim();
        if (!texto.isEmpty()) {
            MensajeDTO dto = new MensajeDTO("CHAT", idSala, usuario, texto);
            ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));
            view.txtAreaChat.append("[Tú]: " + texto + "\n");
            view.txtInputChat.setText("");
        }
    }

    private void dispararFachadaArchivo() {
        JFileChooser selector = new JFileChooser();
        int resultado = selector.showOpenDialog(view);
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = selector.getSelectedFile();
            // Ejecución asíncrona en un hilo independiente para no colgar la cámara web
            new Thread(() -> {
                try {
                    view.txtAreaChat.append("[Sistema]: Subiendo archivo por bloques de 4KB...\n");
                    FachadaTransferencia.subirArchivo(archivoSeleccionado, idSala, usuario);
                    view.txtAreaChat.append("[Sistema]: Archivo compartido con éxito.\n");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error al transferir: " + ex.getMessage());
                }
            }).start();
        }
    }

    private void abandonarReunion() {
        MensajeDTO dto = new MensajeDTO("SALIR", idSala, usuario, "");
        ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));
        System.exit(0); // Cierra flujos físicos de red y destruye hilos visuales
    }
}