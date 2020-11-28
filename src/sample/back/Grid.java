package sample.back;

import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.HashSet;

public class Grid {
    private int gridSize;
    public Cell[][] map;
    private int playerPieces;
    private int aiPieces;
    private ArrayList<Cell> pitLocations;
    private int pitsPerRow;
    private int numOfPWumpus;
    private int numOfPHero;
    private int numOfPMage;
    private ArrayList<Character> playerDeadPieces;
    private ArrayList<Character> aiDeadPieces;
    private int numOfAWumpus;
    private int numOfAHero;
    private int numOfAMage;

    public Grid(){
        this.gridSize = 3;
        this.pitsPerRow = (gridSize/3) - 1;

        this.numOfPWumpus = (gridSize/3);
        this.numOfPHero = (gridSize/3);
        this.numOfPMage = (gridSize/3);

        this.numOfAWumpus = (gridSize/3);
        this.numOfAHero = (gridSize/3);
        this.numOfAMage = (gridSize/3);

        this.pitLocations = new ArrayList<Cell>();
        initializeMap();
    }

    public Grid(int gridSize){
        this.gridSize = gridSize;
        this.pitsPerRow = (gridSize/3) - 1;
        this.numOfPWumpus = (gridSize/3);
        this.numOfPHero = (gridSize/3);
        this.numOfPMage = (gridSize/3);
        this.numOfAWumpus = (gridSize/3);
        this.numOfAHero = (gridSize/3);
        this.numOfAMage = (gridSize/3);
        this.pitLocations = new ArrayList<Cell>();
        initializeMap();
    }

    public Grid(int gridSize, boolean newMap){
        this.gridSize = gridSize;
        this.pitsPerRow = (gridSize/3) - 1;
        this.numOfPWumpus = (gridSize/3);
        this.numOfPHero = (gridSize/3);
        this.numOfPMage = (gridSize/3);
        this.numOfAWumpus = (gridSize/3);
        this.numOfAHero = (gridSize/3);
        this.numOfAMage = (gridSize/3);
        map = new Cell[gridSize][gridSize];
        for(int row = 0; row < gridSize; row++){
            for(int column = 0; column < gridSize; column++) {
                map[row][column] = new Cell(row, column);
            }
        }
        this.pitLocations = new ArrayList<Cell>();
    }

    public Grid(Grid copy){

        this.gridSize = copy.getMapSize();
        this.pitsPerRow = copy.getPitsPerRow();
        this.aiPieces = copy.getAICount();
        this.playerPieces = copy.getPlayerCount();

        this.numOfPHero = copy.getNumOfPHero();
        this.numOfPMage = copy.getNumOfPMage();
        this.numOfPWumpus = copy.getNumOfPWumpus();

        this.numOfAHero = copy.getNumOfPHero();
        this.numOfAMage = copy.getNumOfPMage();
        this.numOfAWumpus = copy.getNumOfPWumpus();

        this.pitLocations = new ArrayList<Cell>();
        this.pitLocations.addAll(copy.getPitLocations());

        this.aiDeadPieces = copy.getAIDeadPieces();
        this.playerDeadPieces = copy.getPlayerDeadPieces();

        this.map = new Cell[gridSize][gridSize];
        for(int row = 0; row < gridSize; row++){
            for(int col = 0; col < gridSize; col++) {
                this.map[row][col] = copy.getCell(row, col).copy();
            }
        }
    }

    public void initializeMap(){
        playerPieces = gridSize;
        aiPieces = gridSize;
        map = new Cell[gridSize][gridSize];
        for(int row = 1; row < gridSize - 1; row++){
            for(int column = 0; column < gridSize; column++){
                map[row][column] = new Cell(row, column);
                map[row][column].setPitProb(((double) (gridSize/3) - 1)/(gridSize));
            }
            int numOfPits = (gridSize/3) - 1;
            while(numOfPits > 0){
                int pitCol = (int) (Math.random() * gridSize);
                if(map[row][pitCol].getType() == 'E'){
                    map[row][pitCol].setType('P');
                    this.pitLocations.add(map[row][pitCol]);
                    numOfPits--;
                }
            }
        }
        for(int col = 0; col < gridSize; col++){
            char type = 'W';
            switch ((col%3)){
                case 0:
                    type = 'W';
                    break;
                case 1:
                    type = 'H';
                    break;
                case 2:
                    type = 'M';
                    break;
                default:
                    System.out.println("Error: the initialization of the hero/mage/wumpus is wrong");
                    break;
            }
            map[0][col] = new Cell(type, '1', 0, col);
            switch (type) {
                case 'W':
                    map[0][col].setWumpusProb(1);
                    break;
                case 'H':
                    map[0][col].setHeroProb(1);
                    break;
                case 'M':
                    map[0][col].setMageProb(1);
                    break;
                default:
                    System.out.println("Unknown type in initializing the board");
            }
            map[0][col].setPitProb(0);
            map[gridSize-1][col] = new Cell(type, '2', gridSize-1, col);
            map[gridSize-1][col].setPitProb(0);
        }
    }

    public int getNumOfPWumpus(){ return this.numOfPWumpus; }
    public int getNumOfPHero(){ return this.numOfPHero; }
    public int getNumOfPMage(){ return this.numOfPMage; }

    public void setNumOfPWumpus(int wumpus) { this.numOfPWumpus = wumpus; }
    public void setNumOfPHero(int hero) { this.numOfPHero = hero; }
    public void setNumOfPMage(int mage) { this.numOfPMage = mage; }

    public int getNumOfAWumpus(){ return this.numOfAWumpus; }
    public int getNumOfAHero(){ return this.numOfAHero; }
    public int getNumOfAMage(){ return this.numOfAMage; }

    public void setNumOfAWumpus(int wumpus) { this.numOfAWumpus = wumpus; }
    public void setNumOfAHero(int hero) { this.numOfAHero = hero; }
    public void setNumOfAMage(int mage) { this.numOfAMage = mage; }

    public ArrayList<Character> getAIDeadPieces(){
        return this.aiDeadPieces;
    }

    public ArrayList<Character> getPlayerDeadPieces(){
        return this.playerDeadPieces;
    }

    public void setAIDeadPieces(ArrayList<Character> aiDeadPieces){
        this.aiDeadPieces = aiDeadPieces;
    }

    public void setPlayerDeadPieces(ArrayList<Character> playerDeadPieces){
        this.playerDeadPieces = playerDeadPieces;
    }
    
    public ArrayList<Cell> getPitLocations() { return this.pitLocations; }

    public int getPlayerCount(){
        return playerPieces;
    }

    public int getAICount(){
        return aiPieces;
    }



    public Cell getCell(int row, int col){
        return map[row][col];
    }

    public int getMapSize(){
        return gridSize;
    }

    public int getPitsPerRow() { return this.pitsPerRow; }

    public void setPitsPerRow(int pits) { this.pitsPerRow = pits; }

    public boolean checkOutOfBounds(int x, int y){
        return x < 0 || x >= gridSize || y < 0 || y >= gridSize;
    }

    public void setProbability(Cell copy, int row, int col){
        this.map[row][col].setMageProb(copy.getMageProb());
        this.map[row][col].setWumpusProb(copy.getWumpusProb());
        this.map[row][col].setPitProb(copy.getPitProb());
        this.map[row][col].setHeroProb(copy.getHeroProb());
    }

    public ArrayList<Cell> getAICells(){
        ArrayList<Cell> aiCells = new ArrayList<Cell>();
        for(int row = 0; row < gridSize; row++){
            for(int col = 0; col < gridSize; col++){
                if(map[row][col].belongToPlayer() == '2'){
                    aiCells.add(map[row][col]);
                }
            }
        }
        return aiCells;
    }

    public ArrayList<Cell> getPlayerCells(){
        ArrayList<Cell> playerCells = new ArrayList<Cell>();
        for(int row = 0; row < gridSize; row++){
            for(int col = 0; col < gridSize; col++){
                if(map[row][col].belongToPlayer() == '1'){
                    playerCells.add(map[row][col]);
                }
            }
        }
        return playerCells;
    }

    public void setCell(Cell c1){
        this.map[c1.getRow()][c1.getCol()].setType(c1.getType());
        this.map[c1.getRow()][c1.getCol()].setPlayerPiece(c1.belongToPlayer());
    }

    public boolean isNeighbor(Cell cell1, int row, int col){
        ArrayList<Cell> neighbors = getNeighbors(row, col);
        return neighbors.contains(cell1);
    }

    public int getNeighborsCount(int row, int col){
        if(checkOutOfBounds(row, col)){
            System.out.println("Out of bounds point entered: [X: " + row + "] [Y: " + col + "]");
            return -1;
        }
        int neighbors = 0;
        for(int posX = row - 1; posX <= row + 1; posX++){
            for(int posY = col - 1; posY <= col + 1; posY++){
                if(!checkOutOfBounds(posX, posY) && (posX != row || posY != col)){
                    neighbors++;
                }
            }
        }
        return neighbors;
    }

    public ArrayList<Cell> getNeighbors(int row, int col){
        //Safety check to make sure you're not getting neighbors for out of bounds points
        if(checkOutOfBounds(row, col)){
            System.out.println("Out of bounds point entered: [X: " + row + "] [Y: " + col + "]");
            return null;
        }

        //Check the 3x3 area, starting from top left corner to bottom right corner
        ArrayList<Cell> neighbors = new ArrayList<Cell>();
        for(int posX = row - 1; posX <= row + 1; posX++){
            for(int posY = col - 1; posY <= col + 1; posY++){
                if(!checkOutOfBounds(posX, posY) && (posX != row || posY != col)){
                    //System.out.println("POSX: " + posX + " POSY: " + posY);
                    //map[posX][posY].printCell();
                    neighbors.add(map[posX][posY]);
                }
            }
        }
        return neighbors;
    }

    public void destroyCell(Cell cell1){
        if(cell1.belongToPlayer() == '1'){
            cell1.reset();
            char type = cell1.getType();
            switch (type) {
                case 'W':
                    this.numOfPWumpus--;
                    playerDeadPieces.add('W');
                    break;
                case 'H':
                    this.numOfPHero--;
                    playerDeadPieces.add('H');
                    break;
                case 'M':
                    this.numOfPMage--;
                    playerDeadPieces.add('M');
                    break;
                default:
                    System.out.println("Trying to destroy some invalid type! " + type);
            }
            playerPieces -= 1;
        }else if(cell1.belongToPlayer() == '2'){
            cell1.reset();
            char type = cell1.getType();
            switch (type) {
                case 'W':
                    this.numOfAWumpus--;
                    aiDeadPieces.add('W');
                    break;
                case 'H':
                    this.numOfAHero--;
                    aiDeadPieces.add('H');
                    break;
                case 'M':
                    this.numOfAMage--;
                    aiDeadPieces.add('M');
                    break;
                default:
                    System.out.println("Trying to destroy some invalid type! " + type);
            }
            aiPieces -= 1;
        }else{
            System.out.println("Error: trying to destroy a piece that is not a player or AI piece");
        }
    }

    public void setPlayerPieces(int count){
        this.playerPieces = count;
    }

    public void setAIPieces(int count){
        this.aiPieces = count;
    }

    public void printMap(){
        System.out.println("Number of Player Pieces: " + this.playerPieces);
        System.out.println("Number of AI Pieces: " + this.aiPieces);
        for(int row = 0; row < gridSize; row++){
            for(int col = 0; col < gridSize; col++){
                System.out.print(map[row][col].getType());
            }
            System.out.println();
        }
    }

}
