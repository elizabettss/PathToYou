package client;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.border.Border;
import shared.XMLMessageBuilder;


public class ChatClientGUI {

    private static class MessageEntry {
        String text;     
        String time;   

        MessageEntry(String text, String time) {
            this.text = text;
            this.time = time;
        }
    }

    private String userName;
    private String mood = "😊";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String currentChat = "Local Chat";

    private Map<String, java.util.List<MessageEntry>> chatMessages = new HashMap<>();

    private Map<String, String> userMoods = new HashMap<>();

    private JFrame frame;
    private JLabel headerLabel;
    private JLabel dateLabel;
    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private JButton attachButton;
    private JTextField inputField;
    private JButton sendButton;
    private JTextField searchField;
    private JPanel friendPanel;
    private JScrollPane friendScroll;
    private JPanel profilePanel;
    private JLabel profileLabel;
    private JPopupMenu profileMenu;
    private Map<String, JPanel> userPanels = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, EEEE", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");


    public ChatClientGUI(String host, int port, String userName) throws IOException {
        this.userName = userName.trim();

        this.socket = new Socket(host, port);
        this.out    = new PrintWriter(socket.getOutputStream(), true);
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String joinXml = XMLMessageBuilder.buildJoinMessage(this.userName, this.mood);
        out.println(joinXml);

        String firstResponse = in.readLine();
        if (firstResponse != null) {
            String firstType = XMLMessageBuilder.parseType(firstResponse);
            if ("error".equalsIgnoreCase(firstType)) {
                String body = XMLMessageBuilder.parseBody(firstResponse);
                JOptionPane.showMessageDialog(
                        null,
                        "Connection failed: " + body,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                socket.close();
                throw new IOException("Server error: " + body);
            }
        }

        initGUI();
        chatMessages.put("Local Chat", new ArrayList<>());
        if (firstResponse != null) {
            XMLMessageParser.parseMessage(firstResponse, this);
        }

        new Thread(this::listenFromServer).start();
    }

    private void initGUI() {
        frame = new JFrame("PathToYou – Local Chat");
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel appHeader = new JPanel(new BorderLayout());
        appHeader.setBackground(new Color(36, 73, 125));
        appHeader.setPreferredSize(new Dimension(0, 50));
        JLabel titleLabel = new JLabel("\u2022\u2022\u2022 PathToYou \u2022\u2022\u2022", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        appHeader.add(titleLabel, BorderLayout.CENTER);
        frame.add(appHeader, BorderLayout.NORTH);

        JPanel leftMain = new JPanel(new BorderLayout());

        headerLabel = new JLabel("Local Chat", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Serif", Font.BOLD, 35));
        headerLabel.setOpaque(true);
        headerLabel.setBackground(new Color(173, 216, 230));  
        headerLabel.setForeground(new Color(36, 73, 125));   
        headerLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(36, 73, 125)),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)
        ));

        dateLabel = new JLabel(LocalDate.now().format(DATE_FORMAT), SwingConstants.CENTER);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateLabel.setOpaque(true);
        dateLabel.setBackground(Color.WHITE);
        dateLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel containerTop = new JPanel(new BorderLayout());
        containerTop.add(headerLabel, BorderLayout.NORTH);
        containerTop.add(dateLabel, BorderLayout.SOUTH);
        leftMain.add(containerTop, BorderLayout.NORTH);

        messagePanel = new JPanel();
        messagePanel.setBackground(Color.WHITE);
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        leftMain.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(173, 216, 230));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(5, 0, 0, 0, new Color(36, 73, 125)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        attachButton = new JButton("📎");
        attachButton.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        attachButton.setBackground(new Color(173, 216, 230));
        attachButton.setFocusPainted(false);
        attachButton.addActionListener(e -> showAttachMenu());
        inputPanel.add(attachButton, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputField.addActionListener(e -> sendMessage());
        inputPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("▶");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        sendButton.setFocusPainted(false);
        sendButton.setBackground(new Color(173, 216, 230));
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        leftMain.add(inputPanel, BorderLayout.SOUTH);

        JPanel rightMain = new JPanel(new BorderLayout());
        rightMain.setPreferredSize(new Dimension(250, 0));
        rightMain.setBackground(Color.WHITE);  

        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(Color.WHITE);  
        searchContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel searchTitle = new JLabel("Find your friend", SwingConstants.CENTER);
        searchTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchTitle.setForeground(new Color(36, 73, 125));
        searchContainer.add(searchTitle, BorderLayout.NORTH);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setToolTipText("Search by Name");
        searchField.setBackground(new Color(36, 73, 125));
        searchField.setForeground(Color.WHITE);
        searchField.setCaretColor(Color.WHITE);
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 5, 0, new Color(36, 73, 125)),
        BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        searchField.addActionListener(e -> {
            String text = searchField.getText().trim();
            if (!text.isEmpty()) {
                searchUserAndOpenChat(text);
            }
        });
        searchContainer.add(searchField, BorderLayout.CENTER);

        rightMain.add(searchContainer, BorderLayout.NORTH);

        friendPanel = new JPanel();
        friendPanel.setLayout(new BoxLayout(friendPanel, BoxLayout.Y_AXIS));
        friendPanel.setBackground(Color.WHITE);  

        friendScroll = new JScrollPane(friendPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        friendScroll.getViewport().setBackground(Color.WHITE);

        rightMain.add(friendScroll, BorderLayout.CENTER);

        {
            JPanel lcPanel = new JPanel(new BorderLayout());
            lcPanel.setBackground(Color.WHITE);
            lcPanel.setPreferredSize(new Dimension(0, 25));
            lcPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            lcPanel.setBorder(BorderFactory.createLineBorder(new Color(36, 73, 125), 2, true));

            JLabel lcLabel = new JLabel("Local Chat", SwingConstants.CENTER);
            lcLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18)); 
            lcLabel.setForeground(new Color(36, 73, 125));
            lcLabel.setOpaque(true);
            lcLabel.setBackground(new Color(173, 216, 230));
            lcLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lcLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switchChat("Local Chat");
                }
            });

            lcPanel.add(lcLabel, BorderLayout.CENTER);
            friendPanel.add(lcPanel);
            friendPanel.add(Box.createVerticalStrut(5));
        }

        profilePanel = new JPanel(new BorderLayout());
        profilePanel.setBackground(new Color(36, 73, 125)); 

        profileLabel = new JLabel("me (" + userName + ") " + mood, SwingConstants.CENTER);
        profileLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profileLabel.setForeground(Color.WHITE);
        profileLabel.setOpaque(true);
        profileLabel.setBackground(new Color(36, 73, 125));
        profileLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        profileLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        profileMenu = new JPopupMenu();
        JMenuItem renameItem     = new JMenuItem("Change my name");
        JMenuItem changeMoodItem = new JMenuItem("Set mood");
        JMenuItem exitItem       = new JMenuItem("Exit");
        renameItem.addActionListener(e -> changeName());
        changeMoodItem.addActionListener(e -> setMood());
        exitItem.addActionListener(e -> {
            out.println(XMLMessageBuilder.buildLeaveMessage(userName));
            System.exit(0);
        });
        profileMenu.add(renameItem);
        profileMenu.add(changeMoodItem);
        profileMenu.add(exitItem);

        profileLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                profileMenu.show(profileLabel, e.getX(), e.getY());
            }
        });
        profilePanel.add(profileLabel, BorderLayout.CENTER);
        rightMain.add(profilePanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftMain,
                rightMain
        );
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerSize(4);
        splitPane.setBackground(new Color(36, 73, 125));
        BasicSplitPaneUI ui = (BasicSplitPaneUI) splitPane.getUI();
        ui.getDivider().setBackground(new Color(36, 73, 125));

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setVisible(true);


        dateLabel.setText(LocalDate.now().format(DATE_FORMAT));
        showHistoryForCurrentChat();
    }

    private void showAttachMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem reactionItem = new JMenuItem("Send Reaction");
        JMenuItem fileItem     = new JMenuItem("Send File");

        reactionItem.addActionListener(e -> showReactionDialog());
        fileItem.addActionListener(e -> sendFile());

        menu.add(reactionItem);
        menu.add(fileItem);
        menu.show(attachButton, 0, attachButton.getHeight());
    }

    private void showReactionDialog() {
        String[] reactions = {"😊", "😢", "😠", "🤩", "😴", "😐"};
        String selected = (String) JOptionPane.showInputDialog(
                frame,
                "Choose your reaction:",
                "Send Reaction",
                JOptionPane.PLAIN_MESSAGE,
                null,
                reactions,
                reactions[0]
        );
        if (selected != null && !selected.isEmpty()) {
            sendEmotionToCurrentChat(selected);
        }
    }

    private void sendEmotionToCurrentChat(String emoji) {
        String xml;
        if ("Local Chat".equalsIgnoreCase(currentChat)) {
            xml = XMLMessageBuilder.buildEmotionMessage(userName, "Local Chat", emoji);
        } else {
            xml = XMLMessageBuilder.buildEmotionMessage(userName, currentChat, emoji);
        }
        out.println(xml);
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                String encoded = Base64.getEncoder().encodeToString(content);
                String fileName = file.getName();

                String xml;
                if ("Local Chat".equalsIgnoreCase(currentChat)) {
                    xml = XMLMessageBuilder.buildFileMessage(userName, "Local Chat", fileName, encoded);
                } else {
                    xml = XMLMessageBuilder.buildFileMessage(userName, currentChat, fileName, encoded);
                }
                out.println(xml);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error reading file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String xml;
        if ("Local Chat".equalsIgnoreCase(currentChat)) {
            xml = XMLMessageBuilder.buildBroadcastMessage(userName, text);
        } else {
            xml = XMLMessageBuilder.buildPrivateMessage(userName, currentChat, text);
        }
        out.println(xml);
        inputField.setText("");
    }

    private void listenFromServer() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                XMLMessageParser.parseMessage(line, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            addMessage("Local Chat", "❌ Disconnected from server");
        }
    }

    public void switchChat(String chatName) {
        this.currentChat = chatName;

        if ("Local Chat".equalsIgnoreCase(chatName)) {
            headerLabel.setText("Local Chat");
        } else {
            String emo = userMoods.getOrDefault(chatName, "😊");
            headerLabel.setText(chatName + " " + emo);
        }

        showHistoryForCurrentChat();
    }

    private void showHistoryForCurrentChat() {
        messagePanel.removeAll();

        dateLabel.setText(LocalDate.now().format(DATE_FORMAT));

        java.util.List<MessageEntry> history =
                chatMessages.getOrDefault(currentChat, Collections.emptyList());
        for (MessageEntry entry : history) {
            String html = "<html><body style='font-family:Segoe UI; font-size:12px;'>"
                        + entry.text
                        + "<br><span style='font-size:10px; color:gray; white-space:nowrap;'>"
                        + entry.time
                        + "</span></body></html>";
            JLabel lbl = new JLabel(html);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            messagePanel.add(lbl);
        }
        messagePanel.revalidate();
        messagePanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void addMessage(String chatName, String msg) {
        dateLabel.setText(LocalDate.now().format(DATE_FORMAT));
        String time = LocalDateTime.now().format(TIME_FORMAT);

        chatMessages
                .computeIfAbsent(chatName, k -> new ArrayList<>())
                .add(new MessageEntry(msg, time));

        if (chatName.equalsIgnoreCase(currentChat)) {
            String html = "<html><body style='font-family:Segoe UI; font-size:12px;'>"
                        + msg
                        + "<br><span style='font-size:10px; color:gray; white-space:nowrap;'>"
                        + time
                        + "</span></body></html>";
            JLabel lbl = new JLabel(html);
            lbl.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            messagePanel.add(lbl);
            messagePanel.revalidate();
            messagePanel.repaint();
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        }
    }

    private void searchUserAndOpenChat(String toUser) {
        boolean found = false;
        for (String other : userPanels.keySet()) {
            if (other.equalsIgnoreCase(toUser)) {
                switchChat(other);
                found = true;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(frame, "User \"" + toUser + "\" not found.", "Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public void addUser(String name) {
        SwingUtilities.invokeLater(() -> {
            for (String existing : userPanels.keySet()) {
                if (existing.equalsIgnoreCase(name)) {
                    return;
                }
            }
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(173, 216, 230));  
            panel.setPreferredSize(new Dimension(0, 50));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            panel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

            String displayMood = userMoods.getOrDefault(name, "😊");
            JLabel nameLabel = new JLabel(name + " " + displayMood, SwingConstants.LEFT);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            nameLabel.setForeground(new Color(36, 73, 125));
            nameLabel.setOpaque(false);
            nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            nameLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switchChat(name);
                }
            });
            nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

            JLabel hugIcon = new JLabel("Send a hug", SwingConstants.CENTER);
            hugIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            hugIcon.setForeground(new Color(36, 73, 125));  
            hugIcon.setOpaque(true);
            hugIcon.setBackground(new Color(173, 216, 230)); 
            hugIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            Border line = BorderFactory.createLineBorder(new Color(36, 73, 125), 1); 
            Border padding = BorderFactory.createEmptyBorder(5, 15, 5, 15);
            hugIcon.setBorder(BorderFactory.createCompoundBorder(line, padding));
            hugIcon.setPreferredSize(new Dimension(100, 50));
            hugIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    sendHug(name);  
                }
            });

            panel.add(nameLabel, BorderLayout.CENTER);
            panel.add(hugIcon, BorderLayout.EAST);

            friendPanel.add(panel);
            friendPanel.add(Box.createVerticalStrut(5));
            userPanels.put(name, panel);

            friendPanel.revalidate();
            friendPanel.repaint();
        });
    }

    public void removeUser(String name) {
        SwingUtilities.invokeLater(() -> {
            JPanel panel = userPanels.remove(name);
            if (panel != null) {
                friendPanel.remove(panel);
                friendPanel.revalidate();
                friendPanel.repaint();
            }
        });
    }

    public void renameUser(String oldName, String newName) {
        SwingUtilities.invokeLater(() -> {

            String currentMood = userMoods.getOrDefault(oldName, "😊");
            userMoods.remove(oldName);
            userMoods.put(newName, currentMood);

            if (chatMessages.containsKey(oldName)) {
                java.util.List<MessageEntry> hist = chatMessages.remove(oldName);
                chatMessages.put(newName, hist);
            }

            boolean wasCurrent = oldName.equalsIgnoreCase(currentChat);
            if (wasCurrent) {
                currentChat = newName;
                headerLabel.setText(newName + " " + currentMood);
            }

            JPanel panel = userPanels.remove(oldName);
            if (panel != null) {
                friendPanel.remove(panel);

                JPanel newPanel = new JPanel(new BorderLayout());
                newPanel.setBackground(new Color(173, 216, 230));
                newPanel.setPreferredSize(new Dimension(0, 50));
                newPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                newPanel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

                JLabel nameLabel = new JLabel(newName + " " + currentMood, SwingConstants.LEFT);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
                nameLabel.setForeground(new Color(36, 73, 125));
                nameLabel.setOpaque(false);
                nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                nameLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        switchChat(newName);
                    }
                });
                nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

                JLabel hugIcon = new JLabel("Send a hug", SwingConstants.CENTER);
                hugIcon.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                hugIcon.setForeground(new Color(36, 73, 125));
                hugIcon.setOpaque(false);
                hugIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                hugIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        sendHug(newName);
                    }
                });
                hugIcon.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 10));
                hugIcon.setPreferredSize(new Dimension(100, 50));

                newPanel.add(nameLabel, BorderLayout.CENTER);
                newPanel.add(hugIcon, BorderLayout.EAST);

                userPanels.put(newName, newPanel);
                friendPanel.add(newPanel);
                friendPanel.add(Box.createVerticalStrut(5));
            }
            friendPanel.revalidate();
            friendPanel.repaint();
        });
    }

    public void updateMood(String name, String newMood) {
        userMoods.put(name, newMood);
        SwingUtilities.invokeLater(() -> {
            JPanel panel = userPanels.get(name);
            if (panel != null) {
                JLabel nameLabel = (JLabel) panel.getComponent(0);
                nameLabel.setText(name + " " + newMood);
            }

            if (name.equalsIgnoreCase(currentChat)) {
                headerLabel.setText(name + " " + newMood);
            }
        });
    }

    private void sendHug(String toUser) {
        if (toUser == null || toUser.isEmpty()) return;
        String hugXml = XMLMessageBuilder.buildHugMessage(userName, toUser);
        out.println(hugXml);
    }
    private void changeName() {
        String newName = JOptionPane.showInputDialog(frame, "Enter new name:", userName);
        if (newName != null) {
            newName = newName.trim();
            if (!newName.isEmpty()) {
                String renameXml = XMLMessageBuilder.buildRenameMessage(userName, newName);
                out.println(renameXml);
                userName = newName;
                profileLabel.setText("me (" + userName + ") " + mood);
            }
        }
    }

    private void setMood() {
        Object[] options = { "😊 Happy", "😢 Sad", "😠 Angry", "🤩 Excited", "😴 Sleepy", "😐 Neutral" };
        String selected = (String) JOptionPane.showInputDialog(
                frame,
                "Select your mood:",
                "Set Mood",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );
        if (selected != null) {
            String newMood = selected.split(" ")[0];
            mood = newMood;
            String moodXml = XMLMessageBuilder.buildMoodChangeMessage(userName, mood);
            out.println(moodXml);
            profileLabel.setText("me (" + userName + ") " + mood);
            if (currentChat.equals("Local Chat")) {
                headerLabel.setText("Local Chat");
            }
        }
    }
    public String getUserName() {
        return userName;
    }
}
