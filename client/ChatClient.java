package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import shared.XMLMessageBuilder;


public class ChatClient {
    private String userName;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendButton;
    private JButton attachButton;
    private JPanel friendPanel;
    private JPanel profilePanel;
    private JLabel profileLabel;
    private String mood = "😊";
    private String currentDate;

    private static final Map<String, String> moodOptions = Map.of(
        "Happy 😊", "😊",
        "Sad 😢", "😢",
        "Angry 😠", "😠",
        "Excited 🤩", "🤩",
        "Sleepy 😴", "😴",
        "Neutral 😐", "😐"
    );

    public ChatClient(String host, int port, String userName) {
        this.userName = userName;
        try {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Не вдалося підключитися до сервера:\n" + e.getMessage(),
                    "Помилка з'єднання",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        this.currentDate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm (EEEE)", Locale.forLanguageTag("uk")));
        out.println(XMLMessageBuilder.buildJoinMessage(userName, mood));
        initGUI();
        listen();
    }

    private void initGUI() {
        frame = new JFrame("PathToYou – Local Chat");
        frame.setSize(1000, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(36, 73, 125));
        JLabel titleLabel = new JLabel("\u2022\u2022\u2022 PathToYou \u2022\u2022\u2022", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());

        JPanel chatHeader = new JPanel(new BorderLayout());
        JLabel localLabel = new JLabel("  👥 Local Chat");
        localLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatHeader.add(localLabel, BorderLayout.WEST);

        JLabel dateLabel = new JLabel(currentDate);
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        dateLabel.setForeground(Color.GRAY);
        chatHeader.add(dateLabel, BorderLayout.EAST);

        centerPanel.add(chatHeader, BorderLayout.NORTH);
 
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel leftInput = new JPanel(new BorderLayout());
        attachButton = new JButton("+");
        attachButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        attachButton.addActionListener(e -> showAttachMenu());
        leftInput.add(attachButton, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.addActionListener(e -> sendMessage());
        leftInput.add(inputField, BorderLayout.CENTER);

        inputPanel.add(leftInput, BorderLayout.CENTER);

        sendButton = new JButton("▶");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));

        JTextField searchField = new JTextField("Find your friend");
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Find your friend")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Find your friend");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        rightPanel.add(searchField, BorderLayout.NORTH);

        friendPanel = new JPanel();
        friendPanel.setLayout(new BoxLayout(friendPanel, BoxLayout.Y_AXIS));
        JScrollPane friendScroll = new JScrollPane(friendPanel);
        rightPanel.add(friendScroll, BorderLayout.CENTER);

        profilePanel = new JPanel();
        profilePanel.setBackground(new Color(200, 220, 245));
        profilePanel.setLayout(new BorderLayout());

        profileLabel = new JLabel("me (" + userName + ") " + mood, SwingConstants.CENTER);
        profileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profileLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPopupMenu profileMenu = new JPopupMenu();
        JMenuItem rename = new JMenuItem("Change my name");
        JMenuItem changeMood = new JMenuItem("Set mood");
        JMenuItem exit = new JMenuItem("Exit");

        rename.addActionListener(e -> changeName());
        changeMood.addActionListener(e -> setMood());
        exit.addActionListener(e -> {
            out.println(XMLMessageBuilder.buildLeaveMessage(userName));
            System.exit(0);
        });

        profileMenu.add(rename);
        profileMenu.add(changeMood);
        profileMenu.add(exit);

        profileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileLabel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                profileMenu.show(profileLabel, e.getX(), e.getY());
            }
        });

        profilePanel.add(profileLabel, BorderLayout.CENTER);
        rightPanel.add(profilePanel, BorderLayout.SOUTH);

        frame.add(rightPanel, BorderLayout.EAST);
        frame.setVisible(true);
    }

    private void showAttachMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem("Send emotion"));
        menu.add(new JMenuItem("Send photo/video"));
        menu.add(new JMenuItem("Send file"));
        menu.show(attachButton, 0, attachButton.getHeight());
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            String displayText = userName + " " + mood + " [" + timestamp + "]: " + message;
            String xmlMessage = XMLMessageBuilder.buildBroadcastMessage(userName, displayText);

            JLabel formatted = new JLabel(displayText);
            formatted.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            messagePanel.add(formatted);
            messagePanel.revalidate();
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

            inputField.setText("");
            out.println(xmlMessage);
        }
    }

    private void listen() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String type = XMLMessageBuilder.parseType(line);
                    String from = XMLMessageBuilder.parseSender(line);
                    String body = XMLMessageBuilder.parseBody(line);

                    if (type.equals("join")) {
                        JLabel joined = new JLabel("🔵 " + from + " has joined the chat");
                        joined.setForeground(Color.BLUE);
                        messagePanel.add(joined);
                    } else if (type.equals("leave")) {
                        JLabel left = new JLabel("🔴 " + from + " has left the chat");
                        left.setForeground(Color.RED);
                        messagePanel.add(left);
                    } else {
                        JLabel msg = new JLabel(body);
                        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                        messagePanel.add(msg);
                    }
                    messagePanel.revalidate();
                    SwingUtilities.invokeLater(() -> {
                        JScrollBar vertical = scrollPane.getVerticalScrollBar();
                        vertical.setValue(vertical.getMaximum());
                    });
                }
            } catch (IOException e) {
                JLabel error = new JLabel("[Disconnected from server]");
                error.setForeground(Color.RED);
                messagePanel.add(error);
                messagePanel.revalidate();
            }
        }).start();
    }

    private void changeName() {
        String newName = JOptionPane.showInputDialog(frame, null, "Enter new name:", JOptionPane.PLAIN_MESSAGE);
        if (newName != null && !newName.trim().isEmpty()) {
            out.println(XMLMessageBuilder.buildRenameMessage(userName, newName.trim()));
            userName = newName.trim();
            profileLabel.setText("me (" + userName + ") " + mood);
        }
    }

    private void setMood() {
        Object[] options = moodOptions.keySet().toArray();
        String selected = (String) JOptionPane.showInputDialog(
            frame,
            "Select your mood:",
            "Set Mood",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]);

        if (selected != null && moodOptions.containsKey(selected)) {
            mood = moodOptions.get(selected);
            out.println(XMLMessageBuilder.buildMoodChangeMessage(userName, mood));
            profileLabel.setText("me (" + userName + ") " + mood);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }

    static class LoginFrame extends JFrame {
        private JTextField hostField;
        private JTextField portField;
        private JTextField userField;
        private JButton connectButton;
        private JButton cancelButton;

        public LoginFrame() {
            super("Login to PathToYou");
            setSize(350, 200);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridBagLayout());
            setLocationRelativeTo(null);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            add(new JLabel("Server IP:"), gbc);

            hostField = new JTextField();
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            add(hostField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            add(new JLabel("Port:"), gbc);

            portField = new JTextField();
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            add(portField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 1;
            add(new JLabel("Username:"), gbc);

            userField = new JTextField();
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            add(userField, gbc);

            connectButton = new JButton("Connect");
            gbc.gridx = 1;
            gbc.gridy = 3;
            gbc.gridwidth = 1;
            add(connectButton, gbc);

            cancelButton = new JButton("Cancel");
            gbc.gridx = 2;
            gbc.gridy = 3;
            gbc.gridwidth = 1;
            add(cancelButton, gbc);

            connectButton.addActionListener(e -> onConnect());
            cancelButton.addActionListener(e -> System.exit(0));

            setVisible(true);
        }

        private void onConnect() {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            String userName = userField.getText().trim();

            if (host.isEmpty() || portStr.isEmpty() || userName.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Усі поля мають бути заповнені!",
                        "Помилка",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Порт має бути числом!",
                        "Помилка",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            new ChatClient(host, port, userName);
            dispose();
        }
    }
}
