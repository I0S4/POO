public class MensajeChat {
    private String idSala;
    private String usuario;
    private String contenido;

    public MensajeChat(String idSala, String usuario, String contenido) {
        this.idSala = idSala;
        this.usuario = usuario;
        this.contenido = contenido;
    }

    public String getIdSala() { return idSala; }
    public String getUsuario() { return usuario; }
    public String getContenido() { return contenido; }

    public String toJson() {
        return "{\"type\":\"CHAT_MESSAGE\",\"sala\":\"" + idSala + "\",\"usuario\":\"" + usuario + "\",\"mensaje\":\"" + contenido + "\"}";
    }
}