package reseau.view.theme;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.paint.Color;

/**
 * Fabrique de thèmes pour l'application.
 * Cette classe est responsable de la création des thèmes disponibles dans l'application.
 */
public class ThemeFactory {
    
    /**
     * Enumération des thèmes disponibles
     */
    public enum ThemeType {
        CLASSIC("Classique"),
        DARK("Sombre"),
        NEON("Néon"),
        PASTEL("Pastel");
        
        private final String displayName;
        
        ThemeType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Cache des thèmes déjà créés
    private static final Map<ThemeType, Theme> themeCache = new HashMap<>();
    
    /**
     * Crée ou récupère un thème par son type
     * @param type Le type de thème à créer
     * @return Le thème correspondant
     */
    public static Theme getTheme(ThemeType type) {
        // Vérifier si le thème existe déjà dans le cache
        if (themeCache.containsKey(type)) {
            return themeCache.get(type);
        }
        
        // Créer un nouveau thème
        Theme theme;
        switch (type) {
            case DARK:
                theme = createDarkTheme();
                break;
            case NEON:
                theme = createNeonTheme();
                break;
            case PASTEL:
                theme = createPastelTheme();
                break;
            case CLASSIC:
            default:
                theme = createClassicTheme();
        }
        
        // Mettre en cache
        themeCache.put(type, theme);
        return theme;
    }
    
    /**
     * Crée le thème classique (thème par défaut)
     */
    private static Theme createClassicTheme() {
        return new Theme.Builder()
            .setName("Classique")
            .setStylesheetPath("/css/classic.css")
            .setBackgroundColor(Color.web("#f8f9fa"))
            .setBorderColor(Color.web("#dee2e6"))
            .setTextColor(Color.web("#212529"))
            .setXColor(Color.web("#228be6"))
            .setOColor(Color.web("#e64980"))
            .setAccentColor(Color.web("#fcc419"))
            .setFontFamily("System")
            .build();
    }
    
    /**
     * Crée le thème sombre
     */
    private static Theme createDarkTheme() {
        return new Theme.Builder()
            .setName("Sombre")
            .setStylesheetPath("/css/dark.css")
            .setBackgroundColor(Color.web("#343a40"))
            .setBorderColor(Color.web("#495057"))
            .setTextColor(Color.web("#f8f9fa"))
            .setXColor(Color.web("#4dabf7"))
            .setOColor(Color.web("#ff8787"))
            .setAccentColor(Color.web("#ffd43b"))
            .setFontFamily("System")
            .build();
    }
    
    /**
     * Crée le thème néon
     */
    private static Theme createNeonTheme() {
        return new Theme.Builder()
            .setName("Néon")
            .setStylesheetPath("/css/neon.css")
            .setBackgroundColor(Color.web("#0A0E14"))
            .setBorderColor(Color.web("#1A1E24"))
            .setTextColor(Color.web("#E2E8F0"))
            .setXColor(Color.web("#00FFF0"))
            .setOColor(Color.web("#FF1493"))
            .setAccentColor(Color.web("#FFFF00"))
            .setFontFamily("Verdana")
            .build();
    }
    
    /**
     * Crée le thème pastel
     */
    private static Theme createPastelTheme() {
        return new Theme.Builder()
            .setName("Pastel")
            .setStylesheetPath("/css/pastel.css")
            .setBackgroundColor(Color.web("#FFF4E6"))
            .setBorderColor(Color.web("#FFE8CC"))
            .setTextColor(Color.web("#664B3A"))
            .setXColor(Color.web("#FF922B"))
            .setOColor(Color.web("#5C940D"))
            .setAccentColor(Color.web("#EE79C5"))
            .setFontFamily("Comic Sans MS")
            .build();
    }
}