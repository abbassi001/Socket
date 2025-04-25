package reseau.model;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import reseau.view.theme.ThemeFactory.ThemeType;

/**
 * Gestionnaire des thèmes de l'application
 */
public class ThemeManager {
    private static final Logger LOGGER = Logger.getLogger(ThemeManager.class.getName());
    
    // Préférences utilisateur pour sauvegarder les paramètres
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final String PREF_THEME = "app.theme";
    private static final String PREF_SOUND_ENABLED = "app.sound.enabled";
    private static final String PREF_VOLUME = "app.sound.volume";
    
    // Valeurs par défaut
    private static final ThemeType DEFAULT_THEME = ThemeType.CLASSIC;
    private static final boolean DEFAULT_SOUND_ENABLED = true;
    private static final double DEFAULT_VOLUME = 1.0;
    
    // État actuel
    private ThemeType currentTheme;
    private boolean soundEnabled;
    private double volume;
    
    /**
     * Constructeur qui charge les préférences utilisateur
     */
    public ThemeManager() {
        loadPreferences();
    }
    
    /**
     * Charge les préférences sauvegardées
     */
    private void loadPreferences() {
        try {
            // Charger le thème
            String themeName = PREFS.get(PREF_THEME, DEFAULT_THEME.name());
            try {
                currentTheme = ThemeType.valueOf(themeName);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Thème invalide dans les préférences: " + themeName, e);
                currentTheme = DEFAULT_THEME;
            }
            
            // Charger les paramètres audio
            soundEnabled = PREFS.getBoolean(PREF_SOUND_ENABLED, DEFAULT_SOUND_ENABLED);
            volume = PREFS.getDouble(PREF_VOLUME, DEFAULT_VOLUME);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des préférences", e);
            
            // Utiliser les valeurs par défaut en cas d'erreur
            currentTheme = DEFAULT_THEME;
            soundEnabled = DEFAULT_SOUND_ENABLED;
            volume = DEFAULT_VOLUME;
        }
    }
    
    /**
     * Sauvegarde les préférences
     */
    public void savePreferences() {
        try {
            PREFS.put(PREF_THEME, currentTheme.name());
            PREFS.putBoolean(PREF_SOUND_ENABLED, soundEnabled);
            PREFS.putDouble(PREF_VOLUME, volume);
            PREFS.flush();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde des préférences", e);
        }
    }
    
    /**
     * Récupère le thème actuel
     */
    public ThemeType getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Définit le thème actuel
     */
    public void setCurrentTheme(ThemeType theme) {
        this.currentTheme = theme;
    }
    
    /**
     * Vérifie si les sons sont activés
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    /**
     * Active ou désactive les sons
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    /**
     * Récupère le volume (0.0 à 1.0)
     */
    public double getVolume() {
        return volume;
    }
    
    /**
     * Définit le volume (0.0 à 1.0)
     */
    public void setVolume(double volume) {
        if (volume < 0.0) {
            this.volume = 0.0;
        } else if (volume > 1.0) {
            this.volume = 1.0;
        } else {
            this.volume = volume;
        }
    }

    


    /**
 * Enregistre les paramètres dans les préférences utilisateur
 */
public void saveSettings() {
    try {
        Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);
        
        // Sauvegarder le thème actuel
        prefs.put("theme", getCurrentTheme().name());
        
        // Sauvegarder l'état du son
        prefs.putBoolean("soundEnabled", isSoundEnabled());
        
        // Sauvegarder le niveau du volume
        prefs.putDouble("volume", getVolume());
        
        // Forcer l'écriture des préférences
        prefs.flush();
        
        LOGGER.info("Paramètres enregistrés avec succès");
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Erreur lors de l'enregistrement des paramètres", e);
    }
}
    
    // Removed duplicate static method isSoundEnabled()
}