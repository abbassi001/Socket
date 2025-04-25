package reseau.view;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import reseau.controller.GameController;
import reseau.controller.SettingsController;
import reseau.controller.WelcomeController;
import reseau.view.theme.Theme;
import reseau.view.theme.ThemeFactory;
import reseau.view.theme.ThemeFactory.ThemeType;

/**
 * Classe principale de l'application JavaFX qui charge les vues FXML.
 */
public class GameView extends Application {
    private static final Logger LOGGER = Logger.getLogger(GameView.class.getName());
    
    private static String[] commandLineArgs;
    private ThemeType currentTheme = ThemeType.CLASSIC;
    private Stage primaryStage;

    // Plateau de jeu (3x3) initialisé avec des espaces vides
    private char[][] board = new char[3][3];
    
    /**
     * Stocke les arguments de ligne de commande pour une utilisation ultérieure
     */
    public static void setCommandLineArgs(String[] args) {
        commandLineArgs = args;
    }
    
    /**
     * Récupère les arguments de ligne de commande
     */
    public static String[] getCommandLineArgs() {
        return commandLineArgs != null ? commandLineArgs : new String[0];
    }
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Configuration de base de la fenêtre
        primaryStage.setTitle("Morpion");
        primaryStage.setResizable(false);
        
        // Tenter de charger une icône si elle existe
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Impossible de charger l'icône de l'application : {0}", e.getMessage());
        }
        
        // Analyser les arguments de ligne de commande
        parseCommandLineArgs();
        
        // Afficher l'écran de bienvenue ou directement le jeu selon les arguments
        if (shouldShowWelcomeScreen()) {
            showWelcomeScreen();
        } else {
            // Déterminer les paramètres de connexion à partir des arguments
            String serverAddress = "127.0.0.1";
            int port = 55555;
            boolean multiplayerMode = true;
            
            // TODO: Extraire les valeurs des arguments
            
            showGameScreen(multiplayerMode, serverAddress, port);
        }
        
        // Configurer la gestion de la fermeture
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }
    
    /**
     * Analyse les arguments de ligne de commande pour configurer l'application
     */
    private void parseCommandLineArgs() {
        String[] args = getCommandLineArgs();
        
        // TODO: Implémenter la logique d'analyse des arguments
    }
    
    /**
     * Détermine si l'écran d'accueil doit être affiché ou non
     */
    private boolean shouldShowWelcomeScreen() {
        // Par défaut, afficher l'écran d'accueil
        return true;
        
        // TODO: Implémenter la logique basée sur les arguments
    }
    
    /**
     * Affiche l'écran d'accueil
     */
    public void showWelcomeScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/welcome.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur et initialiser
            WelcomeController controller = loader.getController();
            controller.setGameView(this);
            controller.setCurrentTheme(currentTheme);
            controller.initialize();
            
            Scene scene = new Scene(root, 600, 450);
            applyThemeToScene(scene);
            
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'écran d'accueil", e);
            showErrorDialog("Erreur de chargement", 
                          "Impossible de charger l'écran d'accueil", 
                          "Détails : " + e.getMessage());
        }
    }
    
    /**
     * Affiche l'écran de jeu principal
     */
    public void showGameScreen(boolean multiplayerMode, String serverAddress, int port) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            Parent root = loader.load();
            
            GameController controller = loader.getController();
            controller.setGameView(this);
            controller.setCurrentTheme(currentTheme);
            controller.initializeGame(multiplayerMode, serverAddress, port);
            
            Scene scene = new Scene(root, 600, 550);
            applyThemeToScene(scene);
            
            primaryStage.setTitle("Morpion - " + (multiplayerMode ? "Multijoueur" : "Solo"));
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'écran de jeu", e);
            showErrorDialog("Erreur de chargement", 
                          "Impossible de charger l'écran de jeu", 
                          "Détails : " + e.getMessage());
        }
    }
    
    /**
     * Applique le thème actuel à une scène
     */
    private void applyThemeToScene(Scene scene) {
        Theme theme = ThemeFactory.getTheme(currentTheme);
        scene.getStylesheets().clear();
        scene.getStylesheets().add(theme.getStylesheetPath());
    }
    
    /**
     * Définit le thème courant et l'applique
     */
    public void setCurrentTheme(ThemeType themeType) {
        this.currentTheme = themeType;
        
        // Appliquer le nouveau thème à la scène actuelle
        if (primaryStage.getScene() != null) {
            applyThemeToScene(primaryStage.getScene());
        }
    }
    
    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Retourne le thème courant
     */
    public ThemeType getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Retourne la fenêtre principale
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Obtient l'index de la ligne gagnante pour un symbole donné
     * 0-2: lignes horizontales
     * 3-5: colonnes verticales
     * 6: diagonale principale (haut-gauche à bas-droite)
     * 7: diagonale secondaire (haut-droite à bas-gauche)
     * -1: aucune ligne gagnante
     * 
     * @param symbol Le symbole pour lequel chercher une ligne gagnante ('X' ou 'O')
     * @return L'index de la ligne gagnante ou -1 si aucune
     */
    public int getWinningLineIndex(char symbol) {
        // Check rows (0-2)
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) {
                return i;  // winning row index
            }
        }
        
        // Check columns (3-5)
        for (int i = 0; i < 3; i++) {
            if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) {
                return i + 3;  // winning column index + 3
            }
        }
        
        // Check main diagonal (6)
        if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
            return 6;  // main diagonal
        }
        
        // Check secondary diagonal (7)
        if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
            return 7;  // secondary diagonal
        }
        
        return -1;  // no winning line
    }

    /**
     * Obtient les coordonnées des cellules de la ligne gagnante
     * 
     * @param symbol Le symbole gagnant ('X' ou 'O')
     * @param lineIndex L'index de la ligne gagnante (retourné par getWinningLineIndex)
     * @return Un tableau 2D avec les coordonnées [row,col] des cellules gagnantes
     */
    public int[][] getWinningCells(char symbol, int lineIndex) {
        int[][] cells = new int[3][2];  // 3 cells, each with [row,col] coordinates
        
        if (lineIndex < 0) {
            return null;  // No winning line
        }
        
        // Horizontal rows (0-2)
        if (lineIndex >= 0 && lineIndex <= 2) {
            for (int col = 0; col < 3; col++) {
                cells[col][0] = lineIndex;  // row
                cells[col][1] = col;        // column
            }
            return cells;
        }
        
        // Vertical columns (3-5)
        if (lineIndex >= 3 && lineIndex <= 5) {
            int colIndex = lineIndex - 3;
            for (int row = 0; row < 3; row++) {
                cells[row][0] = row;         // row
                cells[row][1] = colIndex;    // column
            }
            return cells;
        }
        
        // Main diagonal (6)
        if (lineIndex == 6) {
            for (int i = 0; i < 3; i++) {
                cells[i][0] = i;  // row
                cells[i][1] = i;  // column
            }
            return cells;
        }
        
        // Secondary diagonal (7)
        if (lineIndex == 7) {
            for (int i = 0; i < 3; i++) {
                cells[i][0] = i;      // row
                cells[i][1] = 2 - i;  // column
            }
            return cells;
        }
        
        return null;  // Should never reach this
    }

    /**
     * Vérifie si le joueur avec le symbole donné a gagné
     * @param symbol Symbole du joueur ('X' ou 'O')
     * @return true si le joueur a gagné
     */
    public boolean checkWin(char symbol) {
        return getWinningLineIndex(symbol) >= 0;
    }

    /**
 * Affiche l'écran des paramètres
 */
public void showSettingsScreen() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
        Parent root = loader.load();
        
        // Récupérer le contrôleur des paramètres
        SettingsController controller = loader.getController();
        controller.setGameView(this);
        controller.setCurrentTheme(currentTheme);
        
        // Créer la scène et appliquer le thème
        Scene scene = new Scene(root, 500, 400);
        applyThemeToScene(scene);
        
        // Mettre à jour le titre et la scène
        primaryStage.setTitle("Morpion - Paramètres");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        LOGGER.info("Écran des paramètres affiché");
    } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'écran des paramètres", e);
        showErrorDialog("Erreur de chargement", 
                      "Impossible de charger l'écran des paramètres", 
                      "Détails : " + e.getMessage());
    }
}

    /**
     * Point d'entrée principal (pour les tests)
     */
    public static void main(String[] args) {
        setCommandLineArgs(args);
        launch(args);
    }
}