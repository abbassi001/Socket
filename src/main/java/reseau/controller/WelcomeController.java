package reseau.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import reseau.view.GameView;
import reseau.view.theme.ThemeFactory.ThemeType;

public class WelcomeController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(WelcomeController.class.getName());
    
    @FXML private Text titleText;
    @FXML private Text xSymbol;
    @FXML private Text oSymbol;
    @FXML private VBox buttonsContainer;
    @FXML private Button playButton;
    @FXML private Button settingsButton;
    @FXML private Button quitButton;
    
    @FXML private VBox modeSelectionContainer;
    @FXML private RadioButton soloModeRadio;
    @FXML private RadioButton multiplayerModeRadio;
    @FXML private ToggleGroup modeGroup;
    
    @FXML private VBox serverConfigContainer;
    @FXML private RadioButton localHostRadio;
    @FXML private RadioButton remoteServerRadio;
    @FXML private ToggleGroup serverGroup;
    @FXML private TextField serverAddressField;
    @FXML private TextField portField;
    @FXML private Button backButton;
    @FXML private Button startButton;
    
    private GameView gameView;
    private ThemeType currentTheme;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Par défaut, la sélection de mode est masquée
        modeSelectionContainer.setVisible(false);
        modeSelectionContainer.setManaged(false);
        
        // Configuration des groupes de boutons radio
        setupToggleGroups();
        
        // Configuration des validations d'entrée
        setupInputValidation();
        
        // Configuration des valeurs par défaut
        serverAddressField.setText("127.0.0.1");
        portField.setText("55555");
        
        // Si une configuration par défaut pour le mode multiplayer
        serverConfigContainer.setVisible(multiplayerModeRadio.isSelected());
        serverConfigContainer.setManaged(multiplayerModeRadio.isSelected());
        serverAddressField.setDisable(localHostRadio.isSelected());
    }
    
    /**
     * Configure le lien avec la vue principale
     */
    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }
    
    /**
     * Définit le thème courant
     */
    public void setCurrentTheme(ThemeType theme) {
        this.currentTheme = theme;
    }
    
    /**
     * Initialise le contrôleur après que tout soit configuré
     */
    public void initialize() {
        // Démarrer les animations
        playWelcomeAnimations();
    }
    
    /**
     * Configure les groupes de boutons radio
     */
    private void setupToggleGroups() {
        // Liaison des actions pour le groupe de mode de jeu
        if (multiplayerModeRadio != null) {
            multiplayerModeRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                serverConfigContainer.setVisible(newVal);
                serverConfigContainer.setManaged(newVal);
            });
        }
        
        // Liaison des actions pour le groupe de serveur
        if (localHostRadio != null) {
            localHostRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    serverAddressField.setText("127.0.0.1");
                    serverAddressField.setDisable(true);
                }
            });
            
            remoteServerRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    if (serverAddressField.getText().equals("127.0.0.1")) {
                        serverAddressField.clear();
                    }
                    serverAddressField.setDisable(false);
                }
            });
        }
    }
    
    /**
     * Configure les validations pour les champs de saisie
     */
    private void setupInputValidation() {
        // Validation du champ de port (uniquement des chiffres)
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                portField.setText(oldVal);
            }
        });
        
        // Validation du champ d'adresse IP (basique pour l'instant)
        serverAddressField.textProperty().addListener((obs, oldVal, newVal) -> {
            startButton.setDisable(remoteServerRadio.isSelected() && newVal.trim().isEmpty());
        });
    }
    
    /**
     * Joue les animations de bienvenue
     */
    private void playWelcomeAnimations() {
        // Animation pour le titre
        FadeTransition fadeTitle = new FadeTransition(Duration.seconds(1), titleText);
        fadeTitle.setFromValue(0);
        fadeTitle.setToValue(1);
        fadeTitle.play();
        
        // Animation pour le X
        RotateTransition rotateX = new RotateTransition(Duration.seconds(1.5), xSymbol);
        rotateX.setByAngle(360);
        rotateX.play();
        
        // Animation pour le O
        ScaleTransition scaleO = new ScaleTransition(Duration.seconds(1), oSymbol);
        scaleO.setFromX(0.2);
        scaleO.setFromY(0.2);
        scaleO.setToX(1.0);
        scaleO.setToY(1.0);
        scaleO.play();
        
        // Animation pour les boutons
        FadeTransition fadeButtons = new FadeTransition(Duration.seconds(0.8), buttonsContainer);
        fadeButtons.setFromValue(0);
        fadeButtons.setToValue(1);
        fadeButtons.setDelay(Duration.seconds(0.5));
        fadeButtons.play();
    }
    
    /**
     * Gestionnaire pour le bouton Jouer
     */
    @FXML
    private void handlePlayButton() {
        // Masquer les boutons principaux et afficher le sélecteur de mode
        buttonsContainer.setVisible(false);
        buttonsContainer.setManaged(false);
        
        modeSelectionContainer.setVisible(true);
        modeSelectionContainer.setManaged(true);
        
        // Animation de transition
        FadeTransition fade = new FadeTransition(Duration.seconds(0.3), modeSelectionContainer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    /**
     * Gestionnaire pour le bouton Paramètres
     */
    @FXML
    private void handleSettingsButton() {
        // Ouvrir la fenêtre des paramètres
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Paramètres");
        alert.setHeaderText("Paramètres du jeu");
        alert.setContentText("Cette fonctionnalité sera disponible dans une future mise à jour.");
        alert.showAndWait();
    }
    
    /**
     * Gestionnaire pour le bouton Quitter
     */
    @FXML
    private void handleQuitButton() {
        if (gameView != null) {
            gameView.getPrimaryStage().close();
        }
    }
    
    /**
     * Gestionnaire pour le bouton Retour
     */
    @FXML
    private void handleBackButton() {
        // Revenir à l'écran principal
        modeSelectionContainer.setVisible(false);
        modeSelectionContainer.setManaged(false);
        
        buttonsContainer.setVisible(true);
        buttonsContainer.setManaged(true);
        
        // Animation de transition
        FadeTransition fade = new FadeTransition(Duration.seconds(0.3), buttonsContainer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }
    
    /**
     * Gestionnaire pour le bouton Commencer
     */
    @FXML
    private void handleStartButton() {
        if (gameView == null) {
            showError("Erreur", "Erreur interne", "Impossible de démarrer le jeu.");
            return;
        }
        
        boolean multiplayerMode = multiplayerModeRadio.isSelected();
        String serverAddress = "127.0.0.1"; // Valeur par défaut
        int port = 55555; // Valeur par défaut
        
        if (multiplayerMode) {
            // Récupérer l'adresse du serveur
            if (remoteServerRadio.isSelected()) {
                serverAddress = serverAddressField.getText().trim();
                if (serverAddress.isEmpty()) {
                    showError("Configuration invalide", "Adresse du serveur manquante", 
                            "Veuillez entrer une adresse de serveur valide.");
                    return;
                }
            }
            
            // Récupérer le port
            try {
                port = Integer.parseInt(portField.getText().trim());
                if (port <= 0 || port > 65535) {
                    throw new NumberFormatException("Port hors limites");
                }
            } catch (NumberFormatException e) {
                showError("Configuration invalide", "Port invalide", 
                        "Veuillez entrer un numéro de port valide (1-65535).");
                return;
            }
        }
        
        // Tout est correct, lancer le jeu
        gameView.showGameScreen(multiplayerMode, serverAddress, port);
    }
    
    /**
     * Affiche une boîte de dialogue d'erreur
     */
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}