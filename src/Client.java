
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Client {

  private static PrintWriter out;
  private static MultiWindowTextGUI gui;

  private static HashMap<String, String> contacts;
  private static HashMap<String, List<String>> messages;
  private static String selectedContactNumber = "0";
  private static InterfaceHandler interfaceHandler;

  private static String serverIp;
  private static String userName;
  private static String userNumber;

  public static void main(String[] args) throws IOException {

    contacts = new HashMap<String, String>();
    messages = new HashMap<String, List<String>>();
    interfaceHandler = new InterfaceHandler();

    // --- Configuração Inicial (sem alterações) ---

    try {
      userNumber = interfaceHandler.promptUserNumber();

      Socket socket = new Socket(serverIp, 12345);
      out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      out.println("REGISTER:" + userNumber + ":" + userName);

      interfaceHandler.gui().addWindow(interfaceHandler.chatInterface(userName, userNumber));
      new Thread(new ServerListener(in, interfaceHandler)).start();
      interfaceHandler.gui().waitForWindowToClose(gui.getWindows().iterator().next());

    } catch (IOException e) {
      System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
    } finally {
      if (interfaceHandler.screen() != null) {
        interfaceHandler.screen().stopScreen();
      }
    }
  }

}