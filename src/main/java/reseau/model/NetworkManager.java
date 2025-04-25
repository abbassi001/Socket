package reseau.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire de la communication réseau pour le jeu de Morpion.
 * Gère la connexion au serveur, l'envoi et la réception de messages.
 */
public class NetworkManager {
    private static final Logger LOGGER = Logger.getLogger(NetworkManager.class.getName());
    private static final int CONNECTION_TIMEOUT = 5000; // 5 secondes
    
    private GameModel gameModel;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread receiverThread;
    private boolean connected = false;
    
    // Paramètres de connexion
    private String serverAddress = "127.0.0.1";
    private int port = 55555;
    
    // État du traitement des données
    private boolean collectingBoardData = false;
    private StringBuilder boardBuffer = new StringBuilder();
    
    /**
     * Constructeur
     * @param gameModel Le modèle de jeu à mettre à jour
     */
    public NetworkManager(GameModel gameModel) {
        this.gameModel = gameModel;
    }
    
    /**
     * Connecte au serveur
     * @param serverAddress Adresse du serveur
     * @param port Port du serveur
     */
    public void connect(String serverAddress, int port) {
        // Mémoriser les paramètres de connexion pour reconnexion éventuelle
        this.serverAddress = serverAddress;
        this.port = port;
        
        // Démarrer la connexion dans un thread séparé pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                LOGGER.info("Connexion au serveur " + serverAddress + ":" + port + "...");
                
                // Update UI to show connecting status
                updateConnectionStatus("Connexion en cours...", false);
                
                // Create socket with timeout
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, port), CONNECTION_TIMEOUT);
                
                // Set up communication streams
                input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                output = new PrintWriter(socket.getOutputStream(), true);
                
                // Connection established
                connected = true;
                updateConnectionStatus("Connecté au serveur", true);
                
                // Start listening for server messages
                startMessageReceiver();
                
            } catch (SocketTimeoutException e) {
                LOGGER.log(Level.SEVERE, "Délai de connexion dépassé", e);
                updateConnectionStatus("Délai de connexion dépassé", false);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Erreur lors de la connexion au serveur", e);
                updateConnectionStatus("Erreur de connexion: " + e.getMessage(), false);
            }
        }).start();
    }
    
    /**
     * Reconnecte au serveur en utilisant les derniers paramètres utilisés
     */
    public void reconnect() {
        disconnect();
        connect(serverAddress, port);
    }
    
    /**
     * Déconnecte du serveur
     */
    public void disconnect() {
        if (!connected) return;
        
        try {
            connected = false;
            
            // Send quit message if possible
            if (output != null) {
                output.println("QUIT");
            }
            
            // Close socket
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            // Interrupt receiver thread
            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
            }
            
            // Clear resources
            input = null;
            output = null;
            socket = null;
            
            // Update UI
            updateConnectionStatus("Déconnecté", false);
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur lors de la fermeture de la connexion", e);
        }
    }
    
    /**
     * Démarre le thread de réception des messages du serveur
     */
    private void startMessageReceiver() {
        receiverThread = new Thread(() -> {
            try {
                String line;
                
                // Traiter les options initiales du serveur
                handleInitialServerInteraction();
                
                // Boucle principale de réception
                while (connected && (line = input.readLine()) != null) {
                    processServerMessage(line);
                }
            } catch (IOException e) {
                if (connected) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la lecture des messages du serveur", e);
                    updateConnectionStatus("Connexion perdue: " + e.getMessage(), false);
                    connected = false;
                }
            }
        });
        
        receiverThread.setDaemon(true);
        receiverThread.start();
    }
    
    /**
     * Gère l'interaction initiale avec le serveur (choix IA ou attente d'adversaire)
     */
    private void handleInitialServerInteraction() throws IOException {
        // Attendre le message de bienvenue
        String welcomeMsg = input.readLine();
        updateStatus(welcomeMsg);
        
        // Attendre la question du choix de mode
        String choiceMsg = input.readLine();
        updateStatus(choiceMsg);
        
        // Choisir d'attendre un adversaire humain
        output.println("WAIT");
        updateStatus("En attente d'un adversaire...");
    }
    
    /**
     * Traite les messages reçus du serveur
     */
    private void processServerMessage(String message) {
        LOGGER.fine("Message reçu: " + message);
        
        // Collecter les données du plateau si en cours
        if (collectingBoardData) {
            boardBuffer.append(message).append("\n");
            
            // Vérifier si c'est la fin du plateau
            if (message.contains("└───┴───┴───┘")) {
                collectingBoardData = false;
                processBoard(boardBuffer.toString());
                boardBuffer.setLength(0);
            }
            return;
        }
        
        // Début des données du plateau
        if (message.contains("┌───┬───┬───┐") || message.contains("0   1   2")) {
            collectingBoardData = true;
            boardBuffer.setLength(0);
            boardBuffer.append(message).append("\n");
            return;
        }
        
        // Pour les autres messages, traiter selon leur contenu
        if (message.contains("commence") || message.contains("commence la partie")) {
            // Indication sur qui commence
            boolean myTurn = message.contains("Vous commencez") || 
                              message.contains("VOTRE TOUR");
            gameModel.updateFromServer(gameModel.getGameState(), myTurn);
            updateStatus(message);
        } else if (message.contains("Votre tour") || message.contains("C'EST VOTRE TOUR")) {
            // C'est notre tour
            gameModel.updateFromServer(gameModel.getGameState(), true);
            updateStatus("🎲 C'est votre tour! Cliquez sur une case pour jouer.");
        } else if (message.contains("attendez") || message.contains("attente")) {
            // Attente du tour de l'adversaire
            gameModel.updateFromServer(gameModel.getGameState(), false);
            updateStatus(message);
        } else if (message.contains("gagné") && !message.contains("perdu")) {
            // Victoire
            updateStatus(message);
            gameModel.notifyGameWon();
        } else if (message.contains("perdu") || message.contains("a gagné")) {
            // Défaite
            updateStatus(message);
            gameModel.notifyGameLost();
        } else if (message.contains("nul")) {
            // Match nul
            updateStatus(message);
            gameModel.notifyGameDraw();
        } else if (message.contains("Coup invalide") || message.contains("occupée")) {
            // Coup invalide, on redonne le tour au joueur
            updateStatus("⛔ " + message);
            gameModel.updateFromServer(gameModel.getGameState(), true);
        } else {
            // Autres messages
            updateStatus(message);
        }
    }
    
    /**
     * Traite les données du plateau reçues du serveur
     */
    private void processBoard(String boardData) {
        try {
            GameState newState = new GameState();
            String[] lines = boardData.split("\n");
            
            for (String line : lines) {
                if (line.contains("│") && line.length() > 3) {
                    // Recherche des lignes qui contiennent des données de cellule
                    if (line.trim().length() > 0 && Character.isDigit(line.trim().charAt(0))) {
                        int rowIdx = Character.getNumericValue(line.trim().charAt(0));
                        
                        if (rowIdx >= 0 && rowIdx <= 2) {
                            // Trouver les positions des barres verticales
                            int[] barPositions = new int[4]; // 4 barres attendues
                            int barCount = 0;
                            
                            for (int pos = 0; pos < line.length() && barCount < 4; pos++) {
                                if (line.charAt(pos) == '│') {
                                    barPositions[barCount++] = pos;
                                }
                            }
                            
                            // Extraire le contenu des cellules
                            if (barCount == 4) {
                                for (int col = 0; col < 3; col++) {
                                    String cellContent = line.substring(barPositions[col] + 1, barPositions[col + 1]).trim();
                                    if (cellContent.equals("X")) {
                                        newState.setCell(rowIdx, col, 'X');
                                    } else if (cellContent.equals("O")) {
                                        newState.setCell(rowIdx, col, 'O');
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Mettre à jour le modèle avec le nouvel état
            gameModel.updateFromServer(newState, gameModel.isMyTurn());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'analyse du plateau", e);
        }
    }
    
    /**
     * Envoie un coup au serveur
     * @param row Ligne (0-2)
     * @param col Colonne (0-2)
     */
    public void sendMove(int row, int col) {
        if (!connected || output == null) return;
        
        String moveCmd = row + "" + col;
        LOGGER.info("Envoi du coup: " + moveCmd);
        output.println(moveCmd);
    }
    
    /**
     * Envoie une demande de nouvelle partie
     */
    public void sendReplayRequest() {
        if (!connected || output == null) return;
        
        LOGGER.info("Demande de nouvelle partie");
        output.println("REPLAY");
    }
    
    /**
     * Met à jour le statut de connexion dans le modèle
     */
    private void updateConnectionStatus(String message, boolean isConnected) {
        LOGGER.info(message);
        
        // Appliquer les mises à jour sur le thread JavaFX
        javafx.application.Platform.runLater(() -> {
            gameModel.setStatusMessage(message);
            gameModel.setConnected(isConnected);
        });
    }
    
    /**
     * Met à jour le message de statut
     */
    private void updateStatus(String message) {
        javafx.application.Platform.runLater(() -> {
            gameModel.setStatusMessage(message);
        });
    }
    
    /**
     * Vérifie si le client est actuellement connecté au serveur
     */
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
    
    /**
     * Récupère l'adresse du serveur
     */
    public String getServerAddress() {
        return serverAddress;
    }
    
    /**
     * Récupère le port du serveur
     */
    public int getPort() {
        return port;
    }
}