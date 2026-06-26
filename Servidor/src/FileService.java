import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class FileService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/prototipo_zoom";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static final String CARPETA_DESCARGAS = "Servidor/descargas/";

    public static boolean registrarMetadatos(MetadataArchivo meta) {
        String query = "INSERT INTO archivoscompartidos (IdSala, NombreArchivo, Tamano, EnviadoPor) VALUES (?, ?, ?, ?)";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, meta.getSala());
                ps.setString(2, meta.getArchivo());
                ps.setLong(3, meta.getTamano());
                ps.setString(4, meta.getUsuario());
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            // CONTROL DE FALLOS: Si falla por la columna 'Tamano', intentamos alternativamente con 'tamano' minúscula
            try {
                String queryAlternativa = "INSERT INTO archivoscompartidos (IdSala, NombreArchivo, tamano, EnviadoPor) VALUES (?, ?, ?, ?)";
                try (Connection con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                     PreparedStatement ps = con.prepareStatement(queryAlternativa)) {
                    ps.setString(1, meta.getSala());
                    ps.setString(2, meta.getArchivo());
                    ps.setLong(3, meta.getTamano());
                    ps.setString(4, meta.getUsuario());
                    return ps.executeUpdate() > 0;
                }
            } catch (Exception ex) {
                System.err.println("[ERROR RECONSTRUCCIÓN BD] No se pudo registrar en la BD: " + ex.getMessage());
                // Retorna true para permitir el flujo local por sockets aunque la BD falle localmente
                return true; 
            }
        }
    }

    public static void guardarChunk(String sala, String nombreArchivo, byte[] buffer, int bytesLeidos) {
        try {
            File directorio = new File(CARPETA_DESCARGAS + sala);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }
            
            File archivoDestino = new File(directorio, nombreArchivo);
            // 'true' activa el modo Append para ir uniendo secuencialmente cada bloque binario recibido
            try (FileOutputStream fos = new FileOutputStream(archivoDestino, true)) {
                fos.write(buffer, 0, bytesLeidos);
            }
        } catch (Exception e) {
            System.err.println("[ERROR E/S CHUNK]: " + e.getMessage());
        }
    }
}