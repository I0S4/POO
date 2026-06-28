package com.grupo2.cliente.views;

import javax.swing.*;
import java.awt.*;

public class DashboardView extends JFrame {
    public JButton btnCrear = new JButton("Crear Nueva Sala (+)");
    public JTextField txtCodigo = new JTextField(10);
    public JButton btnUnirse = new JButton("Unirse a Reunión");

    public DashboardView() {
        setTitle("Dashboard Central de Reuniones");
        setSize(400, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 40));

        btnCrear.putClientProperty("JButton.buttonType", "roundRect");
        btnUnirse.putClientProperty("JButton.buttonType", "roundRect");

        add(btnCrear);
        add(new JLabel("Código:"));
        add(txtCodigo);
        add(btnUnirse);
    }
}