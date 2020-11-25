package sample.back;

import java.util.ArrayList;
import java.util.Scanner;

public class Test {
    public static void main(String[] args){
        Grid original = new Grid(6);
        Logic search = new Logic(original, 4);
        System.out.println("Initial Map Visual:");
        original.printMap();
        Cell start = original.getCell(0,1);
        Cell goal = original.getCell(3, 1);
        search.move(start, goal);
        System.out.println("Fog of War Visual");
        Grid fogOfWar = search.render(true);
        fogOfWar.printMap();
        search.printObservations();
        fogOfWar = search.render(false);
        search.calculateRandomMoveProbability(fogOfWar, false);
        search.updateStateProbabilities(fogOfWar);
        search.calculateRandomMoveProbability(fogOfWar, false);
        search.updateStateProbabilities(fogOfWar);
        search.calculateRandomMoveProbability(fogOfWar, false);
        search.updateStateProbabilities(fogOfWar);
        Cell AIStart = original.getCell(5,0);
        Cell AIGoal = original.getCell(4, 1);
        search.move(AIStart, AIGoal);
        fogOfWar = search.render(false);
        fogOfWar.printMap();
        search.printObservations();
        search.updateStateProbabilities(fogOfWar);
        search.calculateRandomMoveProbability(fogOfWar, false);
        search.updateStateProbabilities(fogOfWar);
        fogOfWar = search.render(false);
        Move bestMove = search.policy();
        bestMove.print();
        fogOfWar = search.render(true);
        fogOfWar.printMap();
        search.printObservations();
    }
}
