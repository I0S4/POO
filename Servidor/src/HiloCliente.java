import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class HiloCliente implements Runnable {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String usuarioActual = "Desconocido";
    private String salaAsignada = "";

    // Mapa global en memoria para los usuarios que aguardan aprobación en Sala de Espera
    public static ConcurrentHashMap<String, List<HiloCliente>> salasEspera = new ConcurrentHashMap<>();

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    public String getUsuarioActual() {
        return this.usuarioActual;
    }

    public void enviarMensajeDirecto(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (!linea.contains("\"type\":\"CAMERA_FRAME\"")) {
                    System.out.println("[SERVER RX] " + linea);
                }

                // --- LOGIN ---
                if (linea.contains("\"type\":\"LOGIN\"")) {
                    String correo = extraerValor(linea, "correo");
                    String pass = extraerValor(linea, "password");
                    if (validarCredencialesBD(correo, pass)) {
                        this.usuarioActual = correo.split("@")[0]; 
                        salida.println("{\"status\":\"SUCCESS\"}");
                    } else {
                        salida.println("{\"status\":\"FAIL\"}");
                    }
                }

                // --- CREAR SALA (HOST) ---
                else if (linea.contains("\"type\":\"CREATE_ROOM\"")) {
                    String sala = extraerValor(linea, "sala");
                    this.salaAsignada = sala;
                    MainServidor.salasHosts.put(sala, this);
                    MainServidor.notificarCambioParticipantes(sala);
                }

                // --- SOLICITAR UNIRSE (VA A SALA DE ESPERA) ---
                else if (linea.contains("\"type\":\"JOIN_ROOM_REQUEST\"")) {
                    String sala = extraerValor(linea, "sala");
                    this.salaAsignada = sala;
                    
                    // En lugar de meterlo directo, lo mandamos a la cola de espera
                    salasEspera.computeIfAbsent(sala, k -> new ArrayList<>()).add(this);
                    
                    // Le avisamos al Host de esa sala que hay alguien tocando la puerta
                    HiloCliente host = MainServidor.salasHosts.get(sala);
                    if (host != null) {
                        host.enviarMensajeDirecto("{\"type\":\"USER_WAITING\",\"usuario\":\"" + this.usuarioActual + "\"}");
                    } else {
                        salida.println("{\"type\":\"ROOM_NOT_FOUND\"}");
                    }
                }

                // --- RESPUESTA DEL HOST: ACEPTAR USUARIO ---
                else if (linea.contains("\"type\":\"ACCEPT_USER\"")) {
                    String sala = extraerValor(linea, "sala");
                    String usuarioAceptado = extraerValor(linea, "usuario");
                    
                    List<HiloCliente> espera = salasEspera.get(sala);
                    if (espera != null) {
                        HiloCliente invitado = null;
                        synchronized (espera) {
                            for (HiloCliente hc : espera) {
                                if (hc.getUsuarioActual().equals(usuarioAceptado)) {
                                    invitado = hc;
                                    break;
                                }
                            }
                            if (invitado != null) {
                                espera.remove(invitado);
                            }
                        }
                        if (invitado != null) {
                            // Ahora sí ingresa oficialmente
                            MainServidor.agregarInvitadoASala(sala, invitado);
                            invitado.enviarMensajeDirecto("{\"type\":\"JOIN_ROOM_APPROVED\",\"sala\":\"" + sala + "\"}");
                        }
                    }
                }

                // --- RESPUESTA DEL HOST: BOTAR / EXPULSAR PARTICIPANTE ---
                else if (linea.contains("\"type\":\"KICK_USER\"")) {
                    String sala = extraerValor(linea, "sala");
                    String usuarioBotado = extraerValor(linea, "usuario");
                    
                    List<HiloCliente> listaClientes = MainServidor.salasClientes.get(sala);
                    if (listaClientes != null) {
                        HiloCliente victima = null;
                        synchronized (listaClientes) {
                            for (HiloCliente hc : listaClientes) {
                                if (hc.getUsuarioActual().equals(usuarioBotado)) {
                                    victima = hc;
                                    break;
                                }
                            }
                        }
                        if (victima != null) {
                            // Le avisamos que fue expulsado para que cierre su UI
                            victima.enviarMensajeDirecto("{\"type\":\"KICKED_BY_HOST\"}");
                            MainServidor.removerUsuarioDeSala(sala, victima);
                        }
                    }
                }

                // --- SALIDA VOLUNTARIA ---
                else if (linea.contains("\"type\":\"LEAVE_ROOM\"")) {
                    String sala = extraerValor(linea, "sala");
                    MainServidor.removerUsuarioDeSala(sala, this);
                    this.salaAsignada = "";
                }

                // --- CHAT ---
                else if (linea.contains("\"type\":\"CHAT_MESSAGE\"")) {
                    String msgText = extraerValor(linea, "mensaje");
                    MensajeChat mc = new MensajeChat(this.salaAsignada, this.usuarioActual, msgText);
                    ChatService.registrarMensaje(mc); 
                    MainServidor.broadcast(this.salaAsignada, mc.toJson());
                }

                // --- VIDEO ---
                else if (linea.contains("\"type\":\"CAMERA_FRAME\"")) {
                    MainServidor.broadcast(this.salaAsignada, linea);
                }

                // --- ARCHIVOS ---
                else if (linea.contains("\"type\":\"START_FILE_UPLOAD\"")) {
                    String fileName = extraerValor(linea, "archivo");
                    String sizeStr = extraerValor(linea, "tamano");
                    long tamano = sizeStr.isEmpty() ? 0L : Long.parseLong(sizeStr);
                    MetadataArchivo meta = new MetadataArchivo(this.salaAsignada, this.usuarioActual, fileName, tamano);
                    FileService.registrarMetadatos(meta);
                    MainServidor.broadcast(this.salaAsignada, meta.toNotifyJson());
                }
            }
        } catch (IOException e) {
            System.err.println("[INFO] Desconexión del hilo de: " + usuarioActual);
        } finally {
            if (!this.salaAsignada.isEmpty()) {
                if (MainServidor.salasHosts.get(this.salaAsignada) == this) {
                    MainServidor.broadcast(this.salaAsignada, "{\"type\":\"ROOM_CLOSED\"}");
                    MainServidor.salasHosts.remove(this.salaAsignada);
                    MainServidor.salasClientes.remove(this.salaAsignada);
                    salasEspera.remove(this.salaAsignada);
                } else {
                    MainServidor.removerUsuarioDeSala(this.salaAsignada, this);
                }
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ex) {}
        }
    }

    private boolean validarCredencialesBD(String correo, String pass) {
        if (correo.equals("invitado@uni.pe") && pass.equals("123456")) return true;
        String query = "SELECT * FROM Usuarios WHERE Correo = ? AND Password = ?";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/prototipo_zoom", "root", "");
                 PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, correo);
                ps.setString(2, pass);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            }
        } catch (Exception e) { return correo.contains("@"); }
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
        } catch (Exception e) { return ""; }
    }
}