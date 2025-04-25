// package reseau;

// import javafx.application.Application;
// import javafx.application.Platform;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.Button;
// import javafx.scene.control.Label;
// import javafx.scene.control.RadioButton;
// import javafx.scene.control.ToggleGroup;
// import javafx.scene.layout.HBox;
// import javafx.scene.layout.VBox;
// import javafx.scene.text.Font;
// import javafx.scene.text.FontWeight;
// import javafx.stage.Stage;

// /**
//  * Enhanced Launcher class for the Morpion game.
//  * This class provides an entry point to launch either the client or server component.
//  */
// public class MorpionLauncher extends Application {

//     private boolean isMultiplayer; // Field to store multiplayer mode

//     private String serverAddress = "127.0.0.1";
//     private int port = 55555;

//     /**
//      * Main method that launches either the client or server application.
//      * 
//      * Usage:
//      * - Launch client (default): 
//      *   mvn exec:java -Dexec.mainClass="reseau.MorpionLauncher"
//      *   mvn exec:java -Dexec.mainClass="reseau.MorpionLauncher" -Dexec.args="client [host] [port]"
//      * 
//      * - Launch server: 
//      *   mvn exec:java -Dexec.mainClass="reseau.MorpionLauncher" -Dexec.args="server [port]"
//      *
//      * - Show help:
//      *   mvn exec:java -Dexec.mainClass="reseau.MorpionLauncher" -Dexec.args="help"
//      * 
//      * @param args Command line arguments
//      */
//     public static void main(String[] args) {
//         if (args.length > 0) {
//             String mode = args[0].toLowerCase();
            
//             switch (mode) {
//                 case "server":
//                     launchServer(args);
//                     break;
//                 case "client":
//                     // Launch directly in client mode with specified parameters
//                     launch(args);
//                     break;
//                 case "help":
//                 case "--help":
//                 case "-h":
//                     showHelp();
//                     break;
//                 default:
//                     System.out.println("❌ Mode non reconnu: " + mode);
//                     showHelp();
//                     System.exit(1);
//                     break;
//             }
//         } else {
//             // If no arguments, launch the GUI launcher
//             launch(args);
//         }
//     }

//     @Override
//     public void start(Stage primaryStage) {
//         Parameters params = getParameters();
//         if (params.getRaw().size() > 0 && params.getRaw().get(0).equalsIgnoreCase("client")) {
//             // If launched with "client" argument, go directly to client mode
//             if (params.getRaw().size() > 1) {
//                 serverAddress = params.getRaw().get(1);
//             }
//             if (params.getRaw().size() > 2) {
//                 try {
//                     port = Integer.parseInt(params.getRaw().get(2));
//                 } catch (NumberFormatException e) {
//                     System.err.println("Format de port invalide, utilisation du port par défaut: " + port);
//                 }
//             }
//             launchClient(serverAddress, port, true); // true = multiplayer mode
//             return;
//         }

//         // Create mode selection UI
//         VBox root = new VBox(20);
//         root.setPadding(new Insets(30));
//         root.setAlignment(Pos.CENTER);

//         // Title
//         Label title = new Label("Morpion Game");
//         title.setFont(Font.font("System", FontWeight.BOLD, 24));

//         // Game mode selection
//         Label modeLabel = new Label("Sélectionnez le mode de jeu:");
//         modeLabel.setFont(Font.font("System", FontWeight.NORMAL, 14));

//         // Radio buttons for game mode
//         ToggleGroup modeGroup = new ToggleGroup();
        
//         RadioButton singlePlayerBtn = new RadioButton("Jouer contre l'ordinateur");
//         singlePlayerBtn.setToggleGroup(modeGroup);
//         singlePlayerBtn.setSelected(true);
        
//         RadioButton multiPlayerBtn = new RadioButton("Jouer contre un autre joueur");
//         multiPlayerBtn.setToggleGroup(modeGroup);

//         // Server address and port input for multiplayer
//         Label serverLabel = new Label("Adresse du serveur: " + serverAddress);
//         Label portLabel = new Label("Port: " + port);

//         // Buttons
//         HBox buttonsBox = new HBox(20);
//         buttonsBox.setAlignment(Pos.CENTER);

//         Button playBtn = new Button("Jouer");
//         playBtn.setPrefWidth(150);
        
//         Button serverBtn = new Button("Lancer un serveur");
//         serverBtn.setPrefWidth(150);

//         buttonsBox.getChildren().addAll(playBtn, serverBtn);

//         // Add all to root
//         root.getChildren().addAll(
//             title,
//             modeLabel,
//             singlePlayerBtn,
//             multiPlayerBtn,
//             serverLabel,
//             portLabel, 
//             buttonsBox
//         );

//         // Button actions
//         playBtn.setOnAction(e -> {
//             boolean isMultiPlayer = multiPlayerBtn.isSelected();
//             launchClient(serverAddress, port, isMultiPlayer);
//         });

//         serverBtn.setOnAction(e -> {
//             // Launch server and close this window
//             launchServerFromUI(port);
//             primaryStage.close();
//         });

//         // Set the scene
//         Scene scene = new Scene(root, 400, 350);
//         primaryStage.setTitle("Morpion Launcher");
//         primaryStage.setScene(scene);
//         primaryStage.show();
//     }

//     private void launchClient(String host, int port, boolean multiPlayer) {
//         // Create arguments array for client
//         String[] clientArgs = {host, String.valueOf(port)};
        
//         try {
//             // Close current window if it exists
//             Stage currentStage = getStageFromButtonAction();
//             if (currentStage != null) {
//                 currentStage.close();
//             }
            
//             // Launch the client application with the specified parameters
//             System.out.println("Lancement du client: " + (multiPlayer ? "mode multijoueur" : "mode contre ordinateur"));
//             MorpionClientFX.setGameModeStatic(multiPlayer); // Use a static method instead
            
//             Stage clientStage = new Stage();
//             MorpionClientFX client = new MorpionClientFX();
//             client.start(clientStage);
//         } catch (Exception ex) {
//             System.err.println("Erreur lors du lancement du client: " + ex.getMessage());
//             ex.printStackTrace();
//         }
//     }

//     private Stage getStageFromButtonAction() {
//         return (Stage) Platform.getImplicitExit() ? null : null; // Placeholder to find the stage
//     }

//     private void launchServerFromUI(int port) {
//         new Thread(() -> {
//             String[] args = {String.valueOf(port)};
//             MorpionServer.main(args);
//         }).start();
//     }
    
//     /**
//      * Launches the Morpion server
//      * @param args Command line arguments
//      */
//     private static void launchServer(String[] args) {
//         System.out.println("🖥️ Lancement du serveur Morpion...");
        
//         // Extract port if provided
//         String[] serverArgs = new String[0];
//         if (args.length > 1) {
//             serverArgs = new String[args.length - 1];
//             System.arraycopy(args, 1, serverArgs, 0, args.length - 1);
//         }
        
//         // Launch server
//         MorpionServer.main(serverArgs);
//     }
    
//     /**
//      * Launches the Morpion client
//      * @param args Command line arguments
//      */
//     private static void launchClient(String[] args) {
//         System.out.println("🎮 Lancement du client Morpion...");
        
//         // Extract host/port if provided
//         String[] clientArgs = new String[0];
//         if (args.length > 1) {
//             clientArgs = new String[args.length - 1];
//             System.arraycopy(args, 1, clientArgs, 0, args.length - 1);
//         }
        
//         // Launch JavaFX client application
//         try {
//             MorpionClientFX client = new MorpionClientFX();
//             client.setGameMode(true); // Default to multiplayer
//             client.main(clientArgs);
//         } catch (Exception e) {
//             System.err.println("Erreur lors du lancement du client: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }


//     public void setGameMode(boolean isMultiplayer) {

//         this.isMultiplayer = isMultiplayer;

//     }
    
//     /**
//      * Displays help information
//      */
//     private static void showHelp() {
//         System.out.println("\n=== Morpion Game Launcher ===\n");
//         System.out.println("Usage:");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\" -Dexec.args=\"[mode] [options]\"\n");
        
//         System.out.println("Modes:");
//         System.out.println("  client [host] [port] - Lance le client (mode par défaut)");
//         System.out.println("  server [port]        - Lance le serveur");
//         System.out.println("  help                 - Affiche cette aide\n");
        
//         System.out.println("Exemples:");
//         System.out.println("  # Lancer le client sur localhost port 55555 (default)");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\"");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\" -Dexec.args=\"client\"");
        
//         System.out.println("\n  # Connecter à un serveur spécifique");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\" -Dexec.args=\"client 192.168.1.10 8080\"");
        
//         System.out.println("\n  # Lancer le serveur sur port par défaut (55555)");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\" -Dexec.args=\"server\"");
        
//         System.out.println("\n  # Lancer le serveur sur port spécifique");
//         System.out.println("  mvn exec:java -Dexec.mainClass=\"reseau.MorpionLauncher\" -Dexec.args=\"server 8080\"\n");
//     }
// }