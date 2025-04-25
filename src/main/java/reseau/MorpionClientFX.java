package reseau;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MorpionClientFX extends Application {
    // Debug flag to enable detailed logging
    private static final boolean DEBUG_MODE = true;
    
    // Connection handling
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread connectionThread;
    private boolean connectionActive = false;
    
    // UI elements
    private Button[][] cells = new Button[3][3];
    private Label statusLabel;
    private Label connectionLabel;
    private Button reconnectButton;
    private Button replayButton;
    
    // Game state
    private char[][] boardState = new char[3][3];
    private boolean myTurn = false;
    private boolean gameOver = false;
    private int lastPlayedRow = -1;
    private int lastPlayedCol = -1;
    private char playerSymbol = 'X';  // Default symbol for player
    
    // Configuration
    private String serverAddress = "127.0.0.1";
    private int port = 55555;
    private boolean multiPlayerMode = true;  // Default to multiplayer mode

    @Override
    public void start(Stage primaryStage) {
        // Process command line arguments if provided
        Parameters params = getParameters();
        if (params.getRaw().size() >= 1) {
            serverAddress = params.getRaw().get(0);
        }
        if (params.getRaw().size() >= 2) {
            try {
                port = Integer.parseInt(params.getRaw().get(1));
            } catch (NumberFormatException e) {
                System.err.println("Format de port invalide, utilisation du port par défaut: " + port);
            }
        }
        if (params.getRaw().size() >= 3) {
            multiPlayerMode = "multi".equalsIgnoreCase(params.getRaw().get(2));
        } else {
            // Show mode selection dialog if not specified in args
            showModeSelectionDialog(primaryStage);
            return; // Exit start method, UI will be initialized after mode selection
        }
        
        initializeMainUI(primaryStage);
    }
    
    private void showModeSelectionDialog(Stage primaryStage) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Morpion - Configuration");
        
        VBox dialogVbox = new VBox(20);
        dialogVbox.setPadding(new Insets(20));
        dialogVbox.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("Morpion");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        
        // Game mode selection
        Label modeLabel = new Label("Mode de jeu:");
        modeLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        ToggleGroup modeGroup = new ToggleGroup();
        RadioButton vsComputerBtn = new RadioButton("Jouer contre l'ordinateur");
        vsComputerBtn.setToggleGroup(modeGroup);
        vsComputerBtn.setSelected(!multiPlayerMode);
        
        RadioButton vsPlayerBtn = new RadioButton("Jouer contre un autre joueur");
        vsPlayerBtn.setToggleGroup(modeGroup);
        vsPlayerBtn.setSelected(multiPlayerMode);
        
        // Network configuration section
        VBox networkBox = new VBox(10);
        networkBox.setAlignment(Pos.CENTER);
        networkBox.setPadding(new Insets(10, 0, 0, 0));
        networkBox.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1px; -fx-padding: 15px;");
        
        Label networkLabel = new Label("Configuration réseau:");
        networkLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // Predefined server options
        ToggleGroup serverGroup = new ToggleGroup();
        
        RadioButton localHostBtn = new RadioButton("Localhost (même machine)");
        localHostBtn.setToggleGroup(serverGroup);
        localHostBtn.setSelected(serverAddress.equals("127.0.0.1") || serverAddress.equals("localhost"));
        
        RadioButton lanServerBtn = new RadioButton("Serveur sur le réseau local");
        lanServerBtn.setToggleGroup(serverGroup);
        lanServerBtn.setSelected(!serverAddress.equals("127.0.0.1") && !serverAddress.equals("localhost"));
        
        // Custom server address and port fields
        GridPane serverDetailsGrid = new GridPane();
        serverDetailsGrid.setHgap(10);
        serverDetailsGrid.setVgap(10);
        serverDetailsGrid.setAlignment(Pos.CENTER);
        
        Label serverLabel = new Label("Adresse IP:");
        TextField serverField = new TextField(serverAddress);
        serverField.setPrefWidth(150);
        serverField.setPromptText("ex: 192.168.1.100");
        
        Label portLabel = new Label("Port:");
        TextField portField = new TextField(String.valueOf(port));
        portField.setPrefWidth(80);
        portField.setPromptText("ex: 55555");
        
        serverDetailsGrid.add(serverLabel, 0, 0);
        serverDetailsGrid.add(serverField, 1, 0);
        serverDetailsGrid.add(portLabel, 0, 1);
        serverDetailsGrid.add(portField, 1, 1);
        
        // Enable/disable fields based on server selection
        serverDetailsGrid.setDisable(localHostBtn.isSelected());
        
        localHostBtn.setOnAction(e -> {
            serverDetailsGrid.setDisable(true);
            serverField.setText("127.0.0.1");
        });
        
        lanServerBtn.setOnAction(e -> {
            serverDetailsGrid.setDisable(false);
            if (serverField.getText().equals("127.0.0.1") || serverField.getText().equals("localhost")) {
                serverField.clear();
            }
        });
        
        // Add all network components to the network box
        networkBox.getChildren().addAll(
            networkLabel, 
            localHostBtn, 
            lanServerBtn, 
            serverDetailsGrid
        );
        
        // Only show network settings for multiplayer mode
        networkBox.setVisible(multiPlayerMode);
        vsPlayerBtn.setOnAction(e -> networkBox.setVisible(true));
        vsComputerBtn.setOnAction(e -> networkBox.setVisible(false));
        
        // Start button
        Button startButton = new Button("Commencer la partie");
        startButton.setPrefWidth(200);
        startButton.setOnAction(e -> {
            try {
                // Update settings based on selections
                multiPlayerMode = vsPlayerBtn.isSelected();
                
                if (multiPlayerMode) {
                    serverAddress = serverField.getText().trim();
                    
                    // Validate server address
                    if (serverAddress.isEmpty() && lanServerBtn.isSelected()) {
                        throw new IllegalArgumentException("L'adresse IP du serveur ne peut pas être vide");
                    }
                    
                    // Validate port
                    try {
                        port = Integer.parseInt(portField.getText().trim());
                        if (port <= 0 || port > 65535) {
                            throw new NumberFormatException("Le port doit être entre 1 et 65535");
                        }
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Le port doit être un nombre valide entre 1 et 65535");
                    }
                }
                
                // Close dialog and initialize main UI
                dialogStage.close();
                initializeMainUI(primaryStage);
            } catch (IllegalArgumentException ex) {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Erreur de configuration");
                alert.setHeaderText(null);
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
        
        dialogVbox.getChildren().addAll(
            titleLabel,
            modeLabel,
            vsComputerBtn,
            vsPlayerBtn,
            networkBox,
            startButton
        );
        
        Scene dialogScene = new Scene(dialogVbox, 450, 500);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
    }
    
    private void initializeMainUI(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Header
        VBox headerBox = new VBox(10);
        headerBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("Morpion");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        headerBox.getChildren().add(title);
        
        // Mode indicator
        Label modeLabel = new Label("Mode: " + (multiPlayerMode ? "Multijoueur" : "Contre l'ordinateur"));
        modeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        headerBox.getChildren().add(modeLabel);
        
        // Connection controls (only for multiplayer)
        if (multiPlayerMode) {
            HBox connectionBox = createConnectionControls();
            headerBox.getChildren().add(connectionBox);
        }
        
        root.setTop(headerBox);
        
        // Game board
        GridPane gameBoard = createGameBoard();
        root.setCenter(gameBoard);
        
        // Status and buttons
        VBox bottomBox = new VBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        
        statusLabel = new Label(multiPlayerMode ? 
                "En attente de connexion..." : 
                "🎲 À votre tour! Cliquez sur une case pour jouer.");
        statusLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));
        statusLabel.setPadding(new Insets(10, 0, 0, 0));
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Replay button
        replayButton = new Button("Nouvelle partie");
        replayButton.setVisible(false);
        replayButton.setOnAction(e -> resetGame());
        
        // Change mode button
        Button changeModeButton = new Button("Changer de mode");
        changeModeButton.setOnAction(e -> {
            if (multiPlayerMode && connectionActive) {
                disconnect();
            }
            showModeSelectionDialog(primaryStage);
        });
        
        buttonBox.getChildren().addAll(replayButton, changeModeButton);
        bottomBox.getChildren().addAll(statusLabel, buttonBox);
        
        root.setBottom(bottomBox);
        
        // Set up scene
        Scene scene = new Scene(root, 400, 520);
        primaryStage.setTitle("Morpion - " + (multiPlayerMode ? "Multijoueur" : "Solo"));
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initialize game state
        resetBoard();
        
        if (multiPlayerMode) {
            // Connect to server in multiplayer mode
            connectToServer();
        } else {
            // Start single player game
            gameOver = false;
            myTurn = true;
            updateStatus("🎲 À votre tour! Cliquez sur une case pour jouer.");
        }
        
        primaryStage.setOnCloseRequest(e -> {
            if (multiPlayerMode) {
                disconnect();
            }
            Platform.exit();
        });
    }
    
    private HBox createConnectionControls() {
        HBox connectionBox = new HBox(10);
        connectionBox.setAlignment(Pos.CENTER);
        
        // Create icon-style indicator
        connectionLabel = new Label("⚫ Déconnecté");
        connectionLabel.setTextFill(Color.RED);
        
        reconnectButton = new Button("Reconnecter");
        reconnectButton.setOnAction(e -> {
            if (!connectionActive && multiPlayerMode) {
                connectToServer();
            }
        });
        
        // Server info label
        Label serverInfoLabel = new Label(serverAddress.equals("127.0.0.1") ? 
                                         "Serveur: localhost:" + port : 
                                         "Serveur: " + serverAddress + ":" + port);
        
        connectionBox.getChildren().addAll(connectionLabel, reconnectButton, serverInfoLabel);
        return connectionBox;
    }
    
    private GridPane createGameBoard() {
        GridPane gameBoard = new GridPane();
        gameBoard.setAlignment(Pos.CENTER);
        gameBoard.setHgap(10);
        gameBoard.setVgap(10);
        
        // Initialize cells array if it's null
        if (cells == null) {
            cells = new Button[3][3];
        }
        
        // Create all 9 button cells with consistent size
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button cell = new Button();
                cell.setMinSize(100, 100);
                cell.setPrefSize(100, 100);
                cell.setMaxSize(100, 100);
                cell.setFont(Font.font("System", FontWeight.BOLD, 36));
                
                // Add click event handler
                final int r = row;
                final int c = col;
                cell.setOnAction(e -> handleCellClick(r, c));
                
                // Store in array and add to grid
                cells[row][col] = cell;
                gameBoard.add(cell, col, row);
                
                // Apply initial style
                cell.setStyle("-fx-background-color: white; -fx-border-color: #aaaaaa; -fx-border-width: 1px;");
            }
        }
        
        return gameBoard;
    }
    
    private void handleCellClick(int row, int col) {
        if (multiPlayerMode) {
            // Multiplayer mode
            if (gameOver) {
                updateStatus("La partie est terminée. Cliquez sur 'Nouvelle partie' pour recommencer.");
                return;
            }
            
            if (!myTurn) {
                updateStatus("Ce n'est pas votre tour!");
                return;
            }
            
            if (!connectionActive) {
                updateStatus("Vous n'êtes pas connecté au serveur!");
                return;
            }
            
            if (boardState[row][col] != ' ') {
                updateStatus("⛔ Cette case est déjà occupée!");
                return;
            }
            
            // Valid move - send to server
            myTurn = false;
            String move = row + "" + col;
            output.println(move);
            debugLog("Envoi du coup au serveur: " + move);
            
            // Update UI with temporary feedback
            lastPlayedRow = row;
            lastPlayedCol = col;
            
            updateStatus("Coup joué : " + move + ". En attente de la réponse du serveur...");
            updateTurnStatus(false);
        } else {
            // Single player mode against computer
            handleSinglePlayerMode(row, col);
        }
    }
    
    private void handleSinglePlayerMode(int row, int col) {
        // Player's move
        if (gameOver || !myTurn || boardState[row][col] != ' ') {
            if (gameOver) {
                updateStatus("La partie est terminée. Cliquez sur 'Nouvelle partie' pour recommencer.");
            } else if (boardState[row][col] != ' ') {
                updateStatus("⛔ Cette case est déjà occupée!");
            }
            return;
        }
        
        // Make player's move
        boardState[row][col] = playerSymbol;
        cells[row][col].setText(String.valueOf(playerSymbol));
        cells[row][col].setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-font-size: 36px;");
        
        // Check if player won
        if (checkWin(playerSymbol)) {
            updateStatus("🏆 Félicitations! Vous avez gagné!");
            gameOver = true;
            showReplayButton();
            return;
        }
        
        // Check for draw
        if (isBoardFull()) {
            updateStatus("🤝 Match nul!");
            gameOver = true;
            showReplayButton();
            return;
        }
        
        // Computer's turn
        myTurn = false;
        updateStatus("💭 L'ordinateur réfléchit...");
        
        // Add a small delay to make it feel more natural
        new Thread(() -> {
            try {
                Thread.sleep(800);
                Platform.runLater(() -> {
                    makeComputerMove();
                    myTurn = true;  // Player's turn again
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void makeComputerMove() {
        char computerSymbol = playerSymbol == 'X' ? 'O' : 'X';
        
        // Try to win
        if (tryWinningMove(computerSymbol)) return;
        
        // Block player if they're about to win
        if (tryBlockingMove(playerSymbol)) return;
        
        // Try center
        if (boardState[1][1] == ' ') {
            makeMoveAt(1, 1, computerSymbol);
            return;
        }
        
        // Try corners
        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        for (int[] corner : corners) {
            if (boardState[corner[0]][corner[1]] == ' ') {
                makeMoveAt(corner[0], corner[1], computerSymbol);
                return;
            }
        }
        
        // Any remaining square
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (boardState[row][col] == ' ') {
                    makeMoveAt(row, col, computerSymbol);
                    return;
                }
            }
        }
    }
    
    private boolean tryWinningMove(char symbol) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (boardState[row][col] == ' ') {
                    // Try move
                    boardState[row][col] = symbol;
                    boolean isWinning = checkWin(symbol);
                    if (isWinning) {
                        if (symbol == playerSymbol) {
                            // Just checking, don't actually make the move
                            boardState[row][col] = ' ';
                        } else {
                            // Make the winning move for computer
                            makeMoveAt(row, col, symbol);
                        }
                        return true;
                    }
                    boardState[row][col] = ' '; // Reset
                }
            }
        }
        return false;
    }
    
    private boolean tryBlockingMove(char playerSymbol) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (boardState[row][col] == ' ') {
                    // Try player move to see if they would win
                    boardState[row][col] = playerSymbol;
                    boolean wouldWin = checkWin(playerSymbol);
                    boardState[row][col] = ' '; // Reset
                    
                    if (wouldWin) {
                        // Block it
                        char computerSymbol = playerSymbol == 'X' ? 'O' : 'X';
                        makeMoveAt(row, col, computerSymbol);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void makeMoveAt(int row, int col, char symbol) {
        boardState[row][col] = symbol;
        
        Platform.runLater(() -> {
            cells[row][col].setText(String.valueOf(symbol));
            cells[row][col].setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 36px;");
            
            // Check if computer won
            if (checkWin(symbol)) {
                updateStatus("😢 L'ordinateur a gagné!");
                gameOver = true;
                showReplayButton();
                return;
            }
            
            // Check for draw
            if (isBoardFull()) {
                updateStatus("🤝 Match nul!");
                gameOver = true;
                showReplayButton();
                return;
            }
            
            updateStatus("🎲 À votre tour!");
        });
    }
    
    private void connectToServer() {
        // Only applicable in multiplayer mode
        if (!multiPlayerMode) return;
        
        // Reset state
        resetBoard();
        gameOver = false;
        myTurn = false;
        updateStatus("Connexion au serveur " + serverAddress + ":" + port + "...");
        
        connectionThread = new Thread(() -> {
            try {
                // Show connecting state
                Platform.runLater(() -> {
                    connectionLabel.setText("🟡 Connexion en cours...");
                    connectionLabel.setTextFill(Color.ORANGE);
                    reconnectButton.setDisable(true);
                });
                
                // Try to connect with timeout
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, port), 5000); // 5 second timeout
                connectionActive = true;
                
                // Set up communication streams with proper encoding
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                output = new PrintWriter(socket.getOutputStream(), true);
                
                Platform.runLater(() -> {
                    connectionLabel.setText("🟢 Connecté");
                    connectionLabel.setTextFill(Color.GREEN);
                    reconnectButton.setDisable(true);
                    updateStatus("Connecté au serveur. En attente du début de la partie...");
                });
                
                // Listen for server messages
                String line;
                StringBuilder boardData = new StringBuilder();
                boolean collectingBoardData = false;
                
                while (connectionActive && (line = input.readLine()) != null) {
                    final String message = line;
                    debugLog("Message du serveur: [" + message + "]");
                    
                    // Check for game start and first turn
                    if (message.contains("commence") || message.contains("commence la partie")) {
                        if (message.contains("Vous commencez") || message.contains("Votre tour")) {
                            Platform.runLater(() -> {
                                myTurn = true;
                                updateStatus("🎲 Vous commencez la partie! C'est votre tour.");
                                updateTurnStatus(true);
                            });
                        } else {
                            Platform.runLater(() -> {
                                myTurn = false;
                                updateStatus("⌛ L'adversaire commence. Attendez votre tour...");
                                updateTurnStatus(false);
                            });
                        }
                        continue;
                    }
                    
                    // Check if this is the start of a board display
                    if (message.contains("┌───┬───┬───┐") || message.contains("0   1   2")) {
                        collectingBoardData = true;
                        boardData.setLength(0); // Clear any previous data
                        boardData.append(message).append("\n");
                        continue;
                    }
                    
                    // If we're collecting board data, keep appending
                    if (collectingBoardData) {
                        boardData.append(message).append("\n");
                        
                        // Check if we've reached the end of the board display
                        if (message.contains("└───┴───┴───┘")) {
                            collectingBoardData = false;
                            final String completeBoard = boardData.toString();
                            
                            Platform.runLater(() -> {
                                updateBoard(completeBoard);
                            });
                            continue;
                        }
                        // Continue collecting more board data
                        continue;
                    }
                    
                    // Handle regular messages
                    Platform.runLater(() -> {
                        if (message.contains("Votre tour") || message.contains("C'est votre tour")) {
                            myTurn = true;
                            updateStatus("🎲 C'est votre tour! Cliquez sur une case.");
                            updateTurnStatus(true);
                        } else if (message.contains("gagné") || message.contains("perdu") || message.contains("nul")) {
                            gameOver = true;
                            showGameResult(message);
                        } else if (message.contains("Coup invalide") || message.contains("occupée")) {
                            myTurn = true; // Give the turn back to the player
                            updateStatus("⛔ " + message);
                            updateTurnStatus(true);
                        } else {
                            updateStatus(message);
                        }
                    });
                }
            } catch (ConnectException e) {
                Platform.runLater(() -> {
                    updateStatus("❌ Connexion refusée: Le serveur n'est pas disponible sur " + 
                                 serverAddress + ":" + port);
                    connectionLabel.setText("⚫ Déconnecté");
                    connectionLabel.setTextFill(Color.RED);
                    reconnectButton.setDisable(false);
                });
            } catch (SocketTimeoutException e) {
                Platform.runLater(() -> {
                    updateStatus("⏱️ Délai de connexion dépassé. Vérifiez l'adresse et le port.");
                    connectionLabel.setText("⚫ Déconnecté");
                    connectionLabel.setTextFill(Color.RED);
                    reconnectButton.setDisable(false);
                });
            } catch (UnknownHostException e) {
                Platform.runLater(() -> {
                    updateStatus("❓ Adresse serveur inconnue: " + serverAddress);
                    connectionLabel.setText("⚫ Déconnecté");
                    connectionLabel.setTextFill(Color.RED);
                    reconnectButton.setDisable(false);
                });
            } catch (Exception e) {
                if (connectionActive) {
                    debugLog("Error in server communication: " + e.getMessage());
                    e.printStackTrace();
                    
                    Platform.runLater(() -> {
                        updateStatus("❌ Erreur de communication: " + e.getMessage());
                        connectionLabel.setText("⚫ Déconnecté");
                        connectionLabel.setTextFill(Color.RED);
                        reconnectButton.setDisable(false);
                    });
                }
            } finally {
                connectionActive = false;
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    debugLog("Error closing socket: " + e.getMessage());
                }
                
                Platform.runLater(() -> {
                    reconnectButton.setDisable(false);
                    connectionLabel.setText("⚫ Déconnecté");
                    connectionLabel.setTextFill(Color.RED);
                });
            }
        });
        
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
    
    private void updateBoard(String boardData) {
        // First log the raw data we received
        debugLog("Raw board data received:\n" + boardData);
        
        // Try to parse it
        parseBoard(boardData);
        
        // Update UI from the parsed board state
        updateUIFromBoardState();
        
        debugLog("UI updated successfully");
    }
    
    private void parseBoard(String boardData) {
        try {
            debugLog("Parsing board data...");
            
            String[] lines = boardData.split("\n");
            
            // Reset board state before parsing
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    boardState[i][j] = ' ';
                }
            }
            
            // Manually extract board data - simpler, more reliable approach
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // Find lines that contain row data - they have the pattern "n │ X │ O │   │"
                // where n is the row number (0-2)
                if (line.contains("│") && line.length() > 3) {
                    // Check if this is a row content line (starts with a digit followed by space and │)
                    if (line.trim().length() > 0 && Character.isDigit(line.trim().charAt(0))) {
                        int rowIdx = Character.getNumericValue(line.trim().charAt(0));
                        
                        if (rowIdx >= 0 && rowIdx <= 2) {
                            // Find the positions of vertical bars
                            int[] barPositions = new int[4]; // We expect 4 vertical bars
                            int barCount = 0;
                            
                            for (int pos = 0; pos < line.length() && barCount < 4; pos++) {
                                if (line.charAt(pos) == '│') {
                                    barPositions[barCount++] = pos;
                                }
                            }
                            
                            // Extract cell contents
                            if (barCount == 4) {
                                for (int col = 0; col < 3; col++) {
                                    // Get text between vertical bars
                                    String cellContent = line.substring(barPositions[col] + 1, barPositions[col + 1]).trim();
                                    if (cellContent.equals("X")) {
                                        boardState[rowIdx][col] = 'X';
                                    } else if (cellContent.equals("O")) {
                                        boardState[rowIdx][col] = 'O';
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Log the parsed board state
            logBoardState();
            
        } catch (Exception e) {
            debugLog("Error parsing board: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateUIFromBoardState() {
        Platform.runLater(() -> {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    final char symbol = boardState[row][col];
                    if (symbol == 'X') {
                        cells[row][col].setText("X");
                        cells[row][col].setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-font-size: 36px;");
                    } else if (symbol == 'O') {
                        cells[row][col].setText("O");
                        cells[row][col].setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 36px;");
                    } else {
                        cells[row][col].setText("");
                        cells[row][col].setStyle("-fx-background-color: white; -fx-border-color: #aaaaaa; -fx-border-width: 1px;");
                    }
                }
            }
            
            // Highlight the last played position if available
            if (lastPlayedRow >= 0 && lastPlayedCol >= 0) {
                String currentStyle = cells[lastPlayedRow][lastPlayedCol].getStyle();
                cells[lastPlayedRow][lastPlayedCol].setStyle(currentStyle + "; -fx-border-color: #00AA00; -fx-border-width: 2px;");
            }
        });
    }
    
    private void updateTurnStatus(boolean isMyTurn) {
        Platform.runLater(() -> {
            if (isMyTurn) {
                statusLabel.setText("🎲 VOTRE TOUR - Cliquez sur une case");
                statusLabel.setStyle("-fx-background-color: #e0ffe0; -fx-padding: 5px; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("⌛ Attente du tour de l'adversaire...");
                statusLabel.setStyle("-fx-background-color: #fff0f0; -fx-padding: 5px;");
            }
        });
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void showGameResult(String result) {
        String title = "Fin de la partie";
        String headerText;
    
        if (result.contains("gagné")) {
            headerText = "🏆 Victoire !";
        } else if (result.contains("perdu") || result.contains("serveur a gagné")) {
            headerText = "😢 Défaite";
        } else {
            headerText = "🤝 Match nul";
        }
    
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(result + "\n\nVoulez-vous rejouer ?");
    
        alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);
    
        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.YES) {
                // Send replay request to the server
                if (connectionActive && output != null) {
                    output.println("REPLAY");
                    resetBoard();
                    updateStatus("En attente d'une nouvelle partie...");
                    gameOver = false;
                }
            } else {
                // Send quit notification
                if (connectionActive && output != null) {
                    output.println("QUIT");
                }
                disconnect();
            }
        });
    }
    
    private void showReplayButton() {
        Platform.runLater(() -> {
            replayButton.setVisible(true);
        });
    }
    
    private void resetGame() {
        resetBoard();
        gameOver = false;
        replayButton.setVisible(false);
        lastPlayedRow = -1;
        lastPlayedCol = -1;
        
        if (multiPlayerMode) {
            if (connectionActive) {
                output.println("REPLAY");
                updateStatus("En attente d'une nouvelle partie...");
            } else {
                connectToServer();
            }
        } else {
            myTurn = true;
            updateStatus("🎲 À votre tour! Cliquez sur une case pour jouer.");
        }
    }
    
    private void resetBoard() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                boardState[row][col] = ' ';
                
                if (cells[row][col] != null) {
                    cells[row][col].setText("");
                    cells[row][col].setStyle("-fx-background-color: white; -fx-border-color: #aaaaaa; -fx-border-width: 1px;");
                }
            }
        }
    }
    
    private boolean checkWin(char symbol) {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (boardState[i][0] == symbol && boardState[i][1] == symbol && boardState[i][2] == symbol) {
                return true;
            }
        }
        
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (boardState[0][i] == symbol && boardState[1][i] == symbol && boardState[2][i] == symbol) {
                return true;
            }
        }
        
        // Check diagonals
        if (boardState[0][0] == symbol && boardState[1][1] == symbol && boardState[2][2] == symbol) {
            return true;
        }
        
        if (boardState[0][2] == symbol && boardState[1][1] == symbol && boardState[2][0] == symbol) {
            return true;
        }
        
        return false;
    }
    
    private boolean isBoardFull() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (boardState[row][col] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    private void disconnect() {
        connectionActive = false;
        
        if (output != null) {
            output.println("QUIT");
        }
        
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                debugLog("Error closing socket: " + e.getMessage());
            }
        }
        
        Platform.runLater(() -> {
            connectionLabel.setText("⚫ Déconnecté");
            connectionLabel.setTextFill(Color.RED);
            reconnectButton.setDisable(false);
            updateStatus("Déconnecté du serveur");
        });
    }
    
    private void debugLog(String message) {
        if (DEBUG_MODE) {
            System.out.println("[CLIENT-DEBUG] " + message);
        }
    }
    
    private void logBoardState() {
        if (DEBUG_MODE) {
            System.out.println("Current Board State:");
            for (int i = 0; i < 3; i++) {
                System.out.println(boardState[i][0] + " | " + boardState[i][1] + " | " + boardState[i][2]);
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}