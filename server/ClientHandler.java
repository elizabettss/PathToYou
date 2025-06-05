package server;

import java.io.*;
import java.net.Socket;
import shared.XMLMessageBuilder;

public class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private BufferedReader in;
    private String userName;
    private String mood = "😊";

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUserName() {
        return userName;
    }

    public String getMood() {
        return mood;
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                String type       = XMLMessageBuilder.parseType(message);
                String from       = XMLMessageBuilder.parseSender(message);
                String to         = XMLMessageBuilder.parseReceiver(message);
                String body       = XMLMessageBuilder.parseBody(message);
                String parsedMood = XMLMessageBuilder.parseMood(message);

                switch (type) {
                    case "join": {
                        String requestedName = from;
                        String requestedMood = (parsedMood != null && !parsedMood.isEmpty())
                                ? parsedMood
                                : "😊";

                        if (server.isUserNameTaken(requestedName)) {
                            out.println("<message type=\"error\"><body>name already taken</body></message>");
                            socket.close();
                            return;
                        }

                        this.userName = requestedName;
                        this.mood     = requestedMood;

                        if (server.addUser(this)) {
                            for (ClientHandler existingHandler : server.getClientHandlers()) {
                                String existingName = existingHandler.getUserName();
                                if (!existingName.equalsIgnoreCase(this.userName)) {
                                    String existingMood = existingHandler.getMood();
                                    this.sendMessage(
                                        XMLMessageBuilder.buildJoinMessage(existingName, existingMood)
                                    );
                                }
                            }

                            String joinXml = XMLMessageBuilder.buildJoinMessage(userName, mood);
                            server.broadcast(joinXml);
                        }
                        break;
                    }

                    case "broadcast": {
                        server.broadcast(message);
                        break;
                    }

                    case "private": {
                        server.sendPrivateMessage(to, message);
                        this.sendMessage(message);
                        break;
                    }

                    case "hug": {
                        if (to != null && !to.isEmpty()) {
                            server.sendPrivateMessage(to, message);
                        }
                        this.sendMessage(message);
                        break;
                    }

                    case "rename": {
                        if (body != null && body.contains("=>")) {
                            String[] parts = body.split("=>");
                            String oldName = parts[0].trim();
                            String newName = parts[1].trim();

                            if (server.renameUser(oldName, newName)) {
                                this.userName = newName;
                                String renameXml = XMLMessageBuilder.buildRenameMessage(oldName, newName);
                                server.broadcast(renameXml);
                            }
                        }
                        break;
                    }

                    case "mood": {
                        this.mood = (parsedMood != null && !parsedMood.isEmpty()) ? parsedMood : this.mood;
                        String moodXml = XMLMessageBuilder.buildMoodChangeMessage(userName, mood);
                        server.broadcast(moodXml);
                        break;
                    }

                    case "file": {
                        String fileName = XMLMessageBuilder.parseFileName(message);
                        String fileData = XMLMessageBuilder.parseFileContent(message);
                        String recipient = to;

                        if (fileName != null && fileData != null) {
                            if ("Local Chat".equalsIgnoreCase(recipient)) {
                                server.broadcast(message);
                            } else {
                                server.sendPrivateMessage(recipient, message);
                                this.sendMessage(message);
                            }
                        }
                        break;
                    }

                    case "emotion": {
                        String recipient = to;
                        if ("Local Chat".equalsIgnoreCase(recipient)) {
                            server.broadcast(message);
                        } else {
                            server.sendPrivateMessage(recipient, message);
                            this.sendMessage(message);
                        }
                        break;
                    }

                    case "leave":
                    case "exit": {
                        break;
                    }

                    default: {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (userName != null) {
                server.removeUser(this);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
