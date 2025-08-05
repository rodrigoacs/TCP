
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
import java.util.HashMap;
import java.util.regex.Pattern;

public class Client {

  private static PrintWriter out;
  private static MultiWindowTextGUI gui;
  private static ActionListBox messagesListBox;
  private static ActionListBox usersListBox;
  private static Label chatLabel;
  private static HashMap<String, String> contacts;

  private static String selectedContactNumber = "0";
  private static String userName;
  private static String userNumber;

  public static void main(String[] args) throws IOException {

    contacts = new HashMap<String, String>();

    // --- Configuração Inicial (sem alterações) ---
    DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
    Screen screen = null;
    try {
      screen = terminalFactory.createScreen();
      screen.startScreen();

      gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));

      String serverIp = "localhost";

      // String serverIp = new TextInputDialogBuilder().setTitle("IP do Servidor")
      // .setDescription("Digite o IP do servidor (ex:
      // localhost)").build().showDialog(gui);
      // if (serverIp == null)
      // return;
      userNumber = new TextInputDialogBuilder().setTitle("Seu Número")
          .setDescription("Digite seu número (ex: 101)")
          .setValidationPattern(Pattern.compile("[0-9]+"), "Apenas números").build().showDialog(gui);
      if (userNumber == null)
        return;
      userName = new TextInputDialogBuilder().setTitle("Seu Nome").setDescription("Digite seu nome (ex: Fulano)")
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
    BasicWindow window = new BasicWindow("Terminal Chat - " + userName + "(" + userNumber + ")");
    window.setHints(Arrays.asList(Window.Hint.EXPANDED));

    Panel mainPanel = new Panel(new GridLayout(2));

    Panel usersPanel = new Panel();
    usersListBox = new ActionListBox(new TerminalSize(25, 20));
    usersPanel.addComponent(new Label("Conversas"));
    usersPanel.addComponent(usersListBox.withBorder(Borders.singleLine()));
    mainPanel.addComponent(usersPanel.withBorder(Borders.doubleLine()));

    Panel chatPanel = new Panel(new LinearLayout());
    mainPanel.addComponent(chatPanel.withBorder(Borders.doubleLine()));

    messagesListBox = new ActionListBox(new TerminalSize(50, 15));

    chatLabel = new Label("Mensagens");

    Panel messageViewer = new Panel();
    messageViewer.addComponent(chatLabel);
    messageViewer.addComponent(messagesListBox.withBorder(Borders.singleLine()));
    chatPanel.addComponent(messageViewer);

    Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
    chatPanel.addComponent(inputPanel, BorderLayout.Location.BOTTOM);
    TextBox messageInput = new TextBox(new TerminalSize(50, 1));
    messageInput.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
    inputPanel.addComponent(messageInput);

    // ################### LÓGICA ALTERADA AQUI ###################
    Button sendButton = new Button("Enviar", () -> {
      String text = messageInput.getText();
      if (text.isEmpty())
        return;

      if (selectedContactNumber == "-1") {
        out.println("BROADCAST:" + text);
      } else {
        out.println("PRIVATE:" + selectedContactNumber + ":" + text);
        messagesListBox.addItem("[Você]: " + text, null);
      }

      // Limpa a caixa de texto após o envio
      messageInput.setText("");
      // Rola a lista para a última mensagem
      messagesListBox.setSelectedIndex(messagesListBox.getItemCount() - 1);

    });
    // #############################################################
    inputPanel.addComponent(sendButton);

    window.setComponent(mainPanel);
    gui.addWindow(window);
  }

  private static class ServerListener implements Runnable {
    private final BufferedReader in;

    public ServerListener(BufferedReader in) {
      this.in = in;
    }

    private void updateContacts(String message) {

      contacts.clear();

      String[] users = message.substring(13).split(",");

      for (String user : users) {

        if (user.isEmpty())
          continue;

        String[] userInfo = user.split(":");

        if (userInfo.length == 0)
          continue;

        if (userInfo[0] == userNumber)
          continue;

        contacts.put(userInfo[0], userInfo[1]);

      }
    }

    private void updateUserList() {
      usersListBox.clearItems();
      usersListBox.addItem("Broadcast", null);

      for (String key : contacts.keySet()) {
        usersListBox.addItem(contacts.get(key),
            () -> {
              int selectedIndex = usersListBox.getSelectedIndex();

              if (usersListBox.getSelectedIndex() == 0) {
                selectedContactNumber = "-1"; // broadcast
                return;
              }

              String contactKey = (String) contacts.keySet().toArray()[selectedIndex - 1];
              selectedContactNumber = contactKey;
              chatLabel.setText("Mensagem > " + contacts.get(contactKey));
            });
      }
    }

    @Override
    public void run() {

      try {
        String serverMessage;
        while ((serverMessage = in.readLine()) != null) {
          final String msg = serverMessage;

          gui.getGUIThread().invokeLater(() -> {

            if (msg.startsWith("UPDATE_USERS:")) {
              updateContacts(msg);
              updateUserList();
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