
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

public class InterfaceHandler {

    private MultiWindowTextGUI gui;
    private Screen screen;

    private static ActionListBox messagesListBox;
    private static ActionListBox usersListBox;
    private static Label chatLabel;

    public InterfaceHandler() {

        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();

        screen = terminalFactory.createScreen();
        screen.startScreen();

        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.BLUE));
    }

    public String promptServerAddres() {
        return new TextInputDialogBuilder().setTitle("IP do Servidor")
                .setDescription("Digite o IP do servidor:").build().showDialog(gui);
    }

    public String promptUserNumber() {
        return new TextInputDialogBuilder().setTitle("Seu Número")
                .setDescription("Digite seu número?")
                .setValidationPattern(Pattern.compile("[0-9]+"), "Apenas números").build().showDialog(gui);
    }

    public String promptUserName() {
        return new TextInputDialogBuilder().setTitle("Seu Nome").setDescription("Digite seu nome:")
                .build().showDialog(gui);
    }

    public MultiWindowTextGUI gui() {
        return this.gui;
    }

    public Screen screen() {
        return this.screen;
    }

    public static BasicWindow chatInterface(String userName, String userNumber) {
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
                messagesListBox.addItem("[Você]: " + text, null);
            } else {
                out.println("PRIVATE:" + selectedContactNumber + ":" + text);
            }

            // Limpa a caixa de texto após o envio
            messageInput.setText("");
            // Rola a lista para a última mensagem
            messagesListBox.setSelectedIndex(messagesListBox.getItemCount() - 1);

        });
        // #############################################################
        inputPanel.addComponent(sendButton);

        window.setComponent(mainPanel);
        return window;
    }
}