import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.awt.Dimension;
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
    private boolean esHost = false;
    private DefaultListModel<String> modeloLista = new DefaultListModel<>();

    // Elementos de la Cámara
    private Webcam webcam;
    private boolean camaraActiva = false;
    private JLabel lblMiCamara;
    private JLabel lblCamaraRemota;
    private JButton btnCamara;

    // Componentes de la UI
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

        initComponents();
        this.getContentPane().setBackground(new java.awt.Color(30, 30, 36));
        lstUsuarios.setModel(modeloLista);
        
        new Thread(this::escucharServidor).start();
    }

    private void initComponents() {
        txtSalaInput = new javax.swing.JTextField(10);
        btnCrearSala = new javax.swing.JButton("Crear");
        btnUnirseSala = new javax.swing.JButton("Unirse");
        
        txtAreaChat = new javax.swing.JTextArea(12, 30);
        txtAreaChat.setEditable(false);
        txtMensajeInput = new javax.swing.JTextField(20);
        btnEnviar = new javax.swing.JButton("Enviar");
        btnCompartirArchivo = new javax.swing.JButton("Subir Archivo");
        btnCamara = new javax.swing.JButton("Activar Cámara");
        
        lstUsuarios = new javax.swing.JList<>();
        lblMiCamara = new JLabel("Mi Cámara", JLabel.CENTER);
        lblCamaraRemota = new JLabel("Cámara Remota", JLabel.CENTER);

        lblMiCamara.setPreferredSize(new Dimension(160, 120));
        lblMiCamara.setBorder(BorderFactory.createLineBorder(java.awt.Color.WHITE));
        lblCamaraRemota.setPreferredSize(new Dimension(160, 120));
        lblCamaraRemota.setBorder(BorderFactory.createLineBorder(java.awt.Color.WHITE));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Plataforma de Reuniones");
        setLayout(new java.awt.FlowLayout());

        add(new JLabel("Sala:")); add(txtSalaInput);
        add(btnCrearSala); add(btnUnirseSala);
        add(new JScrollPane(txtAreaChat));
        add(txtMensajeInput); add(btnEnviar);
        add(btnCompartirArchivo); add(btnCamara);
        add(new JScrollPane(lstUsuarios));
        add(lblMiCamara); add(lblCamaraRemota);

        btnCrearSala.addActionListener(e -> {
            salaActual = txtSalaInput.getText().trim();
            esHost = true;
            salida.println("{\"type\":\"CREATE_ROOM\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\"}");
        });

        btnUnirseSala.addActionListener(e -> {
            salaActual = txtSalaInput.getText().trim();
            esHost = false;
            salida.println("{\"type\":\"JOIN_ROOM_REQUEST\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\"}");
        });

        btnEnviar.addActionListener(e -> {
            String msg = txtMensajeInput.getText().trim();
            if(!msg.isEmpty()) {
                salida.println("{\"type\":\"CHAT_MESSAGE\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"mensaje\":\"" + msg + "\"}");
                txtAreaChat.append("Yo: " + msg + "\n");
                txtMensajeInput.setText("");
            }
        });

        btnCompartirArchivo.addActionListener(e -> subirArchivo());
        btnCamara.addActionListener(e -> alternarCamara());

        setSize(480, 600);
        setLocationRelativeTo(null);
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
                    new Thread(this::bucleVideo).start();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        } else {
            camaraActiva = false;
            if (webcam != null) webcam.close();
            btnCamara.setText("Activar Cámara");
        }
    }

    private void bucleVideo() {
        while (camaraActiva) {
            try {
                BufferedImage img = webcam.getImage();
                if (img != null) {
                    lblMiCamara.setIcon(new ImageIcon(img.getScaledInstance(160, 120, java.awt.Image.SCALE_FAST)));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "jpg", baos);
                    String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    salida.println("{\"type\":\"CAMERA_FRAME\",\"sala\":\"" + salaActual + "\",\"frame\":\"" + b64 + "\"}");
                }
                Thread.sleep(80);
            } catch (Exception e) { break; }
        }
    }

    private void escucharServidor() {
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.contains("\"type\":\"JOIN_ROOM_REQUEST\"") && esHost) {
                    String solicitante = extraerValor(linea, "usuario");
                    int op = JOptionPane.showConfirmDialog(this, "Aceptar a " + solicitante);
                    String est = (op == JOptionPane.YES_OPTION) ? "ACEPTADO" : "RECHAZADO";
                    salida.println("{\"type\":\"ROOM_RESPONSE\",\"usuario\":\"" + solicitante + "\",\"sala\":\"" + salaActual + "\",\"estado\":\"" + est + "\"}");
                }
                else if (linea.contains("\"type\":\"ROOM_RESPONSE\"")) {
                    if (linea.contains("\"estado\":\"ACEPTADO\"")) {
                        txtAreaChat.append("[SISTEMA] Conectado a la sala: " + salaActual + "\n");
                    } else {
                        JOptionPane.showMessageDialog(this, "Rechazado");
                    }
                }
                else if (linea.contains("\"type\":\"CHAT_MESSAGE\"")) {
                    txtAreaChat.append(extraerValor(linea, "usuario") + ": " + extraerValor(linea, "mensaje") + "\n");
                }
                else if (linea.contains("\"type\":\"UPDATE_WAITING_ROOM\"")) {
                    String[] users = extraerValor(linea, "participantes").split(",");
                    modeloLista.clear();
                    for(String u : users) modeloLista.addElement(u);
                }
                else if (linea.contains("\"type\":\"CAMERA_FRAME\"")) {
                    byte[] bytes = Base64.getDecoder().decode(extraerValor(linea, "frame"));
                    BufferedImage remImg = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (remImg != null) {
                        lblCamaraRemota.setIcon(new ImageIcon(remImg.getScaledInstance(160, 120, java.awt.Image.SCALE_FAST)));
                    }
                }
                else if (linea.contains("\"type\":\"FILE_NOTIFY\"")) {
                    txtAreaChat.append("[ARCHIVO] Disponible: " + extraerValor(linea, "archivo") + "\n");
                }
            }
        } catch (Exception e) {}
    }

    private void subirArchivo() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(f); OutputStream os = socket.getOutputStream()) {
                salida.println("{\"type\":\"START_FILE_UPLOAD\",\"sala\":\"" + salaActual + "\",\"usuario\":\"" + miUsuario + "\",\"archivo\":\"" + f.getName() + "\",\"tamano\":" + f.length() + "}");
                Thread.sleep(200);
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) os.write(buf, 0, n);
                os.flush();
                txtAreaChat.append("[SISTEMA] Archivo cargado.\n");
            } catch (Exception e) {}
        }
    }

    private String extraerValor(String json, String clave) {
        try {
            String b = "\"" + clave + "\":\"";
            int i = json.indexOf(b) + b.length();
            return json.substring(i, json.indexOf("\"", i));
        } catch (Exception e) { return ""; }
    }
}