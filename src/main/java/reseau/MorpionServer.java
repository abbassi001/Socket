package reseau;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MorpionServer {
    // Server configuration
    private static final int MAX_CLIENTS = 50;
    private static final int DEFAULT_PORT = 55555;
    private static final boolean DEBUG_MODE = true;
    
    // Thread pool for handling client connections
    private static ExecutorService threadPool;
    
    // Player matching and game management
    private static final List<PlayerHandler> waitingPlayers = new ArrayList<>();
    private static final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Format de port invalide, utilisation du port par défaut: " + port);
            }
        }

        // Create thread pool for handling multiple clients
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🎮 Serveur Morpion en attente de connexion sur le port " + port + "...");
            System.out.println("✨ Serveur multi-joueurs activé avec option IA");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Client connecté : " + clientSocket.getInetAddress());
                
                // Create handler for this new player
                PlayerHandler playerHandler = new PlayerHandler(clientSocket);
                threadPool.submit(playerHandler);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    /**
     * Inner class to represent a game between two players
     */
    private static class Game {
        private final PlayerHandler player1;
        private final PlayerHandler player2;  // Can be null for AI games
        private final String gameId;
        private char[][] board = new char[3][3];
        private boolean gameActive = true;
        private PlayerHandler currentTurn;
        private boolean againstAI = false;
        private boolean startingPlayerAlternates = true;

        /**
         * Constructor for a game between two human players
         */
        public Game(PlayerHandler player1, PlayerHandler player2) {
            this.player1 = player1;
            this.player2 = player2;
            this.gameId = generateGameId();
            this.againstAI = false;
            
            initializeGame();
        }
        
        /**
         * Constructor for a game against the AI
         */
        public Game(PlayerHandler humanPlayer, boolean humanStarts) {
            this.player1 = humanPlayer;
            this.player2 = null;  // No second player for AI games
            this.gameId = generateGameId();
            this.againstAI = true;
            
            initializeGame();
            
            // Set who starts
            if (humanStarts) {
                currentTurn = player1;
                System.out.println("🎲 " + player1.getPlayerName() + " commence la partie contre l'IA");
                player1.sendMessage("🎲 Vous commencez. C'EST VOTRE TOUR (X)! Format: ligne colonne (ex: 01)");
            } else {
                currentTurn = null; // Indicates AI's turn
                System.out.println("🤖 L'IA commence la partie contre " + player1.getPlayerName());
                player1.sendMessage("🤖 L'ordinateur commence. Veuillez attendre...");
                
                // Use a separate thread with a small delay for the AI's first move
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        makeAIMove();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }
        
        private void initializeGame() {
            resetBoard();
            
            // Set player symbols
            player1.setSymbol('X');
            if (player2 != null) {
                player2.setSymbol('O');
            }
            
            // Connect players to this game
            player1.joinGame(this);
            if (player2 != null) {
                player2.joinGame(this);
            }
            
            // Add to active games
            activeGames.put(gameId, this);
            
            if (!againstAI) {
                // Human vs Human: Randomly decide who starts
                boolean player1Starts = random.nextBoolean();
                this.currentTurn = player1Starts ? player1 : player2;
                
                System.out.println("🆕 Nouvelle partie créée: " + gameId + 
                    " - " + player1.getPlayerName() + " (X) vs " + player2.getPlayerName() + " (O)");
                System.out.println("🎲 " + currentTurn.getPlayerName() + " commence la partie");
                
                // Send initial messages to players
                notifyBothPlayers("Partie commencée ! " + player1.getPlayerName() + " (X) vs " + player2.getPlayerName() + " (O)");
                
                // Tell players who starts (explicitly tell each player if it's their turn)
                if (currentTurn == player1) {
                    player1.sendMessage("🎲 Vous commencez! C'EST VOTRE TOUR (X)! Format: ligne colonne (ex: 01)");
                    player2.sendMessage("⌛ " + player1.getPlayerName() + " commence. Attendez votre tour...");
                } else {
                    player2.sendMessage("🎲 Vous commencez! C'EST VOTRE TOUR (O)! Format: ligne colonne (ex: 01)");
                    player1.sendMessage("⌛ " + player2.getPlayerName() + " commence. Attendez votre tour...");
                }
                
                // Send the board
                sendBoard();
            } else {
                // Human vs AI: Information is sent in the constructor
                System.out.println("🆕 Nouvelle partie contre l'IA créée: " + gameId + 
                    " - " + player1.getPlayerName() + " (X) vs IA (O)");
                player1.sendMessage("Partie commencée ! Vous (X) vs Ordinateur (O)");
                sendBoard();
            }
        }
        
        private String generateGameId() {
            return "game-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        
        public void resetBoard() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    board[i][j] = ' ';
                }
            }
        }
        
        public synchronized boolean makeMove(PlayerHandler player, int row, int col) {
            // Debug info
            System.out.println("Tentative de coup: Joueur=" + player.getPlayerName() + 
                ", Position=[" + row + "," + col + "], Tour actuel=" + 
                (currentTurn == player ? "correct" : "incorrect - C'est le tour de " + 
                (currentTurn != null ? currentTurn.getPlayerName() : "l'IA")));
            
            // Check if it's this player's turn
            if (player != currentTurn) {
                player.sendMessage("⛔ Ce n'est pas votre tour!");
                return false;
            }
            
            // Check if the move is valid
            if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ') {
                player.sendMessage("⛔ Coup invalide. La case est occupée ou hors limites.");
                return false;
            }
            
            // Make the move
            board[row][col] = player.getSymbol();
            System.out.println("🎯 " + player.getPlayerName() + " joue en [" + row + "," + col + "]");
            
            // Send updated board
            sendBoard();
            
            // Check if the game is over
            if (checkWin(player.getSymbol())) {
                gameActive = false;
                player.sendMessage("🏆 Félicitations! Vous avez gagné!");
                
                if (!againstAI && player2 != null) {
                    getOtherPlayer(player).sendMessage("😢 Vous avez perdu. " + player.getPlayerName() + " a gagné.");
                }
                
                endGame();
                return true;
            }
            
            if (isBoardFull()) {
                gameActive = false;
                if (againstAI) {
                    player1.sendMessage("🤝 Match nul! Le plateau est plein.");
                } else {
                    notifyBothPlayers("🤝 Match nul! Le plateau est plein.");
                }
                endGame();
                return true;
            }
            
            // Switch turns
            if (againstAI) {
                // AI's turn next
                player1.sendMessage("🤖 Tour de l'ordinateur...");
                currentTurn = null; // Indicates AI's turn
                
                // Small delay before AI move
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        makeAIMove();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                // Switch to other human player
                PlayerHandler nextPlayer = getOtherPlayer(player);
                currentTurn = nextPlayer;
                
                // Notify players of next turn with explicit turn indication
                currentTurn.sendMessage("🎲 C'EST VOTRE TOUR (" + currentTurn.getSymbol() + ")! Format: ligne colonne (ex: 01)");
                getOtherPlayer(currentTurn).sendMessage("⌛ En attente du coup de " + currentTurn.getPlayerName() + "...");
            }
            
            return true;
        }
        
        /**
         * Makes a move for the AI
         */
        private void makeAIMove() {
            if (!gameActive) {
                System.out.println("⚠️ AI tried to move but game is not active");
                return;
            }
            
            System.out.println("🤖 L'IA réfléchit à son coup...");
            
            char aiSymbol = 'O';  // AI is always O
            char humanSymbol = 'X';  // Human is always X
            
            // For the first move, prefer center then corners
            boolean isFirstMove = true;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row][col] != ' ') {
                        isFirstMove = false;
                        break;
                    }
                }
                if (!isFirstMove) break;
            }
            
            if (isFirstMove) {
                System.out.println("🤖 L'IA fait le premier coup de la partie");
                // For first move, strongly prefer center
                if (board[1][1] == ' ') {
                    makeAIMoveAt(1, 1);
                    return;
                }
            }
            
            // Try to win
            if (tryWinningMove(aiSymbol)) return;
            
            // Block human if they're about to win
            if (tryBlockingMove(humanSymbol)) return;
            
            // Try center
            if (board[1][1] == ' ') {
                makeAIMoveAt(1, 1);
                return;
            }
            
            // Try corners
            int[][] corners = {{0, 0}, {0, 2}, {2, 0}, {2, 2}};
            for (int[] corner : corners) {
                if (board[corner[0]][corner[1]] == ' ') {
                    makeAIMoveAt(corner[0], corner[1]);
                    return;
                }
            }
            
            // Any remaining square
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row][col] == ' ') {
                        makeAIMoveAt(row, col);
                        return;
                    }
                }
            }
        }
        
        private boolean tryWinningMove(char symbol) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row][col] == ' ') {
                        // Try move
                        board[row][col] = symbol;
                        boolean isWinning = checkWin(symbol);
                        if (isWinning) {
                            if (symbol == 'X') {  // Human symbol
                                // Just checking, don't actually make the move
                                board[row][col] = ' ';
                            } else {
                                // Make the winning move for AI
                                finishAIMove(row, col);
                            }
                            return true;
                        }
                        board[row][col] = ' ';  // Reset
                    }
                }
            }
            return false;
        }
        
        private boolean tryBlockingMove(char humanSymbol) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (board[row][col] == ' ') {
                        // Try player move to see if they would win
                        board[row][col] = humanSymbol;
                        boolean wouldWin = checkWin(humanSymbol);
                        board[row][col] = ' ';  // Reset
                        
                        if (wouldWin) {
                            // Block it
                            makeAIMoveAt(row, col);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        private void makeAIMoveAt(int row, int col) {
            board[row][col] = 'O';  // AI is always O
            System.out.println("🤖 L'IA joue en [" + row + "," + col + "]");
            finishAIMove(row, col);
        }
        
        private void finishAIMove(int row, int col) {
            // Send updated board
            sendBoard();
            
            player1.sendMessage("🤖 L'ordinateur a joué en " + row + col);
            
            // Check if AI won
            if (checkWin('O')) {
                gameActive = false;
                player1.sendMessage("😢 Vous avez perdu. L'ordinateur a gagné!");
                endGame();
                return;
            }
            
            // Check for draw
            if (isBoardFull()) {
                gameActive = false;
                player1.sendMessage("🤝 Match nul! Le plateau est plein.");
                endGame();
                return;
            }
            
            // Human's turn again
            currentTurn = player1;
            player1.sendMessage("🎲 C'EST VOTRE TOUR (X)! Format: ligne colonne (ex: 01)");
        }
        
        private boolean checkWin(char symbol) {
            // Check rows
            for (int i = 0; i < 3; i++) {
                if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) {
                    return true;
                }
            }
            
            // Check columns
            for (int i = 0; i < 3; i++) {
                if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) {
                    return true;
                }
            }
            
            // Check diagonals
            if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
                return true;
            }
            
            if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
                return true;
            }
            
            return false;
        }
        
        private boolean isBoardFull() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == ' ') {
                        return false;
                    }
                }
            }
            return true;
        }
        
        public void sendBoard() {
            String boardStr = formatBoard();
            player1.sendMessage(boardStr);
            
            if (!againstAI && player2 != null) {
                player2.sendMessage(boardStr);
            }
        }
        
        private String formatBoard() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n    0   1   2\n");
            sb.append("  ┌───┬───┬───┐\n");
            for (int i = 0; i < 3; i++) {
                sb.append(i).append(" │ ");
                for (int j = 0; j < 3; j++) {
                    sb.append(board[i][j] == ' ' ? " " : board[i][j]).append(" │ ");
                }
                sb.append("\n");
                if (i < 2) sb.append("  ├───┼───┼───┤\n");
            }
            sb.append("  └───┴───┴───┘\n");
            return sb.toString();
        }
        
        private void notifyBothPlayers(String message) {
            player1.sendMessage(message);
            
            if (!againstAI && player2 != null) {
                player2.sendMessage(message);
            }
        }
        
        private PlayerHandler getOtherPlayer(PlayerHandler player) {
            if (againstAI) return null;
            return (player == player1) ? player2 : player1;
        }
        
        public void handlePlayerDisconnect(PlayerHandler player) {
            if (!gameActive) return;
            
            gameActive = false;
            System.out.println("🚫 " + player.getPlayerName() + " s'est déconnecté de la partie " + gameId);
            
            // Notify the other player if this is a human vs human game
            if (!againstAI && player2 != null) {
                PlayerHandler otherPlayer = getOtherPlayer(player);
                if (otherPlayer != null) {
                    otherPlayer.sendMessage("❌ " + player.getPlayerName() + " s'est déconnecté. Partie terminée.");
                }
            }
            
            // Clean up
            endGame();
        }
        
        private void endGame() {
            if (againstAI) {
                // For AI games, ask if player wants another game
                player1.sendMessage("🔄 Partie terminée. Tapez REPLAY pour jouer à nouveau ou autre chose pour quitter.");
            } else {
                // For human games, ask both players
                notifyBothPlayers("🔄 Partie terminée. Tapez REPLAY pour jouer à nouveau ou autre chose pour quitter.");
            }
            
            // Remove from active games if all players disconnect
            if (player1 == null || !player1.isConnected()) {
                if (againstAI || player2 == null || !player2.isConnected()) {
                    activeGames.remove(gameId);
                    System.out.println("🗑️ Partie " + gameId + " terminée et supprimée.");
                }
            }
        }
        
        public void handleRematch(PlayerHandler requestingPlayer) {
            if (againstAI) {
                // AI game - restart immediately
                resetBoard();
                gameActive = true;
                
                // Decide who starts - alternate from previous game
                boolean humanStarts = currentTurn == player1;
                
                if (humanStarts) {
                    currentTurn = player1;
                    player1.sendMessage("🆕 Nouvelle partie! Vous commencez.");
                    player1.sendMessage("🎲 C'EST VOTRE TOUR (X)! Format: ligne colonne (ex: 01)");
                } else {
                    currentTurn = null; // Indicates AI's turn
                    player1.sendMessage("🆕 Nouvelle partie! L'ordinateur commence.");
                    player1.sendMessage("🤖 Tour de l'ordinateur...");
                    sendBoard();
                    
                    // AI starts - make first move
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            makeAIMove();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            } else {
                // Human vs Human game
                PlayerHandler otherPlayer = getOtherPlayer(requestingPlayer);
                
                if (otherPlayer == null || !otherPlayer.isConnected()) {
                    requestingPlayer.sendMessage("❌ L'autre joueur n'est plus connecté.");
                    return;
                }
                
                requestingPlayer.setWantsRematch(true);
                
                if (requestingPlayer.wantsRematch() && otherPlayer.wantsRematch()) {
                    // Both players want a rematch
                    resetBoard();
                    gameActive = true;
                    
                    // Reset rematch flags
                    player1.setWantsRematch(false);
                    player2.setWantsRematch(false);
                    
                    // Alternate starting player for fairness
                    if (startingPlayerAlternates) {
                        currentTurn = (currentTurn == player1) ? player2 : player1;
                    } else {
                        // Random selection
                        currentTurn = random.nextBoolean() ? player1 : player2;
                    }
                    
                    System.out.println("🔄 Nouvelle partie entre " + player1.getPlayerName() + " et " + player2.getPlayerName());
                    System.out.println("🎲 " + currentTurn.getPlayerName() + " commence");
                    
                    notifyBothPlayers("🆕 Nouvelle partie commencée! " + currentTurn.getPlayerName() + " commence.");
                    sendBoard();
                    
                    // Explicitly tell who's turn it is
                    currentTurn.sendMessage("🎲 C'EST VOTRE TOUR (" + currentTurn.getSymbol() + ")! Format: ligne colonne (ex: 01)");
                    getOtherPlayer(currentTurn).sendMessage("⌛ En attente du coup de " + currentTurn.getPlayerName() + "...");
                } else {
                    // Still waiting for other player
                    requestingPlayer.sendMessage("⏳ En attente de la réponse de l'autre joueur...");
                    otherPlayer.sendMessage("🔄 " + requestingPlayer.getPlayerName() + " veut rejouer. Tapez REPLAY si vous voulez aussi.");
                }
            }
        }
    }

    /**
     * Inner class to handle each client connection
     */
    private static class PlayerHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader input;
        private PrintWriter output;
        private String playerName;
        private char symbol;
        private Game currentGame;
        private boolean connected = true;
        private boolean wantsRematch = false;

        public PlayerHandler(Socket socket) {
            this.clientSocket = socket;
            this.playerName = "Player-" + clientSocket.getInetAddress().getHostAddress();
        }
        
        @Override
        public void run() {
            try {
                // Setup I/O
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                output = new PrintWriter(clientSocket.getOutputStream(), true);
                
                // Welcome the player
                sendMessage("👋 Bienvenue sur le serveur Morpion!");
                sendMessage("🤔 Souhaitez-vous jouer contre l'IA (tapez AI) ou attendre un autre joueur (tapez WAIT)?");
                
                boolean choiceMade = false;
                String inputLine;
                
                // Wait for AI or WAIT choice
                while (!choiceMade && (inputLine = input.readLine()) != null) {
                    if (inputLine.equalsIgnoreCase("AI")) {
                        choiceMade = true;
                        sendMessage("🤖 Vous allez jouer contre l'IA.");
                        
                        // Decide randomly who starts
                        boolean humanStarts = random.nextBoolean();
                        sendMessage(humanStarts ? 
                            "🎲 Vous commencez la partie." : 
                            "🤖 L'ordinateur commence la partie.");
                        
                        // Start a game against the AI
                        new Game(this, humanStarts);
                    } else if (inputLine.equalsIgnoreCase("WAIT")) {
                        choiceMade = true;
                        sendMessage("⏳ Recherche d'un adversaire humain...");
                        
                        // Add to waiting list or match with waiting player
                        synchronized (waitingPlayers) {
                            if (waitingPlayers.isEmpty()) {
                                waitingPlayers.add(this);
                                sendMessage("⏳ En attente d'un autre joueur...");
                            } else {
                                // Match with first waiting player
                                PlayerHandler opponent = waitingPlayers.remove(0);
                                new Game(opponent, this);
                            }
                        }
                    } else {
                        sendMessage("❓ Option non reconnue. Tapez AI pour jouer contre l'ordinateur ou WAIT pour attendre un autre joueur.");
                    }
                }
                
                // Main communication loop for game commands
                while (connected && (inputLine = input.readLine()) != null) {
                    if (DEBUG_MODE) {
                        System.out.println("[" + playerName + "] Commande reçue: " + inputLine);
                    }
                    
                    processCommand(inputLine);
                }
                
            } catch (IOException e) {
                System.out.println("❌ Erreur avec " + playerName + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }
        
        private void processCommand(String command) {
            if (command.equalsIgnoreCase("QUIT")) {
                sendMessage("👋 Au revoir!");
                disconnect();
                return;
            }
            
            if (command.equalsIgnoreCase("REPLAY")) {
                if (currentGame != null) {
                    currentGame.handleRematch(this);
                }
                return;
            }
            
            // Process game moves (format: row col)
            if (currentGame != null && command.length() == 2 && Character.isDigit(command.charAt(0)) && Character.isDigit(command.charAt(1))) {
                int row = Character.getNumericValue(command.charAt(0));
                int col = Character.getNumericValue(command.charAt(1));
                currentGame.makeMove(this, row, col);
            } else {
                sendMessage("⚠️ Commande non reconnue. Utilisez le format 'ligne colonne' (ex: 01) pour jouer.");
            }
        }
        
        public void sendMessage(String message) {
            if (output != null && connected) {
                output.println(message);
                if (DEBUG_MODE && !message.contains("┌───┬───┬───┐")) {
                    System.out.println("[SERVEUR → " + playerName + "] " + message);
                }
            }
        }
        
        public void joinGame(Game game) {
            this.currentGame = game;
            
            // Remove from waiting list if present
            synchronized (waitingPlayers) {
                waitingPlayers.remove(this);
            }
        }
        
        public void disconnect() {
            connected = false;
            
            try {
                if (currentGame != null) {
                    currentGame.handlePlayerDisconnect(this);
                }
                
                synchronized (waitingPlayers) {
                    waitingPlayers.remove(this);
                }
                
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    System.out.println("🔌 " + playerName + " s'est déconnecté.");
                }
            } catch (IOException e) {
                System.err.println("❌ Erreur lors de la déconnexion: " + e.getMessage());
            }
        }
        
        public void setSymbol(char symbol) {
            this.symbol = symbol;
        }
        
        public char getSymbol() {
            return symbol;
        }
        
        public String getPlayerName() {
            return playerName;
        }
        
        public boolean isConnected() {
            return connected;
        }
        
        public void setWantsRematch(boolean wantsRematch) {
            this.wantsRematch = wantsRematch;
        }
        
        public boolean wantsRematch() {
            return wantsRematch;
        }
    }
    
    private static void debugLog(String message) {
        if (DEBUG_MODE) {
            System.out.println("[SERVER-DEBUG] " + message);
        }
    }
}