package com.grupo2.cliente.views;

import javax.swing.*;
import java.awt.*;

public class SalaView extends JFrame {
    public JPanel pnlVideos = new JPanel(new GridLayout(1, 1, 10, 10));
    public JTextArea txtAreaChat = new JTextArea();
    public JTextField txtInputChat = new JTextField(15);
    public JButton btnEnviarChat = new JButton("Enviar");
    public DefaultListModel<String> modeloLista = new DefaultListModel<>();
    public JList<String> lstParticipantes = new JList<>(modeloLista);
    
    // Controles de la barra inferior oscura
    public JButton btnCamara = new JButton("📷");
    public JButton btnArchivo = new JButton("📎");
    public JButton btnSalir = new JButton("❌");

    public SalaView() {
        setTitle("Sala de Videoconferencia Activa");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Centro: Grid de Video dinámico
        pnlVideos.setBackground(Color.decode("#1E1E24"));
        add(pnlVideos, BorderLayout.CENTER);

        // 2. Derecha: Chat y Participantes unificados
        JPanel pnlDerecho = new JPanel(new GridLayout(2, 1, 5, 5));
        pnlDerecho.setPreferredSize(new Dimension(300, 0));
        
        JPanel pnlChat = new JPanel(new BorderLayout());
        txtAreaChat.setEditable(false);
        pnlChat.add(new JScrollPane(txtAreaChat), BorderLayout.CENTER);
        JPanel pnlInput = new JPanel(new FlowLayout());
        pnlInput.add(txtInputChat); pnlInput.add(btnEnviarChat);
        pnlChat.add(pnlInput, BorderLayout.SOUTH);

        pnlDerecho.add(new JScrollPane(lstParticipantes));
        pnlDerecho.add(pnlChat);
        add(pnlDerecho, BorderLayout.EAST);

        // 3. Abajo: Barra de herramientas minimalista estilo Discord/Zoom
        JPanel pnlControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        pnlControl.setBackground(Color.decode("#111214"));
        btnSalir.setBackground(Color.decode("#ED4245")); // Rojo de colgado
        
        pnlControl.add(btnCamara);
        pnlControl.add(btnArchivo);
        pnlControl.add(btnSalir);
        add(pnlControl, BorderLayout.SOUTH);
    }
}