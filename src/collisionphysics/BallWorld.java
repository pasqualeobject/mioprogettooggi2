package collisionphysics;

import net.yura.domination.engine.Risk;
import net.yura.domination.engine.core.Country;
import net.yura.domination.engine.guishared.MapPanel;

/**
 * @author yura mamyrin
 */
public class BallWorld implements Runnable {

    private static final int UPDATE_RATE = 30;    // Frames per second (fps)
    private static final float EPSILON_TIME = 1e-2f;  // Threshold for zero time

    int[] box = new int[2];
    MapPanel panel;
    /**
     * Array of Ball
     */
    public Ball[] balls;
    boolean running;

    public BallWorld(Risk risk,MapPanel panel, int r) {

        Country[] v = risk.getGame().getCountries();
        int currentNumBalls = v.length;

        this.panel = panel;

        box[0] = panel.getMapWidth();
        box[1] = panel.getMapHeight();

        balls = new Ball[currentNumBalls];
        for (int c=0;c<v.length;c++) {
            balls[c] = new Ball(v[c].getX(), v[c].getY(), r, 2, 270);
        }
        running = true;
        new Thread(this).start();
    }

    public void stop() {
        running = false;
    }

    public void run() {

        while (running) {
            long beginTimeMillis, timeTakenMillis, timeLeftMillis;
            beginTimeMillis = System.currentTimeMillis();

            // Execute one game step
            gameUpdate();
            // Refresh the display
            panel.repaint();

            // Provide the necessary delay to meet the target rate
            timeTakenMillis = System.currentTimeMillis() - beginTimeMillis;
            timeLeftMillis = 1000L / UPDATE_RATE - timeTakenMillis;
            if (timeLeftMillis < 5) timeLeftMillis = 5; // Set a minimum

            // Delay and give other thread a chance
            try {
                Thread.sleep(timeLeftMillis);
            } catch (InterruptedException ex) {
            System.out.println("error");
            }
        }

    }

    public void gameUpdate() {

        int currentNumBalls = balls.length;

        float timeLeft = 1.0f;  // One time-step to begin with

        // Repeat until the one time-step is up
        do {
            // Find the earliest collision up to timeLeft among all objects
            float tMin = timeLeft;

            // Check collision between two balls
            tMin = checkCollision(currentNumBalls,tMin);
            // Check collision between the balls and the box
            for (int i = 0; i < currentNumBalls; i++) {
                balls[i].intersect(box, tMin);
                if (balls[i].earliestCollisionResponse.t < tMin) {
                    tMin = balls[i].earliestCollisionResponse.t;
                }
            }
            // Update all the balls up to the detected earliest collision time tMin,
            // or timeLeft if there is no collision.
            updateBalls(currentNumBalls, tMin);
            timeLeft -= tMin;                // Subtract the time consumed and repeat
        } while (timeLeft > EPSILON_TIME);  // Ignore remaining time less than threshold
    }
    private float checkCollision(int currentNumBalls, float tMin) {
        float time = 0;
        for (int i = 0; i < currentNumBalls; i++) {
            for (int j = 0; j < currentNumBalls; j++) {
                time = checkNumBalls(i,j,tMin);
            }
        }
        return time;
    }
    private float checkNumBalls(int i, int j, float tMin) {
        float timeMin = 0;
        if (i < j) {
            balls[i].intersect(balls[j], tMin);
            if (balls[i].earliestCollisionResponse.t < tMin) {
                timeMin = balls[i].earliestCollisionResponse.t;
            }
        }
        return timeMin;
    }
    private void updateBalls(int currentNumBalls, float tMin) {
        for (int i = 0; i < currentNumBalls; i++) {
            balls[i].update(tMin);
        }
    }
}
