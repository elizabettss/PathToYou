package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import shared.XMLMessageBuilder;


public class Server {
    private Set<String> userNames = new HashSet<>();
    private Map<String, ClientHandler> clients = new HashMap<>();
    private int port;
    private ServerGUI gui;

    private List<String> localHistory = new ArrayList<>();

    private Map<String, List<String>> privateHistory = new HashMap<>();

    public Server(int port) {
        this.port = port;
        this.gui  = new ServerGUI(this);
    }

    public synchronized boolean addUser(ClientHandler client) {
        String name = client.getUserName();
        for (String existing : userNames) {
            if (existing.equalsIgnoreCase(name)) {
                return false;
            }
        }
        clients.put(name, client);
        userNames.add(name);
        gui.addUser(name);
        return true;
    }

    public synchronized void removeUser(ClientHandler client) {
        String name = client.getUserName();
        if (name != null) {
            String leaveXml = XMLMessageBuilder.buildLeaveMessage(name);
            broadcast(leaveXml);

            clients.remove(name);
            userNames.removeIf(s -> s.equalsIgnoreCase(name));

            gui.removeUser(name);
        }
    }

    public synchronized void logChat(String msg) {
        localHistory.add(msg);
        gui.logMessage(msg);
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            gui.logMessage("🌐 Server started on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler client = new ClientHandler(clientSocket, this);
                client.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean isUserNameTaken(String name) {
        for (String existing : userNames) {
            if (existing.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcast(String message) {
        String type = XMLMessageBuilder.parseType(message);

        switch (type) {
            case "join": {
                String from = XMLMessageBuilder.parseSender(message);
                String joinMsg = "🔵 " + from + " has joined the chat";
                logChat(joinMsg);
                break;
            }
            case "leave": {
                String from = XMLMessageBuilder.parseSender(message);
                String leaveMsg = "🔴 " + from + " has left the chat";
                logChat(leaveMsg);
                break;
            }
            case "mood": {
                String from    = XMLMessageBuilder.parseSender(message);
                String newMood = XMLMessageBuilder.parseMood(message);
                String moodMsg = "😎 " + from + " changed mood to " + newMood;
                logChat(moodMsg);
                break;
            }
            case "emotion": {
                String from = XMLMessageBuilder.parseSender(message);
                String body = XMLMessageBuilder.parseBody(message);
                String emoMsg = "🎭 " + from + ": " + body;
                logChat(emoMsg);
                break;
            }
            case "broadcast": {
                String from = XMLMessageBuilder.parseSender(message);
                String body = XMLMessageBuilder.parseBody(message);
                String bcMsg = "💬 " + from + ": " + body;
                logChat(bcMsg);
                break;
            }
            default:
                break;
        }

        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendPrivateMessage(String toUser, String message) {
        String actualRecipient = null;
        for (String existing : clients.keySet()) {
            if (existing.equalsIgnoreCase(toUser)) {
                actualRecipient = existing;
                break;
            }
        }
        if (actualRecipient == null) {
            return;
        }

        String fromUser = XMLMessageBuilder.parseSender(message);
        String body     = XMLMessageBuilder.parseBody(message);
        if (fromUser != null && body != null) {
            String userA = fromUser;
            String userB = actualRecipient;
            String key = (userA.compareToIgnoreCase(userB) < 0)
                    ? userA + ":" + userB
                    : userB + ":" + userA;

            if (!privateHistory.containsKey(key)) {
                gui.addConversation(userA, userB);
                privateHistory.put(key, new ArrayList<>());
            }
            privateHistory.get(key).add(fromUser + ": " + body);
        }

        ClientHandler client = clients.get(actualRecipient);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    public synchronized boolean renameUser(String oldName, String newName) {
        for (String existing : userNames) {
            if (existing.equalsIgnoreCase(newName) && !existing.equalsIgnoreCase(oldName)) {
                return false;
            }
        }
        String actualKey = null;
        for (String existing : userNames) {
            if (existing.equalsIgnoreCase(oldName)) {
                actualKey = existing;
                break;
            }
        }
        if (actualKey == null) {
            return false;
        }

        ClientHandler handler = clients.remove(actualKey);
        userNames.remove(actualKey);

        clients.put(newName, handler);
        userNames.add(newName);

        Map<String, List<String>> updatedPrivate = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : privateHistory.entrySet()) {
            String key = entry.getKey(); 
            String[] parts = key.split(":");
            String a = parts[0], b = parts[1];
            boolean replaced = false;
            if (a.equalsIgnoreCase(oldName)) {
                a = newName;
                replaced = true;
            }
            if (b.equalsIgnoreCase(oldName)) {
                b = newName;
                replaced = true;
            }
            if (replaced) {
                String newKey = (a.compareToIgnoreCase(b) < 0) ? a + ":" + b : b + ":" + a;
                updatedPrivate.put(newKey, entry.getValue());
            } else {
                updatedPrivate.put(key, entry.getValue());
            }
        }
        privateHistory = updatedPrivate;

        gui.renameUser(oldName, newName);
        return true;
    }

    public synchronized List<String> getLocalHistory() {
        return new ArrayList<>(localHistory);
    }

    public synchronized List<String> getPrivateHistory(String userA, String userB) {
        String key = (userA.compareToIgnoreCase(userB) < 0)
                ? userA + ":" + userB
                : userB + ":" + userA;
        return new ArrayList<>(privateHistory.getOrDefault(key, Collections.emptyList()));
    }

    public synchronized Collection<ClientHandler> getClientHandlers() {
        return new ArrayList<>(clients.values());
    }

    public static void main(String[] args) {
        int port = 12345;
        Server server = new Server(port);
        server.execute();
    }
}
