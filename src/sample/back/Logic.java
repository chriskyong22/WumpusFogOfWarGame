package sample.back;

import javafx.beans.Observable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.lang.Math;

public class Logic {

    private Grid map;
    private int depth;
    private Move bestMove;
    private ArrayList<Cell> observations;
    public Logic(Grid map, int depthSearch) {
        this.map = map;
        this.depth = depthSearch;
        observations = new ArrayList<Cell>();
        initialize();
    }


    /***
     * Get the state of the board (with no fog of war)
     * @return map (with no fog of war)
     */
    public Grid getFullState(){
        return map;
    }

    /***
     * Initial State of the board (Before any move is made) (Sets all the probabilities to the correct ones)
     */
    public void initialize(){
        Grid fogOfWar = new Grid(map.getMapSize(), true);
        fogOfWar.setAIPieces(map.getAICount());
        fogOfWar.setPlayerPieces(map.getPlayerCount());
        ArrayList<Cell> positions = new ArrayList<Cell>();

        for(int row = 0; row < map.getMapSize(); row++){
            for(int col = 0; col < map.getMapSize(); col++){
                if(map.getCell(row, col).belongToPlayer() == '2'){
                    positions.add(map.getCell(row, col));
                    fogOfWar.setCell(map.getCell(row, col));
                }else{
                    fogOfWar.getCell(row, col).setType('?');
                }
                fogOfWar.setProbability(map.getCell(row, col), row, col);
            }
        }
        calculateRandomMoveProbability(fogOfWar, false);
        updateStateProbabilities(fogOfWar);
    }

    /**
     * Generates a fog of war map
     * @param isPlayer
     * @return fog of war map
     */
    public Grid render(boolean isPlayer){
        return generateObservations(isPlayer);
    }

    public Grid AInextTurn(){
        //Player Move
        Grid fogOfWar = generateObservations(false);
        updateStateProbabilities(fogOfWar);

        //AI makes a move
        Move bestMove = policy();
        this.bestMove = bestMove; //Used to retrieve the value to print the UI
        move(bestMove.getOrigin(), bestMove.getGoal());
        fogOfWar = generateObservations(false);
        updateStateProbabilities(fogOfWar);

        //Calculate possible moves the Player can make
        calculateRandomMoveProbability(fogOfWar, false);
        updateStateProbabilities(fogOfWar);

        //Return a map containing the player view of the board
        fogOfWar = generateObservations(true);
        return fogOfWar;
    }

    public Move policy(){

        PriorityQueue<Move> queue = new PriorityQueue<Move>(11, (Move m1, Move m2) -> Double.compare(m2.getHeuristicValue(), m1.getHeuristicValue()));
        ArrayList<Cell> pieces = map.getAICells();

        for(Cell piece : pieces){
            ArrayList<Cell> moves = possibleMoves(piece);
            for(Cell move : moves){
                Cell copyOrigin = piece.copy();
                Cell copyGoal = move.copy();
                double reward = calculateReward(piece, move);
                queue.add(new Move(copyOrigin, copyGoal, reward));
            }
        }
        return queue.poll();
    }

    public double calculateReward(Cell origin, Cell goal){
        char type = origin.getType();
        if(goal.getPitProb() > 0){ //Should never attempt to check/move into a cell that has a probability of being a pit
            return -1000;
        }

        ArrayList<Cell> neighbors = map.getNeighbors(origin.getRow(), origin.getCol());
        double value = 0;

        //Checking if running away from a certain piece (if same piece, no difference | if piece is killable, negative value | if piece is going kill your piece, positive value
        for(Cell neighbor : neighbors){
            if(neighbor.equals(goal) || isNeighbor(neighbor, goal)){
                continue;
            }
            switch (type) {
                case 'W':
                    value += neighbor.getHeroProb() * 75 + neighbor.getMageProb() * -50;
                    break;
                case 'H':
                    value += neighbor.getMageProb() * 75 + neighbor.getWumpusProb() * -50;
                    break;
                case 'M':
                    value += neighbor.getWumpusProb() * 75 + neighbor.getHeroProb() * -50;
                    break;
                default:
                    System.out.println("Invalid type, trying to move a piece that is not an AI piece");
                    break;
            }
        }

        //Checking the probability of moving into a cell with a killable/nonkillable/same piece
        switch (type) {
            case 'W':
                value += (goal.getMageProb() * 100) + (goal.getHeroProb() * -150) + (goal.getWumpusProb() * 50);
                break;
            case 'H':
                value += (goal.getMageProb() * -150) + (goal.getHeroProb() * 50) + (goal.getWumpusProb() * 100);
                break;
            case 'M':
                value += (goal.getMageProb() * 50) + (goal.getHeroProb() * 100) + (goal.getWumpusProb() * -150);
                break;
            default:
                System.out.println("Invalid type, trying to move a piece that is not an AI piece");
                break;
        }
        return value;
    }

    /**
     * Part 2 of the project (Before the observations are made, calculate and set the probabilities of the opponent's move choices)
     * @param fogOfWar
     * @param isPlayer
     */
    public void calculateRandomMoveProbability(Grid fogOfWar, boolean isPlayer){
        if(isPlayer){

        }else{
            for(int row = 0; row < map.getMapSize(); row++){
                for(int col = 0; col < map.getMapSize(); col++){
                    int playerPieces = map.getPlayerCount();
                    double[] neighborMovingProbability = neighborMovingProbability(row, col, isPlayer);
                    double wumpusProb = ((1 - (1.0/playerPieces)) * map.getCell(row, col).getWumpusProb()) + neighborMovingProbability[0];
                    double heroProb = ((1 - (1.0/playerPieces)) * map.getCell(row, col).getHeroProb()) + neighborMovingProbability[1];
                    double mageProb = ((1 - (1.0/playerPieces)) * map.getCell(row, col).getMageProb()) + neighborMovingProbability[2];
                    fogOfWar.getCell(row, col).setWumpusProb(wumpusProb);
                    fogOfWar.getCell(row, col).setHeroProb(heroProb);
                    fogOfWar.getCell(row, col).setMageProb(mageProb);
                    fogOfWar.getCell(row, col).setPitProb(map.getCell(row, col).getPitProb());
                }
            }
        }
    }

    public Grid generateObservations(boolean isPlayer){
        Grid fogOfWar = new Grid(map.getMapSize(), true);
        fogOfWar.setAIPieces(map.getAICount());
        fogOfWar.setPlayerPieces(map.getPlayerCount());
        ArrayList<Cell> positions = new ArrayList<Cell>();
        if(isPlayer){
            for(int row = 0; row < map.getMapSize(); row++){
                for(int col = 0; col < map.getMapSize(); col++){
                    if(map.getCell(row, col).belongToPlayer() == '1'){
                        positions.add(map.getCell(row, col));
                        fogOfWar.setCell(map.getCell(row, col));
                    }else{
                        fogOfWar.getCell(row, col).setType('?');
                    }
                    fogOfWar.setProbability(map.getCell(row, col), row, col);
                }
            }
            this.observations = setObservations(positions, fogOfWar, true);
        }else{
            for(int row = 0; row < map.getMapSize(); row++){
                for(int col = 0; col < map.getMapSize(); col++){
                    if(map.getCell(row, col).belongToPlayer() == '2'){
                        positions.add(map.getCell(row, col));
                        fogOfWar.setCell(map.getCell(row, col));
                    }else{
                        fogOfWar.getCell(row, col).setType('?');
                    }
                    fogOfWar.setProbability(map.getCell(row, col), row, col);
                }
            }
            this.observations = setObservations(positions, fogOfWar, false);
            calculateObservationProbability(fogOfWar, this.observations, positions);
        }
        return fogOfWar;
    }

    /**
     * Get the probabilities of a certain cell
     * @param row
     * @param col
     * @return [0] wumpus prob, [1] hero prob, [2] mage prob, [3] pit prob
     */
    public double[] getCurrentProbabilities(int row, int col){
        double[] probabilities = new double[4];
        probabilities[0] = map.getCell(row, col).getWumpusProb();
        probabilities[1] = map.getCell(row, col).getHeroProb();
        probabilities[2] = map.getCell(row, col).getMageProb();
        probabilities[3] = map.getCell(row, col).getPitProb();
        return probabilities;
    }

    public void calculateObservationProbability(Grid fogOfWar, ArrayList<Cell> observations, ArrayList<Cell> pieces){
        for(int row = 0; row < map.getMapSize(); row++){
            for(int col = 0; col < map.getMapSize(); col++){
                Cell temp = map.getCell(row, col);
                if(pieces.contains(temp)){ //If the AI piece is selected, obviously the piece does not have a chance to be an Wumpus/Hero/Mage/Pit
                    fogOfWar.getCell(row, col).setWumpusProb(0);
                    fogOfWar.getCell(row, col).setHeroProb(0);
                    fogOfWar.getCell(row, col).setMageProb(0);
                    fogOfWar.getCell(row, col).setPitProb(0);
                    continue;
                }
                double[] probabilities = getCurrentProbabilities(row, col);
                double[] observationGivenPiece = getObservationGivenPiece(row, col, observations, pieces, fogOfWar);

                //STILL NEED TO CALCULATE P(O), not sure how to do this, need to factor this into the wumpusProb/heroProb/mageProb/pitProb
                double wumpusProb = probabilities[0] * observationGivenPiece[0];
                double heroProb = probabilities[1] * observationGivenPiece[1];
                double mageProb = probabilities[2] * observationGivenPiece[2];
                double pitProb = probabilities[3] * observationGivenPiece[3];

                fogOfWar.getCell(row, col).setWumpusProb(wumpusProb);
                fogOfWar.getCell(row, col).setHeroProb(heroProb);
                fogOfWar.getCell(row, col).setMageProb(mageProb);
                fogOfWar.getCell(row, col).setPitProb(pitProb);
            }
        }
    }

    public double[] getObservationGivenPiece(int row, int col, ArrayList<Cell> observations, ArrayList<Cell> pieces, Grid fogOfWar){
        double[] probabilities = new double[4];
        ArrayList<Cell> observed = new ArrayList<Cell>(); //Represents the pieces that observed a combination of either stench/noise/firemage/breeze and is adjacent to the (row, col) cell

        for(Cell piece : pieces){
            //If this is true, case 1 (we want to find all PIECES that received an observation adjacent to this cell)
            Cell c = map.getCell(row, col);
            if(isNeighbor(piece, c)){
                if(observations.contains(piece)){
                    observed.add(fogOfWar.getCell(piece.getRow(), piece.getCol()));
                }else{ //This is a case where a piece observed no stench, no noise, no fire magic, no breeze and it is adjacent to the (row, col)
                    probabilities[0] = 0;
                    probabilities[1] = 0;
                    probabilities[2] = 0;
                    probabilities[3] = 0;
                    return probabilities;
                }
            }
        }

        if(observed.size() > 0){
            //this is CASE 1
            boolean containWumpus = true;
            boolean containHero = true;
            boolean containMage = true;
            boolean containPit = true;
            //If any of the observations do not observe a stench/noise/firemage/breeze then we know the cell cannot contain wumpus/hero/mage/pit
            for(Cell observation : observed){
                if(!observation.observations.contains('S')){
                    probabilities[0] = 0;
                    containWumpus = false;
                }
                if(!observation.observations.contains('N')){
                    probabilities[1] = 0;
                    containHero = false;
                }
                if(!observation.observations.contains('F')){
                    probabilities[2] = 0;
                    containMage = false;
                }
                if(!observation.observations.contains('B')){
                    probabilities[3] = 0;
                    containPit = false;
                }
            }

            if(containWumpus){
                probabilities[0] = map.getCell(row, col).getWumpusProb(); //PLACEHOLDER, IDK WHAT TO SET THE VALUE TO ATM
            }
            if(containHero){
                probabilities[1] = map.getCell(row, col).getHeroProb(); //PLACEHOLDER, IDK WHAT TO SET THE VALUE TO ATM
            }
            if(containMage){
                probabilities[2] = map.getCell(row, col).getMageProb(); //PLACEHOLDER, IDK WHAT TO SET THE VALUE TO ATM
            }
            if(containPit){
                probabilities[3] = map.getCell(row, col).getPitProb(); //PLACEHOLDER, IDK WHAT TO SET THE VALUE TO ATM
            }
        }else{
            //this is CASE 2
            int wumpusLeft = map.getNumOfPWumpus();
            int heroLeft = map.getNumOfPWumpus();
            int mageLeft = map.getNumOfPWumpus();
            int pitLeft = map.getPitsPerRow();

            Grid copy = new Grid(map);
            for(int r = 0; r < map.getMapSize(); r++){
                for(int c = 0; c < map.getMapSize(); c++){
                    if(r == row && c == col){
                        continue;
                    }
                    double wumpusProb = wumpusLeft == 0 ? 0 : map.getCell(r, c).getWumpusProb() * ((double) (wumpusLeft - 1)/wumpusLeft);
                    double heroProb = heroLeft == 0 ? 0 : map.getCell(r, c).getHeroProb() * ((double) (heroLeft - 1) /heroLeft);
                    double mageProb = mageLeft == 0 ? 0 : map.getCell(r, c).getMageProb() * ((double) (mageLeft - 1) /mageLeft);
                    copy.getCell(r, c).setWumpusProb(wumpusProb);
                    copy.getCell(r, c).setHeroProb(heroProb);
                    copy.getCell(r, c).setMageProb(mageProb);

                    //Set all the pit probabilities = previous ones (otherwise all the pit probabilities will be 0 besides the pit probabilities on the same row as (x,y)
                    copy.getCell(r, c).setPitProb(map.getCell(r, c).getPitProb());
                }
            }

            //Only update the pit probabilities that are in the same row as the (x,y) cell ?? I don't know exactly if this is a good idea
            for(int c = 0; c < map.getMapSize(); c++){
                if(c == col){
                    continue;
                }
                double pitProb = pitLeft == 0 ? 0 : map.getCell(row, c).getPitProb() * ((double) (pitLeft - 1) /pitLeft);
                copy.getCell(row, c).setPitProb(pitProb);
            }
            //Finished creating the new distribution of the map given the formula in case 2, stored in "copy"

            //How to calculate P(O) now? WHEN YOU KNOW Please replace the "0" with the P(O | Wxy), P(O | Hxy)...
            double wumpusPlaceHolder = 0;
            double heroPlaceHolder = 0;
            double magePlaceHolder = 0;
            double pitPlaceHolder = 0;

            probabilities[0] = wumpusPlaceHolder;
            probabilities[1] = heroPlaceHolder;
            probabilities[2] = magePlaceHolder;
            probabilities[3] = pitPlaceHolder;
        }

        return probabilities;
    }


    /**
     * Updates the current probabilities to the one specified in the parameter
     * @param newProbabilities
     */
    public void updateStateProbabilities(Grid newProbabilities){
        for(int row = 0; row < map.getMapSize(); row++){
            for(int col = 0; col < map.getMapSize(); col++){
                Cell copy = newProbabilities.getCell(row, col);
                map.setProbability(copy, row, col);
            }
        }
    }

    /**
     * Returns an array consisting of the probabilities of wumpus/hero/mage neighbors moving into (row, col)
     * @param row
     * @param col
     * @param isPlayer
     * @return An array where [0] represents wumpus, [1] represents hero, [2] represents Mage
     */
    public double[] neighborMovingProbability(int row, int col, boolean isPlayer){
        int opponentPieces = isPlayer ? map.getAICount() : map.getPlayerCount();

        ArrayList<Cell> neighbors = map.getNeighbors(row, col);
        double[] probability = new double[3];
        for(Cell neighbor : neighbors){
            probability[0] += neighbor.getWumpusProb() * (1.0/ (opponentPieces * map.getNeighborsCount(neighbor.getRow(), neighbor.getCol())));
            probability[1] += neighbor.getHeroProb() * (1.0/ (opponentPieces * map.getNeighborsCount(neighbor.getRow(), neighbor.getCol())));
            probability[2] += neighbor.getMageProb() * (1.0/ (opponentPieces * map.getNeighborsCount(neighbor.getRow(), neighbor.getCol())));
        }
        return probability;
    }

    /**
     * Returns a list of observations (only the observations that gave a stench/noise/firemagic/breeze, if you want the observe no stench/noise/breeze then check the pieces list and remove any pieces that are in observation from the pieces list
     * @param pieces
     * @param fogOfWar
     * @param isPlayer
     * @return (Observations are kept in fogOfWar Grid) A list of observations (only observations that gave a stench/noise/firemagic/breeze), does not provide the pieces that observed no stench + no breeze + no noise + no fire magic
     */
    public ArrayList<Cell> setObservations(ArrayList<Cell> pieces, Grid fogOfWar, boolean isPlayer){
        ArrayList<Cell> observations = new ArrayList<Cell>();
        for(Cell cell1 : pieces){
            int row = cell1.getRow();
            int col = cell1.getCol();
            ArrayList<Cell> neighbors = map.getNeighbors(row, col);
            for(Cell neighbor : neighbors){
                int neighborRow = neighbor.getRow();
                int neighborCol = neighbor.getCol();

                if((neighbor.belongToPlayer() == '2' && isPlayer) || neighbor.getType() == 'P' || (neighbor.belongToPlayer() == '1' && !isPlayer)){
                    if(!observations.contains(fogOfWar.getCell(row, col))){
                        observations.add(fogOfWar.getCell(row, col));
                    }
                    char type = neighbor.getType();
                    switch(type){
                        case 'P':
                            fogOfWar.getCell(row, col).observations.add('B'); //Breeze
                            break;
                        case 'W':
                            fogOfWar.getCell(row, col).observations.add('S'); //Stench
                            break;
                        case 'H':
                            fogOfWar.getCell(row, col).observations.add('N'); //Hero Moving
                            break;
                        case 'M':
                            fogOfWar.getCell(row, col).observations.add('F'); //Fire Magic
                            break;
                        default:
                            System.out.println("An error occurred when setting the observations!");
                    }
                }
            }
        }
        return observations;
    }

    public ArrayList<Cell> getObservations(){
        return this.observations;
    }

    public void printObservations(){
        for(Cell c : observations){
            System.out.print( "[" + c.getRow() + ":" + c.getCol() + "] Observations: [");
            for(char observation : c.observations){
                System.out.print(observation + ",");
            }
            System.out.print("]\n");
        }
    }

    public double run(int heuristicSelected){
        /**
        if(checkWin() != -1){
            System.out.println("Game over!\n");
            return -1;
        }
         **/
        double value = alphabeta(this.map, this.depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true, heuristicSelected);
        System.out.println("The value is: " + value);
        //map.printMap();
        //System.out.println("\n\n\n\n");
        move(map.getCell(bestMove.getOrigin().getRow(), bestMove.getOrigin().getCol()),  map.getCell(bestMove.getGoal().getRow(), bestMove.getGoal().getCol()));
        map.printMap();
        return value;
    }

    public void resetMap(Cell c1, Cell c2){
        map.setCell(c1);
        map.setCell(c2);
    }

    public double averageDistanceToPits(boolean AI) {
        double totalAverageDist = 0;

        ArrayList<Cell> piecesToUse = AI ? this.map.getAICells() : this.map.getPlayerCells();
        if (piecesToUse.size() == 0) {
            return 0;
        }

        for (Cell cell : piecesToUse) {
            double totalDist = 0;
            int x1 = cell.getRow();
            int y1 = cell.getCol();
            for (Cell pitCell : this.map.getPitLocations()) {
                int x2 = pitCell.getRow();
                int y2 = pitCell.getCol();
                double euclideanDist = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
                totalDist += euclideanDist;
            }
            totalAverageDist += (totalDist / this.map.getPitLocations().size());
        }

        return totalAverageDist / this.map.getMapSize();
    }

    public ArrayList<Cell> getKillableEnemyLocations(Cell cell) {
        Character piece = cell.getType();

        ArrayList<Cell> killableEnemyLocations = new ArrayList<>();
        HashSet<Character> killableEnemies = new HashSet<>();

        switch (piece) {
            case 'W':
                //killableEnemies.add('W');
                killableEnemies.add('M');
                break;
            case 'H':
                //killableEnemies.add('H');
                killableEnemies.add('W');
                break;
            case 'M':
                //killableEnemies.add('M');
                killableEnemies.add('H');
                break;
        }

        for (int i = 0; i < map.getMapSize(); i++) {
            for (int j = 0; j < map.getMapSize(); j++) {
                // check if the current piece is an opposing player
                if ((cell.belongToPlayer() == '1' && this.map.getCell(i,j).belongToPlayer() == '2') ||
                (cell.belongToPlayer() == '2' && this.map.getCell(i,j).belongToPlayer() == '1')) {
                    //check if this enemy is killable, given our piece
                    if (killableEnemies.contains(this.map.getCell(i,j).getType())) {
                        killableEnemyLocations.add(map.getCell(i,j));
                    }
                }
            }
        }

        return killableEnemyLocations;
    }

    public double getAvgClosestKillableEnemy(boolean AI) {
        double totalMinDist = 0;

        ArrayList<Cell> piecesToUse = AI ? this.map.getAICells() : this.map.getPlayerCells();
        if (piecesToUse.size() == 0) {
            return 0;
        }

        for (Cell cell : piecesToUse) {
            double curMinDist = Integer.MAX_VALUE;
            ArrayList<Cell> killableEnemyLocations = getKillableEnemyLocations(cell);
            if (killableEnemyLocations.size() == 0) {
                continue;
            }
            int x1 = cell.getRow();
            int y1 = cell.getCol();

            for (Cell killableEnemyLocation : killableEnemyLocations) {
                int x2 = killableEnemyLocation.getRow();
                int y2 = killableEnemyLocation.getCol();
                double euclideanDist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
                curMinDist = Math.min(curMinDist, euclideanDist);
            }

            totalMinDist += curMinDist;
        }

        return -1 * (totalMinDist / this.map.getMapSize());
    }

    public double calculatePiecesDifference(boolean AI) {
        // base cases
        if (AI && map.getAICount() == 0)
            return -1000;
        if (!AI && map.getPlayerCount() == 0)
            return -1000;


        double WEIGHT = 10;

        // get the base calculation for the difference in pieces
        double base;
        if (AI)
            base = (map.getAICount() - map.getPlayerCount()) * WEIGHT;
        else
            base =  (map.getPlayerCount() - map.getAICount()) * WEIGHT;

        //now, we will either add or subtract points from the base calculation depending on the number of our
        //wumpus/hero/mage pieces to the opponent's pieces. We want to prioritize maxing our pieces and minimizing the other players
        // i.e. if we have 2 wumpus and they have 0 hero's, this is an extremely good situation for us since hero kills wumpus.
        int AIwumpus, AIhero, AImage;
        int Pwumpus, Phero, Pmage;
        AIwumpus = AIhero = AImage = Pwumpus = Phero = Pmage = 0;
        for (int i = 0; i < map.getMapSize(); i++) {
            for (int j = 0; j < map.getMapSize(); j++) {
                Cell cell = this.map.getCell(i,j);
                if (cell.belongToPlayer() == '2') {
                    switch(cell.getType()) {
                        case 'W':
                            AIwumpus += 1;
                            break;
                        case 'H':
                            AIhero += 1;
                            break;
                        case 'M':
                            AImage += 1;
                            break;
                    }
                } else if (cell.belongToPlayer() == '1') {
                    switch(cell.getType()) {
                        case 'W':
                            Pwumpus += 1;
                            break;
                        case 'H':
                            Phero += 1;
                            break;
                        case 'M':
                            Pmage += 1;
                            break;
                    }
                }
            }
        }

        int wumpusAdvantage, heroAdvantage, mageAdvantage;
        wumpusAdvantage = heroAdvantage = mageAdvantage = 0;
        if (AI) {
             wumpusAdvantage = AIwumpus - Phero;
             heroAdvantage = AIhero - Pmage;
             mageAdvantage = AImage - Pwumpus;
        } else {
             wumpusAdvantage = Pwumpus - AIhero;
             heroAdvantage = Phero - AImage;
             mageAdvantage = Pmage - AIwumpus;
        }
        base = base + wumpusAdvantage*WEIGHT;
        base = base + heroAdvantage*WEIGHT;
        base = base + mageAdvantage*WEIGHT;

        return base;
    }

    public double calculateTotalPieces(boolean AI) {
        double WEIGHT = 10;

        if (AI) { //AI
            return map.getAICount() * WEIGHT;
        }

        // PLAYER
        return map.getPlayerCount() * WEIGHT;
    }


    private ArrayList<Cell> getThreatLocations(Cell cell) {
        Character piece = cell.getType();

        ArrayList<Cell> threatLocations = new ArrayList<>();
        HashSet<Character> threats = new HashSet<>();

        switch (piece){
            case 'W':
                threats.add('H');
                threats.add('W');
                break;
            case 'H':
                threats.add('M');
                threats.add('H');
                break;
            case 'M':
                threats.add('W');
                threats.add('M');
                break;
        }

        for (int i=0; i<map.getMapSize(); i++){
            for (int j=0; j<map.getMapSize(); j++){
                if ((cell.belongToPlayer() == '1' && this.map.getCell(i,j).belongToPlayer() == '2') ||
                        (cell.belongToPlayer() == '2' && this.map.getCell(i,j).belongToPlayer() == '1')) {
                    if(threats.contains(this.map.getCell(i,j).getType())) {
                        threatLocations.add(map.getCell(i,j));
                    }
                }
            }
        }
        return threatLocations;
    }

    public double getAvgFurthestThreat(boolean AI) {
        double totalMaxDist = 0;

        ArrayList<Cell> piecesToUse = AI ? this.map.getAICells() : this.map.getPlayerCells();
        if(piecesToUse.size() == 0){
            return 0;
        }

        for (Cell cell : piecesToUse) {
            double maxDistToEnemy = 0;
            ArrayList<Cell> threatLocations = getThreatLocations(cell);
            if(threatLocations.size() == 0){
                continue;
            }
            int x1 = cell.getRow();
            int y1 = cell.getCol();

            for (Cell threatLocation : threatLocations) {
                int x2 = threatLocation.getRow();
                int y2 = threatLocation.getCol();
                double euclideanDist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
                maxDistToEnemy += euclideanDist;
            }
            totalMaxDist = totalMaxDist + (maxDistToEnemy / threatLocations.size());
        }

        return totalMaxDist / this.map.getMapSize();
    }

    public double calculateHeuristic(int heuristic, boolean AI) { //Heuristics should be in the view of the AI
        double heuristicVal = 0;

        switch(heuristic){
            case 0:
                heuristicVal = averageDistanceToPits(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            case 1:
                heuristicVal = getAvgClosestKillableEnemy(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            case 2:
                heuristicVal = calculatePiecesDifference(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            case 3:
                heuristicVal = calculateTotalPieces(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            case 4:
                heuristicVal = getAvgFurthestThreat(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            case 5:
                heuristicVal =
                        0.05*averageDistanceToPits(AI)
                        + (0.15*getAvgClosestKillableEnemy(AI))
                        + 0.5*calculatePiecesDifference(AI)
                        + 0.2*calculateTotalPieces(AI)
                        + 0.10*getAvgFurthestThreat(AI);
                return AI ? heuristicVal : -1 * heuristicVal;

            default:
                return 0;
        }
    }
    //Initial Call, a = -infinity | b = +infinity
    public double alphabeta(Grid map, int depth, double a, double b, boolean maximizingPlayer, int heuristicSelected) {
        if(depth == 0 || checkWin() != -1){
            return calculateHeuristic(heuristicSelected, maximizingPlayer);
        }
        if(maximizingPlayer) { //AI TURN
            double maxEvaluation = Integer.MIN_VALUE;

            // lambda expression ensures highest heuristic value will be at top.
            PriorityQueue<Move> queue = new PriorityQueue<Move>(11, (Move m1, Move m2) -> Double.compare(m2.getHeuristicValue(), m1.getHeuristicValue()));
            queue.addAll(allPossibleMoves(1, heuristicSelected));

            while(!queue.isEmpty()) {
                Move child = queue.poll();
                int playerCount =  map.getPlayerCount();
                int aiCount = map.getAICount();
                Cell origin = map.getCell(child.getOrigin().getRow(), child.getOrigin().getCol());
                Cell goal = map.getCell(child.getGoal().getRow(), child.getGoal().getCol());
                move(origin, goal);

                double curEvaluation = alphabeta(map, depth - 1, a, b, false, heuristicSelected);
                if(depth == this.depth && curEvaluation > maxEvaluation) { // Selects the best move for the AI (based on initial state of the board (depth == this.depth)
                    bestMove = child;
                }

                // undo move to test next move
                map.setPlayerPieces(playerCount);
                map.setAIPieces(aiCount);
                resetMap(child.getOrigin(), child.getGoal());

                maxEvaluation = Math.max(maxEvaluation, curEvaluation);
                a = Math.max(a, maxEvaluation);
                if(a >= b) {
                    break;
                }
            }
            return maxEvaluation;
        } else { //PLAYER TURN

            double minEvaluation = Integer.MAX_VALUE;

            // lambda expression ensures lowest heuristic value will be at top.
            PriorityQueue<Move> queue = new PriorityQueue<Move>(11, (Move m1, Move m2) -> Double.compare(m1.getHeuristicValue(), m2.getHeuristicValue()));
            queue.addAll(allPossibleMoves(2,heuristicSelected));

            while(!queue.isEmpty()){
                Move child = queue.poll();
                int playerCount =  map.getPlayerCount();
                int aiCount = map.getAICount();
                Cell origin = map.getCell(child.getOrigin().getRow(), child.getOrigin().getCol());
                Cell goal = map.getCell(child.getGoal().getRow(), child.getGoal().getCol());
                move(origin, goal);

                double curEvaluation = alphabeta(map, depth - 1, a, b, true, heuristicSelected);
                minEvaluation = Math.min(minEvaluation, curEvaluation);

                // undo move to test next move
                map.setPlayerPieces(playerCount);
                map.setAIPieces(aiCount);
                resetMap(child.getOrigin(), child.getGoal());

                b = Math.min(b, minEvaluation);
                if(b <= a){
                    break;
                }
            }
            return minEvaluation;
        }
    }
    /**
     * (Remember to check for invalid move beforehand)
     * start moves to goal
     * @param start Cell to move from
     * @param goal Cell to move to
     */
    public void move(Cell start, Cell goal){
        if(goal.isPit()){ //the piece gets destroyed so negative
            map.destroyCell(start);
        }else if(!goal.isEmpty()){
            int result = battle(start, goal);
            switch (result){
                case 0: //Both pieces should be destroyed (neutral)
                    map.destroyCell(start);
                    map.destroyCell(goal);
                    break;
                case 1: //your piece wins, so opponent gets destroyed
                    map.destroyCell(goal);
                    goal.setPlayerPiece(start.belongToPlayer());
                    goal.setType(start.getType());
                    start.reset();
                    break;
                case 2: //opponent piece wins, so your piece should be destroyed (Opponent +1 pieces over you)
                    map.destroyCell(start);
                    break;
            }
        }else{ //Moving into an empty space, nothing really happens no pieces get destroyed
            goal.setPlayerPiece(start.belongToPlayer());
            goal.setType(start.getType());
            start.reset();
        }
    }

    /**
     * Check if a win condition has been met
     * @return -1 if no win condition has been met, 0 if draw, 1 if Player wins, 2 if AI wins
     */
    public int checkWin(){
        int playerPieces = map.getPlayerCount();
        int aiPieces = map.getAICount();
        if(playerPieces == 0 && aiPieces == 0){
            return 0;
        }else if(playerPieces != 0 && aiPieces == 0){
            return 1;
        }else if(playerPieces == 0){ //Note do not need aiPieces != 0 because the two case above fulfill it
            return 2;
        }else{
            return -1;
        }
    }

    /**
     * Gives you an ArrayList of Moves that contains the COPY of the cells
     * @param playerOrAI 1 for AI | 2 for Player
     * @param heuristicSelected the chosen heuristic
     * @return An ArrayList of Moves that contains a COPY of the cells
     */
    public ArrayList<Move> allPossibleMoves(int playerOrAI, int heuristicSelected){
        ArrayList<Move> possibleMoves = new ArrayList<Move>();

        switch(playerOrAI){
            case 1: //AI
                ArrayList<Cell> aiCells = map.getAICells();
                for(Cell aiCell : aiCells){
                    ArrayList<Cell> moves = possibleMoves(aiCell);
                    for(Cell move : moves){
                        Cell copyOrigin = aiCell.copy();
                        Cell copyGoal = move.copy();
                        int playerCount =  map.getPlayerCount();
                        int aiCount = map.getAICount();
                        move(aiCell, move);
                        double heuristic = calculateHeuristic(heuristicSelected, true);
                        resetMap(copyOrigin, copyGoal);
                        map.setPlayerPieces(playerCount);
                        map.setAIPieces(aiCount);
                        possibleMoves.add(new Move(copyOrigin, copyGoal, heuristic));
                    }
                }
                break;
            case 2: //Player
                ArrayList<Cell> playerCells = map.getPlayerCells();
                for(Cell playerCell : playerCells){
                    ArrayList<Cell> moves = possibleMoves(playerCell);
                    for(Cell move : moves){
                        Cell copyOrigin = playerCell.copy();
                        Cell copyGoal = move.copy();
                        int playerCount =  map.getPlayerCount();
                        int aiCount = map.getAICount();
                        move(playerCell, move);
                        double heuristic = calculateHeuristic(heuristicSelected, false);
                        resetMap(copyOrigin, copyGoal);
                        map.setPlayerPieces(playerCount);
                        map.setAIPieces(aiCount);
                        possibleMoves.add(new Move(copyOrigin, copyGoal, heuristic));
                    }
                }
                break;
            default:
                System.out.println("Error: Generating all possible moves for not player or AI");
                break;
        }
        return possibleMoves;
    }

    public ArrayList<Cell> possibleMoves(Cell c1){
        //c1.printCell();
        ArrayList<Cell> moves = map.getNeighbors(c1.getRow(), c1.getCol());
        moves.removeIf(move -> !isValidMove(c1, move));
        return moves;
    }


    public boolean isValidMove(Cell cell1, Cell cell2){
        return !(cell1.belongToPlayer() == '1' && cell2.belongToPlayer() == '1' ||
                cell1.belongToPlayer() == '2' && cell2.belongToPlayer() == '2');
    }

    /**
     * Checks if a player move is valid
     * @param cell1 Origin Cell
     * @param cell2 Destination Cell
     * @return true if can move Origin to Destination, false otherwise
     */
    public boolean validPlayerMove(Cell cell1, Cell cell2){
        return isValidMove(cell1, cell2) && cell1.belongToPlayer() == '1' && isNeighbor(cell1, cell2);
    }

    /**
     * Checks if two cells are neighbors
     * @param c1 Cell 1
     * @param c2 Cell 2
     * @return true if neighbors, false if not neighbors
     */
    public boolean isNeighbor(Cell c1, Cell c2){
        return (c2.getRow() - 1 == c1.getRow() || c2.getRow() + 1 == c1.getRow() || c2.getRow() == c1.getRow()) &&
                (c2.getCol() - 1 == c1.getCol() || c2.getCol() + 1 == c1.getCol() || c2.getCol() == c1.getCol());
    }
    /**
     *
     * @param cell1
     * @param cell2
     * @return 1 if cell1 wins, 0 if draw, 2 if Cell1 loses
     */
    public int battle(Cell cell1, Cell cell2){
        if(cell1.getType() == cell2.getType()){
            return 0;
        }
        if(cell1.getType() == 'H' && cell2.getType() == 'W' ||
            cell1.getType() == 'M' && cell2.getType() == 'H' ||
            cell1.getType() == 'W' && cell2.getType() == 'M'){
            return 1;
        }
        return 2;
    }

}
