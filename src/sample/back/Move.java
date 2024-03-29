package sample.back;

import java.util.Objects;
import java.util.PriorityQueue;

public class Move {

    private Cell origin;
    private Cell goal;
    private double heuristicValue;

    public Move(Cell origin, Cell goal, double heuristicValue){
        this.origin = origin;
        this.goal = goal;
        this.heuristicValue = heuristicValue;
    }

    public void setHeuristicValue(double heuristicValue){
        this.heuristicValue = heuristicValue;
    }
    public double getHeuristicValue(){
        return this.heuristicValue;
    }
    public Cell getOrigin(){
        return origin;
    }

    public Cell getGoal(){
        return goal;
    }

    public void print(){
        System.out.println("[" + origin.getRow() + ":" + origin.getCol() + "]" + " MOVING TO " + "[" + goal.getRow() + ":" + goal.getCol() + "]" + " with reward value " + heuristicValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return Double.compare(move.heuristicValue, heuristicValue) == 0 &&
                Objects.equals(origin, move.origin) &&
                Objects.equals(goal, move.goal);
    }

    @Override
    public String toString() {
        // added this for testing purposes
        return origin.getRow() + ":" + origin.getCol() + " to " + goal.getRow() + ":" + goal.getCol() + " with reward " + this.heuristicValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, goal, heuristicValue);
    }
}
