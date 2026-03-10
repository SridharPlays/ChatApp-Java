import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class ConnectifyServer {
    private static final int TCP_PORT = 5000;
    private static final int MULTICAST_PORT = 4446;
    private static final String MULTICAST_GROUP = "230.0.0.1";

    private static final ExecutorService clientPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("      CONNECTIFY REAL-TIME SERVER         ");
        System.out.println("==========================================");

        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("✅ Server Online. Listening for users on Port " + TCP_PORT);
            System.out.println("✅ Multicast Channel Ready on " + MULTICAST_GROUP + ":" + MULTICAST_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                clientPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastToMulticastGroup(String message) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            byte[] buffer = message.getBytes();

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            udpSocket.send(packet);

            System.out.println("[BROADCAST] " + message);

        } catch (IOException e) {
            System.err.println("Error broadcasting message: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                username = in.readLine();
                String ip = socket.getInetAddress().getHostAddress();

                String joinMsg = "🟢 " + username + " has joined the chat.";
                ConnectifyServer.broadcastToMulticastGroup(joinMsg);

                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    if (clientMessage.equalsIgnoreCase("exit"))
                        break;

                    String time = new SimpleDateFormat("HH:mm").format(new Date());
                    String formattedMsg = String.format("[%s] %s: %s", time, username, clientMessage);

                    ConnectifyServer.broadcastToMulticastGroup(formattedMsg);
                }

            } catch (IOException e) {
            } finally {
                String leaveMsg = "🔴 " + username + " has left the chat.";
                ConnectifyServer.broadcastToMulticastGroup(leaveMsg);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}