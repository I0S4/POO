import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class MainCliente extends javax.swing.JFrame {
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtHost;
    private javax.swing.JPasswordField txtPassword;

    public MainCliente() {
        initComponents();
        this.getContentPane().setBackground(new java.awt.Color(30, 30, 36));
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtCorreo = new javax.swing.JTextField(10);
        txtPassword = new javax.swing.JPasswordField(10);
        txtHost = new javax.swing.JTextField("localhost", 10);
        btnLogin = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Inicio de Sesión - Zoom Prototipo");

        jLabel3.setForeground(Color.WHITE);
        jLabel3.setText("Servidor (Zrok/IP)");
        jLabel1.setForeground(Color.WHITE);
        jLabel1.setText("Correo");
        jLabel2.setForeground(Color.WHITE);
        jLabel2.setText("Contraseña");

        btnLogin.setText("Iniciar Sesión");
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnLogin, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE)
                    .addComponent(txtHost)
                    .addComponent(txtCorreo)
                    .addComponent(txtPassword))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(btnLogin)
                .addContainerGap(25, Short.MAX_VALUE))
        );
        pack();
        setLocationRelativeTo(null);
    }

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {                                         
    try {
        String hostInput = txtHost.getText().trim();
        String hostFinal = hostInput;
        int puertoFinal = 8090; // Puerto local por defecto
        
        // Limpiar protocolos si el usuario los pega
        String limpia = hostInput.replace("tcp://", "").replace("https://", "").replace("http://", "");
        
        if (limpia.contains("zrok.io")) {
            // SI ES ZROK: Forzamos el puerto 443 de salida web, ignorando el 8090
            if (limpia.contains(":")) {
                String[] partes = limpia.split(":");
                hostFinal = partes[0];
            } else {
                hostFinal = limpia;
            }
            puertoFinal = 443; 
            System.out.println("[ZROK DETECTADO] Conectando a: " + hostFinal + " por el puerto: " + puertoFinal);
        } else if (limpia.contains(":")) {
            // Si es IP local con puerto escrito (ej: 192.168.1.15:8090)
            String[] partes = limpia.split(":");
            hostFinal = partes[0];
            puertoFinal = Integer.parseInt(partes[1]);
        } else {
            hostFinal = limpia;
        }

        // Crear la conexión con un timeout de 10 segundos para prevenir lag de datos móviles
        Socket socket = new Socket(hostFinal, puertoFinal);
        socket.setSoTimeout(10000); 
        socket.setTcpNoDelay(true);

        PrintWriter salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        String correo = txtCorreo.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();

        salida.println("{\"type\":\"LOGIN\",\"correo\":\"" + correo + "\",\"password\":\"" + pass + "\"}");
        String jsonResponse = entrada.readLine();
        
        if (jsonResponse != null && jsonResponse.contains("\"status\":\"SUCCESS\"")) {
            JOptionPane.showMessageDialog(this, "¡Login Correcto! Bienvenido.");
            
            // Quitamos el timeout para que el streaming de las cámaras sea continuo
            socket.setSoTimeout(0); 
            
            new VentanaSalas(socket, salida, entrada, correo).setVisible(true);
            this.dispose(); 
        } else {
            JOptionPane.showMessageDialog(this, "Login Incorrecto.");
            socket.close();
        }
    } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Error: No se pudo conectar al servidor Zrok/IP. Asegúrate de que el túnel esté abierto.");
    } 
}
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new MainCliente().setVisible(true));
    }
}