import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MainServidor {
    private static final int PUERTO = 8090;
    
    public static ConcurrentHashMap<String, HiloCliente> salasHosts = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, List<HiloCliente>> salasClientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[SERVIDOR] Escuchando sockets en el puerto " + PUERTO);
            
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                HiloCliente cliente = new HiloCliente(clienteSocket);
                Thread hilo = new Thread(cliente);
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    public static HiloCliente buscarHiloPorUsuarioEnSala(String usuario, String sala) {
        List<HiloCliente> clientes = salasClientes.get(sala);
        if (clientes != null) {
            for (HiloCliente hc : clientes) {
                if (hc.getUsuarioActual().equals(usuario)) return hc;
            }
        }
        HiloCliente host = salasHosts.get(sala);
        if (host != null && host.getUsuarioActual().equals(usuario)) return host;
        return null;
    }

    public static HiloCliente buscarHiloFuga(String usuario) {
        for (HiloCliente host : salasHosts.values()) {
            if (host.getUsuarioActual().equals(usuario)) return host;
        }
        for (List<HiloCliente> lista : salasClientes.values()) {
            for (HiloCliente hc : lista) {
                if (hc.getUsuarioActual().equals(usuario)) return hc;
            }
        }
        return null;
    }

    public static void agregarInvitadoASala(String sala, HiloCliente hilo) {
        salasClientes.computeIfAbsent(sala, k -> new ArrayList<>()).add(hilo);
    }
}