import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.github.sarxos.webcam.Webcam;

public class VentanaSalas extends javax.swing.JFrame {
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String miUsuario;
    private String salaActual = ""; 
    private DefaultListModel<String> modeloLista = new DefaultListModel<>();

    private Webcam webcam;
    private boolean camaraActiva = false;
    private JLabel lblMiCamara;
    private JLabel lblCamaraRemota;
    private JButton btnCamara;
    private JButton btnSalirReunion; 

    private javax.swing.JButton btnCrearSala;
    private javax.swing.JButton btnUnirseSala;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JButton btnCompartirArchivo;
    private javax.swing.JTextField txtSalaInput;
    private javax.swing.JTextField txtMensajeInput;
    private javax.swing.JTextArea txtAreaChat;
    private javax.swing.JList<String> lstUsuarios;

    public VentanaSalas(Socket socket, PrintWriter salida, BufferedReader entrada, String usuario) {
        this.socket = socket;
        this.salida = salida;
        this.entrada = entrada;
        this.miUsuario = usuario;
        
        initComponentsCustom();
        iniciarHiloEscucha();
    }

    private void initComponentsCustom() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sala de Videoconferencia - Usuario: " + miUsuario);
        setSize(950, 600);
        getContentPane().setBackground(new Color(30, 30, 36));
        setLayout(new BorderLayout(10, 10));

        // --- PANEL SUPERIOR: Gestión de Salas ---
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelSuperior.setBackground(new Color(45, 45, 52));
        
        JLabel lblSala = new JLabel("Código Sala:");
        lblSala.setForeground(Color.WHITE);
        txtSalaInput = new javax.swing.JTextField(10);
        
        btnCrearSala = new javax.swing.JButton("Crear Sala (Host)");
        btnUnirseSala = new javax.swing.JButton("Unirse");
        
        btnSalirReunion = new javax.swing.JButton("Salir de la Reunión");
        btnSalirReunion.setBackground(new Color(211, 47, 47));
        btnSalirReunion.setForeground(Color.WHITE);
        btnSalirReunion.setEnabled(false); 

        panelSuperior.add(lblSala);
        panelSuperior.add(txtSalaInput);
        panelSuperior.add(btnCrearSala);
        panelSuperior.add(btnUnirseSala);
        panelSuperior.add(btnSalirReunion);
        add(panelSuperior, BorderLayout.NORTH);

        // --- PANEL CENTRAL: Cámaras de Video ---
        JPanel panelVideo = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        panelVideo.setBackground(new Color(30, 30, 36));

        lblMiCamara = new JLabel("Mi Cámara (Apagada)");
        lblMiCamara.setPreferredSize(new Dimension(320, 240));
        lblMiCamara.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lblMiCamara.setHorizontalAlignment(SwingConstants.CENTER);
        lblMiCamara.setForeground(Color.WHITE);

        lblCamaraRemota = new JLabel("Cámara Remota (Esperando...)");
        lblCamaraRemota.setPreferredSize(new Dimension(320, 240));
        lblCamaraRemota.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        lblCamaraRemota.setHorizontalAlignment(SwingConstants.CENTER);
        lblCamaraRemota.setForeground(Color.WHITE);

        panelVideo.add(lblMiCamara);
        panelVideo.add(lblCamaraRemota);
        add(panelVideo, BorderLayout.CENTER);

        // --- PANEL ESTE: Lista de Participantes ---
        JPanel panelUsuarios = new JPanel(new BorderLayout());
        panelUsuarios.setBackground(new Color(45, 45, 52));
        panelUsuarios.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Participantes", 0, 0, null, Color.WHITE));
        
        lstUsuarios = new javax.swing.JList<>(modeloLista);
        JScrollPane scrollUsuarios = new JScrollPane(lstUsuarios);
        scrollUsuarios.setPreferredSize(new Dimension(180, 0));
        panelUsuarios.add(scrollUsuarios, BorderLayout.CENTER);
        add(panelUsuarios, BorderLayout.EAST);

        // --- PANEL INFERIOR: Chat y Controles ---
        JPanel panelInferior = new JPanel(new BorderLayout(5, 5));
        panelInferior.setBackground(new Color(30, 30, 36));

        txtAreaChat = new javax.swing.JTextArea(8, 50);
        txtAreaChat.setEditable(false);
        txtAreaChat.setBackground(new Color(23, 23, 28));
        txtAreaChat.setForeground(Color.GREEN);
        JScrollPane scrollChat = new JScrollPane(txtAreaChat);
        panelInferior.add(scrollChat, BorderLayout.CENTER);

        JPanel panelControlesChat = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelControlesChat.setBackground(new Color(30, 30, 36));
        
        txtMensajeInput = new javax.swing.JTextField(30);
        btnEnviar = new javax.swing.JButton("Enviar Chat");
        btnCompartirArchivo = new javax.swing.JButton("Compartir Doc");
        btnCamara = new javax.swing.JButton("Encender Cámara");

        panelControlesChat.add(txtMensajeInput);
        panelControlesChat.add(btnEnviar);
        panelControlesChat.add(btnCompartirArchivo);
        panelControlesChat.add(btnCamara);
        panelInferior.add(panelControlesChat, BorderLayout.SOUTH);
        
        add(panelInferior, BorderLayout.SOUTH);

        // --- EVENT LISTENERS ---
        btnCrearSala.addActionListener(e -> {
            String sala = txtSalaInput.getText().trim();
            if(!sala.isEmpty()) {
                salaActual = sala;
                salida.println("{\"type\":\"CREATE_ROOM\",\"sala\":\"" + sala + "\",\"usuario\":\"" + miUsuario + "\"}");
                btnCrearSala.setEnabled(false);
                btnUnirseSala.setEnabled(false);
                btnSalirReunion.setEnabled(true);
                txtAreaChat.append("[SISTEMA] Has creado la sala: " + sala + " como Anfitrión.\n");
            }
        });

        btnUnirseSala.addActionListener(e -> {
            String sala = txtSalaInput.getText().trim();
            if(!sala.isEmpty()) {
                salaActual = sala;
                salida.println("{\"type\":\"JOIN_ROOM_REQUEST\",\"sala\":\"" + sala + "\",\"usuario\":\"" + miUsuario + "\"}");
                btnCrearSala.setEnabled(false);
                btnUnirseSala.setEnabled(false);
                btnSalirReunion.setEnabled(true);
                txtAreaChat.append("[SISTEMA] Solicitando ingreso a la sala: " + sala + "...\n");
            }
        });

        btnEnviar.addActionListener(e -> {
            String msg = txtMensajeInput.getText().trim();
            if (!msg.isEmpty() && !salaActual.isEmpty()) {
                salida.println("{\"type\":\"CHAT_MESSAGE\",\"sala\":\"" + salaActual + "\",\"mensaje\":\"" + msg + "\"}");
                txtMensajeInput.setText("");
            }
        });

        btnCamara.addActionListener(e -> {
            if (salaActual.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Primero debes unirte o crear una sala.");
                return;
            }
            if (!camaraActiva) {
                encenderCamaraLocal();
            } else {
                apagarCamaraLocal();
            }
        });

        btnCompartirArchivo.addActionListener(e -> {
            if (salaActual.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Primero debes unirte o crear una sala.");
                return;
            }
            subirMetadatosArchivo();
        });

        btnSalirReunion.addActionListener(e -> {
            ejecutarDesconexionLimpia();
        });

        // Lógica de Moderación: Doble clic en la lista para expulsar participantes (Host)
        lstUsuarios.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String usuarioSeleccionado = lstUsuarios.getSelectedValue();
                    if (usuarioSeleccionado != null) {
                        String miNombreCorto = miUsuario.contains("@") ? miUsuario.split("@")[0] : miUsuario;
                        
                        if (usuarioSeleccionado.equals(miNombreCorto)) {
                            return; 
                        }

                        // Verificamos si somos el Host
                        if (!btnCrearSala.isEnabled() && !btnUnirseSala.isEnabled() && !salaActual.isEmpty()) {
                            int respuesta = JOptionPane.showConfirmDialog(
                                VentanaSalas.this, 
                                "¿Deseas expulsar de la reunión al participante " + usuarioSeleccionado + "?",
                                "Moderación de Sala (Host)", 
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );

                            if (respuesta == JOptionPane.YES_OPTION) {
                                salida.println("{\"type\":\"KICK_USER\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + usuarioSeleccionado + "\"}");
                            }
                        }
                    }
                }
            }
        });
    }

    private void encenderCamaraLocal() {
        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open();
                camaraActiva = true;
                btnCamara.setText("Apagar Cámara");
                
                new Thread(() -> {
                    while (camaraActiva) {
                        try {
                            BufferedImage frame = webcam.getImage();
                            if (frame != null) {
                                Image escalada = frame.getScaledInstance(lblMiCamara.getWidth(), lblMiCamara.getHeight(), Image.SCALE_SMOOTH);
                                SwingUtilities.invokeLater(() -> lblMiCamara.setIcon(new ImageIcon(escalada)));

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(frame, "jpg", baos);
                                byte[] bytes = baos.toByteArray();
                                String base64 = Base64.getEncoder().encodeToString(bytes);

                                String nombreCorto = miUsuario.contains("@") ? miUsuario.split("@")[0] : miUsuario;
                                salida.println("{\"type\":\"CAMERA_FRAME\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + nombreCorto + "\",\"frame\":\"" + base64 + "\"}");
                            }
                            Thread.sleep(150); 
                        } catch (Exception ex) {
                            break;
                        }
                    }
                }).start();
            } else {
                JOptionPane.showMessageDialog(this, "No se detectó ninguna cámara de video en el sistema.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de inicialización de cámara: " + ex.getMessage());
        }
    }

    private void apagarCamaraLocal() {
        camaraActiva = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        lblMiCamara.setIcon(null);
        lblMiCamara.setText("Mi Cámara (Apagada)");
        btnCamara.setText("Encender Cámara");
    }

    private void subirMetadatosArchivo() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = selector.getSelectedFile();
            long tamano = archivo.length();
            String nombreCorto = miUsuario.contains("@") ? miUsuario.split("@")[0] : miUsuario;
            
            salida.println("{\"type\":\"START_FILE_UPLOAD\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + nombreCorto + "\",\"archivo\":\"" + archivo.getName() + "\",\"tamano\":" + tamano + "}");
        }
    }

    private void ejecutarDesconexionLimpia() {
        apagarCamaraLocal();
        String nombreCorto = miUsuario.contains("@") ? miUsuario.split("@")[0] : miUsuario;
        
        if (salida != null && !salaActual.isEmpty()) {
            salida.println("{\"type\":\"LEAVE_ROOM\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + nombreCorto + "\"}");
        }
        
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); 
            }
        } catch (IOException ex) {
            System.err.println("[CLIENTE] Error al cerrar socket físico: " + ex.getMessage());
        }
        
        salaActual = "";
        modeloLista.clear();
        btnCrearSala.setEnabled(true);
        btnUnirseSala.setEnabled(true);
        btnSalirReunion.setEnabled(false);
        lblCamaraRemota.setIcon(null);
        lblCamaraRemota.setText("Cámara Remota (Esperando...)");
        txtAreaChat.append("[SISTEMA] Has salido de la reunión y se ha liberado el socket de red.\n");
    }

    private void iniciarHiloEscucha() {
        new Thread(() -> {
            try {
                String trama;
                while ((trama = entrada.readLine()) != null) {
                    if (trama.contains("\"type\":\"CHAT_MESSAGE\"")) {
                        String user = extraerValor(trama, "usuario");
                        String msg = extraerValor(trama, "mensaje");
                        SwingUtilities.invokeLater(() -> txtAreaChat.append("[" + user + "]: " + msg + "\n"));
                    }
                    
                    else if (trama.contains("\"type\":\"CAMERA_FRAME\"")) {
                        String emisor = extraerValor(trama, "usuario");
                        String miNombreCorto = miUsuario.contains("@") ? miUsuario.split("@")[0] : miUsuario;
                        
                        if (!emisor.equals(miNombreCorto)) {
                            String base64Image = extraerValor(trama, "frame");
                            try {
                                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                                BufferedImage imgRemota = ImageIO.read(bais);
                                if (imgRemota != null) {
                                    Image escalada = imgRemota.getScaledInstance(lblCamaraRemota.getWidth(), lblCamaraRemota.getHeight(), Image.SCALE_SMOOTH);
                                    SwingUtilities.invokeLater(() -> lblCamaraRemota.setIcon(new ImageIcon(escalada)));
                                }
                            } catch (Exception ex) {
                                // Control de ruido binario menor
                            }
                        }
                    }
                    
                    else if (trama.contains("\"type\":\"FILE_NOTIFY\"")) {
                        String emisor = extraerValor(trama, "usuario");
                        String archivo = extraerValor(trama, "archivo");
                        SwingUtilities.invokeLater(() -> txtAreaChat.append("[SISTEMA] El usuario (" + emisor + ") compartió el documento: " + archivo + "\n"));
                    }
                    
                    else if (trama.contains("\"type\":\"USERS_LIST\"")) {
                        String listaCruda = extraerValor(trama, "usuarios");
                        String[] arr = listaCruda.split(",");
                        SwingUtilities.invokeLater(() -> {
                            modeloLista.clear();
                            for (String u : arr) {
                                if(!u.isEmpty()) modeloLista.addElement(u);
                            }
                        });
                    }

                    else if (trama.contains("\"type\":\"USER_WAITING\"")) {
                        String usuarioEspera = extraerValor(trama, "usuario");
                        int opcion = JOptionPane.showConfirmDialog(
                            this, 
                            "El participante '" + usuarioEspera + "' solicita ingresar a la reunión. ¿Permitir acceso?",
                            "Sala de Espera Activa", 
                            JOptionPane.YES_NO_OPTION
                        );
                        
                        if (opcion == JOptionPane.YES_OPTION) {
                            salida.println("{\"type\":\"ACCEPT_USER\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + usuarioEspera + "\"}");
                        }
                    }

                    else if (trama.contains("\"type\":\"JOIN_ROOM_APPROVED\"")) {
                        SwingUtilities.invokeLater(() -> {
                            txtAreaChat.append("[SISTEMA] ¡Tu ingreso ha sido aprobado por el Anfitrión!\n");
                        });
                    }

                    else if (trama.contains("\"type\":\"KICKED_BY_HOST\"")) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "Has sido retirado de la sala por el anfitrión de la reunión.", "Expulsado", JOptionPane.ERROR_MESSAGE);
                            apagarCamaraLocal();
                            salaActual = "";
                            modeloLista.clear();
                            btnCrearSala.setEnabled(true);
                            btnUnirseSala.setEnabled(true);
                            btnSalirReunion.setEnabled(false);
                            lblCamaraRemota.setIcon(null);
                            lblCamaraRemota.setText("Cámara Remota (Esperando...)");
                        });
                    }
                    
                    else if (trama.contains("\"type\":\"ROOM_CLOSED\"")) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "La sala ha sido finalizada por el Anfitrión.");
                            apagarCamaraLocal();
                            modeloLista.clear();
                            salaActual = "";
                            btnCrearSala.setEnabled(true);
                            btnUnirseSala.setEnabled(true);
                            btnSalirReunion.setEnabled(false);
                            lblCamaraRemota.setIcon(null);
                            lblCamaraRemota.setText("Cámara Remota (Esperando...)");
                        });
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> txtAreaChat.append("[SISTEMA] Error de conexión con el Servidor.\n"));
            }
        }).start();
    }

    private String extraerValor(String json, String clave) {
        try {
            String buscar = "\"" + clave + "\":\"";
            if (!json.contains(buscar)) {
                String buscarNum = "\"" + clave + "\":";
                int inicio = json.indexOf(buscarNum) + buscarNum.length();
                int fin = json.indexOf(",", inicio);
                if (fin == -1) fin = json.indexOf("}", inicio);
                return json.substring(inicio, fin).trim();
            }
            int inicio = json.indexOf(buscar) + buscar.length();
            return json.substring(inicio, json.indexOf("\"", inicio));
        } catch (Exception e) {
            return "";
        }
    }
}