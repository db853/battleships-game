import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StartMenu extends JPanel {
    private MenuListener listener;
    private final int START_BOARD_WIDTH = 500;
    private final int START_BOARD_HEIGHT = 400;

    private Rectangle easyRect;
    private Rectangle mediumRect;
    private Rectangle hardRect;
    private final Font textFont = new Font("Arial", Font.PLAIN, 40);
    private final Color backgroundColour = Color.decode("#87ceeb");
    private final Color hoverColour = backgroundColour.brighter();
    private Color easyRectColour;
    private Color mediumRectColour;
    private Color hardRectColour;
    private boolean easyHovering = false;
    private boolean mediumHovering = false;
    private boolean hardHovering = false;



    StartMenu() {
        this.setPreferredSize(new Dimension(START_BOARD_WIDTH, START_BOARD_HEIGHT));

        final boolean[] inside = new boolean[3];
        easyRectColour = backgroundColour;
        mediumRectColour = backgroundColour;
        hardRectColour = backgroundColour;

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (easyRect == null || mediumRect == null || hardRect == null) {
                    return;
                }

                inside[0] = easyRect.contains(e.getPoint());
                inside[1] = mediumRect.contains(e.getPoint());
                inside[2] = hardRect.contains(e.getPoint());

                if (listener == null) {
                    return;
                }

                if (inside[0]) {
                    listener.onStartClicked("EASY");
                } else if (inside[1]) {
                    listener.onStartClicked("MEDIUM");
                } else if (inside[2]) {
                    listener.onStartClicked("HARD");
                }
            }
        });


        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkStartsHovering(e);
            }
        });


    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawStartMenu(g);
    }

    public void drawStartMenu(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(backgroundColour);
        g2d.fillRect(0, 0, START_BOARD_WIDTH, START_BOARD_HEIGHT);

        FontMetrics textFM = g2d.getFontMetrics(textFont);
        Point[] textPositions = calculateTextPositions(textFM, g2d);

        // Adjust y to top left corner
        int adjustedEasyY = textPositions[1].y - 60;
        int adjustedMediumY = textPositions[2].y - 60;
        int adjustedHardY = textPositions[3].y - 60;

        easyRect = new Rectangle(textPositions[1].x, adjustedEasyY, textFM.stringWidth("Play against easy bot"),
                75);
        mediumRect = new Rectangle(textPositions[2].x, adjustedMediumY, textFM.stringWidth("Play against medium bot"),
                75);
        hardRect = new Rectangle(textPositions[3].x, adjustedHardY, textFM.stringWidth("Play against hard bot"),
                75);

        g2d.setColor(easyRectColour);
        g2d.fillRoundRect(easyRect.x, easyRect.y, easyRect.width, easyRect.height, 50, 75);

        g2d.setColor(mediumRectColour);
        g2d.fillRoundRect(mediumRect.x, mediumRect.y, mediumRect.width, mediumRect.height, 50, 75);

        g2d.setColor(hardRectColour);
        g2d.fillRoundRect(hardRect.x, hardRect.y, hardRect.width, hardRect.height, 50, 75);

        g2d.setColor(Color.WHITE);
        g2d.setFont(textFont);

        g2d.drawString("Battleship", textPositions[0].x, textPositions[0].y);
        g2d.drawString("Play against easy bot", textPositions[1].x, textPositions[1].y);
        g2d.drawString("Play against medium bot", textPositions[2].x, textPositions[2].y);
        g2d.drawString("Play against hard bot", textPositions[3].x, textPositions[3].y);

    }

    public void checkStartsHovering(MouseEvent e) {
        if (easyRect.contains(e.getPoint()) && !easyHovering) {
            easyHovering = true;
            easyRectColour = hoverColour;
            repaint();
        } else if (!easyRect.contains(e.getPoint()) && easyHovering) {
            easyHovering = false;
            easyRectColour = backgroundColour;
            repaint();
        }

        if (mediumRect.contains(e.getPoint()) && !mediumHovering) {
            mediumHovering = true;
            mediumRectColour = hoverColour;
            repaint();
        } else if (!mediumRect.contains(e.getPoint()) && mediumHovering) {
            mediumHovering = false;
            mediumRectColour = backgroundColour;
            repaint();
        }

        if (hardRect.contains(e.getPoint()) && !hardHovering) {
            hardHovering = true;
            hardRectColour = hoverColour;
            repaint();
        } else if (!hardRect.contains(e.getPoint()) && hardHovering) {
            hardHovering = false;
            hardRectColour = backgroundColour;
            repaint();
        }
    }

    public Point[] calculateTextPositions(FontMetrics textFM, Graphics2D g2d) {
        int titleX = (START_BOARD_WIDTH - textFM.stringWidth("Battleship")) / 2;
        int titleY = START_BOARD_HEIGHT / 8;


        int easyX = (START_BOARD_WIDTH - textFM.stringWidth("Play against easy bot")) / 2;
        int easyY = 7 * START_BOARD_HEIGHT / 16;

        int mediumX = (START_BOARD_WIDTH - textFM.stringWidth("Play against medium bot")) / 2;
        int mediumY = 5 * START_BOARD_HEIGHT / 8;

        int hardX = (START_BOARD_WIDTH - textFM.stringWidth("Play against hard bot")) / 2;
        int hardY = 13 * START_BOARD_HEIGHT / 16;


        Point titlePoint = new Point(titleX, titleY);
        Point easyPoint = new Point(easyX, easyY);
        Point mediumPoint = new Point(mediumX, mediumY);
        Point hardPoint = new Point(hardX, hardY);

        return new Point[]{titlePoint, easyPoint, mediumPoint, hardPoint};
    }

    public void setMenuListener(MenuListener listener) {
        this.listener = listener;
    }

}
