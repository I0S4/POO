public class MetadataArchivo {
    private String sala;
    private String usuario;
    private String archivo;
    private long tamano;

    public MetadataArchivo(String sala, String usuario, String archivo, long tamano) {
        this.sala = sala;
        this.usuario = usuario;
        this.archivo = archivo;
        this.tamano = tamano;
    }

    public String toNotifyJson() {
        return "{\"type\":\"FILE_NOTIFY\",\"sala\":\"" + sala + 
               "\",\"usuario\":\"" + usuario + 
               "\",\"archivo\":\"" + archivo + 
               "\",\"tamano\":" + tamano + "}";
    }

    // Getters
    public String getSala() { return sala; }
    public String getUsuario() { return usuario; }
    public String getArchivo() { return archivo; }
    public long getTamano() { return tamano; }
}