package reseau;

/**
 * Point d'entrée simplifié pour le jeu de Morpion.
 * Cette classe détecte les arguments et lance soit le serveur, soit le client.
 */
public class MorpionLauncher {

    /**
     * Point d'entrée principal qui lance soit le client, soit le serveur.
     * 
     * Usage:
     * - Lancer le client (par défaut): 
     *   java -cp target/morpion-game-1.0-SNAPSHOT.jar reseau.MorpionLauncher
     * 
     * - Lancer le serveur: 
     *   java -cp target/morpion-game-1.0-SNAPSHOT.jar reseau.MorpionLauncher server
     * 
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            System.out.println("🖥️ Lancement du serveur Morpion...");
            
            // Extraire le port s'il est fourni
            String[] serverArgs = new String[0];
            if (args.length > 1) {
                serverArgs = new String[] { args[1] };
            }
            
            // Lancer le serveur
            MorpionServer.main(serverArgs);
        } else {
            System.out.println("🎮 Lancement du client Morpion...");
            
            // Lancer le client JavaFX
            MorpionClientFX.main(args);
        }
    }
}