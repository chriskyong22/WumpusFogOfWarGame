package sample.back;

public class Cell {
    private char cellType;
    private char playerPiece;
    private int row;
    private int col;
    private double wumpusProb;
    private double heroProb;
    private double magicProb;
    private double pitProb;


    public Cell(int row, int col){
        this.cellType = 'E';
        this.playerPiece = '0';
        this.row = row;
        this.col = col;
    }

    public Cell(char type, char playerPiece, int row, int col){
        this.cellType = type;
        this.playerPiece = playerPiece;
        this.row = row;
        this.col = col;
    }

    public Cell copy(){
        return new Cell(this.cellType, this.playerPiece, this.row, this.col);
    }

    public int getRow(){
        return this.row;
    }
    public void setRow(int row){
        this.row = row;
    }

    public void setCol(int col){
        this.col = col;
    }

    public int getCol(){
        return this.col;
    }

    public double getWumpusProb() { return this.wumpusProb; }
    public double getHeroProb() { return this.heroProb; }
    public double getMagicProb() { return this.magicProb; }
    public double getPitProb() { return this.pitProb; }

    public void setWumpusProb(double value){
        this.wumpusProb = value;
    }

    public void setHeroProb(double value){
        this.heroProb = value;
    }

    public void setMagicProb(double value){
        this.magicProb = value;
    }

    public void setPitProb(double value){
        this.pitProb = value;
    }

    public boolean isEmpty(){
        return this.cellType == 'E';
    }

    public boolean isPit(){
        return this.cellType == 'P';
    }

    public void reset(){
        this.cellType = 'E';
        this.playerPiece = '0';
    }

    public void printCell(){
        System.out.println("[X: " + this.row + ",Y: " + this.col + "] PlayerPiece: " + this.playerPiece + " cellType: " + this.cellType);
    }

    /**
     * 0 belongs to no one
     * 1 belongs to player
     * 2 belongs to AI
     * @return 0 | no one, 1 | player, 2 | AI
     */
    public char belongToPlayer(){
        return this.playerPiece;
    }

    public void setPlayerPiece(char playerPiece){
        this.playerPiece = playerPiece;
    }

    /**
     * W [Wumpus]
     * H [Human]
     * M [Mage]
     * P [Pit]
     * B [Breeze]
     * S [Stench]
     * N [Hero Noise]
     * F [Fire Magic]
     * @return cell type
     */
    public char getType(){
        return this.cellType;
    }

    public void setType(char type){
        this.cellType = type;
    }

    @Override
    public boolean equals(Object other){
        if(other == null || !(other instanceof Cell)){
            return false;
        }
        return (this.row == ((Cell) other).getRow() && this.col == ((Cell) other).getCol());
    }

}
