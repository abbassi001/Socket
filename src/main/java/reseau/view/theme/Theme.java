package reseau.view.theme;

import javafx.scene.paint.Color;

/**
 * Classe représentant un thème dans l'application.
 * Un thème contient toutes les informations de style nécessaires pour l'interface utilisateur.
 */
public class Theme {
    private final String name;
    private final String stylesheetPath;
    private final Color backgroundColor;
    private final Color borderColor;
    private final Color textColor;
    private final Color xColor;           // Couleur pour le symbole X
    private final Color oColor;           // Couleur pour le symbole O
    private final Color accentColor;      // Couleur d'accent pour les éléments importants
    private final String fontFamily;
    
    /**
     * Constructeur privé - utilisez le Builder pour créer une instance
     */
    private Theme(Builder builder) {
        this.name = builder.name;
        this.stylesheetPath = builder.stylesheetPath;
        this.backgroundColor = builder.backgroundColor;
        this.borderColor = builder.borderColor;
        this.textColor = builder.textColor;
        this.xColor = builder.xColor;
        this.oColor = builder.oColor;
        this.accentColor = builder.accentColor;
        this.fontFamily = builder.fontFamily;
    }
    
    /**
     * Builder pour la création d'un thème
     */
    public static class Builder {
        private String name = "Default";
        private String stylesheetPath = "/css/default.css";
        private Color backgroundColor = Color.WHITE;
        private Color borderColor = Color.LIGHTGRAY;
        private Color textColor = Color.BLACK;
        private Color xColor = Color.BLUE;
        private Color oColor = Color.RED;
        private Color accentColor = Color.GOLD;
        private String fontFamily = "System";
        
        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder setStylesheetPath(String stylesheetPath) {
            this.stylesheetPath = stylesheetPath;
            return this;
        }
        
        public Builder setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }
        
        public Builder setBorderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }
        
        public Builder setTextColor(Color textColor) {
            this.textColor = textColor;
            return this;
        }
        
        public Builder setXColor(Color xColor) {
            this.xColor = xColor;
            return this;
        }
        
        public Builder setOColor(Color oColor) {
            this.oColor = oColor;
            return this;
        }
        
        public Builder setAccentColor(Color accentColor) {
            this.accentColor = accentColor;
            return this;
        }
        
        public Builder setFontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
            return this;
        }
        
        public Theme build() {
            return new Theme(this);
        }
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public String getStylesheetPath() {
        return stylesheetPath;
    }
    
    public Color getBackgroundColor() {
        return backgroundColor;
    }
    
    public Color getBorderColor() {
        return borderColor;
    }
    
    public Color getTextColor() {
        return textColor;
    }
    
    public Color getXColor() {
        return xColor;
    }
    
    public Color getOColor() {
        return oColor;
    }
    
    public Color getAccentColor() {
        return accentColor;
    }
    
    public String getFontFamily() {
        return fontFamily;
    }
    
    /**
     * Convertit une couleur en format CSS
     */
    public String toRgbString(Color color) {
        return String.format("rgb(%d,%d,%d)", 
                            (int)(color.getRed() * 255),
                            (int)(color.getGreen() * 255),
                            (int)(color.getBlue() * 255));
    }
    
    /**
     * Convertit une couleur en format hexadécimal CSS
     */
    public String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                            (int)(color.getRed() * 255),
                            (int)(color.getGreen() * 255),
                            (int)(color.getBlue() * 255));
    }
    
    /**
     * Style CSS en ligne pour un bouton avec la couleur X
     */
    public String getXButtonStyle() {
        return "-fx-text-fill: " + toHexString(xColor) + "; " +
               "-fx-font-family: '" + fontFamily + "'; " +
               "-fx-font-weight: bold; " +
               "-fx-font-size: 36px;";
    }
    
    /**
     * Style CSS en ligne pour un bouton avec la couleur O
     */
    public String getOButtonStyle() {
        return "-fx-text-fill: " + toHexString(oColor) + "; " +
               "-fx-font-family: '" + fontFamily + "'; " +
               "-fx-font-weight: bold; " +
               "-fx-font-size: 36px;";
    }
}