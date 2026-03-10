import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class ChatAppDashboard extends Application {

    private TextArea chatConsole;
    private Label ipStatusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ChatApp - Network Dashboard");

        // UI Setup
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Network Info
        VBox topContainer = new VBox(10);
        Label headerLabel = new Label("System Identity");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ipStatusLabel = new Label("Scanning network...");
        ipStatusLabel.setStyle("-fx-text-fill: #2c3e50;");

        topContainer.getChildren().addAll(headerLabel, ipStatusLabel);
        root.setTop(topContainer);

        // Console/Logs
        chatConsole = new TextArea();
        chatConsole.setEditable(false);
        chatConsole.setWrapText(true);
        chatConsole.setPromptText("System logs and server messages will appear here...");
        root.setCenter(chatConsole);

        // Controls
        Button fetchConfigButton = new Button("Fetch Server Config (URLConnection)");
        fetchConfigButton.setMaxWidth(Double.MAX_VALUE);
        fetchConfigButton.setOnAction(e -> fetchServerMessage());

        BorderPane.setMargin(chatConsole, new Insets(10, 0, 10, 0));
        root.setBottom(fetchConfigButton);

        // Execute InetAddress Logic immediately to show IP
        displayLocalAddress();

        // Scene Setup
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void displayLocalAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String hostName = localHost.getHostName();
            String ipAddress = localHost.getHostAddress();

            ipStatusLabel.setText(String.format("Host: %s | IP: %s", hostName, ipAddress));
            log("Network Interface identified: " + localHost.toString());

        } catch (UnknownHostException e) {
            ipStatusLabel.setText("Status: Offline / Unknown Host");
            log("Error: Could not determine local address.");
        }
    }

    private void fetchServerMessage() {
        log("Attempting to connect to update server...");

        new Thread(() -> {
            try {
                // Target URL
                URL url = new URL("https://connectify-chatapp.onrender.com/"); 

                // Open Connection
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(5000); // 5 seconds timeout
                connection.setReadTimeout(5000);

                // Read Data
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));

                StringBuilder response = new StringBuilder();
                String line;
                int lineCount = 0;

                while ((line = reader.readLine()) != null && lineCount < 5) {
                    response.append(line).append("\n");
                    lineCount++;
                }
                reader.close();

                // Update UI
                Platform.runLater(() -> {
                    log("Server Connection Established!");
                    log("Received Data Header:\n" + response.toString());
                });

            } catch (Exception e) {
                // Update UI on error
                Platform.runLater(() -> log("Connection Failed: " + e.getMessage()));
            }
        }).start();
    }

    private void log(String message) {
        chatConsole.appendText(message + "\n------------------\n");
    }
}