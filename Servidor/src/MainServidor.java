import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MainServidor {
    private static final int PUERTO = 8090;
    
    // Almacenamiento seguro en entornos concurrentes multihilo para las salas activas
    public static ConcurrentHashMap<String, HiloCliente> salasHosts = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, List<HiloCliente>> salasClientes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("[SERVIDOR CONFIGURADO] Escuchando sockets en puerto local: " + PUERTO);
            
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                HiloCliente cliente = new HiloCliente(clienteSocket);
                Thread hilo = new Thread(cliente);
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("[FALLO CRÍTICO] Servidor apagado: " + e.getMessage());
        }
    }

    public static void agregarInvitadoASala(String sala, HiloCliente hilo) {
        salasClientes.computeIfAbsent(sala, k -> new ArrayList<>()).add(hilo);
        notificarCambioParticipantes(sala);
    }

    public static void removerUsuarioDeSala(String sala, HiloCliente hilo) {
        if (sala != null && !sala.isEmpty()) {
            List<HiloCliente> lista = salasClientes.get(sala);
            if (lista != null) {
                synchronized (lista) {
                    lista.remove(hilo);
                }
                if (lista.isEmpty() && !thisSalaTieneHost(sala)) {
                    salasClientes.remove(sala);
                }
            }
            notificarCambioParticipantes(sala);
        }
    }

    private static boolean thisSalaTieneHost(String sala) {
        return salasHosts.containsKey(sala);
    }

    // Método centralizado de retransmisión (Broadcast) a toda la sala
    public static void broadcast(String sala, String mensaje) {
        if (sala == null || sala.isEmpty()) return;

        // Envío obligatorio al Anfitrión (Host)
        HiloCliente host = salasHosts.get(sala);
        if (host != null) {
            host.enviarMensajeDirecto(mensaje);
        }

        // Envío secuencial a todos los invitados dentro de la sala
        List<HiloCliente> lista = salasClientes.get(sala);
        if (lista != null) {
            synchronized (lista) {
                for (HiloCliente hc : lista) {
                    hc.enviarMensajeDirecto(mensaje);
                }
            }
        }
    }

    // CP-09: Construye de forma reactiva la lista de usuarios y limpia fantasmas de la red
    public static void notificarCambioParticipantes(String sala) {
        if (sala == null || sala.isEmpty()) return;
        
        StringBuilder sb = new StringBuilder("{\"type\":\"USERS_LIST\",\"usuarios\":\"");
        HiloCliente host = salasHosts.get(sala);
        if (host != null) {
            sb.append(host.getUsuarioActual()).append(",");
        }
        
        List<HiloCliente> lista = salasClientes.get(sala);
        if (lista != null) {
            synchronized (lista) {
                for (HiloCliente hc : lista) {
                    sb.append(hc.getUsuarioActual()).append(",");
                }
            }
        }
        sb.append("\"}");
        broadcast(sala, sb.toString());
    }
}
