import handlers.UserChatHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class ChatServer extends Thread {

    List<UserChatHandler> usersChats = new ArrayList<>();

    public static void main(String[] args) {
        new ChatServer().start();
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            log.info("Chat server started on port 8080");
            log.info("Waiting for clients to connect...");
            while (true) {
                Socket newClientSocket = serverSocket.accept();
                UserChatHandler newUserChatHandler = new UserChatHandler(newClientSocket, usersChats);
                usersChats.add(newUserChatHandler);
                log.info("New client connected");
                log.info("ip address is {}", newClientSocket.getRemoteSocketAddress());
                newUserChatHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
