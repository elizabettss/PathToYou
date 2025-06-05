package server;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import shared.XMLMessageBuilder;


public class ServerGUI {
    private JFrame frame;
    private JTextArea chatArea;                   

    private JTabbedPane tabbedPane;

    private JButton localChatButton;
    private DefaultListModel<String> convListModel;
    private JList<String> convList;

    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private Server server;

    public ServerGUI(Server server) {
        this.server = server;

        frame = new JFrame("Server Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);

        //Ліва панель
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.BLACK);
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        splitPane.setLeftComponent(chatScroll);

        // Права панель
        tabbedPane = new JTabbedPane();

        // Вкладка "Chats"
        JPanel chatsPanel = new JPanel(new BorderLayout());

        localChatButton = new JButton("Local Chat ");
        localChatButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        localChatButton.addActionListener(e -> {
            showLocalHistory();
            convList.clearSelection();
        });
        chatsPanel.add(localChatButton, BorderLayout.NORTH);

        // Список приватних бесід
        convListModel = new DefaultListModel<>();
        convList = new JList<>(convListModel);
        convList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane convScroll = new JScrollPane(convList);
        chatsPanel.add(convScroll, BorderLayout.CENTER);

        convList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int idx = convList.getSelectedIndex();
                    if (idx >= 0) {
                        String entry = convListModel.get(idx); 
                        String unlabeled = entry.substring(entry.indexOf(". ") + 2);
                        String[] parts = unlabeled.split(" <--> ");
                        if (parts.length == 2) {
                            String userA = parts[0];
                            String userB = parts[1];
                            showPrivateHistory(userA, userB);
                        }
                    } else {
                        showLocalHistory();
                    }
                }
            }
        });

        tabbedPane.addTab("Chats", chatsPanel);

        JPanel usersPanel = new JPanel(new BorderLayout());
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JScrollPane userScroll = new JScrollPane(userList);
        usersPanel.add(userScroll, BorderLayout.CENTER);
        tabbedPane.addTab("Users", usersPanel);

        splitPane.setRightComponent(tabbedPane);

        frame.add(splitPane);
        frame.setVisible(true);

        showLocalHistory();
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (tabbedPane.getSelectedIndex() == 0 && convList.getSelectedIndex() < 0) {
                chatArea.append(message + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }

    public void addConversation(String userA, String userB) {
        SwingUtilities.invokeLater(() -> {
            String convName = userA + " <--> " + userB;

            for (int i = 0; i < convListModel.size(); i++) {
                String existing = convListModel.get(i);
                String unlabeled = existing.contains(". ")
                        ? existing.substring(existing.indexOf(". ") + 2)
                        : existing;
                if (unlabeled.equalsIgnoreCase(convName)) {
                    return;
                }
            }

            int newNumber = convListModel.size() + 1;
            String numbered = newNumber + ". " + convName;
            convListModel.add(0, numbered);

            for (int i = 0; i < convListModel.size(); i++) {
                int displayNum = convListModel.size() - i;
                String entry = convListModel.get(i);
                String unlabeled = entry.contains(". ")
                        ? entry.substring(entry.indexOf(". ") + 2)
                        : entry;
                convListModel.set(i, displayNum + ". " + unlabeled);
            }
        });
    }

    public void renameConversation(String oldName, String newName) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < convListModel.size(); i++) {
                String entry = convListModel.get(i); 
                String unlabeled = entry.contains(". ")
                        ? entry.substring(entry.indexOf(". ") + 2)
                        : entry;
                if (unlabeled.toLowerCase().contains(oldName.toLowerCase())) {
                    String updated = unlabeled.replaceAll("(?i)" + oldName, newName);
                    int number = convListModel.size() - i;
                    convListModel.set(i, number + ". " + updated);
                }
            }
        });
    }

    public void addUser(String userName) {
        SwingUtilities.invokeLater(() -> { 
            for (int i = 0; i < userListModel.size(); i++) {
                if (userListModel.get(i).equalsIgnoreCase(userName)) {
                    return;
                }
            }
            userListModel.addElement(userName);
        });
    }

    public void removeUser(String userName) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < userListModel.size(); i++) {
                if (userListModel.get(i).equalsIgnoreCase(userName)) {
                    userListModel.remove(i);
                    break;
                }
            }
        });
    }

    public void renameUser(String oldName, String newName) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < userListModel.size(); i++) {
                if (userListModel.get(i).equalsIgnoreCase(oldName)) {
                    userListModel.set(i, newName);
                    break;
                }
            }
            renameConversation(oldName, newName);
        });
    }

    private void showLocalHistory() {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            for (String line : server.getLocalHistory()) {
                chatArea.append(line + "\n");
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }


    private void showPrivateHistory(String userA, String userB) {
        SwingUtilities.invokeLater(() -> {
            chatArea.setText("");
            for (String line : server.getPrivateHistory(userA, userB)) {
                chatArea.append(line + "\n");
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
}
