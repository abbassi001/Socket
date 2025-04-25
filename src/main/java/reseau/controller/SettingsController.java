package reseau.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import reseau.model.ThemeManager;
import reseau.view.GameView;
import reseau.view.theme.ThemeFactory.ThemeType;



/**
 * Contrôleur pour l'écran des paramètres
 */
public class SettingsController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());
    
    // Éléments FXML
    @FXML private ComboBox<String> themeComboBox;
    @FXML private CheckBox soundEnabledCheckbox;
    @FXML private Slider volumeSlider;
    @FXML private Button applyButton;
    @FXML private Button cancelButton;
    @FXML private Label volumeLabel;
    
    // Références
    private GameView gameView;
    private ThemeManager themeManager;
    
    // État initial des paramètres (pour annulation)
    private ThemeType initialTheme;
    private boolean initialSoundEnabled;
    private double initialVolume;
    private ThemeType currentTheme;
    
    /**
     * Initialise le contrôleur
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser la ComboBox des thèmes
        initializeThemeComboBox();
        
        // Configurer le slider de volume
        setupVolumeSlider();
        
        // Attacher les écouteurs d'événements
        attachEventListeners();
    }
    
    /**
     * Initialise la ComboBox des thèmes
     */
    private void initializeThemeComboBox() {
        // Ajouter les thèmes disponibles
        themeComboBox.getItems().clear();
        for (ThemeType theme : ThemeType.values()) {
            themeComboBox.getItems().add(theme.getDisplayName());
        }
    }
    
    /**
     * Configure le slider de volume
     */
    private void setupVolumeSlider() {
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);
        volumeSlider.setValue(100); // Volume par défaut
        
        // Mettre à jour le label du volume lorsqu'il change
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int volumePercent = newVal.intValue();
            volumeLabel.setText(volumePercent + "%");
        });
    }
    
    /**
     * Attache les écouteurs d'événements
     */
    private void attachEventListeners() {
        // Activer/désactiver le slider de volume en fonction de la case à cocher
        soundEnabledCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            volumeSlider.setDisable(!newVal);
            volumeLabel.setDisable(!newVal);
        });
    }
    
    /**
     * Configure la référence à la vue principale
     */
    public void setGameView(GameView gameView) {
        this.gameView = gameView;
    }
    
    /**
     * Configure le gestionnaire de thèmes
     * @param themeManager Le gestionnaire de thèmes
     */
    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
        
        if (themeManager != null) {
            // Sauvegarder les paramètres initiaux pour pouvoir annuler
            initialTheme = themeManager.getCurrentTheme();
            initialSoundEnabled = themeManager.isSoundEnabled();
            initialVolume = themeManager.getVolume();
            
            // Mettre à jour les contrôles avec les valeurs actuelles
            soundEnabledCheckbox.setSelected(initialSoundEnabled);
            volumeSlider.setValue(initialVolume * 100);
            
            // Mettre à jour le thème courant
            currentTheme = initialTheme;
            
            // Mettre à jour la ComboBox des thèmes
            ThemeType currentTheme = ThemeType.valueOf(themeManager.getCurrentTheme().name());
            
            for (int i = 0; i < ThemeType.values().length; i++) {
                if (ThemeType.values()[i] == currentTheme) {
                    themeComboBox.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
    
    /**
     * Initialise le contrôleur avec les paramètres actuels
     */
    public void initialize() {
        if (themeManager != null) {
            // Sauvegarder les paramètres initiaux (pour annulation)
            initialTheme = ThemeType.valueOf(themeManager.getCurrentTheme().name());
            initialSoundEnabled = themeManager.isSoundEnabled();
            initialVolume = themeManager.getVolume();
            
            // Sélectionner le thème actuel dans la ComboBox
            selectCurrentTheme();
            
            // Configurer les autres contrôles
            soundEnabledCheckbox.setSelected(initialSoundEnabled);
            volumeSlider.setValue(initialVolume * 100); // Convertir de 0-1 à 0-100%
            volumeSlider.setDisable(!initialSoundEnabled);
            volumeLabel.setDisable(!initialSoundEnabled);
        }
    }
    
    /**
     * Sélectionne le thème actuel dans la ComboBox
     */
    private void selectCurrentTheme() {
        if (themeManager != null && themeComboBox != null) {
            ThemeType currentTheme = ThemeType.valueOf(themeManager.getCurrentTheme().name());
            
            for (int i = 0; i < ThemeType.values().length; i++) {
                if (ThemeType.values()[i] == currentTheme) {
                    themeComboBox.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }
    /**
 * Définit le thème courant et met à jour l'interface
 * @param themeType Le type de thème à définir
 */
public void setCurrentTheme(ThemeType themeType) {
    if (themeType == null) return;
    
    // Stocker le thème
    this.currentTheme = themeType;
    
    // Mettre à jour la sélection dans la combobox si elle est déjà initialisée
    if (themeComboBox != null) {
        Platform.runLater(() -> {
            // Sélectionner le thème correspondant dans la liste
            for (int i = 0; i < ThemeType.values().length; i++) {
                if (ThemeType.values()[i] == themeType) {
                    themeComboBox.getSelectionModel().select(i);
                    break;
                }
            }
        });
    }
}

/**
 * Met à jour le thème courant lorsque l'utilisateur fait une sélection
 */
private void onThemeSelected() {
    int selectedIndex = themeComboBox.getSelectionModel().getSelectedIndex();
    if (selectedIndex >= 0 && selectedIndex < ThemeType.values().length) {
        currentTheme = ThemeType.values()[selectedIndex];
    }
}
    
    /**
     * Gestionnaire pour le bouton Appliquer
     */
    @FXML
    private void handleApply() {
        if (themeManager != null) {
            // Appliquer les changements
            themeManager.setCurrentTheme(currentTheme);
            themeManager.setSoundEnabled(soundEnabledCheckbox.isSelected());
            themeManager.setVolume(volumeSlider.getValue() / 100.0);
            
            // Enregistrer les modifications
            themeManager.saveSettings();
            
            // Fermer la fenêtre
            closeWindow();
        }
    }
    
    /**
     * Gestionnaire pour le bouton Annuler
     */
    @FXML
    private void handleCancel() {
        // Restaurer les paramètres initiaux
        if (themeManager != null) {
            themeManager.setCurrentTheme(initialTheme);
            themeManager.setSoundEnabled(initialSoundEnabled);
            themeManager.setVolume(initialVolume);
        }
        
        // Fermer la fenêtre
        closeWindow();
    }
    
    /**
     * Gestionnaire pour le bouton Défaut
     */
    @FXML
    private void handleDefault() {
        // Réinitialiser tous les paramètres à leurs valeurs par défaut
        themeComboBox.getSelectionModel().select(0); // Premier thème (CLASSIC)
        soundEnabledCheckbox.setSelected(true);
        volumeSlider.setValue(100);
    }
    
    /**
     * Ferme la fenêtre des paramètres
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}