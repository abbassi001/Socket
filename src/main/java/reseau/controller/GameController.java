package reseau.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import reseau.model.GameModel;
import reseau.model.GameState;
import reseau.model.ThemeManager;
import reseau.view.GameView;
import reseau.view.theme.ThemeFactory.ThemeType;

public class GameController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(GameController.class.getName());
    
    // Éléments FXML
    @FXML private GridPane gameBoard;
    @FXML private Pane boardPane;
    @FXML private Label statusLabel;
    @FXML private Label scoreLabel;
    @FXML private Label connectionLabel;
    @FXML private Button replayButton;
    @FXML private Button reconnectButton;
    @FXML private MenuBar menuBar;
    
    // Cellules du jeu
    private Button[][] cells = new Button[3][3];
    
    // Ligne de victoire
    private Line winLine;
    
    // Modèle
    private GameModel gameModel;
    
    // Vue principale
    private GameView gameView;
    
    // Scene
    private Scene gameScene;
    
    // Theme manager
    private ThemeManager themeManager = new ThemeManager();

    // Current theme
    private ThemeType currentTheme;
    
    // Sons
    private AudioClip clickSound;
    private AudioClip winSound;
    private AudioClip loseSound;
    private AudioClip drawSound;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation du modèle
        gameModel = new GameModel();
        
        // Initialisation des cellules du plateau
        initializeBoardCells();
        
        // Initialisation de la ligne de victoire
        setupWinLine();
        
        // Chargement des sons
        loadSounds();
        
        // Liaison des propriétés observables du modèle
        bindModelProperties();
    }
    
    /**
     * Configure la référence à la vue principale
     */
    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }
    
    /**
     * Configure la scène du jeu
     */
    public void setGameScene(Scene scene) {
        this.gameScene = scene;
        
        // Apply the theme once we have the scene
        if (currentTheme != null) {
            applyTheme();
        }
    }
    
    /**
     * Configure le thème actuel
     */
    public void setCurrentTheme(ThemeType theme) {
        this.currentTheme = theme;
        applyTheme();
    }
    
    /**
     * Initialise le jeu avec les paramètres spécifiés
     */
    public void initializeGame(boolean multiplayerMode, String serverAddress, int port) {
        gameModel.initGame(multiplayerMode, serverAddress, port);
        
        // Mise à jour de l'interface
        updateConnectionStatus();
        updateScoreDisplay();
        
        // Animation de démarrage
        playStartGameAnimation();
    }
    
    /**
     * Initialise les cellules du plateau de jeu
     */
    private void initializeBoardCells() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button cell = new Button();
                cell.setPrefSize(100, 100);
                cell.getStyleClass().add("game-cell");
                
                // Configurer les effets de survol
                setupCellHoverEffects(cell);
                
                // Configurer l'action de clic
                final int finalRow = row;
                final int finalCol = col;
                cell.setOnAction(e -> handleCellClick(finalRow, finalCol));
                
                // Ajouter à la grille et stocker dans le tableau
                gameBoard.add(cell, col, row);
                cells[row][col] = cell;
            }
        }
    }
    
    /**
     * Configure les effets de survol pour une cellule
     */
    private void setupCellHoverEffects(Button cell) {
        Glow glowEffect = new Glow(0.5);
        
        cell.setOnMouseEntered(e -> {
            if (cell.getText().isEmpty() && gameModel.isMyTurn() && !gameModel.isGameOver()) {
                cell.setEffect(glowEffect);
                cell.getStyleClass().add("cell-hover");
            }
        });
        
        cell.setOnMouseExited(e -> {
            cell.setEffect(null);
            cell.getStyleClass().remove("cell-hover");
        });
    }
    
    /**
     * Configure la ligne de victoire
     */
    private void setupWinLine() {
        winLine = new Line();
        winLine.setStrokeWidth(5);
        winLine.setStroke(Color.GOLD);
        winLine.setVisible(false);
        winLine.setMouseTransparent(true);
        
        boardPane.getChildren().add(winLine);
    }
    
    /**
     * Charge les sons du jeu
     */
    private void loadSounds() {
        try {
            String clickSoundPath = getClass().getResource("/sounds/click.wav").toExternalForm();
            String winSoundPath = getClass().getResource("/sounds/win.wav").toExternalForm();
            String loseSoundPath = getClass().getResource("/sounds/lose.wav").toExternalForm();
            String drawSoundPath = getClass().getResource("/sounds/draw.wav").toExternalForm();
            
            clickSound = new AudioClip(clickSoundPath);
            winSound = new AudioClip(winSoundPath);
            loseSound = new AudioClip(loseSoundPath);
            drawSound = new AudioClip(drawSoundPath);
        } catch (Exception e) {
            LOGGER.warning("Impossible de charger les sons: " + e.getMessage());
        }
    }
    
    /**
     * Lie les propriétés observables du modèle
     */
    private void bindModelProperties() {
        // Liaison du message de statut
        gameModel.statusMessageProperty().addListener((obs, oldVal, newVal) -> {
            statusLabel.setText(newVal);
        });
        
        // Liaison de l'état du jeu
        gameModel.gameStateProperty().addListener((obs, oldVal, newVal) -> {
            updateBoardDisplay();
        });
        
        // Liaison de l'état de fin de partie
        gameModel.gameOverProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                replayButton.setVisible(true);
                animateGameResult();
            } else {
                replayButton.setVisible(false);
                winLine.setVisible(false);
            }
        });
        
        // Liaison de l'état de connexion
        gameModel.connectedProperty().addListener((obs, oldVal, newVal) -> {
            updateConnectionStatus();
        });
        
        // Liaison des scores
        gameModel.playerScoreProperty().addListener((obs, oldVal, newVal) -> updateScoreDisplay());
        gameModel.opponentScoreProperty().addListener((obs, oldVal, newVal) -> updateScoreDisplay());
        gameModel.drawCountProperty().addListener((obs, oldVal, newVal) -> updateScoreDisplay());
    }
    
    /**
     * Gère le clic sur une cellule
     */
    private void handleCellClick(int row, int col) {
        boolean moveAccepted = gameModel.handleCellClick(row, col);
        
        if (moveAccepted && themeManager.isSoundEnabled() && clickSound != null) {
            clickSound.play();
        }
    }
    
    /**
     * Met à jour l'affichage du plateau
     */
    private void updateBoardDisplay() {
        GameState state = gameModel.getGameState();
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                char symbol = state.getCell(row, col);
                Button cell = cells[row][col];
                
                // Mettre à jour le texte
                cell.setText(symbol == ' ' ? "" : String.valueOf(symbol));
                
                // Appliquer le style approprié
                if (symbol == 'X') {
                    cell.getStyleClass().removeAll("cell-o", "cell-empty");
                    cell.getStyleClass().add("cell-x");
                } else if (symbol == 'O') {
                    cell.getStyleClass().removeAll("cell-x", "cell-empty");
                    cell.getStyleClass().add("cell-o");
                } else {
                    cell.getStyleClass().removeAll("cell-x", "cell-o");
                    cell.getStyleClass().add("cell-empty");
                }
                
                // Mettre en évidence le dernier coup joué
                if (row == state.getLastPlayedRow() && col == state.getLastPlayedCol() && symbol != ' ') {
                    DropShadow highlight = new DropShadow();
                    highlight.setColor(Color.GOLD);
                    highlight.setWidth(20);
                    highlight.setHeight(20);
                    cell.setEffect(highlight);
                } else {
                    cell.setEffect(null);
                }
            }
        }
    }
    
    /**
     * Met à jour l'affichage du score
     */
    private void updateScoreDisplay() {
        int playerScore = gameModel.playerScoreProperty().get();
        int opponentScore = gameModel.opponentScoreProperty().get();
        int draws = gameModel.drawCountProperty().get();
        
        scoreLabel.setText(String.format("Score: %d - %d - %d", playerScore, opponentScore, draws));
    }
    
    /**
     * Met à jour l'état de la connexion
     */
    private void updateConnectionStatus() {
        if (gameModel.isMultiplayerMode()) {
            boolean connected = gameModel.isConnected();
            
            connectionLabel.setText(connected ? "🟢 Connecté" : "⚫ Déconnecté");
            connectionLabel.setTextFill(connected ? Color.GREEN : Color.RED);
            reconnectButton.setDisable(connected);
        } else {
            connectionLabel.setText("Mode solo");
            connectionLabel.setTextFill(Color.GRAY);
            reconnectButton.setDisable(true);
        }
    }
    
    /**
     * Applique le thème actuel
     */
    private void applyTheme() {
        if (themeManager == null || gameScene == null || gameScene.getRoot() == null) {
            return;
        }
        
        // Obtenir le thème depuis ThemeManager
        ThemeType currentTheme = themeManager.getCurrentTheme();
        if (currentTheme == null) {
            return;
        }
        
        // Appliquer le thème à la scène
        String cssPath = "/css/" + currentTheme.toString().toLowerCase() + ".css";
        try {
            String cssUrl = getClass().getResource(cssPath).toExternalForm();
            gameScene.getStylesheets().clear();
            gameScene.getStylesheets().add(cssUrl);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur lors de l'application du thème: " + e.getMessage(), e);
        }
    }
    
    /**
     * Anime le résultat de la partie
     */
    private void animateGameResult() {
        String status = gameModel.getStatusMessage().toLowerCase();
        
        // Jouer le son approprié
        if (themeManager != null && themeManager.isSoundEnabled()) {
            try {
                if (status.contains("gagné") || status.contains("victoire")) {
                    if (winSound != null) winSound.play();
                } else if (status.contains("perdu") || status.contains("défaite")) {
                    if (loseSound != null) loseSound.play();
                } else if (status.contains("nul")) {
                    if (drawSound != null) drawSound.play();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la lecture du son", e);
            }
        }
        
        // Animer les cellules
        animateWinningLine();
    }
    
    /**
     * Anime la ligne de victoire
     */
    private void animateWinningLine() {
        GameState state = gameModel.getGameState();
        char winner = ' ';
        
        // Déterminer le gagnant
        if (state.checkWin('X')) {
            winner = 'X';
        } else if (state.checkWin('O')) {
            winner = 'O';
        } else {
            // Pas de gagnant, possiblement match nul
            animateDrawGame();
            return;
        }
        
        // Trouver l'index de la ligne gagnante (ligne, colonne, ou diagonale)
        int winningLineIndex = state.getWinningLineIndex(winner);
        
        if (winningLineIndex >= 0) {
            animateWinningCells(state.getWinningCells(winner, winningLineIndex));
        }
    }
    
    /**
     * Anime les cellules gagnantes
     */
    private void animateWinningCells(int[][] winningCells) {
        if (winningCells == null) return;
        
        // Animation pour chaque cellule gagnante
        for (int[] cell : winningCells) {
            int row = cell[0];
            int col = cell[1];
            
            if (row >= 0 && row < 3 && col >= 0 && col < 3 && cells[row][col] != null) {
                // Créer une animation de pulsation pour la cellule
                ScaleTransition pulse = new ScaleTransition(Duration.millis(500), cells[row][col]);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.2);
                pulse.setToY(1.2);
                pulse.setCycleCount(4);
                pulse.setAutoReverse(true);
                pulse.play();
                
                // Changer la couleur de fond pour mettre en évidence la victoire
                cells[row][col].setStyle(cells[row][col].getStyle() + "; -fx-background-color: #FFFF66;");
            }
        }
    }
    
    /**
     * Anime un match nul
     */
    private void animateDrawGame() {
        // Animer toutes les cellules pour un match nul
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (cells[row][col] != null) {
                    // Animation de fade pour chaque cellule
                    FadeTransition fade = new FadeTransition(Duration.millis(1000), cells[row][col]);
                    fade.setFromValue(1.0);
                    fade.setToValue(0.5);
                    fade.setCycleCount(2);
                    fade.setAutoReverse(true);
                    fade.play();
                }
            }
        }
    }
    
    /**
     * Joue une animation de démarrage du jeu
     */
    private void playStartGameAnimation() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button cell = cells[row][col];
                
                cell.setOpacity(0);
                
                FadeTransition fade = new FadeTransition(Duration.seconds(0.2), cell);
                fade.setFromValue(0);
                fade.setToValue(1);
                fade.setDelay(Duration.millis((row * 3 + col) * 50));
                
                fade.play();
            }
        }
    }
    
    /* Gestionnaires d'événements FXML */
    
    @FXML
    private void handleReplay() {
        gameModel.resetGame();
    }
    
    @FXML
    private void handleReconnect() {
        gameModel.connectToServer();
    }
    
    @FXML
    private void handleBackToMenu() {
        if (gameModel.isConnected()) {
            gameModel.disconnect();
        }
        
        if (gameView != null) {
            gameView.showWelcomeScreen();
        }
    }
    
    @FXML
    private void handleExit() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Quitter");
        alert.setHeaderText("Confirmation");
        alert.setContentText("Êtes-vous sûr de vouloir quitter le jeu ?");
        
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (gameModel.isConnected()) {
                gameModel.disconnect();
            }
            Platform.exit();
        }
    }
    
    @FXML
    private void handleSettings() {
        // Ouvrir la fenêtre des paramètres
        if (gameView != null) {
            gameView.showSettingsScreen();
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Paramètres");
            alert.setHeaderText("Paramètres");
            alert.setContentText("La fenêtre des paramètres sera disponible dans une future version.");
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleHelp() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Aide");
        alert.setHeaderText("Comment jouer");
        alert.setContentText("1. Alignez trois symboles identiques horizontalement, verticalement ou en diagonale pour gagner.\n" +
                           "2. Jouez tour à tour en cliquant sur une case vide.\n" +
                           "3. Le premier joueur à aligner trois symboles gagne.\n" +
                           "4. Si toutes les cases sont remplies sans alignement, c'est un match nul.");
        alert.showAndWait();
    }
    
    @FXML
    private void handleAbout() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("Morpion");
        alert.setContentText("Version 1.0\n\nJeu de Morpion avec mode solo et multijoueur en réseau.");
        alert.showAndWait();
    }
}