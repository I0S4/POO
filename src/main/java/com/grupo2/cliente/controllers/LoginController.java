package com.grupo2.cliente.controllers;

import com.google.gson.Gson;
import com.grupo2.cliente.conexion.ConexionManager;
import com.grupo2.cliente.views.LoginView;
import com.grupo2.compartido.models.MensajeDTO;
import javax.swing.JOptionPane;

public class LoginController {
    private final LoginView view;

    public LoginController(LoginView view) {
        this.view = view;
        this.view.btnConectar.addActionListener(e -> ejecutarLogin());
        this.view.setVisible(true);
    }

    private void ejecutarLogin() {
        try {
            // Inicializa la conexión por el Singleton
            ConexionManager.getInstancia().conectar(view.txtIP.getText(), 8090);
            
            MensajeDTO dto = new MensajeDTO("LOGIN", "", view.txtUser.getText(), new String(view.txtPass.getPassword()));
            ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dto));
            
            // Transición limpia de interfaces
            view.dispose();
            // Aquí se gatillaría la apertura de la Interfaz 2 (Dashboard)
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Fallo de conexión en red Tailscale: " + ex.getMessage());
        }
    }
}