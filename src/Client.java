
// File: Client.java
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Client {

  private static PrintWriter out;
  private static MultiWindowTextGUI gui;
  private static ActionListBox messagesListBox;
  private static ActionListBox usersListBox;

  public static void main(String[] args) throws IOException {
    // --- Configuração Inicial (sem alterações) ---
    DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
    Screen screen = null;
    try {
      screen = terminalFactory.createScreen();
      screen.startScreen();

      gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));

      String serverIp = new TextInputDialogBuilder().setTitle("IP do Servidor")
          .setDescription("Digite o IP do servidor (ex: localhost)").build().showDialog(gui);
      if (serverIp == null)
        return;
      String userNumber = new TextInputDialogBuilder().setTitle("Seu Número")
          .setDescription("Digite seu número (ex: 101)")
          .setValidationPattern(Pattern.compile("[0-9]+"), "Apenas números").build().showDialog(gui);
      if (userNumber == null)
        return;
      String userName = new TextInputDialogBuilder().setTitle("Seu Nome").setDescription("Digite seu nome (ex: Fulano)")
          .build().showDialog(gui);
      if (userName == null)
        return;

      Socket socket = new Socket(serverIp, 12345);
      out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out.println("REGISTER:" + userNumber + ":" + userName);

      buildGui(userName, userNumber);
      new Thread(new ServerListener(in)).start();
      gui.waitForWindowToClose(gui.getWindows().iterator().next());

    } catch (IOException e) {
      System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
    } finally {
      if (screen != null) {
        screen.stopScreen();
      }
    }
  }

  private static void buildGui(String userName, String userNumber) {
    BasicWindow window = new BasicWindow("Terminal Chat - Logado como " + userName + "(" + userNumber + ")");
    window.setHints(Arrays.asList(Window.Hint.EXPANDED));

    Panel mainContentPanel = new Panel(new BorderLayout());
    Panel topSectionPanel = new Panel(new GridLayout(2));
    mainContentPanel.addComponent(topSectionPanel, BorderLayout.Location.CENTER);

    Panel messagesPanel = new Panel();
    messagesPanel.addComponent(new Label("MENSAGENS").withBorder(Borders.singleLine()));
    messagesListBox = new ActionListBox(new TerminalSize(50, 20));
    messagesPanel.addComponent(messagesListBox);
    topSectionPanel.addComponent(messagesPanel.withBorder(Borders.singleLine()));

    Panel usersPanel = new Panel();
    usersPanel.addComponent(new Label("USUÁRIOS ONLINE").withBorder(Borders.singleLine()));
    usersListBox = new ActionListBox(new TerminalSize(25, 20));
    usersPanel.addComponent(usersListBox);
    topSectionPanel.addComponent(usersPanel.withBorder(Borders.singleLine()));

    Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
    mainContentPanel.addComponent(inputPanel, BorderLayout.Location.BOTTOM);
    TextBox messageInput = new TextBox(new TerminalSize(60, 1));
    messageInput.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
    inputPanel.addComponent(messageInput);

    // ################### LÓGICA ALTERADA AQUI ###################
    Button sendButton = new Button("Enviar", () -> {
      String text = messageInput.getText();
      if (text.isEmpty())
        return;

      if (text.startsWith("@")) {
        // Para mensagens privadas, o servidor envia uma confirmação.
        // Apenas enviamos a mensagem para o servidor.
        String[] parts = text.split(" ", 2);
        if (parts.length == 2) {
          String targetNumber = parts[0].substring(1);
          out.println("PRIVATE:" + targetNumber + ":" + parts[1]);
        }
      } else {
        // Para broadcast, adicionamos a mensagem à nossa tela LOCALMENTE
        // para um feedback instantâneo.
        messagesListBox.addItem("[Você]: " + text, null);
        // E também enviamos ao servidor para que os outros recebam.
        out.println("BROADCAST:" + text);
      }

      // Limpa a caixa de texto após o envio
      messageInput.setText("");
      // Rola a lista para a última mensagem
      messagesListBox.setSelectedIndex(messagesListBox.getItemCount() - 1);
    });
    // #############################################################
    inputPanel.addComponent(sendButton);

    window.setComponent(mainContentPanel);
    gui.addWindow(window);
  }

  private static class ServerListener implements Runnable {
    private final BufferedReader in;

    public ServerListener(BufferedReader in) {
      this.in = in;
    }

    @Override
    public void run() {
      try {
        String serverMessage;
        while ((serverMessage = in.readLine()) != null) {
          final String msg = serverMessage;
          gui.getGUIThread().invokeLater(() -> {
            if (msg.startsWith("UPDATE_USERS:")) {
              usersListBox.clearItems();
              String[] users = msg.substring(13).split(",");
              for (String user : users) {
                if (!user.isEmpty()) {
                  usersListBox.addItem(user, null);
                }
              }
            } else {
              messagesListBox.addItem(msg, null);
              messagesListBox.setSelectedIndex(messagesListBox.getItemCount() - 1);
            }
          });
        }
      } catch (IOException e) {
        gui.getGUIThread()
            .invokeLater(() -> messagesListBox.addItem("[SISTEMA] Conexão com o servidor perdida.", null));
      }
    }
  }
}