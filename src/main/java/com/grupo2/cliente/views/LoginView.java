package com.grupo2.cliente.views;

import javax.swing.*;
import java.awt.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class LoginView extends JFrame {
    public JTextField txtIP = new JTextField("100.100.100.1", 15);
    public JTextField txtUser = new JTextField(15);
    public JPasswordField txtPass = new JPasswordField(15);
    public JButton btnConectar = new JButton("Ingresar a la Plataforma");

    public LoginView() {
        FlatDarkLaf.setup(); // Inyecta Modo Oscuro Moderno de FlatLaf
        setTitle("Acceso al Sistema");
        setSize(350, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("IP VPN:"), gbc);
        gbc.gridx = 1; add(txtIP, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1; add(txtUser, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; add(txtPass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; add(btnConectar, gbc);
    }
}