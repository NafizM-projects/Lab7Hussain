/**
 * Project: Solo Lab 7 Assignment
 * Purpose Details: A space-themed game where the player controls a spaceship,
 *                  shoots projectiles at falling obstacles, collects health power-ups,
 *                  and survives a countdown timer across multiple difficulty levels.
 * Course: IST 242
 * Author: Nafiz Hussain
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class SpaceGame extends JFrame implements KeyListener {

    // ── Constants ────────────────────────────────────────────────────────────

    /** The width of the game window in pixels. */
    private static final int WIDTH = 500;

    /** The height of the game window in pixels. */
    private static final int HEIGHT = 500;

    /** The width of the player's spaceship in pixels. */
    private static final int PLAYER_WIDTH = 50;

    /** The height of the player's spaceship in pixels. */
    private static final int PLAYER_HEIGHT = 50;

    /** The width of each obstacle in pixels. */
    private static final int OBSTACLE_WIDTH = 40;

    /** The height of each obstacle in pixels. */
    private static final int OBSTACLE_HEIGHT = 40;

    /** The width of the projectile in pixels. */
    private static final int PROJECTILE_WIDTH = 5;

    /** The height of the projectile in pixels. */
    private static final int PROJECTILE_HEIGHT = 10;

    /** The number of pixels the player moves per key press. */
    private static final int PLAYER_SPEED = 15;

    /** The base number of pixels obstacles move downward per game tick. */
    private static final int OBSTACLE_SPEED = 3;

    /** The number of pixels the projectile moves upward per game tick. */
    private static final int PROJECTILE_SPEED = 10;

    /** The size (width and height) of a health power-up in pixels. */
    private static final int POWERUP_SIZE = 20;

    /** The number of pixels power-ups move downward per game tick. */
    private static final int POWERUP_SPEED = 2;

    /** The total game duration in seconds. */
    private static final int GAME_DURATION = 60;

    /** The maximum health the player can have. */
    private static final int MAX_HEALTH = 5;

    // ── Score / state ────────────────────────────────────────────────────────

    /** The player's current score. */
    private int score = 0;

    /** The player's current health. */
    private int health = MAX_HEALTH;

    /** The current game level (1–5). */
    private int level = 1;

    /** The number of seconds remaining in the countdown timer. */
    private int timeLeft = GAME_DURATION;

    /** Whether the game is currently in a game-over state. */
    private boolean isGameOver = false;

    /** Whether a projectile is currently visible and active on screen. */
    private boolean isProjectileVisible = false;

    /** Whether the player is currently in the firing cooldown period. */
    private boolean isFiring = false;

    // ── Shield ───────────────────────────────────────────────────────────────

    /** Whether the player's shield is currently active. */
    private boolean shieldActive = false;

    /** The system time in milliseconds when the shield was activated. */
    private long shieldStartTime = 0;

    /** The duration the shield stays active in milliseconds. */
    private long shieldDuration = 3000;

    // ── Player / projectile positions ────────────────────────────────────────

    /** The x-coordinate of the player's spaceship. */
    private int playerX;

    /** The y-coordinate of the player's spaceship. */
    private int playerY;

    /** The x-coordinate of the active projectile. */
    private int projectileX;

    /** The y-coordinate of the active projectile. */
    private int projectileY;

    // ── Collections ──────────────────────────────────────────────────────────

    /** The list of current obstacle positions on screen. */
    private List<Point> obstacles = new ArrayList<>();

    /** The list of star positions used to render the background. */
    private List<Point> stars = new ArrayList<>();

    /** The list of current health power-up positions on screen. */
    private List<Point> powerUps = new ArrayList<>();

    // ── Sprite sheet (4 × 1) ─────────────────────────────────────────────────

    /** The sprite sheet image containing 4 obstacle sprites in a single row. */
    private BufferedImage spriteSheet;

    /** The width of a single sprite on the sprite sheet. */
    private int spriteWidth;

    /** The height of a single sprite on the sprite sheet. */
    private int spriteHeight;

    // ── Ship image ───────────────────────────────────────────────────────────

    /** The image used to render the player's spaceship. */
    private BufferedImage shipImage;

    // ── Audio ────────────────────────────────────────────────────────────────

    /** The file path to the fire sound effect. */
    private String fireSound;

    /** The file path to the collision sound effect. */
    private String collisionSound;

    // ── UI ───────────────────────────────────────────────────────────────────

    /** The main game panel where all drawing takes place. */
    private JPanel gamePanel;

    /** The label that displays the current score in blue. */
    private JLabel scoreLabel;

    /** The main game loop timer that calls update() and repaint() every 20ms. */
    private Timer timer;

    /** The countdown timer that decrements timeLeft every second. */
    private Timer countdownTimer;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constructs and initializes the SpaceGame window.
     * Sets up the game panel, score label, player position, stars, images,
     * audio, game loop timer, and countdown timer.
     */
    public SpaceGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // ── Score label (blue text) ──────────────────────────────────────────
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setBounds(10, 10, 200, 20);
        scoreLabel.setForeground(Color.BLUE);          // Feature 3

        // ── Game panel ───────────────────────────────────────────────────────
        gamePanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };
        gamePanel.setBackground(Color.BLACK);
        gamePanel.add(scoreLabel);
        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        // ── Initial positions ────────────────────────────────────────────────
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        // ── Stars ────────────────────────────────────────────────────────────
        stars = generateStars(200);                     // Feature 1

        // ── Load ship image ──────────────────────────────────────────────────
        try {                                            // Feature 2
            shipImage = ImageIO.read(new File("src/ship(1).png"));
        } catch (IOException e) {
            System.out.println("ship(1).png not found – using default rectangle.");
        }

        // ── Load sprite sheet ────────────────────────────────────────────────
        try {                                            // Feature 4
            spriteSheet = ImageIO.read(new File("src/astro (2).png"));
            spriteWidth  = spriteSheet.getWidth()  / 4; // 4 columns
            spriteHeight = spriteSheet.getHeight();      // 1 row
        } catch (IOException e) {
            System.out.println("astro (2).png not found – using default rectangles.");
        }

        // ── Load audio ───────────────────────────────────────────────────────
        fireSound      = "src/fire.wav";                // Feature 5
        collisionSound = "src/collision.wav";

        // ── Game loop (20 ms ≈ 50 fps) ───────────────────────────────────────
        timer = new Timer(20, e -> {
            if (!isGameOver) {
                update();
                gamePanel.repaint();
            }
        });
        timer.start();

        // ── Countdown timer (1 s tick) ───────────────────────────────────────
        countdownTimer = new Timer(1000, e -> {          // Feature 9
            if (!isGameOver) {
                timeLeft--;
                if (timeLeft <= 0) {
                    timeLeft   = 0;
                    isGameOver = true;
                    countdownTimer.stop();
                    timer.stop();
                }
            }
        });
        countdownTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 1 – Generate stars
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a list of random star positions for the background.
     *
     * @param numStars The number of stars to generate.
     * @return A list of Point objects representing the x, y positions of each star.
     */
    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x, y));
        }
        return starsList;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 1 – Random colour helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a random RGB color for use with star rendering.
     *
     * @return A Color object with random red, green, and blue component values.
     */
    public static Color generateRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return new Color(r, g, b);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 5 – Play a sound file fresh each time (avoids Clip reuse issues)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Plays a WAV audio file on a new background thread.
     * Automatically converts 24-bit PCM audio to 16-bit PCM for Java compatibility.
     * The Clip is closed automatically when playback finishes.
     *
     * @param filename The file path to the WAV audio file to play.
     */
    private void playSound(String filename) {
        if (filename == null) return;
        new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filename));
                AudioFormat baseFormat = ais.getFormat();

                // Convert to 16-bit PCM if needed (Java doesn't support 24-bit natively)
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,                           // force 16-bit
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, // 2 bytes per channel
                        baseFormat.getSampleRate(),
                        false                         // little-endian
                );

                AudioInputStream converted = AudioSystem.getAudioInputStream(targetFormat, ais);
                Clip clip = AudioSystem.getClip();
                clip.open(converted);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                clip.start();
            } catch (Exception e) {
                System.out.println("Could not play sound: " + filename + " – " + e.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Feature 6 – Shield methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Activates the player's shield and records the activation time.
     * While active, the shield blocks obstacle collisions for shieldDuration milliseconds.
     */
    private void activateShield() {
        shieldActive    = true;
        shieldStartTime = System.currentTimeMillis();
    }

    /**
     * Deactivates the player's shield immediately.
     */
    private void deactivateShield() {
        shieldActive = false;
    }

    /**
     * Checks whether the player's shield is currently active.
     * Automatically deactivates the shield if its duration has expired.
     *
     * @return true if the shield is active and within its duration, false otherwise.
     */
    private boolean isShieldActive() {
        if (!shieldActive) return false;
        if (System.currentTimeMillis() - shieldStartTime >= shieldDuration) {
            deactivateShield();
            return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Reset
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resets all game state variables to their initial values and restarts
     * both the game loop timer and the countdown timer.
     * Called when the player presses the ESC key.
     */
    private void reset() {
        score               = 0;
        health              = MAX_HEALTH;
        level               = 1;
        timeLeft            = GAME_DURATION;
        isGameOver          = false;
        isProjectileVisible = false;
        isFiring            = false;
        shieldActive        = false;
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        obstacles.clear();
        powerUps.clear();
        stars = generateStars(200);
        if (!timer.isRunning())          timer.start();
        if (!countdownTimer.isRunning()) countdownTimer.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Draw
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renders all game elements to the screen each frame.
     * Draws the background, stars, player ship, shield, projectile,
     * obstacles, power-ups, HUD (health, timer, level, score),
     * and the game-over overlay when applicable.
     *
     * @param g The Graphics context provided by the game panel's paintComponent method.
     */
    private void draw(Graphics g) {
        // Background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Feature 1 – Stars with random colours, refreshed each frame
        stars = generateStars(200);
        for (Point star : stars) {
            g.setColor(generateRandomColor());
            g.fillOval(star.x, star.y, 2, 2);
        }

        // Feature 2 – Ship (image or fallback rectangle)
        if (shipImage != null) {
            g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        } else {
            g.setColor(Color.BLUE);
            g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        // Feature 6 – Shield overlay
        if (isShieldActive()) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 150, 255, 80));
            g2.fillOval(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10);
            g2.setColor(new Color(0, 200, 255, 200));
            g2.drawOval(playerX - 5, playerY - 5, PLAYER_WIDTH + 10, PLAYER_HEIGHT + 10);
        }

        // Projectile
        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        // Feature 4 – Obstacles via sprite sheet (or fallback red rectangles)
        Random random = new Random();
        for (Point obstacle : obstacles) {
            if (spriteSheet != null) {
                int spriteIndex = random.nextInt(4);
                int spriteX     = spriteIndex * spriteWidth;
                g.drawImage(
                        spriteSheet.getSubimage(spriteX, 0, spriteWidth, spriteHeight),
                        obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT, null
                );
            } else {
                g.setColor(Color.RED);
                g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            }
        }

        // Feature 8 – Power-ups (green "+" shape)
        g.setColor(Color.GREEN);
        for (Point p : powerUps) {
            g.fillRect(p.x + POWERUP_SIZE / 2 - 3, p.y,                        6, POWERUP_SIZE);
            g.fillRect(p.x,                          p.y + POWERUP_SIZE / 2 - 3, POWERUP_SIZE, 6);
        }

        // ── HUD ─────────────────────────────────────────────────────────────
        // Feature 7 – Health bar
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Health:", 10, 40);
        for (int i = 0; i < MAX_HEALTH; i++) {
            g.setColor(i < health ? Color.RED : Color.DARK_GRAY);
            g.fillRect(65 + i * 18, 28, 14, 14);
        }

        // Feature 9 – Countdown timer
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Time: " + timeLeft + "s", WIDTH - 90, 20);

        // Feature 10 – Level
        g.setColor(Color.CYAN);
        g.drawString("Level: " + level, WIDTH - 90, 40);

        // Update score label text (colour already set to blue)
        scoreLabel.setText("Score: " + score);

        // Game-over / win screen
        if (isGameOver) {
            g.setColor(new Color(0, 0, 0, 160));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            String msg = timeLeft <= 0 ? "Time's Up!" : "Game Over!";
            g.drawString(msg, WIDTH / 2 - 75, HEIGHT / 2 - 20);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Final Score: " + score, WIDTH / 2 - 60, HEIGHT / 2 + 15);
            g.drawString("Press ESC to restart", WIDTH / 2 - 85, HEIGHT / 2 + 45);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Update
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates all game logic each frame.
     * Handles obstacle and power-up movement and spawning, projectile movement,
     * level progression, and all collision detection between the player,
     * projectile, obstacles, and power-ups.
     */
    private void update() {
        if (isGameOver) return;

        // Feature 10 – Level progression: increase every 30 pts, cap at 5
        level = Math.min(1 + score / 30, 5);
        int currentObstacleSpeed = OBSTACLE_SPEED + (level - 1);

        // ── Move obstacles ───────────────────────────────────────────────────
        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += currentObstacleSpeed;
            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i--);
            }
        }

        // ── Spawn obstacles (more frequent at higher levels) ─────────────────
        double spawnRate = 0.02 + (level - 1) * 0.008;
        if (Math.random() < spawnRate) {
            int ox = (int)(Math.random() * (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(ox, 0));
        }

        // ── Move power-ups ───────────────────────────────────────────────────
        for (int i = 0; i < powerUps.size(); i++) {
            powerUps.get(i).y += POWERUP_SPEED;
            if (powerUps.get(i).y > HEIGHT) {
                powerUps.remove(i--);
            }
        }

        // ── Spawn power-ups ──────────────────────────────────────────────────
        if (Math.random() < 0.004) {                     // Feature 8
            int px = (int)(Math.random() * (WIDTH - POWERUP_SIZE));
            powerUps.add(new Point(px, 0));
        }

        // ── Move projectile ──────────────────────────────────────────────────
        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;
            if (projectileY < 0) isProjectileVisible = false;
        }

        Rectangle playerRect     = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);

        // ── Player ↔ obstacle collision ──────────────────────────────────────
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle or = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (playerRect.intersects(or)) {
                if (isShieldActive()) {
                    obstacles.remove(i--);               // Shield blocks it
                } else {
                    playSound(collisionSound);           // Feature 5
                    health--;
                    obstacles.remove(i--);
                    if (health <= 0) {
                        isGameOver = true;
                        timer.stop();
                        countdownTimer.stop();
                    }
                }
                break;
            }
        }

        // ── Projectile ↔ obstacle collision ──────────────────────────────────
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle or = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            if (projectileRect.intersects(or)) {
                obstacles.remove(i);
                score += 10;
                isProjectileVisible = false;
                break;
            }
        }

        // ── Player ↔ power-up collision ──────────────────────────────────────
        for (int i = 0; i < powerUps.size(); i++) {      // Feature 8
            Rectangle pr = new Rectangle(powerUps.get(i).x, powerUps.get(i).y, POWERUP_SIZE, POWERUP_SIZE);
            if (playerRect.intersects(pr)) {
                if (health < MAX_HEALTH) health++;
                powerUps.remove(i--);
            }
        }

        // ── Shield auto-expire ───────────────────────────────────────────────
        isShieldActive();                                // checks & deactivates if expired
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  KeyListener
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Handles key press events for player input.
     * LEFT/RIGHT arrows move the ship, SPACE fires a projectile,
     * S activates the shield, and ESC resets the game.
     *
     * @param e The KeyEvent triggered by the player's key press.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {                 // Reset
            reset();
            return;
        }

        if (isGameOver) return;

        if (key == KeyEvent.VK_LEFT && playerX > 0)
            playerX -= PLAYER_SPEED;
        else if (key == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH)
            playerX += PLAYER_SPEED;
        else if (key == KeyEvent.VK_SPACE && !isFiring) {
            isFiring            = true;
            projectileX         = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY         = playerY;
            isProjectileVisible = true;
            playSound(fireSound);                        // Feature 5
            new Thread(() -> {
                try   { Thread.sleep(500); isFiring = false; }
                catch (InterruptedException ex) { ex.printStackTrace(); }
            }).start();
        } else if (key == KeyEvent.VK_S) {               // Feature 6 – 'S' activates shield
            activateShield();
        }
    }

    /**
     * Not used but required by the KeyListener interface.
     *
     * @param e The KeyEvent triggered when a key is typed.
     */
    @Override
    public void keyTyped(KeyEvent e) {}

    /**
     * Not used but required by the KeyListener interface.
     *
     * @param e The KeyEvent triggered when a key is released.
     */
    @Override
    public void keyReleased(KeyEvent e) {}

    // ─────────────────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The entry point of the application.
     * Creates and displays the SpaceGame window on the Swing Event Dispatch Thread.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceGame().setVisible(true));
    }
}