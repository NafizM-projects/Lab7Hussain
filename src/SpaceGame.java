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
    private static final int WIDTH            = 500;
    private static final int HEIGHT           = 500;
    private static final int PLAYER_WIDTH     = 50;
    private static final int PLAYER_HEIGHT    = 50;
    private static final int OBSTACLE_WIDTH   = 40;
    private static final int OBSTACLE_HEIGHT  = 40;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT= 10;
    private static final int PLAYER_SPEED     = 15;
    private static final int OBSTACLE_SPEED   = 3;
    private static final int PROJECTILE_SPEED = 10;
    private static final int POWERUP_SIZE     = 20;
    private static final int POWERUP_SPEED    = 2;
    private static final int GAME_DURATION    = 60;   // seconds
    private static final int MAX_HEALTH       = 5;

    // ── Score / state ────────────────────────────────────────────────────────
    private int  score       = 0;
    private int  health      = MAX_HEALTH;
    private int  level       = 1;
    private int  timeLeft    = GAME_DURATION;
    private boolean isGameOver         = false;
    private boolean isProjectileVisible= false;
    private boolean isFiring           = false;

    // ── Shield ───────────────────────────────────────────────────────────────
    private boolean shieldActive    = false;
    private long    shieldStartTime = 0;
    private long    shieldDuration  = 3000; // ms

    // ── Player / projectile positions ────────────────────────────────────────
    private int playerX, playerY;
    private int projectileX, projectileY;

    // ── Collections ──────────────────────────────────────────────────────────
    private List<Point> obstacles  = new ArrayList<>();
    private List<Point> stars      = new ArrayList<>();
    private List<Point> powerUps   = new ArrayList<>();

    // ── Sprite sheet (4 × 1) ─────────────────────────────────────────────────
    private BufferedImage spriteSheet;
    private int spriteWidth;
    private int spriteHeight;

    // ── Ship image ───────────────────────────────────────────────────────────
    private BufferedImage shipImage;

    // ── Audio ────────────────────────────────────────────────────────────────
    private String fireSound;
    private String collisionSound;

    // ── UI ───────────────────────────────────────────────────────────────────
    private JPanel gamePanel;
    private JLabel scoreLabel;
    private Timer  timer;
    private Timer  countdownTimer;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────
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
        fireSound      = "src/fire.wav";            // Feature 5
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
                        16,                          // force 16-bit
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2, // 2 bytes per channel
                        baseFormat.getSampleRate(),
                        false                        // little-endian
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
    private void activateShield() {
        shieldActive    = true;
        shieldStartTime = System.currentTimeMillis();
    }

    private void deactivateShield() {
        shieldActive = false;
    }

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
    private void reset() {
        score              = 0;
        health             = MAX_HEALTH;
        level              = 1;
        timeLeft           = GAME_DURATION;
        isGameOver         = false;
        isProjectileVisible= false;
        isFiring           = false;
        shieldActive       = false;
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
            g.fillRect(p.x + POWERUP_SIZE / 2 - 3, p.y,               6, POWERUP_SIZE);
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
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ESCAPE) {                 // Reset
            reset();
            return;
        }

        if (isGameOver) return;

        if (key == KeyEvent.VK_LEFT  && playerX > 0)
            playerX -= PLAYER_SPEED;
        else if (key == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH)
            playerX += PLAYER_SPEED;
        else if (key == KeyEvent.VK_SPACE && !isFiring) {
            isFiring           = true;
            projectileX        = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY        = playerY;
            isProjectileVisible= true;
            playSound(fireSound);                        // Feature 5
            new Thread(() -> {
                try   { Thread.sleep(500); isFiring = false; }
                catch (InterruptedException ex) { ex.printStackTrace(); }
            }).start();
        } else if (key == KeyEvent.VK_S) {               // Feature 6 – 'S' activates shield
            activateShield();
        }
    }

    @Override public void keyTyped(KeyEvent e)   {}
    @Override public void keyReleased(KeyEvent e){}

    // ─────────────────────────────────────────────────────────────────────────
    //  Main
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceGame().setVisible(true));
    }
}