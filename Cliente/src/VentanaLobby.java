import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class VentanaLobby extends JFrame {
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String miUsuario;
    private JTextField txtSala;

    public VentanaLobby(Socket socket, PrintWriter salida, BufferedReader entrada, String usuario) {
        this.socket = socket;
        this.salida = salida;
        this.entrada = entrada;
        this.miUsuario = usuario;

        setTitle("Gestión de Reuniones");
        setSize(400, 180);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(30, 30, 36));
        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        JLabel lbl = new JLabel("Código de la Sala:");
        lbl.setForeground(Color.WHITE);
        txtSala = new JTextField(15);

        JButton btnCrear = new JButton("Crear Sala (Host)");
        JButton btnUnirse = new JButton("Unirse a Sala");

        btnCrear.addActionListener(e -> {
            String sala = txtSala.getText().trim();
            salida.println("{\"type\":\"CREATE_ROOM\",\"sala\":\"" + sala + "\",\"usuario\":\"" + miUsuario + "\"}");
            abrirSalaInteractiva(sala, true);
        });

        btnUnirse.addActionListener(e -> {
            String sala = txtSala.getText().trim();
            salida.println("{\"type\":\"JOIN_ROOM_REQUEST\",\"sala\":\"" + sala + "\",\"usuario\":\"" + miUsuario + "\"}");
            abrirSalaInteractiva(sala, false);
        });

        add(lbl); add(txtSala);
        add(btnCrear); add(btnUnirse);
    }

    private void abrirSalaInteractiva(String sala, boolean esHost) {
        new VentanaReunionCompleta(socket, salida, entrada, miUsuario, sala, esHost).setVisible(true);
        this.dispose();
    }
}