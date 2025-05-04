package handlers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class UserChatHandler extends Thread {

    private String userName;
    private final Socket socket;
    private final List<UserChatHandler> activeUsersChats;

    public UserChatHandler(Socket socket, List<UserChatHandler> activeUsersChats) {
        this.socket = socket;
        this.activeUsersChats = activeUsersChats;
    }

    @Override
    public void run() {
        while (true) {
            try {
                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message = bufferedReader.readLine();
                if (message.trim().equalsIgnoreCase("exit")) {
                    exit();
                    return;
                }
                if (message.trim().contains(">")) {
                    String receiverUserName = message.split(">")[0].trim();
                    String messageContent = message.split(">")[1].trim();
                    UserChatHandler userToSend = activeUsersChats
                            .stream()
                            .filter(u -> u.getUserName() != null)
                            .filter(u -> u.getUserName().equalsIgnoreCase(receiverUserName))
                            .findFirst()
                            .orElse(null);
                    if (userToSend == null) {
                        sendMeMessage("User '" + receiverUserName + "' not found or not set a username yet.");
                        continue;
                    }
                    userToSend.sendMeMessage(this.userName + " : " + messageContent);
                    continue;
                }
                if (message.trim().contains("username:")) {
                    setUserName(message.trim().split(":")[1].trim());
                    this.sendMeMessage("username set as: " + userName);
                    continue;
                }
                if (userName == null) {
                    this.sendMeMessage("You should enter a user name first");
                    continue;
                }

                broadcastMessage(userName, message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private synchronized void broadcastMessage(String userName, String message) {
        this.activeUsersChats.forEach(chatHandler -> {
            try {
                chatHandler.sendMeMessage(userName + " : " + message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("{} broadcast message : {}", this.userName, message);
    }

    private void exit(){
        activeUsersChats.remove(this);
        broadcastMessage(userName, "Disconnected");
        log.info("{} disconnected", this.userName);
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setUserName(String userName) {
        this.userName = userName;
    }

    public void sendMeMessage(String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(message);
    }
}
