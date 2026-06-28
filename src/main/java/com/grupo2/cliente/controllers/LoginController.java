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
            String correo = view.txtUser.getText().trim();
            String password = new String(view.txtPass.getPassword()); // Asumiendo que usas JPasswordField
            String ip = view.txtIP.getText().trim();

            // 1. Conectamos al servidor de forma síncrona para esta transacción
            ConexionManager.getInstancia().conectar(ip, 8090);

            // 2. Despachamos el DTO de autenticación en formato JSON
            MensajeDTO dtoEnvio = new MensajeDTO("LOGIN", "", correo, password);
            ConexionManager.getInstancia().enviarTexto(new Gson().toJson(dtoEnvio));

            // 3. Capturamos la respuesta inmediata del servidor por el canal de red
            java.io.BufferedReader entrada = new java.io.BufferedReader(
                new java.io.InputStreamReader(ConexionManager.getInstancia().getSocket().getInputStream())
            );
        
        String jsonRespuesta = entrada.readLine();
        
        if (jsonRespuesta != null) {
            MensajeDTO dtoRespuesta = new Gson().fromJson(jsonRespuesta, MensajeDTO.class);
            
            // 4. Conmutación de interfaces si la base de datos dio luz verde
            if ("LOGIN_OK".equals(dtoRespuesta.getTipo())) {
                view.dispose(); // Destruimos el Login de la RAM
                
                // Levantamos el Dashboard de inmediato pasándole el usuario logueado
                javax.swing.SwingUtilities.invokeLater(() -> {
                    com.grupo2.cliente.views.DashboardView dashboardVista = new com.grupo2.cliente.views.DashboardView();
                    new com.grupo2.cliente.controllers.DashboardController(dashboardVista, correo);
                });
            } else {
                JOptionPane.showMessageDialog(view, "Acceso denegado: " + dtoRespuesta.getContenido(), "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
            }
        }

    } catch (Exception ex) {
            JOptionPane.showMessageDialog(view, "Fallo de conexión en red local/Tailscale: " + ex.getMessage(), "Error de Red", JOptionPane.ERROR_MESSAGE);
        }
    }
}