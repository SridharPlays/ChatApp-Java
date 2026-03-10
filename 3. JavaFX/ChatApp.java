import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.*;
import java.util.Optional;

public class ChatApp extends Application {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chatapp_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Sridhar@2006";

    private Stage primaryStage;
    private String currentUser = "";

    // UI Components
    private ListView<ChatMessage> chatList = new ListView<>();
    private TextField txtInput = new TextField();
    private Timeline refreshTimer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Chat App - Full CRUD");
        showLoginScreen();
        stage.show();
    }

    // Login Screen
    private void showLoginScreen() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        Label title = new Label("Chat Login");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField txtUser = new TextField();
        txtUser.setPromptText("Username");
        txtUser.setMaxWidth(200);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Password");
        txtPass.setMaxWidth(200);

        Button btnLogin = new Button("Login");
        Button btnSignup = new Button("Register");

        btnLogin.setOnAction(e -> {
            if (authenticate(txtUser.getText(), txtPass.getText())) {
                currentUser = txtUser.getText();
                showChatScreen();
            } else {
                showAlert("Error", "Invalid Login");
            }
        });

        btnSignup.setOnAction(e -> register(txtUser.getText(), txtPass.getText()));

        layout.getChildren().addAll(title, txtUser, txtPass, btnLogin, btnSignup);
        primaryStage.setScene(new Scene(layout, 350, 400));
    }

    // ChatScreen
    private void showChatScreen() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        Label lblUser = new Label("User: " + currentUser);
        lblUser.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEdit = new Button("Edit Selected");
        Button btnDelete = new Button("Delete Selected");
        Button btnLogout = new Button("Logout");

        btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        topBar.getChildren().addAll(lblUser, spacer, btnEdit, btnDelete, btnLogout);
        root.setTop(topBar);

        root.setCenter(chatList);

        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        txtInput.setPromptText("Type message...");
        HBox.setHgrow(txtInput, Priority.ALWAYS);
        Button btnSend = new Button("SEND");
        btnSend.setDefaultButton(true);

        bottomBar.getChildren().addAll(txtInput, btnSend);
        root.setBottom(bottomBar);

        btnSend.setOnAction(e -> {
            if (!txtInput.getText().trim().isEmpty()) {
                createMessage(txtInput.getText().trim());
                txtInput.clear();
                refreshMessages();
            }
        });

        btnEdit.setOnAction(e -> handleEditAction());

        btnDelete.setOnAction(e -> handleDeleteAction());

        // Logout
        btnLogout.setOnAction(e -> {
            if (refreshTimer != null)
                refreshTimer.stop();
            showLoginScreen();
        });

        refreshMessages();
        refreshTimer = new Timeline(new KeyFrame(Duration.seconds(2), e -> refreshMessages()));
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();

        primaryStage.setScene(new Scene(root, 600, 500));
        primaryStage.setTitle("Chat - " + currentUser);
    }

    private void handleEditAction() {
        ChatMessage selected = chatList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Warning", "No message selected.");
            return;
        }

        if (!selected.getSender().equals(currentUser)) {
            showAlert("Access Denied", "You can only edit your own messages.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getMessage());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Update your message:");
        dialog.setContentText("New text:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> {
            updateMessage(selected.getId(), newText);
            refreshMessages();
        });
    }

    private void handleDeleteAction() {
        ChatMessage selected = chatList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Warning", "No message selected.");
            return;
        }

        if (!selected.getSender().equals(currentUser)) {
            showAlert("Access Denied", "You can only delete your own messages.");
            return;
        }

        deleteMessage(selected.getId());
        refreshMessages();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void createMessage(String text) {
        String sql = "INSERT INTO messages (sender, message_text) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUser);
            pstmt.setString(2, text);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshMessages() {
        ObservableList<ChatMessage> items = FXCollections.observableArrayList();
        String sql = "SELECT id, sender, message_text FROM messages ORDER BY timestamp ASC";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new ChatMessage(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("message_text")));
            }

            Platform.runLater(() -> {
                if (chatList.getItems().size() != items.size()) {
                    chatList.setItems(items);
                    chatList.scrollTo(items.size() - 1);
                } else {
                    chatList.setItems(items);
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateMessage(int id, String newText) {
        String sql = "UPDATE messages SET message_text = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newText);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteMessage(int id) {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean authenticate(String user, String pass) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            return pstmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void register(String user, String pass) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.executeUpdate();
            showAlert("Success", "Registered!");
        } catch (SQLException e) {
            showAlert("Error", "Username taken");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class ChatMessage {
        private int id;
        private String sender;
        private String message;

        public ChatMessage(int id, String sender, String message) {
            this.id = id;
            this.sender = sender;
            this.message = message;
        }

        public int getId() {
            return id;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "[" + sender + "]: " + message;
        }
    }
}