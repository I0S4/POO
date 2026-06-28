package com.grupo2.cliente.controllers;

import com.google.gson.Gson;
import com.grupo2.cliente.conexion.ConexionManager;
import com.grupo2.cliente.views.DashboardView;
import com.grupo2.cliente.views.SalaView;
import com.grupo2.compartido.models.MensajeDTO;
import javax.swing.JOptionPane;

public class DashboardController {
    private final DashboardView view;
    private final String usuarioLogueado;

    public DashboardController(DashboardView view, String usuario) {
        this.view = view;
        this.usuarioLogueado = usuario;
        
        // Listeners limpios sin funciones anónimas gigantes
        this.view.btnCrear.addActionListener(e -> solicitarCrearSala());
        this.view.btnUnirse.addActionListener(e -> solicitarUnirseSala());
        this.view.setVisible(true);
    }

    private void solicitarCrearSala() {
        MensajeDTO dto = new MensajeDTO("CREAR_SALA", "", usuarioLogueado, "Nueva Sala");
        ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));
        abrirSala("SALA_HOST_TEMP"); // Cambia el estado y simula la entrada
    }

    private void solicitarUnirseSala() {
        String codigo = view.txtCodigo.getText().trim();
        if (codigo.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Digita el código de la sala de Tailscale.");
            return;
        }
        MensajeDTO dto = new MensajeDTO("UNIR_SALA", codigo, usuarioLogueado, "");
        ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));
        abrirSala(codigo);
    }

    private void abrirSala(String codigoSala) {
        view.dispose(); // Destruye el Dashboard de la RAM inmediatamente
        SalaView salaView = new SalaView();
        new SalaController(salaView, codigoSala, usuarioLogueado);
    }
}