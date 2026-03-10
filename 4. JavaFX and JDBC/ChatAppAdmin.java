import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;

public class ChatAppAdmin extends Application {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chatapp_db1?connectTimeout=2000";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Sridhar@2006";

    private Connection conn;
    private Statement stmt;
    private ResultSet rs;

    private TextField txtId = new TextField();
    private TextField txtUsername = new TextField();
    private TextField txtStatus = new TextField();
    private ImageView imgProfile = new ImageView();
    private Label lblMetadata = new Label("Metadata: Not loaded");
    private TextArea txtJoinResult = new TextArea();
    private File selectedImageFile;

    @Override
    public void start(Stage primaryStage) {
        // Form Layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(15);

        // Configure Inputs
        txtId.setEditable(false);
        txtId.setPromptText("ID");
        txtUsername.setEditable(false);
        txtUsername.setPromptText("Username");
        txtStatus.setPromptText("Status Message");

        grid.addRow(0, new Label("User ID:"), txtId);
        grid.addRow(1, new Label("Username:"), txtUsername);
        grid.addRow(2, new Label("Status:"), txtStatus);

        // Image Area
        imgProfile.setFitWidth(120);
        imgProfile.setFitHeight(120);
        imgProfile.setPreserveRatio(true);
        imgProfile.setStyle("-fx-border-color: gray;");

        Button btnUpload = new Button("Upload Image");
        VBox imageBox = new VBox(10, new Label("Profile Picture:"), imgProfile, btnUpload);
        imageBox.setAlignment(Pos.CENTER);

        // Main Layout container
        HBox topSection = new HBox(20, grid, imageBox);
        topSection.setAlignment(Pos.CENTER_LEFT);

        // Navigation Buttons
        Button btnFirst = new Button("<< First");
        Button btnPrev = new Button("< Prev");
        Button btnNext = new Button("Next >");
        Button btnLast = new Button("Last >>");
        HBox navBox = new HBox(10, btnFirst, btnPrev, btnNext, btnLast);
        navBox.setAlignment(Pos.CENTER);

        // Action Buttons
        Button btnUpdate = new Button("Update Record in DB");
        btnUpdate.setMaxWidth(Double.MAX_VALUE);

        Button btnJoin = new Button("Run JOIN Query (Show Roles)");
        txtJoinResult.setPrefHeight(100);
        txtJoinResult.setEditable(false);

        // Metadata Label
        lblMetadata.setStyle("-fx-text-fill: blue; -fx-font-size: 10px;");

        // Root Container
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(
                topSection,
                navBox,
                btnUpdate,
                new Separator(),
                lblMetadata,
                btnJoin,
                txtJoinResult);

        btnFirst.setOnAction(e -> moveCursor("FIRST"));
        btnLast.setOnAction(e -> moveCursor("LAST"));
        btnNext.setOnAction(e -> moveCursor("NEXT"));
        btnPrev.setOnAction(e -> moveCursor("PREV"));

        btnUpload.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
            selectedImageFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedImageFile != null) {
                imgProfile.setImage(new Image(selectedImageFile.toURI().toString()));
            }
        });

        btnUpdate.setOnAction(e -> updateRecord());
        btnJoin.setOnAction(e -> performJoin());

        Scene scene = new Scene(root, 600, 650);
        primaryStage.setTitle("Chat App Admin Panel");
        primaryStage.show();

        connectAndLoad();
    }

    private void connectAndLoad() {
        try {
            // Check if Driver Exists
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

            // Create Scrollable Statement
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            rs = stmt.executeQuery("SELECT user_id, username, status_message, profile_image FROM users");

            // Load Metadata
            ResultSetMetaData meta = rs.getMetaData();
            lblMetadata.setText("Connected to Table: " + meta.getTableName(1) + " | Columns: " + meta.getColumnCount());

            // Show first record
            if (rs.next()) {
                displayCurrentRow();
            } else {
                showAlert("Empty Database", "Connection successful, but 'users' table has no data.");
            }

        } catch (ClassNotFoundException e) {
            showAlert("Driver Error", "MySQL JDBC Driver not found in libraries!");
            e.printStackTrace();
        } catch (SQLException e) {
            showAlert("Connection Error", "Check your DB Name, User, and Password.\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayCurrentRow() {
        try {
            txtId.setText(String.valueOf(rs.getInt("user_id")));
            txtUsername.setText(rs.getString("username"));
            txtStatus.setText(rs.getString("status_message"));

            // Load Image BLOB
            Blob blob = rs.getBlob("profile_image");
            if (blob != null) {
                InputStream is = blob.getBinaryStream();
                imgProfile.setImage(new Image(is));
            } else {
                imgProfile.setImage(null);
            }
            selectedImageFile = null; // Reset upload selection
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void moveCursor(String direction) {
        try {
            if (rs == null)
                return;
            boolean success = switch (direction) {
                case "FIRST" -> rs.first();
                case "LAST" -> rs.last();
                case "NEXT" -> rs.next();
                case "PREV" -> rs.previous();
                default -> false;
            };

            if (success) {
                displayCurrentRow();
            } else {
                // Prevent going out of bounds
                if (direction.equals("NEXT"))
                    rs.afterLast();
                if (direction.equals("PREV"))
                    rs.beforeFirst();
            }
        } catch (SQLException e) {
            showAlert("Navigation Error", e.getMessage());
        }
    }

    private void updateRecord() {
        try {
            if (rs == null)
                return;

            // Update String
            rs.updateString("status_message", txtStatus.getText());

            // Update Image (only if a new one was picked)
            if (selectedImageFile != null) {
                FileInputStream fis = new FileInputStream(selectedImageFile);
                rs.updateBinaryStream("profile_image", fis, (int) selectedImageFile.length());
            }

            rs.updateRow(); // Commit to DB
            showAlert("Success", "Record updated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Update Failed", e.getMessage());
        }
    }

    private void performJoin() {
        String query = "SELECT u.username, r.role_name FROM users u JOIN user_roles r ON u.role_id = r.role_id";
        try (Statement s2 = conn.createStatement(); ResultSet rs2 = s2.executeQuery(query)) {
            StringBuilder sb = new StringBuilder();
            while (rs2.next()) {
                sb.append("User: ").append(rs2.getString("username"))
                        .append("  -->  Role: ").append(rs2.getString("role_name"))
                        .append("\n");
            }
            txtJoinResult.setText(sb.toString());
        } catch (SQLException e) {
            showAlert("Join Error", e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}