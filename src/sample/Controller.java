package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import sample.back.*;

import java.awt.*;
import java.util.ArrayList;

public class Controller {

    //inject FXML objects
    @FXML
    private Label gameStatusLabel;
    @FXML
    private TextField dimField;
    @FXML
    private Pane gridPane;
    //@FXML
    //private TextField depthField;
    //@FXML
    //private Label valueLabel;
    //@FXML
    //private ListView<String> heuristicList;
    @FXML
    private Label currLabel;
    @FXML
    private Label legendLabel;
    @FXML
    private RadioButton randRadio;
    @FXML
    private RadioButton custRadio;
    @FXML
    private Label probLabel;
    @FXML
    private TextArea playerObsArea;
    @FXML
    private TextArea aiObsArea;
    @FXML
    private TextArea deathsArea;

    Grid g = new Grid();
    Logic l = new Logic(g,3);
    //String[] heuristics = new String[]{"1. Distance To Pits","2. Closest Killable Enemy","3. Difference in Pieces","4. Total Pieces", "5. Maximum Distance From Threat", "6. Weighted Heuristics 1-5"};
    Point start = null;
    Point goal = null;


    @FXML
    public void initialize(){
        //heuristicList.getItems().addAll(heuristics);
        legendLabel.setText("Legend: Blue - Player Pieces, Green - AI Pieces.\nDefault Player Movement Model: Random Movement.");
    }


    /**
     * Starts a game with the user's chosen dimensions.
     */
    @FXML
    public void startGame(){

        int dimension = Integer.parseInt(dimField.getText());
        g = new Grid(dimension);
        l = new Logic(g,3);
        buildGrid(l.render(true));
    }

    /*
     * Sets the search depth per user selection. Default is 3.

    @FXML
    public void selectDepth(){
        l = new Logic(g,(int) Double.parseDouble(depthField.getText()));
    }
    */

    @FXML
    public void modelSelect(){
        int playerMovementChoice;
        if (custRadio.isSelected()) playerMovementChoice = 1;
        else playerMovementChoice = 0;
        Grid fog = l.render(true);
        l.calculateRandomMoveProbability(fog,false,playerMovementChoice);
        buildGrid(fog);
    }

    /**
     * Builds a grid in the output pane
     * Color Coding:
     * player piece - blue
     * ai piece - green
     * @param g - grid to be built
     */
    public void buildGrid(Grid g){
        //clear pane
        gridPane.getChildren().clear();

        int dim = g.getMapSize();
        int size = 75;
        int offset = 200;
        int gap = 5;
        ImageView[][] cells = new ImageView[dim][dim];
        Image phero = new Image("/sample/phero.png");
        Image aihero = new Image("/sample/aihero.png");
        Image pit = new Image("/sample/pit.png");
        Image aimage = new Image("/sample/aimage.jpg");
        Image pmage = new Image("/sample/pmage.jpg");
        Image pwumpus = new Image("/sample/pwumpus.png");
        Image aiwumpus = new Image("/sample/aiwumpus.png");
        Image empty = new Image("/sample/empty.png");
        Image fog = new Image("/sample/fog.png");

        for (int y = 0; y < dim; y++){
            for (int x = 0; x < dim; x++){
                final int finalX = x;
                final int finalY = y;
                cells[x][y] = new ImageView();
                cells[x][y].setFitHeight(size);
                cells[x][y].setFitWidth(size);
                cells[x][y].setX(offset + x*(size + gap));
                cells[x][y].setY(offset + y*(size + gap));
                cells[x][y].setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) select(finalX,finalY);
                    else if (e.getButton() == MouseButton.SECONDARY) showProbability(finalX,finalY, g);});
                Cell currCell = g.map[y][x];
                Image hero = null;
                Image wumpus = null;
                Image mage = null;


                if(currCell.belongToPlayer() == '1'){
                    hero = phero;
                    wumpus = pwumpus;
                    mage = pmage;
                }
                else if (currCell.belongToPlayer() == '2'){
                    hero = aihero;
                    wumpus = aiwumpus;
                    mage = aimage;
                }


                switch(currCell.getType()){
                    case 'E':
                        cells[x][y].setImage(empty);
                        break;
                    case 'H':
                        cells[x][y].setImage(hero);
                        break;
                    case 'M':
                        cells[x][y].setImage(mage);
                        break;
                    case 'W':
                        cells[x][y].setImage(wumpus);
                        break;
                    case 'P':
                        cells[x][y].setImage(pit);
                        break;
                    case '?':
                        cells[x][y].setImage(fog);
                }

                gridPane.getChildren().add(cells[x][y]);

            }
        }
        StringBuilder deathMessage = new StringBuilder();

        ArrayList<Character> aiDeaths = l.getAIDeadPieces();
        ArrayList<Character> playerDeaths = l.getPlayerDeadPieces();

        if(!aiDeaths.isEmpty()) {
            deathMessage = new StringBuilder("AI Dead Pieces:\n");
            for (char c : aiDeaths) {
                deathMessage.append(mapTypeToString(c)).append("\n");
            }
        }
        if(!playerDeaths.isEmpty()){
            deathMessage.append("Human Dead Pieces:\n");
            for (char c : playerDeaths) {
                deathMessage.append(mapTypeToString(c)).append("\n");
            }
        }
        deathsArea.setText(deathMessage.toString());
        //System.out.println(aiDeaths.size());
        //System.out.println(playerDeaths.size());
    }


    /**
     * Selects a start/goal pair based on user input.
     * @param x - cell x coordinate
     * @param y - cell y coordinate
     */
    private void select(int x, int y){
        if (start == null){
            start = new Point(x,y);
            currLabel.setText("Current Selection: [" + x + "," + y + "]");
            return;
        }
        else{
            goal = new Point(x,y);
        }
        Cell startCell = g.getCell((int) start.getY(),(int) start.getX());
        Cell goalCell = g.getCell((int) goal.getY(),(int) goal.getX());

        if(l.validPlayerMove(startCell,goalCell)) {
            l.move(startCell, goalCell);
            //ArrayList<Cell> observations = new ArrayList<>();
            Grid tmp = l.render(true);
            buildGrid(tmp);
            ArrayList<Cell> observations = l.getObservations();
            if(!observations.isEmpty()) {
                for (Cell o : observations) {
                    ArrayList<Character> obs = o.observations;
                    for (Character c : obs) {
                        playerObsArea.setText(playerObsArea.getText() + "\nPlayer Observed " + mapTypeToString(c) + " at " + "(" + Integer.toString(o.getRow()) + ", " + Integer.toString(o.getCol()) + ")");
                    }
                }
                playerObsArea.appendText("\n");
            }

            currLabel.setText("Moving to: [" + x + "," + y + "]");
        }
        start = null;
        goal = null;
    }

    /**
     * Toggles no fog
     */
    @FXML
    private void noFog(){
        buildGrid(l.getFullState());
    }

    /**
     * Toggles player fog only
     */
    @FXML
    private void playerFog(){
        buildGrid(l.render(true));
        ArrayList<Cell> observations = l.getObservations();
        if(!observations.isEmpty()) {
            for (Cell o : observations) {
                ArrayList<Character> obs = o.observations;
                for (Character c : obs) {
                    playerObsArea.setText(playerObsArea.getText() + "\nPlayer Observed " + mapTypeToString(c) + " at " + "(" + Integer.toString(o.getRow()) + ", " + Integer.toString(o.getCol()) + ")");
                }
            }
            playerObsArea.appendText("\n");
        }
    }

    /**
     * Toggles ai fog only
     */
    @FXML
    private void aiFog(){
        buildGrid(l.render(false));
        ArrayList<Cell> observations = l.getObservations();
        if(!observations.isEmpty()) {
            for (Cell o : observations) {
                ArrayList<Character> obs = o.observations;
                for (Character c : obs) {
                    aiObsArea.setText(aiObsArea.getText() + "\nAI Observed " + mapTypeToString(c) + " at " + "(" + Integer.toString(o.getRow()) + ", " + Integer.toString(o.getCol()) + ")");
                }
            }
            aiObsArea.appendText("\n");
        }
    }

    /**
     * shows probability of selected cell
     * @param x - cell x position
     * @param y - cell y position
     */
    public void showProbability(int x, int y, Grid g){
        double wumpProb =  g.getCell(y, x).getWumpusProb();
        double mageProb = g.getCell(y, x).getMageProb();
        double heroProb = g.getCell(y, x).getHeroProb();
        double pitProb = g.getCell(y, x).getPitProb();
        probLabel.setText("Wumpus Probability: " + wumpProb + "\nMage Probability: "+ mageProb + "\nHero Prob: "+heroProb+"\nPit Probability: "+pitProb);
        /*
        System.out.println("X:" + x + " Y: " + y);
        System.out.println("Wumpus Prob " + g.getCell(y, x).getWumpusProb());
        System.out.println("Mage Prob " + g.getCell(y, x).getMageProb());
        System.out.println("Hero Prob " + g.getCell(y, x).getHeroProb());
        System.out.println("Pit Prob " + g.getCell(y, x).getPitProb());
         */
    }
    /**
     * Runs the AI's move and checks if the game is over.
     */
    @FXML
    private void nextTurn(){
        /*
        double val;
        int heuristicSelected = heuristicList.getSelectionModel().getSelectedIndex();
        if(heuristicSelected == -1){
            val = l.run(0);
        }
        else{val = l.run(heuristicSelected);}

        valueLabel.setText("Move Value: \n" + val);
         */


        int playerMovementChoice;
        if (custRadio.isSelected()) playerMovementChoice = 1;
        else playerMovementChoice = 0;
        //g = l.AInextTurn(playerMovementChoice);

        buildGrid(l.AInextTurn(playerMovementChoice));

        //buildGrid(g);
        int gameCon = l.checkWin();

        switch (gameCon){
            case 0:
                gameStatusLabel.setText("Game Status: \n It's a Draw");
                break;
            case 1:
                gameStatusLabel.setText("Game Status: \n Player Wins");
                break;
            case 2:
                gameStatusLabel.setText("Game Status: \n AI Wins");
                break;
            default:
                gameStatusLabel.setText("Game Status: \n In Progress");
                break;
        }
    }

    private String mapTypeToString(char c){
        switch (c){
            case 'F':
                return "Fire Magic";
            case 'B':
                return "Breeze";
            case 'S':
                return "Stench";
            case 'N':
                return "Noise";
            case 'W':
                return "Wumpus";
            case 'M':
                return "Mage";
            case 'H':
                return "Hero";
        }
        return "";
    }
}
