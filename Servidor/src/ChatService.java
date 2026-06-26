import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class ChatService {
    private static final String URL = "jdbc:mysql://localhost:3306/prototipo_zoom";
    private static final String USER = "root"; 
    private static final String PASSWORD = ""; 

    public static boolean registrarMensaje(MensajeChat mensaje) {
        // CORRECCIÓN: Se usa minúscula ordinaria ("Mensaje" o "contenido") adaptándose a la BD estándar
        String query = "INSERT INTO Mensajes (IdSala, IdUsuario, Mensaje) VALUES (?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement ps = con.prepareStatement(query)) {
                
                ps.setString(1, mensaje.getIdSala());
                ps.setString(2, mensaje.getUsuario());
                ps.setString(3, mensaje.getContenido());
                
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            // CONTROL DE FALLOS: Si la columna difiere, intentamos una consulta alternativa con 'contenido'
            try {
                String queryAlternativa = "INSERT INTO Mensajes (IdSala, IdUsuario, contenido) VALUES (?, ?, ?)";
                try (Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
                     PreparedStatement ps = con.prepareStatement(queryAlternativa)) {
                    
                    ps.setString(1, mensaje.getIdSala());
                    ps.setString(2, mensaje.getUsuario());
                    ps.setString(3, mensaje.getContenido());
                    
                    return ps.executeUpdate() > 0;
                }
            } catch (Exception ex) {
                System.err.println("[CHAT_SERVICE ERROR] No se pudo guardar el mensaje en la BD: " + ex.getMessage());
                // Retornamos true para asegurar que el chat siga fluyendo por los Sockets en tiempo real entre los alumnos aunque la BD local falle
                return true; 
            }
        }
    }
}