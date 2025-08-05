import java.io.BufferedReader;

public class ServerListener implements Runnable {

    private final BufferedReader input;
    private InterfaceHandler interfaceHandler;

    public ServerListener(BufferedReader in, InterfaceHandler interfaceHandler) {
        this.input = in;
        this.interfaceHandler = interfaceHandler;
    }

    private void updateContacts(String message) {

        contacts.clear();
        contacts.put("-1", "Broadcast");

        String[] users = message.substring(13).split(",");

        for (String user : users) {

            if (user.isEmpty())
                continue;

            String[] userInfo = user.split(":");

            if (userInfo.length == 0)
                continue;

            if (userInfo[0].equals(userNumber))
                continue;

            contacts.put(userInfo[0], userInfo[1]);

        }
    }

    private void updateUserList() {
        usersListBox.clearItems();

        for (String key : contacts.keySet()) {
            usersListBox.addItem(contacts.get(key),
                    () -> {
                        selectedContactNumber = (String) contacts.keySet().toArray()[usersListBox.getSelectedIndex()];
                        chatLabel.setText("Mensagem > " + contacts.get(key));
                    });
        }
    }

    @Override
    public void run() {

        try {
            String serverMessage;
            while ((serverMessage = input.readLine()) != null) {
                final String msg = serverMessage;

                interfaceHandler.gui().getGUIThread().invokeLater(() -> {

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
            interfaceHandler.gui().getGUIThread()
                    .invokeLater(() -> messagesListBox.addItem("[SISTEMA] Conexão com o servidor perdida.", null));
        }
    }
}