package reseau.model;

/**
 * Classe représentant l'état du jeu de Morpion à un moment donné.
 * Cette classe est immuable une fois créée (sauf via les setters).
 */
public class GameState {
    // Plateau de jeu
    private final char[][] board = new char[3][3];
    
    // Position du dernier coup joué
    private int lastPlayedRow = -1;
    private int lastPlayedCol = -1;
    
    /**
     * Constructeur par défaut qui initialise un plateau vide
     */
    public GameState() {
        resetBoard();
    }
    
    /**
     * Réinitialise le plateau de jeu
     */
    public void resetBoard() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                board[row][col] = ' ';
            }
        }
        lastPlayedRow = -1;
        lastPlayedCol = -1;
    }
    
    /**
     * Retourne le contenu d'une cellule du plateau
     */
    public char getCell(int row, int col) {
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            throw new IndexOutOfBoundsException("Position invalide: [" + row + "," + col + "]");
        }
        return board[row][col];
    }
    
    /**
     * Modifie le contenu d'une cellule du plateau
     */
    public void setCell(int row, int col, char value) {
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            throw new IndexOutOfBoundsException("Position invalide: [" + row + "," + col + "]");
        }
        board[row][col] = value;
        lastPlayedRow = row;
        lastPlayedCol = col;
    }
    
    /**
     * Vérifie si le symbole spécifié a gagné
     */
    public boolean checkWin(char symbol) {
        // Vérifier les lignes
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) {
                return true;
            }
        }
        
        // Vérifier les colonnes
        for (int i = 0; i < 3; i++) {
            if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) {
                return true;
            }
        }
        
        // Vérifier les diagonales
        if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
            return true;
        }
        
        if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Vérifie si le plateau est plein (match nul)
     */
    public boolean isBoardFull() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (board[row][col] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Crée une copie profonde de l'état du jeu
     */
    public GameState copy() {
        GameState copy = new GameState();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                copy.board[row][col] = this.board[row][col];
            }
        }
        copy.lastPlayedRow = this.lastPlayedRow;
        copy.lastPlayedCol = this.lastPlayedCol;
        return copy;
    }
    
    /**
     * Retourne la position de la dernière ligne jouée
     */
    public int getLastPlayedRow() {
        return lastPlayedRow;
    }
    
    /**
     * Retourne la position de la dernière colonne jouée
     */
    public int getLastPlayedCol() {
        return lastPlayedCol;
    }
    
    /**
     * Représentation textuelle du plateau
     */
    @Override
    public String toString() {
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
    
    /**
     * Equals et hashCode pour comparaison d'états
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameState that = (GameState) o;
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (this.board[row][col] != that.board[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                result = 31 * result + board[row][col];
            }
        }
        return result;
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


}