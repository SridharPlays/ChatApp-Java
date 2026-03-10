import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConnectifyClientGUI extends Application {

    private static final String SERVER_HOST = "localhost";
    private static final int TCP_PORT = 5000;
    private static final int MULTICAST_PORT = 4446;
    private static final String MULTICAST_GROUP = "230.0.0.1";

    private Stage primaryStage;
    private TextArea chatArea;
    private TextField messageInput;
    private Button sendButton;

    private Socket tcpSocket;
    private PrintWriter out;
    private String username;
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Connectify - Realtime Chat");

        showLoginScreen();
    }

    private void showLoginScreen() {
        // Layout
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f0f2f5;");

        // Components
        Label titleLabel = new Label("Connectify");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#3b82f6"));

        Label subtitleLabel = new Label("Enter your name to join the global chat");
        subtitleLabel.setTextFill(Color.GRAY);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(250);
        usernameField.setPrefHeight(35);

        Button joinButton = new Button("Join Chat");
        joinButton.setPrefWidth(250);
        joinButton.setPrefHeight(35);
        joinButton.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        // Action
        joinButton.setOnAction(e -> {
            String name = usernameField.getText().trim();
            if (!name.isEmpty()) {
                connectToServer(name);
            }
        });

        // Allow pressing Enter to join
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)
                joinButton.fire();
        });

        root.getChildren().addAll(titleLabel, subtitleLabel, usernameField, joinButton);
        Scene loginScene = new Scene(root, 400, 500);
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void showChatScreen() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        HBox header = new HBox();
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label brandLabel = new Label("Connectify Global");
        brandLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        Label userLabel = new Label("Logged in as: " + username);
        userLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        VBox headerText = new VBox(2, brandLabel, userLabel);
        header.getChildren().add(headerText);
        root.setTop(header);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: #f9fafb; -fx-font-family: 'Segoe UI'; -fx-font-size: 14px;");
        root.setCenter(chatArea);

        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(15));
        bottomBar.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");
        bottomBar.setAlignment(Pos.CENTER);

        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.setPrefHeight(40);
        HBox.setHgrow(messageInput, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setPrefHeight(40);
        sendButton.setPrefWidth(80);
        sendButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold;");

        sendButton.setOnAction(e -> sendMessage());
        messageInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER)
                sendMessage();
        });

        bottomBar.getChildren().addAll(messageInput, sendButton);
        root.setBottom(bottomBar);

        Scene chatScene = new Scene(root, 500, 600);
        primaryStage.setScene(chatScene);

        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void connectToServer(String name) {
        this.username = name;
        try {
            tcpSocket = new Socket(SERVER_HOST, TCP_PORT);
            out = new PrintWriter(tcpSocket.getOutputStream(), true);

            out.println(username);

            Thread listenerThread = new Thread(this::listenForMulticastMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            showChatScreen();

        } catch (IOException e) {
            showError("Connection Failed", "Could not connect to server at " + SERVER_HOST);
        }
    }

    private void sendMessage() {
        String msg = messageInput.getText().trim();
        if (!msg.isEmpty() && out != null) {
            out.println(msg); // TCP Send
            messageInput.clear();
        }
    }

    private void listenForMulticastMessages() {
        try (MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            multicastSocket.joinGroup(group);

            byte[] buffer = new byte[1024];

            while (isRunning) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);

                String receivedMsg = new String(packet.getData(), 0, packet.getLength());

                Platform.runLater(() -> appendMessage(receivedMsg));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String message) {
        chatArea.appendText(message + "\n\n");
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        isRunning = false;
        if (out != null)
            out.println("exit");
        if (tcpSocket != null)
            tcpSocket.close();
    }
}