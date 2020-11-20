package sample.back;

import java.util.ArrayList;
import java.util.Scanner;

public class Test {
    public static void main(String[] args){
        Grid test = new Grid(6);
        Logic temp = new Logic(test, 4);
        System.out.println("Initial Map Visual:");
        test.printMap();
        Grid initial = temp.initialize();
        System.out.println("Fog of War Visual");
        initial.printMap();
        initial = temp.render(true);
        initial.printMap();
        initial = temp.render(false);
        initial.printMap();
    }
}
