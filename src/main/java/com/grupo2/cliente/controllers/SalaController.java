package com.grupo2.cliente.controllers;

import com.google.gson.Gson;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
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
    
    // Variables de control para el hardware de la cámara
    private Webcam miCamara = null;
    private boolean camaraEncendida = false;

    public SalaController(SalaView view, String idSala, String usuario) {
        this.view = view;
        this.idSala = idSala;
        this.usuario = usuario;

        // Registro de acciones del UI
        this.view.btnEnviarChat.addActionListener(e -> transmitirMensajeTexto());
        this.view.btnArchivo.addActionListener(e -> dispararFachadaArchivo());
        this.view.btnSalir.addActionListener(e -> abandonarReunion());
        this.view.btnCamara.addActionListener(e -> conmutarCamaraWeb());
        
        // Listener para descargar archivos con doble clic en la lista
        this.view.lstParticipantes.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String itemSeleccionado = view.lstParticipantes.getSelectedValue();
                    if (itemSeleccionado != null && itemSeleccionado.contains("[Archivo]")) {
                        String nombreArchivo = itemSeleccionado.replace("[Archivo] ", "").trim();
                        dispararFachadaDescarga(nombreArchivo);
                    }
                }
            }
        });

        this.view.modeloLista.addElement(usuario + " (Tú)");
        this.view.setVisible(true);
    }

    private void conmutarCamaraWeb() {
        if (!camaraEncendida) {
            new Thread(() -> {
                try {
                    view.txtAreaChat.append("[Sistema]: Inicializando hardware de video...\n");
                    miCamara = Webcam.getDefault();
                    
                    if (miCamara != null) {
                        miCamara.setViewSize(miCamara.getViewSizes()[0]);
                        
                        WebcamPanel panelVideo = new WebcamPanel(miCamara);
                        panelVideo.setFPSDisplayed(true);
                        panelVideo.setMirrored(true);
                        
                        view.pnlVideos.removeAll();
                        view.pnlVideos.add(panelVideo);
                        view.pnlVideos.revalidate();
                        view.pnlVideos.repaint();
                        
                        view.btnCamara.setText("🎥 Encendida");
                        camaraEncendida = true;
                        view.txtAreaChat.append("[Sistema]: Transmisión de video local activa.\n");
                    } else {
                        JOptionPane.showMessageDialog(view, "No se detectó ninguna cámara web física.", "Error de Hardware", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    view.txtAreaChat.append("[Error Video]: " + ex.getMessage() + "\n");
                }
            }).start();
        } else {
            apagarCamaraSegura();
            view.btnCamara.setText("📷");
            camaraEncendida = false;
            view.txtAreaChat.append("[Sistema]: Cámara web apagada.\n");
        }
    }

    private void transmitirMensajeTexto() {
        String texto = view.txtInputChat.getText().trim();
        if (!texto.isEmpty()) {
            MensajeDTO dto = new MensajeDTO("CHAT", idSala, usuario, texto);
            ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto) + "\n");
            view.txtAreaChat.append("[Tú]: " + texto + "\n");
            view.txtInputChat.setText("");
        }
    }

    private void dispararFachadaArchivo() {
        JFileChooser selector = new JFileChooser();
        int resultado = selector.showOpenDialog(view);
        
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivoSeleccionado = selector.getSelectedFile();
            new Thread(() -> {
                try {
                    view.txtAreaChat.append("[Sistema]: Subiendo archivo por bloques de 4KB...\n");
                    FachadaTransferencia.subirArchivo(archivoSeleccionado, idSala, usuario);
                    view.txtAreaChat.append("[Sistema]: Archivo compartido: " + archivoSeleccionado.getName() + "\n");
                    
                    // Añadir visualmente el archivo a la lista de la sala
                    SwingUtilities.invokeLater(() -> view.modeloLista.addElement("[Archivo] " + archivoSeleccionado.getName()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(view, "Error al transferir: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }).start();
        }
    }

    private void dispararFachadaDescarga(String nombreArchivo) {
        new Thread(() -> {
            try {
                view.txtAreaChat.append("[Sistema]: Iniciando descarga de: " + nombreArchivo + "...\n");
                // Enviamos "1" temporalmente como idArchivo ficticio para la demostración
                FachadaTransferencia.descargarArchivo("1", nombreArchivo, idSala);
                view.txtAreaChat.append("[Sistema]: Descarga completa. Guardado en carpeta descargas.\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(view, "Error al descargar el archivo: " + ex.getMessage(), "Error de Descarga", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void apagarCamaraSegura() {
        if (miCamara != null && miCamara.isOpen()) {
            miCamara.close();
        }
        view.pnlVideos.removeAll();
        view.pnlVideos.revalidate();
        view.pnlVideos.repaint();
    }

    private void abandonarReunion() {
        apagarCamaraSegura();
        MensajeDTO dto = new MensajeDTO("SALIR", idSala, usuario, "");
        ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto) + "\n");
        System.exit(0);
    }
}