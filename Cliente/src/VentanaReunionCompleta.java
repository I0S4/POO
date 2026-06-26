import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import javax.imageio.ImageIO;
import com.github.sarxos.webcam.Webcam;

public class VentanaReunionCompleta extends JFrame {
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String miUsuario, salaActual;
    private boolean esHost;

    private JTextArea areaChat;
    private JTextField txtMensaje;
    private DefaultListModel<String> modeloUsuarios;
    private JList<String> listaUsuarios;
    
    // Paneles de Video
    private JLabel lblMiCamara;
    private JLabel lblCamaraRemota;
    private JButton btnCamara;
    private boolean camaraActiva = false;
    private Webcam webcam;

    public VentanaReunionCompleta(Socket socket, PrintWriter salida, BufferedReader entrada, String usuario, String sala, boolean esHost) {
        this.socket = socket;
        this.salida = salida;
        this.entrada = entrada;
        this.miUsuario = usuario;
        this.salaActual = sala;
        this.esHost = esHost;

        setTitle("Sala Activa: " + sala + " | Usuario: " + usuario);
        setSize(900, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(40, 40, 45));
        setLayout(new BorderLayout(10, 10));

        // --- PANEL IZQUIERDO: CHAT Y ACCIONES ---
        JPanel panelIzquierdo = new JPanel(new BorderLayout(5, 5));
        panelIzquierdo.setPreferredSize(new Dimension(350, 500));
        
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        panelIzquierdo.add(new JScrollPane(areaChat), BorderLayout.CENTER);

        JPanel panelControlChat = new JPanel(new BorderLayout());
        txtMensaje = new JTextField();
        JButton btnEnviar = new JButton("Enviar");
        btnEnviar.addActionListener(e -> enviarMensaje());
        panelControlChat.add(txtMensaje, BorderLayout.CENTER);
        panelControlChat.add(btnEnviar, BorderLayout.EAST);
        panelIzquierdo.add(panelControlChat, BorderLayout.SOUTH);

        // --- PANEL CENTRAL: MULTIMEDIA VIDEO ---
        JPanel panelVideos = new JPanel(new GridLayout(1, 2, 10, 10));
        panelVideos.setBackground(new Color(30, 30, 35));
        
        lblMiCamara = new JLabel("Cámara Local Apagada", JLabel.CENTER);
        lblMiCamara.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblMiCamara.setForeground(Color.WHITE);
        
        lblCamaraRemota = new JLabel("Sin Video Remoto", JLabel.CENTER);
        lblCamaraRemota.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblCamaraRemota.setForeground(Color.WHITE);

        panelVideos.add(lblMiCamara);
        panelVideos.add(lblCamaraRemota);

        // --- PANEL DERECHO: PARTICIPANTES ---
        JPanel panelDerecho = new JPanel(new BorderLayout());
        panelDerecho.setPreferredSize(new Dimension(180, 500));
        modeloUsuarios = new DefaultListModel<>();
        listaUsuarios = new JList<>(modeloUsuarios);
        panelDerecho.add(new JScrollPane(listaUsuarios), BorderLayout.CENTER);
        
        // --- BOTONES DE ACCIÓN SUPERIOR ---
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnCamara = new JButton("Encender Cámara");
        btnCamara.addActionListener(e -> alternarCamara());
        JButton btnArchivo = new JButton("Compartir Documento");
        btnArchivo.addActionListener(e -> subirArchivo());
        panelSuperior.add(btnCamara);
        panelSuperior.add(btnArchivo);

        add(panelIzquierdo, BorderLayout.WEST);
        add(panelVideos, BorderLayout.CENTER);
        add(panelDerecho, BorderLayout.EAST);
        add(panelSuperior, BorderLayout.NORTH);

        // Iniciar el hilo escuchador del servidor
        new Thread(this::escucharServidor).start();
    }

    private void alternarCamara() {
        if (!camaraActiva) {
            try {
                webcam = Webcam.getDefault();
                if (webcam != null) {
                    webcam.setViewSize(new Dimension(320, 240));
                    webcam.open();
                    camaraActiva = true;
                    btnCamara.setText("Apagar Cámara");
                    new Thread(this::bucleTransmisionVideo).start();
                } else {
                    JOptionPane.showMessageDialog(this, "No se detectó hardware de cámara.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error cámara: " + ex.getMessage());
            }
        } else {
            camaraActiva = false;
            if (webcam != null) webcam.close();
            btnCamara.setText("Encender Cámara");
            lblMiCamara.setIcon(null);
            lblMiCamara.setText("Cámara Local Apagada");
        }
    }

    private void bucleTransmisionVideo() {
        while (camaraActiva) {
            try {
                BufferedImage frame = webcam.getImage();
                if (frame != null) {
                    // Pintar localmente
                    lblMiCamara.setIcon(new ImageIcon(frame));
                    
                    // Comprimir a JPG en memoria
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(frame, "jpg", baos);
                    byte[] bytesImg = baos.toByteArray();
                    
                    // Convertir a Base64 String plano para enviarlo por línea de comandos
                    String stringBase64 = Base64.getEncoder().encodeToString(bytesImg);
                    
                    salida.println("{\"type\":\"CAMERA_FRAME\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"frame\":\"" + stringBase64 + "\"}");
                }
                Thread.sleep(70); // Control de Framerate (~14 FPS) para balancear la carga de red por zrok
            } catch (Exception e) { break; }
        }
    }

    private void escucharServidor() {
        try {
            String trama;
            while ((trama = entrada.readLine()) != null) {
                if (trama.contains("\"type\":\"JOIN_ROOM_REQUEST\"") && esHost) {
                    String solicitante = extraerValor(trama, "usuario");
                    int op = JOptionPane.showConfirmDialog(this, "El usuario " + solicitante + " desea ingresar a la sala.", "Petición de Ingreso", JOptionPane.YES_NO_OPTION);
                    String estado = (op == JOptionPane.YES_OPTION) ? "ACEPTADO" : "RECHAZADO";
                    salida.println("{\"type\":\"ROOM_RESPONSE\",\"usuario\":\"" + solicitante + "\",\"sala\":\"" + salaActual + "\",\"estado\":\"" + estado + "\"}");
                }
                else if (trama.contains("\"type\":\"ROOM_RESPONSE\"")) {
                    String estado = extraerValor(trama, "estado");
                    if (!estado.equals("ACEPTADO")) {
                        JOptionPane.showMessageDialog(this, "Acceso Denegado por el Anfitrión.");
                        System.exit(0);
                    }
                }
                else if (trama.contains("\"type\":\"CHAT_MESSAGE\"")) {
                    areaChat.append(extraerValor(trama, "usuario") + ": " + extraerValor(trama, "mensaje") + "\n");
                }
                else if (trama.contains("\"type\":\"UPDATE_WAITING_ROOM\"")) {
                    String[] users = extraerValor(trama, "participantes").split(",");
                    modeloUsuarios.clear();
                    for (String u : users) modeloUsuarios.addElement(u);
                }
                else if (trama.contains("\"type\":\"CAMERA_FRAME\"")) {
                    // Decodificar render binario remoto enviado por la otra PC
                    String base64Data = extraerValor(trama, "frame");
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    BufferedImage imageRemota = ImageIO.read(bais);
                    if (imageRemota != null) {
                        lblCamaraRemota.setText("");
                        lblCamaraRemota.setIcon(new ImageIcon(imageRemota));
                    }
                }
                else if (trama.contains("\"type\":\"FILE_NOTIFY\"")) {
                    areaChat.append("[SISTEMA] Archivo compartido disponible: " + extraerValor(trama, "archivo") + "\n");
                }
            }
        } catch (IOException e) {
            areaChat.append("[SISTEMA] Conexión con el servidor interrumpida.\n");
        }
    }

    private void enviarMensaje() {
        String msg = txtMensaje.getText().trim();
        if (!msg.isEmpty()) {
            salida.println("{\"type\":\"CHAT_MESSAGE\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"mensaje\":\"" + msg + "\"}");
            areaChat.append("Yo: " + msg + "\n");
            txtMensaje.setText("");
        }
    }

    private void subirArchivo() {
        JFileChooser selector = new JFileChooser();
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = selector.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(archivo);
                 OutputStream os = socket.getOutputStream()) {
                
                long tamano = archivo.length();
                salida.println("{\"type\":\"START_FILE_UPLOAD\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"archivo\":\"" + archivo.getName() + "\",\"tamano\":" + tamano + "}");
                
                // Esperar sincronización
                Thread.sleep(300);
                byte[] buffer = new byte[4096];
                int leidos;
                while ((leidos = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, leidos);
                }
                os.flush();
                areaChat.append("[INFO] Archivo enviado con éxito.\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error subida: " + ex.getMessage());
            }
        }
    }

    private String extraerValor(String json, String clave) {
        try {
            String buscar = "\"" + clave + "\":\"";
            int inicio = json.indexOf(buscar) + buscar.length();
            return json.substring(inicio, json.indexOf("\"", inicio));
        } catch (Exception e) { return ""; }
    }
}
