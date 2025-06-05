package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import shared.XMLMessageBuilder;



public class XMLMessageParser {

    public static void parseMessage(String xml, ChatClientGUI gui) {
        String type = XMLMessageBuilder.parseType(xml);
        String from = XMLMessageBuilder.parseSender(xml);
        String to   = XMLMessageBuilder.parseReceiver(xml);
        String body = XMLMessageBuilder.parseBody(xml);
        String mood = XMLMessageBuilder.parseMood(xml);

        switch (type) {
            
            case "join":
                if (from != null && !from.isEmpty() && !from.equalsIgnoreCase(gui.getUserName())) {
                    gui.addUser(from);
                }
                gui.addMessage("Local Chat", "🔵 " + from + " has joined the chat");
                break;

            case "leave":
                if (from != null && !from.isEmpty()) {
                    gui.removeUser(from);
                }
                gui.addMessage("Local Chat", "🔴 " + from + " has left the chat");
                break;

            case "broadcast":
                gui.addMessage("Local Chat", "💬 " + from + ": " + body);
                break;

            case "private":
                if (from.equalsIgnoreCase(gui.getUserName())) {
                    gui.addMessage(to, "🔒 You ➡ " + to + ": " + body);
                } else if (to.equalsIgnoreCase(gui.getUserName())) {
                    gui.addMessage(from, "🔒 " + from + " ➡ You: " + body);
                }
                break;

            case "hug":
                if (from.equalsIgnoreCase(gui.getUserName())) {
                    gui.addMessage(to, "🤗 You sent a hug to " + to);
                } else if (to.equalsIgnoreCase(gui.getUserName())) {
                    gui.addMessage(from, "🤗 " + from + " sent you a hug");
                }
                break;

            case "rename":
                if (body != null && body.contains("=>")) {
                    String[] parts = body.split("=>");
                    String oldName = parts[0].trim();
                    String newName = parts[1].trim();
                    gui.addMessage("Local Chat", "✏️ " + oldName + " changed name to " + newName);
                    gui.renameUser(oldName, newName);
                }
                break;

            case "mood":
                gui.addMessage("Local Chat", "😎 " + from + " changed mood to " + mood);
                gui.updateMood(from, mood);
                break;

            case "emotion":
                if ("Local Chat".equalsIgnoreCase(to)) {
                    gui.addMessage("Local Chat", "Reaction " + from + ": " + body);
                } else {
                    if (from.equalsIgnoreCase(gui.getUserName())) {
                        gui.addMessage(to, "Reaction You: " + body);
                    } else if (to.equalsIgnoreCase(gui.getUserName())) {
                        gui.addMessage(from, "Reaction " + from + ": " + body);
                    }
                }
                break;

            case "file":
                String fileName = XMLMessageBuilder.parseFileName(xml);
                String fileData = XMLMessageBuilder.parseFileContent(xml);
                if (fileName != null && fileData != null) {
                    try {
                        byte[] data = Base64.getDecoder().decode(fileData);
                        File outFile = new File("received_" + fileName);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        fos.write(data);
                        fos.close();

                        if ("Local Chat".equalsIgnoreCase(to)) {
                            gui.addMessage("Local Chat", "📥 " + from + " sent file: " + fileName);
                        } else {
                            if (from.equalsIgnoreCase(gui.getUserName())) {
                                gui.addMessage(to, "You sent file: " + fileName);
                            } else if (to.equalsIgnoreCase(gui.getUserName())) {
                                gui.addMessage(from, "📁 " + from + " sent you file: " + fileName);
                            }
                        }
                    } catch (IOException ex) {
                        if ("Local Chat".equalsIgnoreCase(to)) {
                            gui.addMessage("Local Chat", "❌ Error saving file: " + fileName);
                        } else {
                            if (from.equalsIgnoreCase(gui.getUserName())) {
                                gui.addMessage(to, "❌ Error sending file: " + fileName);
                            } else if (to.equalsIgnoreCase(gui.getUserName())) {
                                gui.addMessage(from, "❌ Error saving file: " + fileName);
                            }
                        }
                    }
                } else {
                    gui.addMessage("Local Chat", "⚠️ Invalid file message from " + from);
                }
                break;

            case "error":
                gui.addMessage("Local Chat", "⚠️ Server error: " + body);
                break;

            default:
                gui.addMessage("Local Chat", "⚠️ Unknown message type: " + type);
                break;
        }
    }
}
