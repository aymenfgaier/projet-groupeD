package evacuation.ui;

import evacuation.agent.Agent;
import evacuation.simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * Fenêtre principale de l'application de simulation d'évacuation.
 * Contient le rendu du graphe, les contrôles de simulation,
 * un panneau de statistiques live et un journal de messages.
 */
public class MainFrame extends JFrame {

    private SimulationEngine engine;
    private GraphRenderer    renderer;

    // Contrôles
    private JButton   btnInitialiser;
    private JButton   btnTick;
    private JButton   btnLancer;
    private JSlider   sliderVitesse;
    private JLabel    lblVitesse;
    private Timer     autoTimer;

    // Statistiques
    private JLabel lblStatsDeplacement;
    private JLabel lblStatsPsycho;
    private JLabel lblStatsTick;
    private JLabel lblAgentInfo;

    // Journal
    private JTextArea logArea;

    public MainFrame(SimulationEngine engine) {
        this.engine   = engine;
        this.renderer = new GraphRenderer(engine);

        // Callback sélection agent
        renderer.setOnAgentSelectionne(this::onAgentSelectionne);

        setTitle("Simulation d'évacuation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));

        add(renderer,             BorderLayout.CENTER);
        add(creerPanneauEst(),    BorderLayout.EAST);
        add(creerPanneauSud(),    BorderLayout.SOUTH);
        creerMenuBar();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        redirectConsoleToLog();
    }

    // -------------------------------------------------------------------------
    // Construction de l'interface
    // -------------------------------------------------------------------------

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
        p.setBorder(BorderFactory.createTitledBorder("Contrôles"));

        btnInitialiser = new JButton("Initialiser");
        btnTick        = new JButton("Tick +1");
        btnLancer      = new JButton("▶ Lancer auto");

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
        p.setBorder(BorderFactory.createTitledBorder("Vitesse de simulation"));

        // Slider : 50ms (rapide) à 2000ms (lent), valeur initiale 800ms
        sliderVitesse = new JSlider(50, 2000, 800);
        sliderVitesse.setInverted(true); // gauche = rapide, droite = lent
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
        lblStatsDeplacement = new JLabel("<html>—</html>");
        lblStatsPsycho      = new JLabel("<html>—</html>");

        for (JLabel lbl : new JLabel[]{lblStatsTick, lblStatsDeplacement, lblStatsPsycho}) {
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            lbl.setFont(new Font("Arial", Font.PLAIN, 11));
            p.add(lbl);
            p.add(Box.createVerticalStrut(4));
        }
        return p;
    }

    private JPanel creerSectionAgentInfo() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Agent sélectionné"));

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
        JMenuItem itemSauvegarder = new JMenuItem("Sauvegarder la simulation…");
        JMenuItem itemCharger     = new JMenuItem("Charger une simulation…");

        itemSauvegarder.addActionListener(e -> sauvegarderSimulation());
        itemCharger.addActionListener(e -> chargerSimulation());

        menuFichier.add(itemSauvegarder);
        menuFichier.add(itemCharger);
        menuBar.add(menuFichier);

        JMenu menuAgents = new JMenu("Agents");
        JMenuItem itemSupprimer = new JMenuItem("Supprimer l'agent sélectionné");
        itemSupprimer.addActionListener(e -> supprimerAgentSelectionne());
        menuAgents.add(itemSupprimer);
        menuBar.add(menuAgents);

        setJMenuBar(menuBar);
    }

    // -------------------------------------------------------------------------
    // Actions des contrôles
    // -------------------------------------------------------------------------

    private void onInitialiser(ActionEvent e) {
        engine.initialiser();
        renderer.rafraichir();
        majStatistiques();
        btnTick.setEnabled(true);
        btnLancer.setEnabled(true);
        btnInitialiser.setEnabled(false);
    }

    private void onTick(ActionEvent e) {
        effectuerTick();
    }

    private void onLancer(ActionEvent e) {
        if (autoTimer.isRunning()) {
            autoTimer.stop();
            btnLancer.setText("▶ Lancer auto");
        } else {
            autoTimer.setDelay(sliderVitesse.getValue());
            autoTimer.start();
            btnLancer.setText("⏸ Pause");
        }
    }

    private void effectuerTick() {
        if (!engine.isEnCours()) return;
        engine.tick();
        renderer.rafraichir();
        majStatistiques();
        if (!engine.isEnCours()) {
            autoTimer.stop();
            btnLancer.setText("▶ Lancer auto");
            btnTick.setEnabled(false);
            btnLancer.setEnabled(false);
        }
    }

    /** Appelé quand l'utilisateur clique sur un agent dans le rendu. */
    private void onAgentSelectionne(Agent agent) {
        if (agent == null) {
            lblAgentInfo.setText("<html><i>Aucun agent sélectionné</i></html>");
            return;
        }
        String dest = agent.getDestination() != null ? agent.getDestination().getNom() : "aucune";
        int    restant = Math.max(0, agent.getChemin().size()
                - agent.getChemin().indexOf(agent.getPosition()) - 1);
        lblAgentInfo.setText("<html>"
                + "<b>" + agent.getId() + "</b><br>"
                + "Pos : " + agent.getPosition().getNom() + "<br>"
                + "Dest : " + dest + "<br>"
                + "Étapes restantes : " + restant + "<br>"
                + "Vitesse : " + agent.getVitesse() + "<br>"
                + "Psycho : " + agent.getEtatPsychologique() + "<br>"
                + "Comportement : " + agent.getComportement() + "<br>"
                + "État : " + agent.getEtatDeplacement()
                + "</html>");
    }

    private void supprimerAgentSelectionne() {
        // Récupère l'agent sélectionné depuis le renderer via le dernier clic
        // (le renderer expose l'agent via le callback, on passe par le label)
        // Alternative simple : bouton disponible uniquement si un agent est connu
        JOptionPane.showMessageDialog(this,
                "Sélectionnez un agent en cliquant dessus, puis utilisez ce menu.",
                "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Statistiques
    // -------------------------------------------------------------------------

    private void majStatistiques() {
        int total = engine.getAgents().size();
        int[] d   = engine.getStatsDeplacement();
        int[] p   = engine.getStatsPsychologiques();

        lblStatsTick.setText("Tick : " + engine.getTick());

        lblStatsDeplacement.setText(String.format(
                "<html>Déplacement (%d agents) :<br>"
                + "&nbsp;▶ En mvt : %d (%.0f%%)<br>"
                + "&nbsp;✔ Arrivés : %d (%.0f%%)<br>"
                + "&nbsp;✖ Bloqués : %d (%.0f%%)</html>",
                total,
                d[1], pct(d[1], total),
                d[2], pct(d[2], total),
                d[3], pct(d[3], total)));

        lblStatsPsycho.setText(String.format(
                "<html>Psychologique :<br>"
                + "&nbsp;Calme : %d &nbsp; Panique : %d &nbsp; Folie : %d</html>",
                p[0], p[1], p[2]));
    }

    private double pct(int val, int total) {
        return total == 0 ? 0 : (val * 100.0 / total);
    }

    // -------------------------------------------------------------------------
    // Import / Export
    // -------------------------------------------------------------------------

    private void sauvegarderSimulation() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Sauvegarder la simulation");
        fc.setFileFilter(new FileNameExtensionFilter("Simulation (*.sim)", "sim"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith(".sim")) f = new File(f.getPath() + ".sim");
            try {
                engine.sauvegarder(f);
                JOptionPane.showMessageDialog(this, "Simulation sauvegardée :\n" + f.getPath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(),
                        "Erreur de sauvegarde", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void chargerSimulation() {
        if (autoTimer.isRunning()) autoTimer.stop();

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Charger une simulation");
        fc.setFileFilter(new FileNameExtensionFilter("Simulation (*.sim)", "sim"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                engine = SimulationEngine.charger(fc.getSelectedFile());
                renderer = new GraphRenderer(engine);
                renderer.setOnAgentSelectionne(this::onAgentSelectionne);

                // Remplace le composant central
                getContentPane().removeAll();
                setLayout(new BorderLayout(6, 6));
                add(renderer,          BorderLayout.CENTER);
                add(creerPanneauEst(), BorderLayout.EAST);
                add(creerPanneauSud(), BorderLayout.SOUTH);
                creerMenuBar();
                revalidate();
                repaint();
                redirectConsoleToLog();

                btnTick.setEnabled(true);
                btnLancer.setEnabled(true);
                majStatistiques();
                JOptionPane.showMessageDialog(this, "Simulation chargée avec succès.");
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(),
                        "Erreur de chargement", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Redirection console → journal
    // -------------------------------------------------------------------------

    private void redirectConsoleToLog() {
        java.io.PrintStream ps = new java.io.PrintStream(System.out) {
            @Override
            public void println(String x) {
                super.println(x);
                SwingUtilities.invokeLater(() -> {
                    if (logArea != null) {
                        logArea.append(x + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    }
                });
            }
        };
        System.setOut(ps);
    }
}
