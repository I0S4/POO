package com.grupo2.cliente;

import com.formdev.flatlaf.FlatDarkLaf;
import com.grupo2.cliente.controllers.LoginController;
import com.grupo2.cliente.views.LoginView;

public class MainCliente {
    public static void main(String[] args) {
        // 1. Inyectar Look and Feel moderno en el hilo de la UI
        FlatDarkLaf.setup();
        
        // 2. Despertar la vista y entregarle el control al controlador
        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginView vistaLogin = new LoginView();
            new LoginController(vistaLogin);
        });
    }
}