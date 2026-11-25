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
 * Лабораторна робота №7. Завдання 2. Варіант 7.
 * Включає:
 * 1. Абстрактний клас BaseAI (спадкоємець Thread) зі спільною колекцією об'єктів.
 * 2. Спадкоємці CapitalHouseAI та WoodenHouseAI з алгоритмами переміщення Варіанту 7.
 * 3. Синхронізація фонових потоків та малювання виключно в EDT.
 * 4. Кнопки «Пауза» та «Продовжити» для кожного виду об'єктів через wait() / notifyAll().
 * 5. Випадаючі списки (JComboBox) для динамічної зміни пріоритету потоків.
 */
public class Task2Simulation extends JFrame {

    private static final double SPEED_V = 2.5;
    private static final int HOUSE_SIZE = 30;

    private final List<House> houses = new ArrayList<>();
    private final Object collectionLock = new Object();

    private final SimulationPanel simulationPanel;
    private final CapitalHouseAI capitalAI;
    private final WoodenHouseAI woodenAI;
    private final Random random = new Random();

    public Task2Simulation() {
        super("Лабораторна робота №7 — Завдання 2 (Варіант 7)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 660);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        simulationPanel = new SimulationPanel(houses, collectionLock);
        add(simulationPanel, BorderLayout.CENTER);

        capitalAI = new CapitalHouseAI(houses, collectionLock, SPEED_V, 920, 540, simulationPanel::repaint);
        woodenAI = new WoodenHouseAI(houses, collectionLock, SPEED_V, 920, 540, simulationPanel::repaint);

        simulationPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = simulationPanel.getWidth();
                int h = simulationPanel.getHeight();
                capitalAI.updateAreaSize(w, h);
                woodenAI.updateAreaSize(w, h);
            }
        });

        add(createControlPanel(), BorderLayout.SOUTH);

        // Початкові об'єкти
        for (int i = 0; i < 6; i++) {
            addRandomHouse(true);
            addRandomHouse(false);
        }

        capitalAI.start();
        woodenAI.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                capitalAI.stopThread();
                woodenAI.stopThread();
            }
        });
    }

    private JPanel createControlPanel() {
        JPanel mainControl = new JPanel(new GridLayout(2, 1, 5, 5));
        mainControl.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Панель керування потоком капітальних будинків
        JPanel capitalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        capitalPanel.setBorder(BorderFactory.createTitledBorder("Капітальні будинки (CapitalHouseAI)"));

        JButton addCapBtn = new JButton("Додати капітальний");
        JButton pauseCapBtn = new JButton("Пауза");
        JButton resumeCapBtn = new JButton("Продовжити");
        resumeCapBtn.setEnabled(false);

        addCapBtn.addActionListener(e -> addRandomHouse(true));
        pauseCapBtn.addActionListener(e -> {
            capitalAI.pauseThread();
            pauseCapBtn.setEnabled(false);
            resumeCapBtn.setEnabled(true);
        });
        resumeCapBtn.addActionListener(e -> {
            capitalAI.resumeThread();
            resumeCapBtn.setEnabled(false);
            pauseCapBtn.setEnabled(true);
        });

        capitalPanel.add(addCapBtn);
        capitalPanel.add(pauseCapBtn);
        capitalPanel.add(resumeCapBtn);
        capitalPanel.add(new JLabel("Пріоритет потоку:"));
        capitalPanel.add(createPriorityComboBox(capitalAI));

        // Панель керування потоком дерев'яних будинків
        JPanel woodenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        woodenPanel.setBorder(BorderFactory.createTitledBorder("Дерев'яні будинки (WoodenHouseAI)"));

        JButton addWoodBtn = new JButton("Додати дерев'яний");
        JButton pauseWoodBtn = new JButton("Пауза");
        JButton resumeWoodBtn = new JButton("Продовжити");
        resumeWoodBtn.setEnabled(false);

        addWoodBtn.addActionListener(e -> addRandomHouse(false));
        pauseWoodBtn.addActionListener(e -> {
            woodenAI.pauseThread();
            pauseWoodBtn.setEnabled(false);
            resumeWoodBtn.setEnabled(true);
        });
        resumeWoodBtn.addActionListener(e -> {
            woodenAI.resumeThread();
            resumeWoodBtn.setEnabled(false);
            pauseWoodBtn.setEnabled(true);
        });

        woodenPanel.add(addWoodBtn);
        woodenPanel.add(pauseWoodBtn);
        woodenPanel.add(resumeWoodBtn);
        woodenPanel.add(new JLabel("Пріоритет потоку:"));
        woodenPanel.add(createPriorityComboBox(woodenAI));

        mainControl.add(capitalPanel);
        mainControl.add(woodenPanel);
        return mainControl;
    }

    private JComboBox<String> createPriorityComboBox(Thread thread) {
        String[] options = {"MIN_PRIORITY (1)", "NORM_PRIORITY (5)", "MAX_PRIORITY (10)"};
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setSelectedIndex(1);

        comboBox.addActionListener(e -> {
            switch (comboBox.getSelectedIndex()) {
                case 0 -> thread.setPriority(Thread.MIN_PRIORITY);
                case 1 -> thread.setPriority(Thread.NORM_PRIORITY);
                case 2 -> thread.setPriority(Thread.MAX_PRIORITY);
            }
        });
        return comboBox;
    }

    private void addRandomHouse(boolean isCapital) {
        int w = Math.max(200, simulationPanel.getWidth() > 0 ? simulationPanel.getWidth() : 920);
        int h = Math.max(200, simulationPanel.getHeight() > 0 ? simulationPanel.getHeight() : 540);
        double x = random.nextInt(w - HOUSE_SIZE);
        double y = random.nextInt(h - HOUSE_SIZE);

        synchronized (collectionLock) {
            if (isCapital) {
                houses.add(new CapitalHouse(x, y));
            } else {
                houses.add(new WoodenHouse(x, y));
            }
        }
        simulationPanel.repaint();
    }

    // ==================== ІЄРАРХІЯ ОБ'ЄКТІВ ====================
    public abstract static class House {
        protected double x;
        protected double y;
        protected double targetX;
        protected double targetY;
        protected boolean targetInitialized = false;
        protected boolean moving = true;

        public House(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public abstract void draw(Graphics g);
    }

    public static class CapitalHouse extends House {
        public CapitalHouse(double x, double y) {
            super(x, y);
        }

        @Override
        public void draw(Graphics g) {
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

    public static class WoodenHouse extends House {
        public WoodenHouse(double x, double y) {
            super(x, y);
        }

        @Override
        public void draw(Graphics g) {
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

    // ==================== АБСТРАКТНИЙ ПОТІК BaseAI ====================
    public abstract static class BaseAI extends Thread {
        protected final List<House> houses;
        protected final Object collectionLock;
        protected final Runnable repaintCallback;
        protected final double speed;
        protected volatile int areaWidth;
        protected volatile int areaHeight;

        private volatile boolean running = true;
        private volatile boolean paused = false;
        private final Object pauseLock = new Object();

        public BaseAI(String name, List<House> houses, Object collectionLock,
                      double speed, int areaWidth, int areaHeight, Runnable repaintCallback) {
            super(name);
            this.houses = houses;
            this.collectionLock = collectionLock;
            this.speed = speed;
            this.areaWidth = areaWidth;
            this.areaHeight = areaHeight;
            this.repaintCallback = repaintCallback;
        }

        public void updateAreaSize(int width, int height) {
            this.areaWidth = width;
            this.areaHeight = height;
        }

        // Призупинення потоку через прапорець та монітор pauseLock
        public void pauseThread() {
            paused = true;
        }

        // Відновлення потоку через notifyAll()
        public void resumeThread() {
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notifyAll();
            }
        }

        public void stopThread() {
            running = false;
            resumeThread();
            interrupt();
        }

        @Override
        public void run() {
            while (running) {
                synchronized (pauseLock) {
                    while (paused && running) {
                        try {
                            pauseLock.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                if (!running) break;

                synchronized (collectionLock) {
                    updatePositions();
                }

                if (repaintCallback != null) {
                    repaintCallback.run();
                }

                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        protected void moveTowardsTarget(House house) {
            double dx = house.targetX - house.x;
            double dy = house.targetY - house.y;
            double distance = Math.hypot(dx, dy);

            if (distance <= speed) {
                house.x = house.targetX;
                house.y = house.targetY;
                house.moving = false;
            } else {
                house.x += (dx / distance) * speed;
                house.y += (dy / distance) * speed;
            }
        }

        protected abstract void updatePositions();
    }

    // ==================== СПАДКОЄМЕЦЬ 1: CapitalHouseAI ====================
    public static class CapitalHouseAI extends BaseAI {
        private final Random random = new Random();

        public CapitalHouseAI(List<House> houses, Object collectionLock,
                              double speed, int width, int height, Runnable repaintCallback) {
            super("CapitalHouseAI-Thread", houses, collectionLock, speed, width, height, repaintCallback);
        }

        @Override
        protected void updatePositions() {
            if (areaWidth <= 0 || areaHeight <= 0) return;
            double halfW = areaWidth / 2.0;
            double halfH = areaHeight / 2.0;

            for (House house : houses) {
                if (!(house instanceof CapitalHouse) || !house.moving) continue;

                if (!house.targetInitialized) {
                    // Перевірка потрапляння у ліву верхню чверть [0, 0] розміром w/2 x h/2
                    if (house.x >= 0 && house.x <= halfW - HOUSE_SIZE &&
                            house.y >= 0 && house.y <= halfH - HOUSE_SIZE) {
                        house.moving = false;
                        continue;
                    }
                    house.targetX = random.nextDouble() * Math.max(1, halfW - HOUSE_SIZE);
                    house.targetY = random.nextDouble() * Math.max(1, halfH - HOUSE_SIZE);
                    house.targetInitialized = true;
                }

                moveTowardsTarget(house);
            }
        }
    }

    // ==================== СПАДКОЄМЕЦЬ 2: WoodenHouseAI ====================
    public static class WoodenHouseAI extends BaseAI {
        private final Random random = new Random();

        public WoodenHouseAI(List<House> houses, Object collectionLock,
                             double speed, int width, int height, Runnable repaintCallback) {
            super("WoodenHouseAI-Thread", houses, collectionLock, speed, width, height, repaintCallback);
        }

        @Override
        protected void updatePositions() {
            if (areaWidth <= 0 || areaHeight <= 0) return;
            double halfW = areaWidth / 2.0;
            double halfH = areaHeight / 2.0;

            for (House house : houses) {
                if (!(house instanceof WoodenHouse) || !house.moving) continue;

                if (!house.targetInitialized) {
                    // Перевірка потрапляння у праву нижню чверть [w/2, h/2] розміром w/2 x h/2
                    if (house.x >= halfW && house.x <= areaWidth - HOUSE_SIZE &&
                            house.y >= halfH && house.y <= areaHeight - HOUSE_SIZE) {
                        house.moving = false;
                        continue;
                    }
                    house.targetX = halfW + random.nextDouble() * Math.max(1, halfW - HOUSE_SIZE);
                    house.targetY = halfH + random.nextDouble() * Math.max(1, halfH - HOUSE_SIZE);
                    house.targetInitialized = true;
                }

                moveTowardsTarget(house);
            }
        }
    }

    // ==================== ПАНЕЛЬ ВІЗУАЛІЗАЦІЇ (EDT) ====================
    public static class SimulationPanel extends JPanel {
        private final List<House> houses;
        private final Object collectionLock;

        public SimulationPanel(List<House> houses, Object collectionLock) {
            this.houses = houses;
            this.collectionLock = collectionLock;
            setBackground(new Color(238, 245, 238));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

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

            synchronized (collectionLock) {
                for (House house : houses) {
                    house.draw(g2);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Task2Simulation().setVisible(true));
    }
}