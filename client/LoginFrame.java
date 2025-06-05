package client;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import shared.XMLMessageBuilder;


public class LoginFrame extends JFrame {
    private JTextField ipField;
    private JTextField nameField;

    public LoginFrame() {
        setTitle("PathToYou");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 240);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());


        // Верхний заголовок
        JLabel header = new JLabel("\u2022\u2022\u2022 PathToYou \u2022\u2022\u2022", SwingConstants.CENTER);
        header.setOpaque(true);
        header.setBackground(new Color(36, 73, 125));       
        header.setForeground(Color.WHITE);                
        header.setFont(new Font("Serif", Font.BOLD, 30)); 
        add(header, BorderLayout.NORTH);

        // Центральная панель с полями и кнопками
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(173, 216, 230)); 
       
        panel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(36, 73, 125), 2, true), 
            BorderFactory.createEmptyBorder(15, 20, 15, 20)  
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel ipLabel = new JLabel("ip - address:");
        ipLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        ipLabel.setForeground(new Color(36, 73, 125));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(ipLabel, gbc);

        ipField = new JTextField();
        ipField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ipField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(36, 73, 125), 2, true), 
            BorderFactory.createEmptyBorder(4, 8, 4, 8)       
        ));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        panel.add(ipField, gbc);

        JLabel nameLabel = new JLabel("username:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(new Color(36, 73, 125));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(nameLabel, gbc);

        nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(36, 73, 125), 2, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        panel.add(nameField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false); 

        JButton continueButton = new JButton("Continue");
        continueButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        continueButton.setBackground(new Color(36, 73, 125)); 
        continueButton.setForeground(Color.WHITE);             
        continueButton.setFocusPainted(false);
        continueButton.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(36, 73, 125), 2, true),
            BorderFactory.createEmptyBorder(5, 20, 5, 20)
        ));
        continueButton.setOpaque(true);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setBackground(new Color(173, 216, 230));              
        cancelButton.setForeground(new Color(36, 73, 125));   
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(36, 73, 125), 2, true),
            BorderFactory.createEmptyBorder(5, 20, 5, 20)
        ));
        cancelButton.setOpaque(true);

        continueButton.addActionListener((ActionEvent e) -> connect());
        cancelButton.addActionListener(e -> System.exit(0));

        buttons.add(continueButton);
        buttons.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;   
        gbc.weightx = 1.0;
        gbc.insets = new Insets(12, 3, 3, 3);
        panel.add(buttons, gbc);
        add(panel, BorderLayout.CENTER);
    }

    private void connect() {
        String ip = ipField.getText().trim();
        String name = nameField.getText().trim();
        if (ip.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter both IP and username.",
                "Warning",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        try {
            new ChatClientGUI(ip, 12345, name);
            dispose();
        } catch (IOException ex) {
            String msg = ex.getMessage();
            if (msg != null && msg.contains("name already taken")) {
                JOptionPane.showMessageDialog(
                    this,
                    "That name was already taken",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Connection failed: " + msg,
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
