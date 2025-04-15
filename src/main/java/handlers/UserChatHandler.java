package handlers;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.List;

@Slf4j
public class UserChatHandler extends Thread {

    private final String id;

    private final Socket socket;
    private final List<UserChatHandler> activeUsersChats;

    public UserChatHandler(String id, Socket socket, List<UserChatHandler> activeUsersChats) {
        this.socket = socket;
        this.id = id;
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
                    activeUsersChats.remove(this);
                    broadcastMessage("User with id : " + this.id + " disconnected");
                    log.info("User with id : {} disconnected", this.id);
                    socket.close();
                    return;
                }
                broadcastMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private synchronized void broadcastMessage(String message) {
        this.activeUsersChats.forEach(chatHandler -> {
            try {
                chatHandler.sendMeMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("User with id : {} broadcast message : {}", this.id, message);
    }

    public void sendMeMessage(String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(message);
    }
}
