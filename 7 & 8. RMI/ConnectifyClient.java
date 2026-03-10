import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ConnectifyClient extends Application {

    private ChatServer serverStub;
    private ChatClient callbackObj;
    private TextArea chatArea;
    private String username;

    private class ChatCallback extends UnicastRemoteObject implements ChatClient {
        protected ChatCallback() throws RemoteException {
            super();
        }

        @Override
        public void receiveMessage(String message) throws RemoteException {
            Platform.runLater(() -> chatArea.appendText(message + "\n"));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // UI Setup
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        TextField nameField = new TextField();
        nameField.setPromptText("Enter Username");
        TextField inputField = new TextField();
        inputField.setPromptText("Type message...");
        Button connectBtn = new Button("Connect");
        Button sendBtn = new Button("Send");
        sendBtn.setDisable(true);

        HBox topBar = new HBox(10, nameField, connectBtn);
        HBox bottomBar = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        VBox root = new VBox(10, topBar, chatArea, bottomBar);
        root.setPadding(new Insets(15));
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        root.setStyle("-fx-base: #2b2b2b; -fx-control-inner-background: #1e1e1e; -fx-text-fill: white;");
        chatArea.setStyle("-fx-text-fill: white; -fx-control-inner-background: #1e1e1e;");

        connectBtn.setOnAction(e -> {
            username = nameField.getText().trim();
            if (!username.isEmpty()) {
                try {
                    serverStub = (ChatServer) Naming.lookup("rmi://localhost/ConnectifyLive");

                    callbackObj = new ChatCallback();

                    serverStub.registerClient(callbackObj, username);

                    nameField.setDisable(true);
                    connectBtn.setDisable(true);
                    sendBtn.setDisable(false);
                    sendBtn.setDefaultButton(true);
                } catch (Exception ex) {
                    chatArea.appendText("Connection Failed: " + ex.getMessage() + "\n");
                }
            }
        });

        sendBtn.setOnAction(e -> {
            String msg = inputField.getText();
            if (!msg.isEmpty() && serverStub != null) {
                try {
                    serverStub.broadcastMessage(username + ": " + msg);
                    inputField.clear();
                } catch (Exception ex) {
                    chatArea.appendText("Send Error: " + ex.getMessage() + "\n");
                }
            }
        });

        primaryStage.setOnCloseRequest(event -> {
            try {
                if (serverStub != null && callbackObj != null) {
                    serverStub.disconnectClient(callbackObj, username);
                }
            } catch (Exception ex) {
            }
            System.exit(0);
        });

        primaryStage.setTitle("Connectify Live");
        primaryStage.setScene(new Scene(root, 450, 550));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}