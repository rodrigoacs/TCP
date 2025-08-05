
// File: Server.java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
  // Usamos ConcurrentHashMap para segurança em ambiente com múltiplas threads.
  // Mapeia o número do cliente (String) ao seu manipulador (ClientHandler).
  private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
  private static final int PORT = 12345; // Porta que o servidor escutará

  public void start() {
    System.out.println("Servidor de Chat iniciado na porta " + PORT);
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      while (true) {
        // Aguarda um cliente se conectar e aceita a conexão
        Socket clientSocket = serverSocket.accept();
        System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());

        // Cria um novo manipulador para o cliente em uma nova thread
        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
        new Thread(clientHandler).start();
      }
    } catch (IOException e) {
      System.err.println("Erro no servidor: " + e.getMessage());
    }
  }

  // Envia uma mensagem para todos os clientes, exceto para quem enviou.
  public void broadcastMessage(String message, ClientHandler sender) {
    System.out.println("Broadcast de " + sender.getClientInfo() + ": " + message);
    for (ClientHandler client : clients.values()) {
      if (client != sender) {
        client.sendMessage("[Broadcast de " + sender.getClientName() + "]: " + message);
      }
    }
  }

  // Envia uma mensagem privada para um cliente específico.
  public boolean sendPrivateMessage(String targetNumber, String message, ClientHandler sender) {
    ClientHandler targetClient = clients.get(targetNumber);
    if (targetClient != null) {
      System.out.println("Mensagem privada de " + sender.getClientInfo() + " para " + targetClient.getClientInfo());
      targetClient.sendMessage("[Privado de " + sender.getClientName() + "]: " + message);
      sender.sendMessage("[Você -> " + targetClient.getClientName() + "]: " + message);
      return true;
    }
    System.out.println("Falha ao enviar msg privada: cliente " + targetNumber + " não encontrado.");
    return false;
  }

  public boolean isClientNumberTaken(String clientNumber) {
    return clients.containsKey(clientNumber);
  }

  // Adiciona um novo cliente à lista e notifica a todos.
  public void addClient(String clientNumber, ClientHandler handler) {
    clients.put(clientNumber, handler);
    broadcastUserList();
  }

  // Remove um cliente e notifica a todos.
  public void removeClient(String clientNumber) {
    clients.remove(clientNumber);
    broadcastUserList();
  }

  // Envia a lista atualizada de usuários para todos os clientes.
  public void broadcastUserList() {
    StringBuilder userList = new StringBuilder("UPDATE_USERS:");
    for (ClientHandler client : clients.values()) {
      userList.append(client.getClientInfo()).append(",");
    }
    // Envia a lista para todos
    for (ClientHandler client : clients.values()) {
      client.sendMessage(userList.toString());
    }
  }

  public static void main(String[] args) {
    Server server = new Server();
    server.start();
  }
}