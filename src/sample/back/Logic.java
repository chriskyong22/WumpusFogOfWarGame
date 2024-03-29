package sample.back;

import javafx.beans.Observable;

import java.lang.reflect.Array;
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
    private HashSet<Cell> wumpusLocations;
    private HashSet<Cell> heroLocations;
    private HashSet<Cell> mageLocations;


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
    }

    /**
     * Generates a fog of war map
     * @param isPlayer
     * @return fog of war map
     */
    public Grid render(boolean isPlayer){
        return generateObservations(isPlayer);
    }

    public Grid AInextTurn(int playerMovement){
        //Calculate possible moves the Player can make
        calculateRandomMoveProbability(map, false, playerMovement);

        //Player Move
        Grid fogOfWar = generateObservations(false);
        updateStateProbabilities(fogOfWar);

        //AI makes a move
        Move bestMove = policy();
        this.bestMove = bestMove; //Used to retrieve the value to print the UI
        System.out.println("Best move: " + bestMove.toString());
        Cell origin = map.getCell(bestMove.getOrigin().getRow(), bestMove.getOrigin().getCol());
        Cell goal = map.getCell(bestMove.getGoal().getRow(), bestMove.getGoal().getCol());
        move(origin, goal);
        fogOfWar = generateObservations(false);
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

    private double getWeightedDistanceForPlayerMove(int r, int c, char playerPiece, char aiPiece) {
        // base cases, check if player has this piece
        if (playerPiece == 'W' && map.getNumOfPWumpus() == 0)
            return 0;
        if (playerPiece == 'H' && map.getNumOfPHero() == 0)
            return 0;
        if (playerPiece == 'M' && map.getNumOfPMage() == 0)
            return 0;

        // base cases, check if AI has an enemy that player can kill
        if (playerPiece == 'W' && map.getNumOfAMage() == 0)
            return 1;
        if (playerPiece == 'H' && map.getNumOfAWumpus() == 0)
            return 1;
        if (playerPiece == 'M' && map.getNumOfAHero() == 0)
            return 1;

        // if we did not return yet, that means if the player has playerPiece at this cell, there is a killable enemy piece somewhere
        // lets find the minimum distance to that piece
        double minDist = Double.MAX_VALUE;
        for (int row = 0; row < map.getMapSize(); row++) {
            for (int col = 0; col < map.getMapSize(); col++) {
                Cell curCell = map.getCell(row, col);
                if (curCell.belongToPlayer() == '2' && curCell.getType() == aiPiece) {
                    minDist = Math.min(minDist, Math.sqrt((r-row)*(r-row) + (c-col)*(c-col)));
                }
            }
        }

        double maxDistanceInBoard = map.getMapSize() * Math.sqrt(2);
        return 1 - (minDist / maxDistanceInBoard);
    }

    /**
     * Part 2 of the project (Before the observations are made, calculate and set the probabilities of the opponent's move choices)
     * @param fogOfWar
     * @param isPlayer
     * @param playerMovement
     */
    public void calculateRandomMoveProbability(Grid fogOfWar, boolean isPlayer, int playerMovement){
        if(isPlayer){

        }else{
            switch (playerMovement) {
                case 1:
                    double totalWumpusProb = 0;
                    double totalHeroProb = 0;
                    double totalMageProb = 0;
                    for(int row = 0; row < map.getMapSize(); row++){
                        for(int col = 0; col < map.getMapSize(); col++){
                            int playerPieces = map.getPlayerCount();
                            double[] neighborMovingProbability = neighborMovingProbability(row, col, isPlayer);
                            double wumpusWeight = getWeightedDistanceForPlayerMove(row, col, 'W', 'M');
                            double heroWeight = getWeightedDistanceForPlayerMove(row, col, 'H', 'W');
                            double mageWeight = getWeightedDistanceForPlayerMove(row, col, 'M', 'H');
                            double wumpusProb = (row+1)*wumpusWeight * (((1 - (1.0/playerPieces)) * map.getCell(row, col).getWumpusProb()) + neighborMovingProbability[0]);
                            double heroProb = (row+1)*heroWeight * (((1 - (1.0/playerPieces)) * map.getCell(row, col).getHeroProb()) + neighborMovingProbability[1]);
                            double mageProb = (row+1)*mageWeight * (((1 - (1.0/playerPieces)) * map.getCell(row, col).getMageProb()) + neighborMovingProbability[2]);
                            fogOfWar.getCell(row, col).setWumpusProb(wumpusProb);
                            fogOfWar.getCell(row, col).setHeroProb(heroProb);
                            fogOfWar.getCell(row, col).setMageProb(mageProb);
                            fogOfWar.getCell(row, col).setPitProb(map.getCell(row, col).getPitProb());
                            totalWumpusProb += wumpusProb;
                            totalHeroProb += heroProb;
                            totalMageProb += mageProb;
                        }
                    }
                    // normalize the probability distribution
                    for(int row = 0; row < map.getMapSize(); row++){
                        for(int col = 0; col < map.getMapSize(); col++){
                            fogOfWar.getCell(row, col).setWumpusProb(fogOfWar.getCell(row, col).getWumpusProb()/totalWumpusProb);
                            fogOfWar.getCell(row, col).setHeroProb(fogOfWar.getCell(row, col).getHeroProb()/totalHeroProb);
                            fogOfWar.getCell(row, col).setMageProb(fogOfWar.getCell(row, col).getMageProb()/totalMageProb);
                        }
                    }
                    break;
                default:
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
                    break;
            }
        }
    }

    public boolean gridHasSameObservations(Grid grid1, Grid grid2) {
        for (int row = 0; row < map.getMapSize(); row++) {
            for (int col = 0; col < map.getMapSize(); col++) {
                if (!grid1.getCell(row,col).hasSameObservations(grid2.getCell(row,col)))
                    return false;
            }
        }
        return true;
    }

    public void setObservations(Grid modified){
        ArrayList<Cell> AIpieces = modified.getAICells();
        for(Cell AIpiece : AIpieces){
            int row = AIpiece.getRow();
            int col = AIpiece.getCol();
            ArrayList<Cell> neighbors = modified.getNeighbors(row, col);
            for(Cell neighbor : neighbors){
                if((neighbor.belongToPlayer() == '1' || neighbor.getType() == 'P')){
                    char type = neighbor.getType();
                    switch(type){
                        case 'P':
                            if(!modified.getCell(row, col).observations.contains('B')){
                                modified.getCell(row, col).observations.add('B'); //Breeze
                            }
                            break;
                        case 'W':
                            if(!modified.getCell(row, col).observations.contains('S')){
                                modified.getCell(row, col).observations.add('S'); //Stench
                            }
                            break;
                        case 'H':
                            if(!modified.getCell(row, col).observations.contains('N')){
                                modified.getCell(row, col).observations.add('N'); //Hero Moving
                            }
                            break;
                        case 'M':
                            if(!modified.getCell(row, col).observations.contains('F')){
                                modified.getCell(row, col).observations.add('F'); //Fire Magic
                            }
                            break;
                        default:
                            System.out.println("An error occurred when setting the observations!");
                    }
                }
            }
        }
    }

    public Grid onlyAiPiecesAndPits() {
        Grid onlyAIPiecesAndPits = new Grid(this.map.getMapSize(), true);
        for(int row = 0; row < this.map.getMapSize(); row++){
            for(int col = 0; col < this.map.getMapSize(); col++){
                if(map.getCell(row, col).getType() == 'P'){
                    onlyAIPiecesAndPits.getCell(row, col).setType('P');
                }else if(map.getCell(row,col).belongToPlayer() == '2'){
                    onlyAIPiecesAndPits.getCell(row,col).setType(map.getCell(row, col).getType());
                    onlyAIPiecesAndPits.getCell(row,col).setPlayerPiece('2');
                }else{
                    onlyAIPiecesAndPits.getCell(row,col).setType('E');
                }
            }
        }
        return onlyAIPiecesAndPits;
    }

    public ArrayList<Character> listOfPlayerPieces(){
        ArrayList<Character> playerPieces = new ArrayList<Character>();
        int counter = this.map.getNumOfPWumpus();
        while(counter != 0){
            counter--;
            playerPieces.add('W');
        }
        counter = this.map.getNumOfPHero();
        while(counter != 0){
            counter--;
            playerPieces.add('H');
        }
        counter = this.map.getNumOfPMage();
        while(counter != 0){
            counter--;
            playerPieces.add('M');
        }
        return playerPieces;
    }

    public void getPossibleLocations(){
        HashSet<Cell> wumpusLocation=new HashSet();
        HashSet<Cell> heroLocation=new HashSet();
        HashSet<Cell> mageLocation=new HashSet();

        for(Cell observationCell : this.observations){
            ArrayList<Cell> neighbors = this.map.getNeighbors(observationCell.getRow(), observationCell.getCol());

            neighbors.removeIf(neighbor -> neighbor.getType() == 'P');
            neighbors.removeIf(neighbor -> neighbor.belongToPlayer() == '2');

            for(char observation : observationCell.observations){
                switch (observation) {
                    case 'S':
                        wumpusLocation.addAll(neighbors);
                        break;
                    case 'N':
                        heroLocation.addAll(neighbors);
                        break;
                    case 'F':
                        mageLocation.addAll(neighbors);
                        break;
                    case 'B':
                        break;
                    default:
                        System.out.println("[DEBUG] Observations for possible locations is wrong");
                }
            }
        }

        this.wumpusLocations = wumpusLocation;
        this.heroLocations = heroLocation;
        this.mageLocations = mageLocation;
    }
    

    // player pieces has to be of the form ['W', 'W', 'H', 'M'] if the player has 2 wumpus, 1 hero, 1 mage alive. order doesnt matter
    public ArrayList<Grid> getAllObservationStates(Grid curGrid, ArrayList<Grid> states, int r, int c, ArrayList<Character> playerPieces,
                                                   ArrayList<Cell> posWumpus, ArrayList<Cell> posHero, ArrayList<Cell> posMage) {
          if (playerPieces.isEmpty()) {
              Grid gameBoard = new Grid(this.map);
              setObservations(curGrid);
              setObservations(gameBoard);
              if (gridHasSameObservations(curGrid, gameBoard)) {
                  states.add(new Grid(curGrid));
              }
          } else {
              for (Character piece : playerPieces) {
                  for (int row = r; row < map.getMapSize(); row++) {
                      for (int col = c; col < map.getMapSize(); col++) {
                          if (curGrid.getCell(row,col).getType() == 'E') {
                              Grid curGridCopy = new Grid(curGrid);
                              curGridCopy.getCell(row,col).setType(piece);
                              curGridCopy.getCell(row, col).setPlayerPiece('1');
                              ArrayList<Character> playerPiecesCopy = new ArrayList<>(playerPieces);
                              playerPiecesCopy.remove(playerPiecesCopy.indexOf(piece));
                              ArrayList<Cell> copyPosWumpus = new ArrayList<Cell>(posWumpus);
                              ArrayList<Cell> copyPosHero = new ArrayList<Cell>(posHero);
                              ArrayList<Cell> copyPosMage = new ArrayList<Cell>(posMage);

                              if (piece.equals('W')) {
                                  copyPosWumpus.add(curGridCopy.getCell(row,col));
                              } else if (piece.equals('H')) {
                                  copyPosHero.add(curGridCopy.getCell(row,col));
                              } else if (piece.equals('M')) {
                                  copyPosMage.add(curGridCopy.getCell(row,col));
                              }

                              int w, h, m;
                              w = 0;
                              h = 0;
                              m = 0;
                              for (Character ch : playerPiecesCopy) {
                                  if (ch.equals('W'))
                                      w++;
                                  else if (ch.equals('H'))
                                      h++;
                                  else if (ch.equals('M'))
                                      m++;
                              }
                              if (w == 0) {
                                  boolean wValid = false;
                                  if (this.wumpusLocations.isEmpty())
                                      wValid = true;
                                  for (Cell pos : copyPosWumpus) {
                                      if (this.wumpusLocations.contains(pos))
                                          wValid = true;
                                  }
                                  if (!wValid)
                                      continue;
                              }
                              if (h == 0) {
                                  boolean hValid = false;
                                  if (this.heroLocations.isEmpty())
                                      hValid = true;
                                  for (Cell pos : copyPosHero) {
                                      if (this.heroLocations.contains(pos))
                                          hValid = true;
                                  }
                                  if (!hValid)
                                      continue;
                              }
                              if (m == 0) {
                                  boolean mValid = false;
                                  if (this.mageLocations.isEmpty())
                                      mValid = true;
                                  for (Cell pos : copyPosMage) {
                                      if (this.mageLocations.contains(pos))
                                          mValid = true;
                                  }
                                  if (!mValid)
                                      continue;
                              }

                              //find next starting position
                              int _r, _c;
                              if (col + 1 < map.getMapSize()) {
                                  _r = r;
                                  _c = c+1;
                              } else {
                                  _r = r+1;
                                  _c = 0;
                              }

                              //recursive call
                              getAllObservationStates(curGridCopy, states, _r, _c, playerPiecesCopy, copyPosWumpus, copyPosHero, copyPosMage);
                          }
                      }
                  }
              }
          }
          return states;
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
        }else {
            for (int row = 0; row < map.getMapSize(); row++) {
                for (int col = 0; col < map.getMapSize(); col++) {
                    if (map.getCell(row, col).belongToPlayer() == '2') {
                        positions.add(map.getCell(row, col));
                        fogOfWar.setCell(map.getCell(row, col));
                    } else {
                        fogOfWar.getCell(row, col).setType('?');
                    }
                    fogOfWar.setProbability(map.getCell(row, col), row, col);
                }
            }
            this.observations = setObservations(positions, fogOfWar, false);
            calculateObservationProbability(fogOfWar, this.observations, positions);
        }
       // getPossibleLocations();
       // ArrayList<Grid> observedStates = getAllObservationStates(onlyAiPiecesAndPits(), new ArrayList<Grid>(), 0, 0, listOfPlayerPieces(), new ArrayList<Cell>(), new ArrayList<Cell>(), new ArrayList<Cell>());
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

    public void calculateObservationProbability(Grid fogOfWar, ArrayList<Cell> observations, ArrayList<Cell> pieces) {
        double observationProbability = calculateFullObservationProbability(this.map, observations, pieces);
        for (int row = 0; row < map.getMapSize(); row++) {
            for (int col = 0; col < map.getMapSize(); col++) {
                Cell temp = map.getCell(row, col);
                if (pieces.contains(temp)) { // If the cell is an AI piece, obviously no chance for wumpus/hero/mage/pit
                    fogOfWar.getCell(row, col).setWumpusProb(0);
                    fogOfWar.getCell(row, col).setHeroProb(0);
                    fogOfWar.getCell(row, col).setMageProb(0);
                    fogOfWar.getCell(row, col).setPitProb(0);
                    continue;
                }
                double[] probabilities = getCurrentProbabilities(row, col);
                double[] observationGivenPiece = getObservationGivenPiece(row, col, observations, pieces, fogOfWar);
                if (observationProbability == 0) {
                    System.out.println("ERROR");
                }
                double wumpusProb = (probabilities[0] * observationGivenPiece[0]) / observationProbability;
                double heroProb = (probabilities[1] * observationGivenPiece[1]) / observationProbability;
                double mageProb = (probabilities[2] * observationGivenPiece[2]) / observationProbability;
                double pitProb = (probabilities[3] * observationGivenPiece[3]) / observationProbability;

                if (wumpusProb > 1) {
                    wumpusProb = 1;
                }
                if (heroProb > 1) {
                    heroProb = 1;
                }
                if (mageProb > 1) {
                    mageProb = 1;
                }
                if (pitProb > 1) {
                    pitProb = 1;
                }
                if (this.map.getNumOfPWumpus() == 0) {
                    wumpusProb = 0;
                }
                if (this.map.getNumOfPHero() == 0) {
                    heroProb = 0;
                }
                if (this.map.getNumOfPMage() == 0) {
                    mageProb = 0;
                }
                fogOfWar.getCell(row, col).setWumpusProb(wumpusProb);
                fogOfWar.getCell(row, col).setHeroProb(heroProb);
                fogOfWar.getCell(row, col).setMageProb(mageProb);
                fogOfWar.getCell(row, col).setPitProb(pitProb);
            }
        }
        int pitProbEqualZero = 0;
        for (int row = 1; row < map.getMapSize() - 1; row++) {
            pitProbEqualZero = 0;
            for (int col = 0; col < map.getMapSize(); col++) {
                if (fogOfWar.getCell(row, col).getPitProb() == 0) {
                    pitProbEqualZero++;
                }
            }
            for (int col = 0; col < map.getMapSize(); col++) {
                if (fogOfWar.getCell(row, col).getPitProb() > 0) {
                    fogOfWar.getCell(row, col).setPitProb((double) 1 / (map.getMapSize() - pitProbEqualZero));
                }
            }
        }
        if(map.getMapSize() == 3){
            ArrayList<Cell> observes = new ArrayList<Cell>();
            ArrayList<Character> observe = new ArrayList<Character>();
            for(Cell observationCell : observations){
                observes.addAll(fogOfWar.getNeighbors(observationCell.getRow(), observationCell.getCol()));
                for(int row = 0; row <map.getMapSize(); row++){
                    for(int col = 0; col < map.getMapSize(); col++){
                        Cell temp = fogOfWar.getCell(row, col);
                        if (pieces.contains(temp)) { // If the cell is an AI piece, obviously no chance for wumpus/hero/mage/pit
                            continue;
                        }
                        if(!observes.contains(temp)){
                            for(char observation : observationCell.observations){
                                switch(observation) {
                                    case 'S':
                                        fogOfWar.getCell(row, col).setWumpusProb(0);
                                        break;
                                    case 'N':
                                        fogOfWar.getCell(row, col).setHeroProb(0);
                                        break;
                                    case 'F':
                                        fogOfWar.getCell(row, col).setMageProb(0);
                                        break;
                                }
                                observe.add(observation);
                            }
                        }
                    }
                }
                observes.clear();
            }
            for(int row = 0; row < map.getMapSize(); row++){
                for(int col = 0; col < map.getMapSize(); col++){
                    Cell temp = fogOfWar.getCell(row, col);
                    if(pieces.contains(temp)){
                        continue;
                    }
                    for(char c : observe){
                        switch (c){
                            case 'S':
                                if(fogOfWar.getCell(row, col).getWumpusProb() > 0) {
                                    fogOfWar.getCell(row, col).setWumpusProb(1);
                                }
                                break;
                            case 'N':
                                if(fogOfWar.getCell(row, col).getHeroProb() > 0) {
                                    fogOfWar.getCell(row, col).setHeroProb(1);
                                }
                                break;
                            case 'F':
                                if(fogOfWar.getCell(row, col).getMageProb() > 0) {
                                    fogOfWar.getCell(row, col).setMageProb(1);
                                }
                                break;
                        }
                    }
                }
            }

        }
    }

    public Grid AIObservation(int playerMovement){
        Grid oldMap = new Grid(map);

        calculateRandomMoveProbability(map, false, playerMovement);
        Grid fogOfWar = generateObservations(false);

        updateStateProbabilities(oldMap);
        return fogOfWar;
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
                }else{ //If an adjacent cell is a AI piece and the AI piece observes no stench/no hero/no wumpus/no mage then return 0 for all probabilities
                    probabilities[0] = 0;
                    probabilities[1] = 0;
                    probabilities[2] = 0;
                    probabilities[3] = 0;
                    return probabilities;
                }
            }
        }

        boolean containWumpus = true;
        boolean containHero = true;
        boolean containMage = true;
        boolean containPit = true;
        if(observed.size() > 0){
            //this is CASE 1
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

        }
        //this is CASE 2
        int wumpusLeft = map.getNumOfPWumpus();
        int heroLeft = map.getNumOfPWumpus();
        int mageLeft = map.getNumOfPWumpus();
        int pitLeft = map.getMapSize() - 2;

        /*
        double totalMageProb = 0;
        double totalHeroProb = 0;
        double totalWumpusProb = 0;
        double totalPitProb = 0;
        for(int r = 0; r < map.getMapSize(); r++) {
            for (int c = 0; c < map.getMapSize(); c++) {
                if (r == row && c == col) {
                    continue;
                }
                totalWumpusProb += map.getCell(r, c).getWumpusProb();
                totalHeroProb += map.getCell(r,c).getHeroProb();
                totalMageProb += map.getCell(r,c).getMageProb();
                totalPitProb += map.getCell(r,c).getPitProb();
            }
        }
        */
        if(containWumpus){
            Grid copy = new Grid(map);
            for(int r = 0; r < map.getMapSize(); r++){
                for(int c = 0; c < map.getMapSize(); c++){
                    if(r == row && c == col){
                        copy.getCell(r, c).setWumpusProb(1);
                        copy.getCell(r, c).setHeroProb(0);
                        copy.getCell(r, c).setMageProb(0);
                        copy.getCell(r, c).setPitProb(0);
                        continue;
                    }
                    double wumpusProb = wumpusLeft == 0 ? 0 : map.getCell(r, c).getWumpusProb() * ((double) (wumpusLeft - 1)/wumpusLeft);
                    double heroProb = map.getCell(r,c).getHeroProb() * (1-map.getCell(r,c).getHeroProb());
                    double mageProb = map.getCell(r,c).getMageProb() * (1-map.getCell(r,c).getMageProb());
                    double pitProb =  map.getCell(r,c).getPitProb() * (1-map.getCell(r,c).getPitProb());
                    /*
                    double heroProb = heroLeft * (map.getCell(r,c).getHeroProb() / totalHeroProb);
                    double mageProb = mageLeft * (map.getCell(r,c).getMageProb() / totalMageProb);
                    double pitProb = pitLeft * (map.getCell(r,c).getPitProb() / totalPitProb);
                     */
                    copy.getCell(r, c).setWumpusProb(wumpusProb);
                    copy.getCell(r, c).setHeroProb(heroProb);
                    copy.getCell(r, c).setMageProb(mageProb);
                    copy.getCell(r, c).setPitProb(pitProb);
                }
            }
            probabilities[0] =  calculateFullObservationProbability(copy, observations, pieces);
        }
        if(containHero){
            Grid copy = new Grid(map);
            for(int r = 0; r < map.getMapSize(); r++){
                for(int c = 0; c < map.getMapSize(); c++){
                    if(r == row && c == col){
                        copy.getCell(r, c).setWumpusProb(0);
                        copy.getCell(r, c).setHeroProb(1);
                        copy.getCell(r, c).setMageProb(0);
                        copy.getCell(r, c).setPitProb(0);
                        continue;
                    }
                    double wumpusProb = map.getCell(r,c).getWumpusProb() * (1-map.getCell(r,c).getWumpusProb());
                    double heroProb = heroLeft == 0 ? 0 : map.getCell(r, c).getHeroProb() * ((double) (heroLeft - 1)/heroLeft);
                    double mageProb = map.getCell(r,c).getMageProb() * (1-map.getCell(r,c).getMageProb());
                    double pitProb =  map.getCell(r,c).getPitProb() * (1-map.getCell(r,c).getPitProb());

                    /*
                    double wumpusProb = wumpusLeft * (map.getCell(r,c).getWumpusProb() / totalWumpusProb);
                    double mageProb = mageLeft * (map.getCell(r,c).getMageProb() / totalMageProb);
                    double pitProb = pitLeft * (map.getCell(r,c).getPitProb() / totalPitProb);
                     */
                    copy.getCell(r, c).setWumpusProb(wumpusProb);
                    copy.getCell(r, c).setHeroProb(heroProb);
                    copy.getCell(r, c).setMageProb(mageProb);
                    copy.getCell(r, c).setPitProb(pitProb);
                }
            }
            probabilities[1] = calculateFullObservationProbability(copy, observations, pieces);
        }
        if(containMage){
            Grid copy = new Grid(map);
            for(int r = 0; r < map.getMapSize(); r++){
                for(int c = 0; c < map.getMapSize(); c++){
                    if(r == row && c == col){
                        copy.getCell(r, c).setWumpusProb(0);
                        copy.getCell(r, c).setHeroProb(0);
                        copy.getCell(r, c).setMageProb(1);
                        copy.getCell(r, c).setPitProb(0);
                        continue;
                    }
                    double wumpusProb = map.getCell(r,c).getWumpusProb() * (1-map.getCell(r,c).getWumpusProb());
                    double heroProb = map.getCell(r,c).getHeroProb() * (1-map.getCell(r,c).getHeroProb());
                    double mageProb = mageLeft == 0 ? 0 : map.getCell(r, c).getMageProb() * ((double) (mageLeft - 1)/mageLeft);
                    double pitProb =  map.getCell(r,c).getPitProb() * (1-map.getCell(r,c).getPitProb());
                    /*
                    double wumpusProb = wumpusLeft * (map.getCell(r,c).getWumpusProb() / totalWumpusProb);
                    double heroProb = heroLeft * (map.getCell(r,c).getHeroProb() / totalHeroProb);
                    double mageProb = mageLeft == 0 ? 0 : map.getCell(r, c).getMageProb() * ((double) (mageLeft - 1)/mageLeft);
                    double pitProb = pitLeft * (map.getCell(r,c).getPitProb() / totalPitProb);
                     */
                    copy.getCell(r, c).setWumpusProb(wumpusProb);
                    copy.getCell(r, c).setHeroProb(heroProb);
                    copy.getCell(r, c).setMageProb(mageProb);
                    copy.getCell(r, c).setPitProb(pitProb);
                }
            }
            probabilities[2] =  calculateFullObservationProbability(copy, observations, pieces);
        }
        if(containPit){
            Grid copy = new Grid(map);
            for(int r = 0; r < map.getMapSize(); r++){
                for(int c = 0; c < map.getMapSize(); c++){
                    if(r == row && c == col){
                        copy.getCell(r, c).setWumpusProb(0);
                        copy.getCell(r, c).setHeroProb(0);
                        copy.getCell(r, c).setMageProb(0);
                        copy.getCell(r, c).setPitProb(1);
                        continue;
                    }
                    double wumpusProb = map.getCell(r,c).getWumpusProb() * (1-map.getCell(r,c).getWumpusProb());
                    double heroProb = map.getCell(r,c).getHeroProb() * (1-map.getCell(r,c).getHeroProb());
                    double mageProb = map.getCell(r,c).getMageProb() * (1-map.getCell(r,c).getMageProb());
                    double pitProb = pitLeft == 0 ? 0 : map.getCell(r, c).getPitProb() * ((double) (pitLeft - 1)/pitLeft);
                    /*
                    double wumpusProb = wumpusLeft * (map.getCell(r,c).getWumpusProb() / totalWumpusProb);
                    double heroProb = heroLeft * (map.getCell(r,c).getHeroProb() / totalHeroProb);
                    double mageProb = mageLeft * (map.getCell(r,c).getMageProb() / totalMageProb);
                    double pitProb = pitLeft == 0 ? 0 : map.getCell(r, c).getPitProb() * ((double) (pitLeft - 1)/pitLeft);
                    */
                    copy.getCell(r, c).setWumpusProb(wumpusProb);
                    copy.getCell(r, c).setHeroProb(heroProb);
                    copy.getCell(r, c).setMageProb(mageProb);
                    copy.getCell(r, c).setPitProb(pitProb);
                }
            }
            probabilities[3] = calculateFullObservationProbability(copy, observations, pieces);
        }
        return probabilities;
    }

    private double factorial(int n) {
        double fact = 1;
        int i = 1;
        while(i <= n) {
            fact *= i;
            i++;
        }
        return fact;
    }

    public double calculateFullObservationProbability(Grid map, ArrayList<Cell> observations, ArrayList<Cell> pieces){

        ArrayList<Cell> dontCheck = new ArrayList<Cell>();
        dontCheck.addAll(pieces);
        ArrayList<Cell> neighborsPossible = new ArrayList<Cell>();

        for(Cell piece : pieces){
            ArrayList<Cell> neighbors = map.getNeighbors(piece.getRow(), piece.getCol());
            for(Cell neighbor : neighbors){
                if(!dontCheck.contains(neighbor)){
                    dontCheck.add(neighbor);
                    neighborsPossible.add(neighbor);
                }
                if(!observations.contains(piece)){
                    neighborsPossible.remove(neighbor);
                }
            }
        }


        int playerPiece = map.getPlayerCount();
        int totalCells = map.getMapSize() * map.getMapSize();
        int occupiedCells = map.getAICount() + map.getPlayerCount() + ((map.getMapSize() - 2) * map.getPitsPerRow());

        double observeProb = 1;
        int playerPcsExamined = 0;

        for(Cell observation : observations){
            if(observation.observations.contains("B")){
                playerPcsExamined -= 1;
            }
            ArrayList<Cell> possibleMoves = possibleMoves(observation);
            int n = possibleMoves.size();
            int o = observation.observations.size();
            observeProb *= (factorial(n) / factorial(n-o));
            playerPcsExamined += o;
        }
        int numOfEmptyCells = totalCells - dontCheck.size();
        int value = (playerPiece - playerPcsExamined);
        observeProb *= (factorial(numOfEmptyCells) / factorial(numOfEmptyCells - value));
        observeProb /= (factorial(totalCells) / factorial(totalCells - occupiedCells));
        return observeProb;

        /*
        ArrayList<Double> summations = new ArrayList<Double>();
        for(Cell observation : observations){
            double pieceObservationProbability = 0;
            ArrayList<Cell> neighbors = map.getNeighbors(observation.getRow(), observation.getCol());
            for(char type : observation.observations){
                for(Cell neighbor : neighbors){
                    if(neighborsPossible.contains(neighbor)){
                        switch (type) {
                            case 'S':
                                pieceObservationProbability += neighbor.getWumpusProb();
                                break;
                            case 'N':
                                pieceObservationProbability += neighbor.getHeroProb();
                                break;
                            case 'F':
                                pieceObservationProbability += neighbor.getMageProb();
                                break;
                            case 'B':
                                pieceObservationProbability += neighbor.getPitProb();
                                break;
                            default:
                                System.out.println("Error occurred when trying to normalize the probabilities of each piece");
                        }
                    }
                }
            }
            summations.add(pieceObservationProbability);
        }
        double otherCells = 0;
        for(int row = 0; row < map.getMapSize(); row++){
            for(int col = 0; col < map.getMapSize(); col++){
                Cell checkCell = map.getCell(row, col);
                if(dontCheck.contains(checkCell)){
                    continue;
                }else{
                    otherCells += checkCell.getWumpusProb();
                    otherCells += checkCell.getHeroProb();
                    otherCells += checkCell.getMageProb();
                    otherCells += checkCell.getPitProb();
                }
            }
        }
        summations.add(otherCells);
        double ObservationProbability = 1;
        for(double value : summations){
            ObservationProbability *= value;
        }
        return ObservationProbability;
        */
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
                            if(!fogOfWar.getCell(row, col).observations.contains('B')){
                                fogOfWar.getCell(row, col).observations.add('B'); //Breeze
                            }
                            break;
                        case 'W':
                            if(!fogOfWar.getCell(row, col).observations.contains('S')){
                                fogOfWar.getCell(row, col).observations.add('S'); //Stench
                            }
                            break;
                        case 'H':
                            if(!fogOfWar.getCell(row, col).observations.contains('N')){
                                fogOfWar.getCell(row, col).observations.add('N'); //Hero Moving
                            }
                            break;
                        case 'M':
                            if(!fogOfWar.getCell(row, col).observations.contains('F')){
                                fogOfWar.getCell(row, col).observations.add('F'); //Fire Magic
                            }
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

    public ArrayList<Character> getAIDeadPieces(){
        return this.map.getAIDeadPieces();
    }

    public ArrayList<Character> getPlayerDeadPieces(){
        return this.map.getPlayerDeadPieces();
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
