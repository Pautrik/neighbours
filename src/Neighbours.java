import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.sqrt;
import static java.lang.System.exit;
import static java.lang.System.nanoTime;

/*
 *  Program to simulate segregation.
 *  See : http://nifty.stanford.edu/2014/mccown-schelling-model-segregation/
 *
 * NOTE:
 * - JavaFX first calls method init() and then method start() far below.
 * - To test uncomment call to test() first in init() method!
 *
 */
// Extends Application because of JavaFX (just accept for now)
public class Neighbours extends Application {

    // Enumeration type for the Actors
    enum Actor {
        BLUE, RED, NONE   // NONE used for empty locations
    }

    // Enumeration type for the state of an Actor
    enum State {
        UNSATISFIED,
        SATISFIED,
        NA     // Not applicable (NA), used for NONEs
    }

    // Below is the *only* accepted instance variable (i.e. variables outside any method)
    // This variable may *only* be used in methods init() and updateWorld()
    Actor[][] world;              // The world is a square matrix of Actors

    // This is the method called by the timer to update the world
    // (i.e move unsatisfied) approx each 1/60 sec.
    void updateWorld() {
        // % of surrounding neighbours that are like me
        final double threshold = 0.7;
        ArrayList<Point> noneLocations = new ArrayList<>();
        ArrayList<Point> dissatisfiedLocations = new ArrayList<>();

        setNoneAndDissatisfiedActorLocations(noneLocations, dissatisfiedLocations, threshold);
        if(dissatisfiedLocations.size() != 0)
            relocateDissatisfied(noneLocations, dissatisfiedLocations);
        else
            System.out.println("Everybody is satisfied");
    }

    // This method initializes the world variable with a random distribution of Actors
    // Method automatically called by JavaFX runtime (before graphics appear)
    // Don't care about "@Override" and "public" (just accept for now)
    @Override
    public void init() {
        //test();    // <---------------- Uncomment to TEST!

        // %-distribution of RED and BLUE
        double[] dist = {0.45, 0.45};
        // Number of locations (places) in world (square)
        int nLocations = 900;

        populateWorld(nLocations, dist);

        // Should be last
        fixScreenSize(nLocations);
    }


    // ------- Methods ------------------

    // TODO write the methods here, implement/test bottom up

    private void populateWorld(int nLocations, double[] dist) {
        int sideLength = (int)Math.sqrt(nLocations);
        world = new Actor[sideLength][sideLength];

        for(Actor[] dimension : world)
            Arrays.fill(dimension, Actor.NONE);

        int nRed = (int)(dist[0] * nLocations);
        int nBlue = (int)(dist[1] * nLocations);

        addColorsToGrid(sideLength, nRed, Actor.RED);
        addColorsToGrid(sideLength, nBlue, Actor.BLUE);
    }

    private void addColorsToGrid(int sideLength, int nActors, Actor color) {
        int x, y;
        while(nActors > 0) {
             x = ThreadLocalRandom.current().nextInt(sideLength);
             y = ThreadLocalRandom.current().nextInt(sideLength);

             if(world[x][y] == Actor.NONE) {
                 world[x][y] = color;
                 nActors--;
             }
        }
    }


    private void setNoneAndDissatisfiedActorLocations(ArrayList<Point> noneLocations, ArrayList<Point> dissatisfiedLocations, double threshold) {
        for(int x = 0; x < world.length; x++) {
            for(int y = 0; y < world[0].length; y++) {
                Point point = new Point(x, y);
                if(world[x][y] == Actor.NONE) {
                    noneLocations.add(point);
                }
                else if(isDissatisfied(point, threshold)) {
                    dissatisfiedLocations.add(point);
                }
            }
        }
    }

    private boolean isDissatisfied(Point p, double threshold) {
        int nBlue = 0;
        int nRed = 0;
        
        int x, y;
        for(int xOffset = -1; xOffset <= 1; xOffset++) {
            for(int yOffset = -1; yOffset <= 1; yOffset++) {
                x = p.x + xOffset;
                y = p.y + yOffset;
                if(xOffset == 0 && yOffset == 0 || x < 0 || x >= world.length || y < 0 || y >= world[0].length)
                    continue;
                else if(world[x][y] == Actor.BLUE)
                    nBlue++;
                else if(world[x][y] == Actor.RED)
                    nRed++;
            }
        }

        int quota;
        if(nBlue + nRed > 0) {
            if (world[p.x][p.y] == Actor.BLUE)
                quota = nBlue / (nBlue + nRed);
            else
                quota = nRed / (nBlue + nRed);

            return quota >= threshold;
        }
        else {
            return false;
        }
    }

    private void relocateDissatisfied(ArrayList<Point> noneLocations, ArrayList<Point> dissatisfiedLocations) {
        int noneIndex;
        Point nonePoint;
        for(Point dissatisfied : dissatisfiedLocations) {
            noneIndex = ThreadLocalRandom.current().nextInt(noneLocations.size());
            nonePoint = noneLocations.get(noneIndex);

            world[nonePoint.x][nonePoint.y] = world[dissatisfied.x][dissatisfied.y];
            world[dissatisfied.x][dissatisfied.y] = Actor.NONE;
            noneLocations.remove(noneIndex);
        }
    }

    // ------- Testing -------------------------------------

    // Here you run your tests i.e. call your logic methods
    // to see that they really work
    void test() {
        // A small hard coded world for testing
        Actor[][] testWorld = new Actor[][]{
                {Actor.RED, Actor.RED, Actor.NONE},
                {Actor.NONE, Actor.BLUE, Actor.NONE},
                {Actor.RED, Actor.NONE, Actor.BLUE}
        };
        double th = 0.5;   // Simple threshold used for testing
        int size = testWorld.length;

        // TODO test methods

        exit(0);
    }

    // Helper method for testing (NOTE: reference equality)
    <T> int count(T[] arr, T toFind) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == toFind) {
                count++;
            }
        }
        return count;
    }

    // *****   NOTHING to do below this row, it's JavaFX stuff  ******

    double width = 400;   // Size for window
    double height = 400;
    long previousTime = nanoTime();
    final long interval = 450000000;
    double dotSize;
    final double margin = 50;

    void fixScreenSize(int nLocations) {
        // Adjust screen window depending on nLocations
        dotSize = (width - 2 * margin) / sqrt(nLocations);
        if (dotSize < 1) {
            dotSize = 2;
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Build a scene graph
        Group root = new Group();
        Canvas canvas = new Canvas(width, height);
        root.getChildren().addAll(canvas);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a timer
        AnimationTimer timer = new AnimationTimer() {
            // This method called by FX, parameter is the current time
            public void handle(long currentNanoTime) {
                long elapsedNanos = currentNanoTime - previousTime;
                if (elapsedNanos > interval) {
                    updateWorld();
                    renderWorld(gc, world);
                    previousTime = currentNanoTime;
                }
            }
        };

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulation");
        primaryStage.show();

        timer.start();  // Start simulation
    }


    // Render the state of the world to the screen
    public void renderWorld(GraphicsContext g, Actor[][] world) {
        g.clearRect(0, 0, width, height);
        int size = world.length;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                double x = dotSize * col + margin;
                double y = dotSize * row + margin;

                if (world[row][col] == Actor.RED) {
                    g.setFill(Color.RED);
                } else if (world[row][col] == Actor.BLUE) {
                    g.setFill(Color.BLUE);
                } else {
                    g.setFill(Color.WHITE);
                }
                g.fillOval(x, y, dotSize, dotSize);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
