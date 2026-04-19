package com.threadium.chat.client;

import com.threadium.chat.common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ChatClientApp extends Application implements ServerConnection.MessageListener {

    private ServerConnection connection;

    private Stage primaryStage;
    private Scene loginScene;
    private Scene registerScene;
    private Scene chatScene;

    // Chat UI elements
    private ListView<String> roomListView;

    // Modern Chat layout replacements
    private ScrollPane chatScrollPane;
    private VBox chatMessagesBox;

    private TextField messageField;
    private Label currentRoomLabel;
    private Label currentUserLabel;

    private String currentRoom = null;
    private String loggedInUsername = null;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.connection = new ServerConnection();
        this.connection.setListener(this);

        initLoginScene();
        initRegisterScene();
        initChatScene();

        primaryStage.setTitle("Threadium");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private void initLoginScene() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);

        Label title = new Label("Login");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter Username");
        usernameField.setMaxWidth(200);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.setMaxWidth(200);

        Button loginBtn = new Button("Login");
        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            if (!u.isEmpty() && !p.isEmpty()) {
                loggedInUsername = u;
                showServerConnectionWindow(u, p, true);
            } else {
                showAlert("Error", "Please enter both username and password.");
            }
        });

        Hyperlink createAccountLink = new Hyperlink("No account? Create account");
        createAccountLink.setOnAction(e -> {
            usernameField.clear();
            passwordField.clear();
            primaryStage.setScene(registerScene); // switch to register page
        });

        vbox.getChildren().addAll(title, usernameField, passwordField, loginBtn, createAccountLink);
        loginScene = new Scene(vbox, 500, 350);
    }

    private void initRegisterScene() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);

        Label title = new Label("Create Account");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter Username");
        usernameField.setMaxWidth(200);

        PasswordField passField1 = new PasswordField();
        passField1.setPromptText("Enter Password");
        passField1.setMaxWidth(200);

        PasswordField passField2 = new PasswordField();
        passField2.setPromptText("Confirm Password");
        passField2.setMaxWidth(200);

        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p1 = passField1.getText();
            String p2 = passField2.getText();

            if (u.isEmpty() || p1.isEmpty() || p2.isEmpty()) {
                showAlert("Error", "Please fill in all fields.");
                return;
            }

            if (!p1.equals(p2)) {
                showAlert("Error", "Passwords do not match.");
                return;
            }

            showServerConnectionWindow(u, p1, false);
        });

        Hyperlink backToLogin = new Hyperlink("Back to Login");
        backToLogin.setOnAction(e -> {
            usernameField.clear();
            passField1.clear();
            passField2.clear();
            primaryStage.setScene(loginScene);
        });

        vbox.getChildren().addAll(title, usernameField, passField1, passField2, registerBtn, backToLogin);
        registerScene = new Scene(vbox, 500, 350);

    }

    private void showServerConnectionWindow(String u, String p, boolean isLogin) {
        Stage stage = new Stage();
        stage.setTitle("Connect to Server");

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        Label title = new Label("Server Connection");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ToggleGroup group = new ToggleGroup();
        RadioButton localRb = new RadioButton("Localhost");
        localRb.setToggleGroup(group);
        localRb.setSelected(true);

        RadioButton remoteRb = new RadioButton("Other Address (IPv4)");
        remoteRb.setToggleGroup(group);

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Enter IPv4 Address");
        ipField.disableProperty().bind(localRb.selectedProperty());

        TextField portField = new TextField("8080");
        portField.setPromptText("Enter Port (default 8080)");

        Button connectBtn = new Button("Connect");
        connectBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");

        connectBtn.setOnAction(e -> {
            String host = localRb.isSelected() ? "localhost" : ipField.getText().trim();
            String portStr = portField.getText().trim();
            int port = 8080;
            try {
                if (!portStr.isEmpty())
                    port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                showAlert("Invalid Port", "Please enter a valid port number.");
                return;
            }

            if (host.isEmpty()) {
                showAlert("Invalid Host", "Please enter a host address.");
                return;
            }

            if (connection.connect(host, port)) {
                if (isLogin) {
                    connection.sendLogin(u, p);
                } else {
                    connection.sendRegister(u, p);
                }
                stage.close();
            } else {
                showAlert("Connection Error", "Cannot connect to server at " + host + ":" + port);
            }
        });

        vbox.getChildren().addAll(title, localRb, remoteRb, ipField, new Label("Port:"), portField, connectBtn);
        stage.setScene(new Scene(vbox, 350, 400));
        stage.show();
    }

    private void initChatScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f0f2f5;"); // slightly gray app background

        // Left panel (Rooms)
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 0 1 0 0;");

        Label roomsLbl = new Label("Available Rooms:");
        roomListView = new ListView<>();
        roomListView.setStyle("-fx-background-insets: 0; -fx-padding: 0;");
        roomListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(currentRoom)) {
                joinRoom(newVal);
            }
        });

        TextField newRoomField = new TextField();
        newRoomField.setPromptText("New room name...");
        Button createRoomBtn = new Button("Create/Join");
        createRoomBtn.setOnAction(e -> {
            String rName = newRoomField.getText().trim();
            if (!rName.isEmpty()) {
                joinRoom(rName);
                newRoomField.clear();
            }
        });

        currentUserLabel = new Label();
        currentUserLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 14px;");

        leftPanel.getChildren().addAll(currentUserLabel, new Separator(), roomsLbl, roomListView,
                new HBox(5, newRoomField, createRoomBtn));

        // Center panel (Chat)
        VBox centerPanel = new VBox();

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        topBar.setAlignment(Pos.CENTER_LEFT);

        currentRoomLabel = new Label("Select a room to join");
        currentRoomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #333;");

        Button leaveRoomBtn = new Button("Leave Room");
        leaveRoomBtn.setDisable(true); // enabled when joined
        leaveRoomBtn.setOnAction(e -> leaveRoom());

        Button deleteAccountBtn = new Button("Delete Account");
        deleteAccountBtn.setStyle("-fx-text-fill: red;");
        deleteAccountBtn.setOnAction(e -> promptDeleteAccount());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(currentRoomLabel, spacer, leaveRoomBtn, deleteAccountBtn);

        // Chat messages layout
        chatMessagesBox = new VBox(10);
        chatMessagesBox.setPadding(new Insets(20));
        chatMessagesBox.setStyle("-fx-background-color: transparent;");

        chatScrollPane = new ScrollPane(chatMessagesBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane
                .setStyle("-fx-background: #e5ddd5; -fx-background-color: #e5ddd5; -fx-border-color: transparent;"); // whatsapp
                                                                                                                     // style
                                                                                                                     // bg
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Auto-scroll logic
        chatMessagesBox.heightProperty().addListener((observable, oldValue, newValue) -> {
            chatScrollPane.setVvalue(1.0);
        });

        // Input Panel
        HBox inputPanel = new HBox(10);
        inputPanel.setPadding(new Insets(10, 20, 10, 20));
        inputPanel.setStyle("-fx-background-color: #f0f2f5;"); // match root

        messageField = new TextField();
        messageField.setPromptText("Type a message...");
        messageField.setStyle("-fx-background-radius: 20px; -fx-padding: 8px 15px;");
        messageField.setDisable(true);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        Button sendBtn = new Button("Send");
        sendBtn.setStyle(
                "-fx-background-color: #0084ff; -fx-text-fill: white; -fx-background-radius: 20px; -fx-padding: 8px 20px;");
        sendBtn.setDisable(true);
        sendBtn.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage()); // enter key

        // Keep UI synced
        currentRoomLabel.textProperty().addListener((obs, old, newVal) -> {
            boolean inRoom = currentRoom != null;
            messageField.setDisable(!inRoom);
            sendBtn.setDisable(!inRoom);
            leaveRoomBtn.setDisable(!inRoom);
        });

        inputPanel.getChildren().addAll(messageField, sendBtn);
        centerPanel.getChildren().addAll(topBar, chatScrollPane, inputPanel);

        root.setLeft(leftPanel);
        root.setCenter(centerPanel);

        chatScene = new Scene(root, 800, 600);
    }

    private void promptDeleteAccount() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Delete Account");
        dialog.setHeaderText("To delete your account, please confirm your password.");

        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

        PasswordField pwd = new PasswordField();
        pwd.setPromptText("Password");

        VBox vbox = new VBox(pwd);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == deleteButtonType) {
                return pwd.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            if (!password.isEmpty()) {
                connection.sendDeleteAccount(loggedInUsername, password);
            }
        });
    }

    private void joinRoom(String roomName) {
        currentRoom = roomName;
        currentRoomLabel.setText("Room: " + roomName);
        chatMessagesBox.getChildren().clear();
        connection.sendJoinRoom(roomName);
    }

    private void leaveRoom() {
        if (currentRoom != null) {
            connection.sendLeaveRoom();
            currentRoom = null;
            currentRoomLabel.setText("Select a room to join");
            chatMessagesBox.getChildren().clear();
        }
    }

    private void sendMessage() {
        String content = messageField.getText().trim();
        if (!content.isEmpty() && currentRoom != null) {
            connection.sendChatMessage(content, currentRoom);
            messageField.clear();
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case REGISTER_ACK:
                    showInfo("Success", message.getContent() + "\nYou can now log in.");
                    primaryStage.setScene(loginScene);
                    connection.close(); // forcefully disconnect so they have to properly log in
                    break;
                case REGISTER_REJECT:
                    showAlert("Registration Failed", message.getContent());
                    connection.close();
                    break;
                case DELETE_ACCOUNT_ACK:
                    showInfo("Account Deleted", message.getContent());
                    currentRoom = null;
                    loggedInUsername = null;
                    currentRoomLabel.setText("Select a room to join");
                    chatMessagesBox.getChildren().clear();
                    primaryStage.setScene(loginScene);
                    connection.close();
                    break;
                case LOGIN_ACK:
                    currentUserLabel.setText("Logged in as: " + loggedInUsername);
                    primaryStage.setScene(chatScene);
                    break;
                case LOGIN_REJECT:
                    showAlert("Login Failed", message.getContent());
                    connection.close();
                    break;
                case CREATE_OR_JOIN_ROOM:
                    // Server forces UI sync, e.g. reconnecting to an anchored room
                    currentRoom = message.getRoomName();
                    chatMessagesBox.getChildren().clear();
                    currentRoomLabel.setText("Room: " + currentRoom);
                    break;
                case ROOM_LIST_UPDATE:
                    updateRoomList(message.getContent());
                    break;
                case ROOM_MESSAGE:
                    if (currentRoom != null && message.getRoomName().equals(currentRoom)) {
                        appendMessageToChat(message);
                    }
                    break;
                case ERROR:
                    showAlert("Error", message.getContent());
                    break;
                default:
                    break;
            }
        });
    }

    private void updateRoomList(String commaSeparatedRooms) {
        roomListView.getItems().clear();
        if (commaSeparatedRooms != null && !commaSeparatedRooms.isEmpty()) {
            String[] rooms = commaSeparatedRooms.split(",");
            for (String room : rooms) {
                if (!room.trim().isEmpty()) {
                    roomListView.getItems().add(room.trim());
                }
            }
        }
    }

    private void appendMessageToChat(Message message) {
        boolean isMine = message.getSender().equals(loggedInUsername);
        boolean isSystem = message.getSender().equals("System");

        HBox row = new HBox();
        row.setPadding(new Insets(2, 5, 2, 5));

        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(450);

        String time = message.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm"));

        if (isSystem) {
            row.setAlignment(Pos.CENTER);
            Label sysLabel = new Label(message.getContent() + " (" + time + ")");
            sysLabel.setStyle(
                    "-fx-font-size: 11px; -fx-text-fill: #555; -fx-background-color: #e1e2e3; -fx-padding: 3px 10px; -fx-background-radius: 10px;");
            row.getChildren().add(sysLabel);
        } else {
            Label senderLabel = new Label(message.getSender());
            senderLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: "
                    + (isMine ? "#005a9e" : "#e53935") + ";");

            Label contentLabel = new Label(message.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111;");

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");

            HBox timeBox = new HBox(timeLabel);
            timeBox.setAlignment(Pos.CENTER_RIGHT);

            if (!isMine) {
                bubble.getChildren().add(senderLabel); // only show name if it's someone else
            }
            bubble.getChildren().addAll(contentLabel, timeBox);

            if (isMine) {
                row.setAlignment(Pos.CENTER_RIGHT);
                // Light Blue Background for user
                bubble.setStyle("-fx-background-color: #add8e6; -fx-background-radius: 10px 0px 10px 10px;");
            } else {
                row.setAlignment(Pos.CENTER_LEFT);
                // White Background for others
                bubble.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 0px 10px 10px 10px;");
            }
            row.getChildren().add(bubble);
        }

        chatMessagesBox.getChildren().add(row);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @Override
    public void stop() {
        connection.close(); // User closes the app window.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
