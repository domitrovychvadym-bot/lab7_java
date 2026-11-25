import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Лабораторна робота №7. Завдання 1. Варіант 7.
 * Два окремі потоки керування рухом об'єктів:
 * 1. Капітальні будинки -> рух у ліву верхню чверть [0, 0, w/2, h/2] зі швидкістю V.
 * 2. Дерев'яні будинки -> рух у праву нижню чверть [w/2, h/2, w/2, h/2] зі швидкістю V.
 */
public class Task1Simulation extends JFrame {

    // Константи симуляції згідно із завданням
    private static final double SPEED_V = 2.5;
    private static final int HOUSE_SIZE = 30;

    private final List<House> houses = new ArrayList<>();
    private final Object lock = new Object();
    private final SimulationPanel panel;

    private final CapitalHouseThread capitalThread;
    private final WoodenHouseThread woodenThread;
    private final Random random = new Random();

    public Task1Simulation() {
        super("Лабораторна робота №7 — Завдання 1 (Варіант 7)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        panel = new SimulationPanel();
        add(panel, BorderLayout.CENTER);

        // Панель для додавання нових будинків
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        JButton addCapitalBtn = new JButton("Додати капітальний будинок");
        JButton addWoodenBtn = new JButton("Додати дерев'яний будинок");

        addCapitalBtn.addActionListener(e -> spawnHouse(true));
        addWoodenBtn.addActionListener(e -> spawnHouse(false));

        controlPanel.add(addCapitalBtn);
        controlPanel.add(addWoodenBtn);
        add(controlPanel, BorderLayout.SOUTH);

        // Ініціалізація початкових об'єктів
        for (int i = 0; i < 6; i++) {
            spawnHouse(true);
            spawnHouse(false);
        }

        // Створення та запуск двох окремих потоків керування рухом
        capitalThread = new CapitalHouseThread();
        woodenThread = new WoodenHouseThread();

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panel.repaint();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                capitalThread.stopRunning();
                woodenThread.stopRunning();
            }
        });

        capitalThread.start();
        woodenThread.start();
    }

    private void spawnHouse(boolean isCapital) {
        int w = Math.max(200, panel.getWidth() > 0 ? panel.getWidth() : 850);
        int h = Math.max(200, panel.getHeight() > 0 ? panel.getHeight() : 520);
        double x = random.nextInt(w - HOUSE_SIZE);
        double y = random.nextInt(h - HOUSE_SIZE);

        synchronized (lock) {
            if (isCapital) {
                houses.add(new CapitalHouse(x, y));
            } else {
                houses.add(new WoodenHouse(x, y));
            }
        }
        panel.repaint();
    }

    // --- Ієрархія об'єктів симуляції ---
    private abstract static class House {
        double x, y;
        double targetX, targetY;
        boolean targetInitialized = false;
        boolean moving = true;

        House(double x, double y) {
            this.x = x;
            this.y = y;
        }

        abstract void draw(Graphics g);
    }

    private static class CapitalHouse extends House {
        CapitalHouse(double x, double y) {
            super(x, y);
        }

        @Override
        void draw(Graphics g) {
            int ix = (int) Math.round(x);
            int iy = (int) Math.round(y);
            g.setColor(new Color(65, 90, 125));
            g.fillRect(ix, iy, HOUSE_SIZE, HOUSE_SIZE);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(ix - 2, iy - 4, HOUSE_SIZE + 4, 5);
            g.setColor(Color.CYAN);
            g.fillRect(ix + 5, iy + 6, 7, 7);
            g.fillRect(ix + 18, iy + 6, 7, 7);
        }
    }

    private static class WoodenHouse extends House {
        WoodenHouse(double x, double y) {
            super(x, y);
        }

        @Override
        void draw(Graphics g) {
            int ix = (int) Math.round(x);
            int iy = (int) Math.round(y);
            g.setColor(new Color(165, 100, 42));
            g.fillRect(ix, iy + 10, HOUSE_SIZE, HOUSE_SIZE - 10);
            g.setColor(new Color(135, 40, 30));
            int[] xs = {ix - 3, ix + HOUSE_SIZE / 2, ix + HOUSE_SIZE + 3};
            int[] ys = {iy + 10, iy, iy + 10};
            g.fillPolygon(xs, ys, 3);
            g.setColor(Color.YELLOW);
            g.fillRect(ix + 11, iy + 15, 8, 8);
        }
    }

    // --- Потік 1: Капітальні будинки ---
    private class CapitalHouseThread extends Thread {
        private volatile boolean running = true;

        CapitalHouseThread() {
            super("CapitalHouse-Thread");
        }

        public void stopRunning() {
            running = false;
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                int w = panel.getWidth();
                int h = panel.getHeight();
                if (w > 0 && h > 0) {
                    double halfW = w / 2.0;
                    double halfH = h / 2.0;

                    synchronized (lock) {
                        for (House house : houses) {
                            if (!(house instanceof CapitalHouse) || !house.moving) continue;

                            if (!house.targetInitialized) {
                                // Якщо одразу згенерувався в межах лівої верхньої чверті [0,0; w/2,h/2] -> зупиняється
                                if (house.x >= 0 && house.x <= halfW - HOUSE_SIZE &&
                                        house.y >= 0 && house.y <= halfH - HOUSE_SIZE) {
                                    house.moving = false;
                                    continue;
                                }
                                house.targetX = random.nextDouble() * Math.max(1, halfW - HOUSE_SIZE);
                                house.targetY = random.nextDouble() * Math.max(1, halfH - HOUSE_SIZE);
                                house.targetInitialized = true;
                            }

                            moveStep(house);
                        }
                    }
                    panel.repaint();
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // --- Потік 2: Дерев'яні будинки ---
    private class WoodenHouseThread extends Thread {
        private volatile boolean running = true;

        WoodenHouseThread() {
            super("WoodenHouse-Thread");
        }

        public void stopRunning() {
            running = false;
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                int w = panel.getWidth();
                int h = panel.getHeight();
                if (w > 0 && h > 0) {
                    double halfW = w / 2.0;
                    double halfH = h / 2.0;

                    synchronized (lock) {
                        for (House house : houses) {
                            if (!(house instanceof WoodenHouse) || !house.moving) continue;

                            if (!house.targetInitialized) {
                                // Якщо одразу згенерувався в межах правої нижньої чверті [w/2,h/2; w,h] -> зупиняється
                                if (house.x >= halfW && house.x <= w - HOUSE_SIZE &&
                                        house.y >= halfH && house.y <= h - HOUSE_SIZE) {
                                    house.moving = false;
                                    continue;
                                }
                                house.targetX = halfW + random.nextDouble() * Math.max(1, halfW - HOUSE_SIZE);
                                house.targetY = halfH + random.nextDouble() * Math.max(1, halfH - HOUSE_SIZE);
                                house.targetInitialized = true;
                            }

                            moveStep(house);
                        }
                    }
                    panel.repaint();
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void moveStep(House house) {
        double dx = house.targetX - house.x;
        double dy = house.targetY - house.y;
        double dist = Math.hypot(dx, dy);

        if (dist <= SPEED_V) {
            house.x = house.targetX;
            house.y = house.targetY;
            house.moving = false;
        } else {
            house.x += (dx / dist) * SPEED_V;
            house.y += (dy / dist) * SPEED_V;
        }
    }

    // --- Панель відображення (EDT) ---
    private class SimulationPanel extends JPanel {
        SimulationPanel() {
            setBackground(new Color(238, 245, 238));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Підсвічування цільових чвертей
            g2.setColor(new Color(100, 149, 237, 45));
            g2.fillRect(0, 0, w / 2, h / 2);
            g2.setColor(Color.BLUE);
            g2.drawString("Ліва верхня чверть [0,0; w/2,h/2] — Капітальні будинки", 10, 20);

            g2.setColor(new Color(210, 105, 30, 45));
            g2.fillRect(w / 2, h / 2, w / 2, h / 2);
            g2.setColor(new Color(139, 69, 19));
            g2.drawString("Права нижня чверть [w/2,h/2; w,h] — Дерев'яні будинки", w / 2 + 10, h / 2 + 20);

            g2.setColor(Color.GRAY);
            g2.drawLine(w / 2, 0, w / 2, h);
            g2.drawLine(0, h / 2, w, h / 2);

            synchronized (lock) {
                for (House house : houses) {
                    house.draw(g2);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Task1Simulation().setVisible(true));
    }
}