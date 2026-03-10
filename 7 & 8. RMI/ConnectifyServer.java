import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

interface ChatServer extends Remote {
    void registerClient(ChatClient client, String username) throws RemoteException;
    void broadcastMessage(String message) throws RemoteException;
    void disconnectClient(ChatClient client, String username) throws RemoteException;
}

interface ChatClient extends Remote {
    void receiveMessage(String message) throws RemoteException;
}

public class ConnectifyServer extends UnicastRemoteObject implements ChatServer {
    private List<ChatClient> connectedClients;

    protected ConnectifyServer() throws RemoteException {
        super();
        connectedClients = new ArrayList<>();
    }

    @Override
    public synchronized void registerClient(ChatClient client, String username) throws RemoteException {
        connectedClients.add(client);
        System.out.println(username + " joined the server.");
        broadcastMessage("[System]: " + username + " has joined Connectify!");
    }

    @Override
    public synchronized void broadcastMessage(String message) throws RemoteException {
        System.out.println("Broadcasting: " + message);
        
        for (int i = connectedClients.size() - 1; i >= 0; i--) {
            try {
                connectedClients.get(i).receiveMessage(message);
            } catch (RemoteException e) {
                connectedClients.remove(i);
            }
        }
    }

    @Override
    public synchronized void disconnectClient(ChatClient client, String username) throws RemoteException {
        connectedClients.remove(client);
        System.out.println(username + " left the server.");
        broadcastMessage("[System]: " + username + " has left the chat.");
    }

    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            ConnectifyServer server = new ConnectifyServer();
            Naming.rebind("ConnectifyLive", server);
            System.out.println(">>> Connectify Live Server is Running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}