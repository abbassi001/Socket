module reseau {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires java.prefs;
    requires java.desktop;
    requires javafx.media;
    requires javafx.graphics;
    
    opens reseau to javafx.fxml;
    opens reseau.controller to javafx.fxml;
    
    exports reseau;
    exports reseau.controller;
    exports reseau.view;
    exports reseau.model;

}