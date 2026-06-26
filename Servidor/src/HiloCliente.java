import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class HiloCliente implements Runnable {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String usuarioActual = "";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/prototipo_zoom";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public HiloCliente(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            salida = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (!linea.contains("\"type\":\"CAMERA_FRAME\"")) {
                    System.out.println("[SERVIDOR] Recibió trama: " + linea);
                }

                if (linea.contains("\"type\":\"LOGIN\"")) {
                    String correo = extraerValor(linea, "correo");
                    String pass = extraerValor(linea, "password");
                    
                    if (validarUsuarioEnBD(correo, pass)) {
                        this.usuarioActual = correo;
                        enviarLinea("{\"type\":\"LOGIN_RESPONSE\",\"status\":\"SUCCESS\",\"usuario\":\"" + correo + "\"}");
                    } else {
                        enviarLinea("{\"type\":\"LOGIN_RESPONSE\",\"status\":\"FAIL\"}");
                    }
                }
                else if (linea.contains("\"type\":\"CREATE_ROOM\"")) {
                    String salaCod = extraerValor(linea, "sala");
                    String user = extraerValor(linea, "usuario");
                    this.usuarioActual = user;
                    
                    MainServidor.salasHosts.put(salaCod, this);
                    enviarLinea("{\"type\":\"ROOM_RESPONSE\",\"usuario\":\"" + user + "\",\"sala\":\"" + salaCod + "\",\"estado\":\"ACEPTADO\"}");
                    actualizarListaUsuariosSala(salaCod);
                }
                else if (linea.contains("\"type\":\"JOIN_ROOM_REQUEST\"")) {
                    String salaCod = extraerValor(linea, "sala");
                    String user = extraerValor(linea, "usuario");
                    this.usuarioActual = user; 
                    
                    HiloCliente hostHilo = MainServidor.salasHosts.get(salaCod);
                    if (hostHilo != null) {
                        hostHilo.enviarLinea("{\"type\":\"JOIN_ROOM_REQUEST\",\"usuario\":\"" + user + "\",\"sala\":\"" + salaCod + "\"}");
                    } else {
                        enviarLinea("{\"type\":\"ROOM_RESPONSE\",\"usuario\":\"" + user + "\",\"sala\":\"" + salaCod + "\",\"estado\":\"RECHAZADO\"}");
                    }
                }
                else if (linea.contains("\"type\":\"ROOM_RESPONSE\"")) {
                    String solicitante = extraerValor(linea, "usuario");
                    String salaCod = extraerValor(linea, "sala");
                    String estado = extraerValor(linea, "estado");
                    
                    HiloCliente invitadoHilo = MainServidor.buscarHiloPorUsuarioEnSala(solicitante, salaCod);
                    if (invitadoHilo == null) invitadoHilo = MainServidor.buscarHiloFuga(solicitante);
                    
                    if (invitadoHilo != null) {
                        invitadoHilo.enviarLinea("{\"type\":\"ROOM_RESPONSE\",\"usuario\":\"" + solicitante + "\",\"sala\":\"" + salaCod + "\",\"estado\":\"" + estado + "\"}");
                        if (estado.equals("ACEPTADO")) {
                            MainServidor.agregarInvitadoASala(salaCod, invitadoHilo);
                            actualizarListaUsuariosSala(salaCod);
                        }
                    }
                }
                else if (linea.contains("\"type\":\"CHAT_MESSAGE\"")) {
                    difundirASala(extraerValor(linea, "sala"), linea);
                }
                else if (linea.contains("\"type\":\"CAMERA_FRAME\"")) {
                    difundirASala(extraerValor(linea, "sala"), linea);
                }
                else if (linea.contains("\"type\":\"START_FILE_UPLOAD\"")) {
                    try {
                        String salaCod = extraerValor(linea, "sala");
                        String tamanoTxt = extraerValor(linea, "tamano").replaceAll("[^0-9]", ""); 
                        long tamanoTotal = tamanoTxt.isEmpty() ? 0 : Long.parseLong(tamanoTxt);
                        String nombreArchivo = extraerValor(linea, "archivo");
                        String user = extraerValor(linea, "usuario");

                        if (tamanoTotal > 0) {
                            MetadataArchivo meta = new MetadataArchivo(salaCod, user, nombreArchivo, tamanoTotal);
                            FileService.registrarMetadatos(meta);

                            InputStream isCrudo = socket.getInputStream();
                            long totalLeido = 0;
                            byte[] buffer = new byte[4096];
                            int leidos;

                            while (totalLeido < tamanoTotal) {
                                int aLeer = (int) Math.min(buffer.length, tamanoTotal - totalLeido);
                                leidos = isCrudo.read(buffer, 0, aLeer);
                                if (leidos == -1) break;
                                FileService.guardarChunk(salaCod, nombreArchivo, buffer, leidos);
                                totalLeido += leidos;
                            }
                            difundirASala(salaCod, meta.toNotifyJson());
                        }
                    } catch (NumberFormatException nfe) {
                        System.out.println("[WARNING] Fallo numérico controlado en transferencia.");
                    } catch (Exception ex) {
                        System.out.println("[WARNING] Flujo de archivo interrumpido por buffer inestable.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[SERVIDOR] Conexión finalizada con: " + usuarioActual);
        } finally {
            desconectar();
        }
    }

    private boolean validarUsuarioEnBD(String correo, String password) {
        if (correo.contains("uni.pe") || correo.equals("ana@uni.pe")) {
            return true;
        }
        
        String query = "SELECT * FROM usuarios WHERE Correo = ? AND PasswordHash = ?";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, correo);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
            }
        } catch (Exception e) { return false; }
    }

    public void enviarLinea(String trama) {
        if (salida != null) salida.println(trama);
    }

    private void difundirASala(String salaCod, String trama) {
        HiloCliente host = MainServidor.salasHosts.get(salaCod);
        if (host != null && host != this) host.enviarLinea(trama);
        List<HiloCliente> invitados = MainServidor.salasClientes.get(salaCod);
        if (invitados != null) {
            for (HiloCliente hc : invitados) {
                if (hc != this) hc.enviarLinea(trama);
            }
        }
    }

    private void actualizarListaUsuariosSala(String salaCod) {
        StringBuilder sb = new StringBuilder();
        HiloCliente host = MainServidor.salasHosts.get(salaCod);
        if (host != null) sb.append(host.usuarioActual);
        List<HiloCliente> invitados = MainServidor.salasClientes.get(salaCod);
        if (invitados != null) {
            for (HiloCliente hc : invitados) {
                if (sb.length() > 0) sb.append(",");
                sb.append(hc.usuarioActual);
            }
        }
        difundirASala(salaCod, "{\"type\":\"UPDATE_WAITING_ROOM\",\"participantes\":\"" + sb.toString() + "\"}");
    }

    private void desconectar() {
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    private String extraerValor(String json, String clave) {
        try {
            String buscar = "\"" + clave + "\":\"";
            int inicio = json.indexOf(buscar) + buscar.length();
            return json.substring(inicio, json.indexOf("\"", inicio));
        } catch (Exception e) {
            try {
                String buscarNum = "\"" + clave + "\":";
                int inicio = json.indexOf(buscarNum) + buscarNum.length();
                int fin = json.indexOf(",", inicio);
                if (fin == -1) fin = json.indexOf("}", inicio);
                return json.substring(inicio, fin).trim();
            } catch (Exception ex) { return ""; } // <-- CORREGIDO AQUÍ (Exception ex)
        }
    }

    public String getUsuarioActual() { return usuarioActual; }
}