package evacuation.ui;

import evacuation.agent.Agent;
import evacuation.simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class MainFrame extends JFrame {

    private SimulationEngine engine;
    private GraphRenderer    renderer;
    private JButton   btnInitialiser, btnTick, btnLancer;
    private JSlider   sliderVitesse;
    private JLabel    lblVitesse;
    private Timer     autoTimer;
    private JLabel    lblStatsDeplacement, lblStatsPsycho, lblStatsTick, lblAgentInfo;
    private JTextArea logArea;

    public MainFrame(SimulationEngine engine) {
        this.engine   = engine;
        this.renderer = new GraphRenderer(engine);
        renderer.setOnAgentSelectionne(this::onAgentSelectionne);
        setTitle("Simulation d'evacuation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));
        add(renderer,          BorderLayout.CENTER);
        add(creerPanneauEst(), BorderLayout.EAST);
        add(creerPanneauSud(), BorderLayout.SOUTH);
        creerMenuBar();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        redirectConsoleToLog();
    }

    private JPanel creerPanneauEst() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(210, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));
        panel.add(creerSectionControles());
        panel.add(Box.createVerticalStrut(10));
        panel.add(creerSectionVitesse());
        panel.add(Box.createVerticalStrut(10));
        panel.add(creerSectionStatistiques());
        panel.add(Box.createVerticalStrut(10));
        panel.add(creerSectionAgentInfo());
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel creerSectionControles() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Controles"));
        btnInitialiser = new JButton("Initialiser");
        btnTick        = new JButton("Tick +1");
        btnLancer      = new JButton("Lancer auto");
        btnTick.setEnabled(false);
        btnLancer.setEnabled(false);
        btnInitialiser.addActionListener(this::onInitialiser);
        btnTick.addActionListener(this::onTick);
        btnLancer.addActionListener(this::onLancer);
        autoTimer = new Timer(800, e -> effectuerTick());
        for (JButton btn : new JButton[]{btnInitialiser, btnTick, btnLancer}) {
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(190, 30));
            p.add(Box.createVerticalStrut(4));
            p.add(btn);
        }
        p.add(Box.createVerticalStrut(4));
        return p;
    }

    private JPanel creerSectionVitesse() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Vitesse"));
        sliderVitesse = new JSlider(50, 2000, 800);
        sliderVitesse.setInverted(true);
        sliderVitesse.setMajorTickSpacing(500);
        sliderVitesse.setPaintTicks(true);
        sliderVitesse.setMaximumSize(new Dimension(190, 50));
        sliderVitesse.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblVitesse = new JLabel("800 ms / tick");
        lblVitesse.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblVitesse.setFont(new Font("Arial", Font.PLAIN, 11));
        sliderVitesse.addChangeListener(e -> {
            int val = sliderVitesse.getValue();
            lblVitesse.setText(val + " ms / tick");
            autoTimer.setDelay(val);
        });
        p.add(sliderVitesse);
        p.add(lblVitesse);
        return p;
    }

    private JPanel creerSectionStatistiques() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Statistiques"));
        lblStatsTick        = new JLabel("Tick : 0");
        lblStatsDeplacement = new JLabel("<html>-</html>");
        lblStatsPsycho      = new JLabel("<html>-</html>");
        for (JLabel lbl : new JLabel[]{lblStatsTick, lblStatsDeplacement, lblStatsPsycho}) {
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setFont(new Font("Arial", Font.PLAIN, 11));
            p.add(lbl); p.add(Box.createVerticalStrut(4));
        }
        return p;
    }

    private JPanel creerSectionAgentInfo() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Agent selectionne"));
        lblAgentInfo = new JLabel("<html><i>Cliquez sur un agent</i></html>");
        lblAgentInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAgentInfo.setFont(new Font("Arial", Font.PLAIN, 11));
        p.add(lblAgentInfo);
        return p;
    }

    private JScrollPane creerPanneauSud() {
        logArea = new JTextArea(5, 60);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Journal"));
        return scroll;
    }

    private void creerMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFichier = new JMenu("Fichier");
        JMenuItem itemSauvegarder = new JMenuItem("Sauvegarder...");
        JMenuItem itemCharger     = new JMenuItem("Charger...");
        itemSauvegarder.addActionListener(e -> sauvegarderSimulation());
        itemCharger.addActionListener(e -> chargerSimulation());
        menuFichier.add(itemSauvegarder);
        menuFichier.add(itemCharger);
        menuBar.add(menuFichier);
        setJMenuBar(menuBar);
    }

    private void onInitialiser(ActionEvent e) {
        engine.initialiser(); renderer.rafraichir(); majStatistiques();
        btnTick.setEnabled(true); btnLancer.setEnabled(true); btnInitialiser.setEnabled(false);
    }

    private void onTick(ActionEvent e) { effectuerTick(); }

    private void onLancer(ActionEvent e) {
        if (autoTimer.isRunning()) { autoTimer.stop(); btnLancer.setText("Lancer auto"); }
        else { autoTimer.setDelay(sliderVitesse.getValue()); autoTimer.start(); btnLancer.setText("Pause"); }
    }

    private void effectuerTick() {
        if (!engine.isEnCours()) return;
        engine.tick(); renderer.rafraichir(); majStatistiques();
        if (!engine.isEnCours()) {
            autoTimer.stop(); btnLancer.setText("Lancer auto");
            btnTick.setEnabled(false); btnLancer.setEnabled(false);
        }
    }

    private void onAgentSelectionne(Agent agent) {
        if (agent == null) { lblAgentInfo.setText("<html><i>Aucun agent</i></html>"); return; }
        String dest = agent.getDestination() != null ? agent.getDestination().getNom() : "aucune";
        int restant = Math.max(0, agent.getChemin().size() - agent.getChemin().indexOf(agent.getPosition()) - 1);
        lblAgentInfo.setText("<html><b>" + agent.getId() + "</b><br>"
                + "Pos : " + agent.getPosition().getNom() + "<br>"
                + "Dest : " + dest + "<br>"
                + "Etapes : " + restant + "<br>"
                + "Vitesse : " + agent.getVitesse() + "<br>"
                + "Psycho : " + agent.getEtatPsychologique() + "<br>"
                + "Etat : " + agent.getEtatDeplacement() + "</html>");
    }

    private void majStatistiques() {
        int total = engine.getAgents().size();
        int[] d = engine.getStatsDeplacement();
        int[] p = engine.getStatsPsychologiques();
        lblStatsTick.setText("Tick : " + engine.getTick());
        lblStatsDeplacement.setText(String.format(
                "<html>Deplacement (%d) :<br>&nbsp;Mvt:%d Arr:%d Blq:%d</html>", total, d[1], d[2], d[3]));
        lblStatsPsycho.setText(String.format(
                "<html>Psycho :<br>&nbsp;C:%d P:%d F:%d</html>", p[0], p[1], p[2]));
    }

    private void sauvegarderSimulation() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Simulation (*.sim)", "sim"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".sim")) f = new File(f.getPath() + ".sim");
            try { engine.sauvegarder(f); JOptionPane.showMessageDialog(this, "Sauvegarde OK."); }
            catch (IOException ex) { JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void chargerSimulation() {
        if (autoTimer.isRunning()) autoTimer.stop();
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Simulation (*.sim)", "sim"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                engine = SimulationEngine.charger(fc.getSelectedFile());
                renderer = new GraphRenderer(engine);
                renderer.setOnAgentSelectionne(this::onAgentSelectionne);
                getContentPane().removeAll();
                setLayout(new BorderLayout(6, 6));
                add(renderer, BorderLayout.CENTER);
                add(creerPanneauEst(), BorderLayout.EAST);
                add(creerPanneauSud(), BorderLayout.SOUTH);
                creerMenuBar(); revalidate(); repaint();
                redirectConsoleToLog();
                btnTick.setEnabled(true); btnLancer.setEnabled(true); majStatistiques();
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void redirectConsoleToLog() {
        java.io.PrintStream ps = new java.io.PrintStream(System.out) {
            @Override public void println(String x) {
                super.println(x);
                SwingUtilities.invokeLater(() -> {
                    if (logArea != null) { logArea.append(x + "\n"); logArea.setCaretPosition(logArea.getDocument().getLength()); }
                });
            }
        };
        System.setOut(ps);
    }
}
