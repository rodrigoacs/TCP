
// File: ClientHandler.java
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
  private final Socket clientSocket;
  private final Server server;
  private PrintWriter out;
  private String clientNumber;
  private String clientName;

  public ClientHandler(Socket socket, Server server) {
    this.clientSocket = socket;
    this.server = server;
  }

  @Override
  public void run() {
    try (
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
      this.out = out;

      // 1. Processo de Cadastro
      String registrationLine = in.readLine(); // Formato esperado: "REGISTER:numero:nome"
      String[] parts = registrationLine.split(":", 3);
      if (parts.length == 3 && "REGISTER".equals(parts[0])) {
        this.clientNumber = parts[1];
        this.clientName = parts[2];
        server.addClient(clientNumber, this);
        System.out.println("Cliente " + getClientInfo() + " registrado.");
      } else {
        System.out.println("Falha no registro. Fechando conexão.");
        return; // Encerra a thread se o registro falhar
      }

      // 2. Loop de Leitura de Mensagens
      String clientMessage;
      while ((clientMessage = in.readLine()) != null) {
        System.out.println("Mensagem recebida de " + getClientInfo() + ": " + clientMessage);
        if (clientMessage.startsWith("BROADCAST:")) {
          String message = clientMessage.substring(10);
          server.broadcastMessage(message, this);
        } else if (clientMessage.startsWith("PRIVATE:")) {
          String[] privateParts = clientMessage.split(":", 3); // Formato: "PRIVATE:numero_destino:mensagem"
          if (privateParts.length == 3) {
            if (!server.sendPrivateMessage(privateParts[1], privateParts[2], this)) {
              sendMessage("[ERRO]: Usuário " + privateParts[1] + " não encontrado ou offline.");
            }
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Cliente " + getClientInfo() + " desconectado.");
    } finally {
      if (clientNumber != null) {
        server.removeClient(clientNumber);
      }
      try {
        clientSocket.close();
      } catch (IOException e) {
        // Silencioso
      }
    }
  }

  // Método para o servidor enviar uma mensagem para este cliente.
  public void sendMessage(String message) {
    if (out != null) {
      out.println(message);
    }
  }

  public String getClientInfo() {
    return clientNumber + ":" + clientName;
    //return clientName + "(" + clientNumber + ")";
  }

  public String getClientName() {
    return clientName;
  }
}