import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.awt.Dimension;
import java.awt.Image;
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
        
        initComponentsManual();
        conectarHiloLector();
    }

    private void initComponentsManual() {
        setTitle("Sala de Reuniones UNI - Usuario: " + miUsuario);
        setSize(950, 600);
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setBackground(new java.awt.Color(30, 30, 36));
        setLayout(null);

        JLabel lblSala = new JLabel("Código Sala:");
        lblSala.setForeground(java.awt.Color.WHITE);
        lblSala.setBounds(20, 20, 100, 25);
        add(lblSala);

        txtSalaInput = new javax.swing.JTextField();
        txtSalaInput.setBounds(110, 20, 120, 25);
        add(txtSalaInput);

        btnCrearSala = new javax.swing.JButton("Crear (Host)");
        btnCrearSala.setBounds(240, 20, 120, 25);
        btnCrearSala.addActionListener(e -> {
            salaActual = txtSalaInput.getText().trim();
            if(!salaActual.isEmpty()){
                salida.println("{\"type\":\"CREATE_ROOM\",\"sala\":\"" + salaActual + "\"}");
                btnCrearSala.setEnabled(false);
                btnUnirseSala.setEnabled(false);
            }
        });
        add(btnCrearSala);

        btnUnirseSala = new javax.swing.JButton("Unirse");
        btnUnirseSala.setBounds(370, 20, 100, 25);
        btnUnirseSala.addActionListener(e -> {
            salaActual = txtSalaInput.getText().trim();
            if(!salaActual.isEmpty()){
                salida.println("{\"type\":\"JOIN_ROOM_REQUEST\",\"sala\":\"" + salaActual + "\"}");
                btnCrearSala.setEnabled(false);
                btnUnirseSala.setEnabled(false);
            }
        });
        add(btnUnirseSala);

        // Chat Panel
        txtAreaChat = new javax.swing.JTextArea();
        txtAreaChat.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(txtAreaChat);
        scrollChat.setBounds(20, 70, 450, 350);
        add(scrollChat);

        txtMensajeInput = new javax.swing.JTextField();
        txtMensajeInput.setBounds(20, 435, 340, 30);
        add(txtMensajeInput);

        btnEnviar = new javax.swing.JButton("Enviar");
        btnEnviar.setBounds(370, 435, 100, 30);
        btnEnviar.addActionListener(e -> enviarMensajeChat());
        add(btnEnviar);

        btnCompartirArchivo = new javax.swing.JButton("Subir Documento");
        btnCompartirArchivo.setBounds(20, 480, 200, 30);
        btnCompartirArchivo.addActionListener(e -> subirArchivoSeguro());
        add(btnCompartirArchivo);

        // Participantes
        lstUsuarios = new javax.swing.JList<>(modeloLista);
        JScrollPane scrollUsuarios = new JScrollPane(lstUsuarios);
        scrollUsuarios.setBounds(490, 70, 180, 150);
        add(scrollUsuarios);

        // Paneles de Video
        lblMiCamara = new JLabel("Mi Cámara", SwingConstants.CENTER);
        lblMiCamara.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));
        lblMiCamara.setBounds(490, 240, 200, 150);
        add(lblMiCamara);

        lblCamaraRemota = new JLabel("Cámara Participante", SwingConstants.CENTER);
        lblCamaraRemota.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY));
        lblCamaraRemota.setBounds(710, 240, 200, 150);
        add(lblCamaraRemota);

        btnCamara = new javax.swing.JButton("Activar Cámara");
        btnCamara.setBounds(490, 405, 160, 30);
        btnCamara.addActionListener(e -> alternarCamara());
        add(btnCamara);
    }

    private void enviarMensajeChat() {
        String texto = txtMensajeInput.getText().trim();
        if (!texto.isEmpty() && !salaActual.isEmpty()) {
            salida.println("{\"type\":\"CHAT_MESSAGE\",\"sala\":\"" + salaActual + "\",\"mensaje\":\"" + texto + "\"}");
            txtMensajeInput.setText("");
        }
    }

    // CP-07: Envío seguro por bloques en formato Base64 para no romper la lectura por líneas del Servidor
    private void subirArchivoSeguro() {
        if (salaActual.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Primero debes crear o unirte a una sala.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            new Thread(() -> {
                try (FileInputStream fis = new FileInputStream(f)) {
                    // Mensaje de control inicial
                    salida.println("{\"type\":\"START_FILE_UPLOAD\",\"sala\":\"" + salaActual + "\",\"archivo\":\"" + f.getName() + "\"}");
                    
                    byte[] buffer = new byte[3000]; // Fragmentos controlados para evitar desbordamientos de buffer
                    int bytesLeidos;
                    while ((bytesLeidos = fis.read(buffer)) != -1) {
                        byte[] datosReales = bytesLeidos == 3000 ? buffer : java.util.Arrays.copyOf(buffer, bytesLeidos);
                        String chunkBase64 = Base64.getEncoder().encodeToString(datosReales);
                        salida.println("{\"type\":\"FILE_CHUNK\",\"sala\":\"" + salaActual + "\",\"archivo\":\"" + f.getName() + "\",\"data\":\"" + chunkBase64 + "\"}");
                    }
                    salida.println("{\"type\":\"FILE_END\",\"sala\":\"" + salaActual + "\",\"archivo\":\"" + f.getName() + "\"}");
                    
                    SwingUtilities.invokeLater(() -> txtAreaChat.append("[SISTEMA] Envío completado de: " + f.getName() + "\n"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    private void alternarCamara() {
        if (!camaraActiva) {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.setViewSize(new Dimension(176, 144)); // Tamaño adecuado para transferencia de sockets
                    webcam.open();
                    camaraActiva = true;
                    btnCamara.setText("Apagar Cámara");
                    iniciarStreamingCamara();
                } else {
                    JOptionPane.showMessageDialog(this, "No se detectó hardware de cámara.");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Dispositivo ocupado por otro proceso.");
            }
        } else {
            camaraActiva = false;
            if (webcam != null) webcam.close();
            btnCamara.setText("Activar Cámara");
            lblMiCamara.setIcon(null);
            lblMiCamara.setText("Cámara Apagada");
        }
    }

    private void iniciarStreamingCamara() {
        new Thread(() -> {
            while (camaraActiva && !socket.isClosed()) {
                try {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        // Actualizar UI Local
                        SwingUtilities.invokeLater(() -> {
                            lblMiCamara.setIcon(new ImageIcon(frame.getScaledInstance(lblMiCamara.getWidth(), lblMiCamara.getHeight(), Image.SCALE_FAST)));
                        });

                        // Convertir a JPEG comprimido en memoria
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(frame, "jpg", baos);
                        String frame64 = Base64.getEncoder().encodeToString(baos.toByteArray());

                        // Enviar trama al Servidor
                        if (!salaActual.isEmpty()) {
                            salida.println("{\"type\":\"CAMERA_FRAME\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"frame\":\"" + frame64 + "\"}");
                        }
                    }
                    Thread.sleep(150); // Control de FPS óptimo para evitar saturación de red
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }

    private void conectarHiloLector() {
        new Thread(() -> {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    final String trama = linea;
                    
                    // CP-06: Mensajes de Chat entrantes
                    if (trama.contains("\"type\":\"CHAT_MESSAGE\"")) {
                        String user = extraerValor(trama, "usuario");
                        String msg = extraerValor(trama, "mensaje");
                        SwingUtilities.invokeLater(() -> txtAreaChat.append(user + ": " + msg + "\n"));
                    }
                    
                    // CP-08: Fotogramas de cámaras de otros integrantes
                    else if (trama.contains("\"type\":\"CAMERA_FRAME\"")) {
                        String frame64 = extraerValor(trama, "frame");
                        byte[] imageBytes = Base64.getDecoder().decode(frame64);
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        if (img != null) {
                            SwingUtilities.invokeLater(() -> {
                                lblCamaraRemota.setIcon(new ImageIcon(img.getScaledInstance(lblCamaraRemota.getWidth(), lblCamaraRemota.getHeight(), Image.SCALE_FAST)));
                            });
                        }
                    }
                    
                    // CP-07: Notificación de archivo disponible para descargar
                    else if (trama.contains("\"type\":\"FILE_NOTIFY\"")) {
                        String arch = extraerValor(trama, "archivo");
                        String autor = extraerValor(trama, "usuario");
                        SwingUtilities.invokeLater(() -> txtAreaChat.append("[DOCUMENTO] " + autor + " compartió: " + arch + " (Almacenado en servidor)\n"));
                    }
                    
                    // Actualización de lista de participantes en pantalla (CP-09)
                    else if (trama.contains("\"type\":\"UPDATE_PARTICIPANTS\"")) {
                        String listaString = extraerValor(trama, "lista");
                        String[] arr = listaString.split(",");
                        SwingUtilities.invokeLater(() -> {
                            modeloLista.clear();
                            for (String u : arr) {
                                if(!u.isEmpty()) modeloLista.addElement(u);
                            }
                        });
                    }
                    
                    // CP-10: Cierre forzado de la sesión por el Host
                    else if (trama.contains("\"type\":\"ROOM_CLOSED\"")) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, "La sala de reuniones ha sido cerrada por el Anfitrión.");
                            camaraActiva = false;
                            if (webcam != null) webcam.close();
                            this.dispose();
                        });
                        break;
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> txtAreaChat.append("[SISTEMA] Desconectado del servidor de reuniones.\n"));
            }
        }).start();
    }

    private String extraerValor(String json, String clave) {
        try {
            String buscar = "\"" + clave + "\":\"";
            int inicio = json.indexOf(buscar) + buscar.length();
            return json.substring(inicio, json.indexOf("\"", inicio));
        } catch (Exception e) {
            return "";
        }
    }
}