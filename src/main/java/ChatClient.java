import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatClient extends JFrame {
    private JTextField usernameField;
    private JTextField messageField;
    private JTextArea chatArea;
    private JButton connectButton;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel statusLabel;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ExecutorService executorService;
    private boolean connected = false;
    private String username = "";

    public ChatClient() {
        
        super("Chat Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel serverLabel = new JLabel("Server:");
        JTextField serverField = new JTextField("localhost", 10);
        JLabel portLabel = new JLabel("Port:");
        JTextField portField = new JTextField("8081", 5);
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField(10);
        connectButton = new JButton("Connect");

        connectionPanel.add(serverLabel);
        connectionPanel.add(serverField);
        connectionPanel.add(portLabel);
        connectionPanel.add(portField);
        connectionPanel.add(usernameLabel);
        connectionPanel.add(usernameField);
        connectionPanel.add(connectButton);

        statusLabel = new JLabel("Not connected");
        topPanel.add(connectionPanel, BorderLayout.CENTER);
        topPanel.add(statusLabel, BorderLayout.SOUTH);

        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));

        
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        
        add(topPanel, BorderLayout.NORTH);
        add(chatScrollPane, BorderLayout.CENTER);
        add(userScrollPane, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        
        connectButton.addActionListener(e -> {
            if (!connected) {
                String server = serverField.getText().trim();
                String portText = portField.getText().trim();
                username = usernameField.getText().trim();

                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a username");
                    return;
                }

                try {
                    int port = Integer.parseInt(portText);
                    connect(server, port);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid port number");
                }
            } else {
                disconnect();
            }
        });

        sendButton.addActionListener(e -> sendMessage());

        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected) {
                    disconnect();
                }
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        });

        executorService = Executors.newSingleThreadExecutor();

        setVisible(true);
    }

    private void connect(String server, int port) {
        try {
            socket = new Socket(server, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            
            writer.println("username:" + username);

            
            executorService.execute(this::readMessages);

            connected = true;
            connectButton.setText("Disconnect");
            sendButton.setEnabled(true);
            statusLabel.setText("Connected as: " + username);

            
            userListModel.addElement(username + " (you)");

            appendToChatArea("Connected to server!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage());
        }
    }

    private void disconnect() {
        if (connected) {
            try {
                writer.println("exit");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connected = false;
                connectButton.setText("Connect");
                sendButton.setEnabled(false);
                statusLabel.setText("Not connected");
                userListModel.clear();
                appendToChatArea("Disconnected from server.");
            }
        }
    }

    private void sendMessage() {
        if (connected && messageField.getText().trim().length() > 0) {
            String recipient = userList.getSelectedValue();
            String message = messageField.getText().trim();

            if (recipient != null && !recipient.equals(username + " (you)")) {
                
                String actualUsername = recipient;
                if (recipient.endsWith(" (you)")) {
                    actualUsername = recipient.substring(0, recipient.length() - 6);
                }
                writer.println(actualUsername + "> " + message);
                appendToChatArea("You to " + actualUsername + ": " + message);
            } else {
                
                writer.println(message);
            }

            messageField.setText("");
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    if (finalMessage.contains("username set as:")) {
                        
                        statusLabel.setText("Connected as: " + username);
                    } else {
                        
                        appendToChatArea(finalMessage);

                        
                        if (finalMessage.contains(" : ")) {
                            String sender = finalMessage.substring(0, finalMessage.indexOf(" : "));
                            if (!userListModel.contains(sender) && !sender.equals(username)) {
                                userListModel.addElement(sender);
                            }
                        }
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    appendToChatArea("Connection lost: " + e.getMessage());
                    disconnect();
                });
            }
        }
    }

    private void appendToChatArea(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}