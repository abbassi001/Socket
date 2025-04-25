package reseau.model;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modèle contenant la logique du jeu de Morpion.
 * Implémente le pattern Observer avec les propriétés JavaFX.
 */
public class GameModel {
    private static final Logger LOGGER = Logger.getLogger(GameModel.class.getName());
    
    // Propriétés observables
    private final ObjectProperty<GameState> gameState = new SimpleObjectProperty<>(new GameState());
    private final BooleanProperty gameOver = new SimpleBooleanProperty(false);
    private final BooleanProperty myTurn = new SimpleBooleanProperty(false);
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("");
    private final IntegerProperty playerScore = new SimpleIntegerProperty(0);
    private final IntegerProperty opponentScore = new SimpleIntegerProperty(0);
    private final IntegerProperty drawCount = new SimpleIntegerProperty(0);
    
    // Configuration
    private final BooleanProperty multiplayerMode = new SimpleBooleanProperty(false);
    
    // Caractères des joueurs
    private char playerSymbol = 'X';
    private char opponentSymbol = 'O';
    
    // Gestionnaire réseau
    private NetworkManager networkManager;
    
    /**
     * Constructeur
     */
    public GameModel() {
        resetGame();
        networkManager = new NetworkManager(this);
    }
    
    /**
     * Initialise le jeu avec les paramètres de configuration
     */
    public void initGame(boolean multiplayer, String serverAddress, int port) {
        this.multiplayerMode.set(multiplayer);
        
        if (multiplayer) {
            // Mode multijoueur - connecter au serveur
            connected.set(false);
            networkManager.connect(serverAddress, port);
        } else {
            // Mode solo contre l'IA
            resetGame();
            myTurn.set(true);
            statusMessage.set("🎲 À votre tour! Cliquez sur une case pour jouer.");
        }
    }
    
    /**
     * Réinitialise l'état du jeu
     */
    public void resetGame() {
        gameState.get().resetBoard();
        gameOver.set(false);
        
        if (!isMultiplayerMode()) {
            myTurn.set(true);
            statusMessage.set("🎲 À votre tour! Cliquez sur une case pour jouer.");
        } else if (isConnected()) {
            networkManager.sendReplayRequest();
            statusMessage.set("En attente d'une nouvelle partie...");
        }
    }
    
    /**
     * Gère un clic sur une cellule du plateau
     * @return true si le coup a été joué avec succès
     */
    public boolean handleCellClick(int row, int col) {
        if (gameOver.get()) {
            statusMessage.set("La partie est terminée. Cliquez sur 'Rejouer' pour une nouvelle partie.");
            return false;
        }
        
        if (!myTurn.get()) {
            statusMessage.set("Ce n'est pas votre tour!");
            return false;
        }
        
        if (multiplayerMode.get() && !connected.get()) {
            statusMessage.set("Vous n'êtes pas connecté au serveur!");
            return false;
        }
        
        if (gameState.get().getCell(row, col) != ' ') {
            statusMessage.set("⛔ Cette case est déjà occupée!");
            return false;
        }
        
        if (multiplayerMode.get()) {
            // Mode multijoueur - envoyer le coup au serveur
            myTurn.set(false);
            networkManager.sendMove(row, col);
            statusMessage.set("Coup joué: " + row + "" + col + ". En attente de la réponse du serveur...");
            return true;
        } else {
            // Mode solo contre l'IA
            return handleSinglePlayerMove(row, col);
        }
    }
    
    /**
     * Traite un coup en mode solo contre l'IA
     */
    private boolean handleSinglePlayerMove(int row, int col) {
        // Jouer le coup du joueur
        gameState.get().setCell(row, col, playerSymbol);
        
        // Vérifier si le joueur a gagné
        if (gameState.get().checkWin(playerSymbol)) {
            gameOver.set(true);
            playerScore.set(playerScore.get() + 1);
            statusMessage.set("🏆 Félicitations! Vous avez gagné!");
            return true;
        }
        
        // Vérifier s'il y a match nul
        if (gameState.get().isBoardFull()) {
            gameOver.set(true);
            drawCount.set(drawCount.get() + 1);
            statusMessage.set("🤝 Match nul!");
            return true;
        }
        
        // Tour de l'IA
        myTurn.set(false);
        statusMessage.set("💭 L'ordinateur réfléchit...");
        
        // Ajouter un délai pour simuler la réflexion de l'IA
        new Thread(() -> {
            try {
                Thread.sleep(800);
                Platform.runLater(() -> makeAIMove());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Interruption pendant le délai IA", e);
            }
        }).start();
        
        return true;
    }
    
    /**
     * Fait jouer l'IA
     */
    private void makeAIMove() {
        // Stratégie de l'IA (implémentation simple)
        boolean hasMoved = false;
        
        // 1. Essayer de gagner
        hasMoved = tryWinningMove(opponentSymbol);
        if (hasMoved) return;
        
        // 2. Bloquer le joueur s'il est sur le point de gagner
        hasMoved = tryWinningMove(playerSymbol);
        if (hasMoved) return;
        
        // 3. Jouer au centre si possible
        if (gameState.get().getCell(1, 1) == ' ') {
            gameState.get().setCell(1, 1, opponentSymbol);
            finishAIMove();
            return;
        }
        
        // 4. Jouer dans un coin
        int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
        for (int[] corner : corners) {
            if (gameState.get().getCell(corner[0], corner[1]) == ' ') {
                gameState.get().setCell(corner[0], corner[1], opponentSymbol);
                finishAIMove();
                return;
            }
        }
        
        // 5. Jouer sur le premier emplacement disponible
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (gameState.get().getCell(row, col) == ' ') {
                    gameState.get().setCell(row, col, opponentSymbol);
                    finishAIMove();
                    return;
                }
            }
        }
    }
    
    /**
     * Essaie de trouver un coup gagnant
     */
    private boolean tryWinningMove(char symbol) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (gameState.get().getCell(row, col) == ' ') {
                    // Essayer le coup
                    GameState tempState = gameState.get().copy();
                    tempState.setCell(row, col, symbol);
                    
                    if (tempState.checkWin(symbol)) {
                        if (symbol == opponentSymbol) {
                            // IA gagne
                            gameState.get().setCell(row, col, opponentSymbol);
                            finishAIMove();
                            return true;
                        } else {
                            // Bloquer le joueur
                            gameState.get().setCell(row, col, opponentSymbol);
                            finishAIMove();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Finalise le coup de l'IA
     */
    private void finishAIMove() {
        // Vérifier si l'IA a gagné
        if (gameState.get().checkWin(opponentSymbol)) {
            gameOver.set(true);
            opponentScore.set(opponentScore.get() + 1);
            statusMessage.set("😢 L'ordinateur a gagné!");
            return;
        }
        
        // Vérifier s'il y a match nul
        if (gameState.get().isBoardFull()) {
            gameOver.set(true);
            drawCount.set(drawCount.get() + 1);
            statusMessage.set("🤝 Match nul!");
            return;
        }
        
        // Retour au tour du joueur
        myTurn.set(true);
        statusMessage.set("🎲 À votre tour!");
    }
    
    /**
     * Se connecte au serveur
     */
    public void connectToServer() {
        if (networkManager != null) {
            networkManager.reconnect();
        }
    }
    
    /**
     * Se déconnecte du serveur
     */
    public void disconnect() {
        if (networkManager != null) {
            networkManager.disconnect();
        }
    }
    
    /**
     * Callback appelé quand le serveur envoie un nouvel état de jeu
     */
    public void updateFromServer(GameState newState, boolean isMyTurn) {
        gameState.set(newState);
        myTurn.set(isMyTurn);
    }
    
    /**
     * Callback appelé quand une partie est gagnée
     */
    public void notifyGameWon() {
        gameOver.set(true);
        playerScore.set(playerScore.get() + 1);
    }
    
    /**
     * Callback appelé quand une partie est perdue
     */
    public void notifyGameLost() {
        gameOver.set(true);
        opponentScore.set(opponentScore.get() + 1);
    }
    
    /**
     * Callback appelé quand une partie est nulle
     */
    public void notifyGameDraw() {
        gameOver.set(true);
        drawCount.set(drawCount.get() + 1);
    }
    
    /**
     * Met à jour le message de statut
     */
    public void setStatusMessage(String message) {
        statusMessage.set(message);
    }
    
    /**
     * Met à jour l'état de connexion
     */
    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }
    
    // Getters pour les propriétés observables
    
    public ObjectProperty<GameState> gameStateProperty() {
        return gameState;
    }
    
    public GameState getGameState() {
        return gameState.get();
    }
    
    public BooleanProperty gameOverProperty() {
        return gameOver;
    }
    
    public boolean isGameOver() {
        return gameOver.get();
    }
    
    public BooleanProperty myTurnProperty() {
        return myTurn;
    }
    
    public boolean isMyTurn() {
        return myTurn.get();
    }
    
    public BooleanProperty connectedProperty() {
        return connected;
    }
    
    public boolean isConnected() {
        return connected.get();
    }
    
    public StringProperty statusMessageProperty() {
        return statusMessage;
    }
    
    public String getStatusMessage() {
        return statusMessage.get();
    }
    
    public IntegerProperty playerScoreProperty() {
        return playerScore;
    }
    
    public IntegerProperty opponentScoreProperty() {
        return opponentScore;
    }
    
    public IntegerProperty drawCountProperty() {
        return drawCount;
    }
    
    public BooleanProperty multiplayerModeProperty() {
        return multiplayerMode;
    }
    
    public boolean isMultiplayerMode() {
        return multiplayerMode.get();
    }
}