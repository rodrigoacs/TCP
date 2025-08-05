import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Arrays;

public class TerminalChat {

    public static void main(String[] args) throws IOException {
        DefaultTerminalFactory terminalFactory = new DefaultTerminalFactory();
        Terminal terminal = null;
        try {
            terminal = terminalFactory.createTerminal();
            Screen screen = new TerminalScreen(terminal);
            screen.startScreen();

            // Crie a GUI principal
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));

            // Crie a janela principal
            BasicWindow window = new BasicWindow("Terminal Chat");
            window.setHints(Arrays.asList(Window.Hint.EXPANDED));

            // --- Painel de Conteúdo Principal com BorderLayout ---
            // Este layout divide a tela em Centro e Fundo (Bottom)
            Panel mainContentPanel = new Panel(new BorderLayout());

            // --- Seção Superior (Centro do BorderLayout) ---
            // Painel que conterá as mensagens e a lista de usuários, lado a lado
            Panel topSectionPanel = new Panel(new GridLayout(2));
            mainContentPanel.addComponent(topSectionPanel, BorderLayout.Location.CENTER);

            // Painel de Mensagens (à esquerda)
            Panel messagesPanel = new Panel();
            messagesPanel.addComponent(new Label("MENSAGENS").withBorder(Borders.singleLine()));
            ActionListBox messagesListBox = new ActionListBox(new TerminalSize(50, 20));
            messagesListBox.addItem("Fulano: Olá pessoal", null);
            messagesListBox.addItem("Ciclano: Tudo bem!", null);
            messagesPanel.addComponent(messagesListBox);

            topSectionPanel.addComponent(messagesPanel.withBorder(Borders.singleLine()));

            // Painel de Usuários (à direita)
            Panel usersPanel = new Panel();
            usersPanel.addComponent(new Label("USUARIOS").withBorder(Borders.singleLine()));
            ActionListBox usersListBox = new ActionListBox(new TerminalSize(20, 20));
            usersListBox.addItem("Fulano: Online", null);
            usersListBox.addItem("Ciclano: Online", null);
            usersListBox.addItem("Beltrano: Offline", null);
            usersPanel.addComponent(usersListBox);

            topSectionPanel.addComponent(usersPanel.withBorder(Borders.singleLine()));

            // --- Seção Inferior (Fundo do BorderLayout) ---
            // Painel que conterá a caixa de texto e o botão de enviar
            Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            mainContentPanel.addComponent(inputPanel, BorderLayout.Location.BOTTOM);

            TextBox messageInput = new TextBox(new TerminalSize(60, 1));
            messageInput.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            inputPanel.addComponent(messageInput);

            Button sendButton = new Button("Enviar", () -> {
                String message = messageInput.getText();
                if (!message.trim().isEmpty()) {
                    messagesListBox.addItem("Você: " + message, null);
                    messageInput.setText("");
                }
            });
            inputPanel.addComponent(sendButton);

            // Define o painel de conteúdo principal na janela
            window.setComponent(mainContentPanel);

            // Adiciona a janela à GUI e aguarda
            gui.addWindowAndWait(window);

        } finally {
            if (terminal != null) {
                terminal.close();
            }
        }
    }
}