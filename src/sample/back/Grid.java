package sample.back;

import java.util.ArrayList;
import java.util.HashSet;

public class Grid {
    private int gridSize;
    public Cell[][] map;
    private int playerPieces;
    private int aiPieces;
    private ArrayList<Cell> pitLocations;
    private int pitsPerRow;
    private int numOfWumpus;
    private int numOfHero;
    private int numOfMage;

    public Grid(){
        this.gridSize = 3;
        this.pitsPerRow = (gridSize/3) - 1;
        this.numOfWumpus = (gridSize/3);
        this.numOfHero = (gridSize/3);
        this.numOfMage = (gridSize/3);

        this.pitLocations = new ArrayList<Cell>();
        initializeMap();
    }
    public Grid(int gridSize){
        this.gridSize = gridSize;
        this.pitsPerRow = (gridSize/3) - 1;
        this.numOfWumpus = (gridSize/3);
        this.numOfHero = (gridSize/3);
        this.numOfMage = (gridSize/3);
        this.pitLocations = new ArrayList<Cell>();
        initializeMap();
    }
    public Grid(int gridSize, boolean newMap){
        this.gridSize = gridSize;
        this.pitsPerRow = (gridSize/3) - 1;
        this.numOfWumpus = (gridSize/3);
        this.numOfHero = (gridSize/3);
        this.numOfMage = (gridSize/3);
        map = new Cell[gridSize][gridSize];
        for(int row = 0; row < gridSize; row++){
            for(int column = 0; column < gridSize; column++) {
                map[row][column] = new Cell(row, column);
            }
        }
        this.pitLocations = new ArrayList<Cell>();
    }

    public void initializeMap(){
        playerPieces = gridSize;
        aiPieces = gridSize;
        map = new Cell[gridSize][gridSize];
        for(int row = 1; row < gridSize - 1; row++){
            for(int column = 0; column < gridSize; column++){
                map[row][column] = new Cell(row, column);
                if(((gridSize/3) - 1) > 0){
                    map[row][column].setPitProb(1.0/(gridSize));
                }
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
                    map[0][col].setMagicProb(1);
                    break;
                default:
                    System.out.println("Unknown type in initializing the board");
            }
            map[0][col].setPitProb(0);
            map[gridSize-1][col] = new Cell(type, '2', gridSize-1, col);
            map[gridSize-1][col].setPitProb(0);
        }
    }

    public int getNumOfWumpus(){ return this.numOfWumpus; }
    public int getNumOfHero(){ return this.numOfHero; }
    public int getNumOfMage(){ return this.numOfMage; }

    public void setNumOfWumpus(int wumpus) { this.numOfWumpus = wumpus; }
    public void setNumOfHero(int hero) { this.numOfHero = hero; }
    public void setNumOfMage(int mage) { this.numOfMage = mage; }

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
        this.map[row][col].setMagicProb(copy.getMagicProb());
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
        if(neighbors.contains(cell1)){
            return true;
        }
        return false;
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
            playerPieces -= 1;
        }else if(cell1.belongToPlayer() == '2'){
            cell1.reset();
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
