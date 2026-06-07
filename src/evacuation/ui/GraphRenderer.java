package evacuation.ui;

import evacuation.agent.Agent;
import evacuation.graph.Edge;
import evacuation.graph.Graph;
import evacuation.graph.Node;
import evacuation.graph.Sortie;
import evacuation.graph.ZoneDanger;
import evacuation.simulation.SimulationEngine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class GraphRenderer extends JPanel {

    private static final int NODE_RADIUS = 20;
    private static final int SCALE       = 80;
    private static final int OFFSET      = 50;

    private SimulationEngine engine;
    private Agent agentSelectionne;
    private java.util.function.Consumer<Agent> onAgentSelectionne;

    public GraphRenderer(SimulationEngine engine) {
        this.engine = engine;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(800, 600));
        ajouterGestionSouris();
    }

    public void setOnAgentSelectionne(java.util.function.Consumer<Agent> callback) {
        this.onAgentSelectionne = callback;
    }

    public void rafraichir() { repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] densite = engine.getDensiteParNoeud();
        dessinerAretes(g2);
        dessinerCheminSelectionne(g2);
        dessinerNoeuds(g2, densite);
        dessinerAgents(g2);
        dessinerLegende(g2);
    }

    private void dessinerAretes(Graphics2D g) {
        g.setStroke(new BasicStroke(2));
        for (Edge edge : engine.getGraph().getEdges()) {
            Node src = edge.getSource(), dest = edge.getDestination();
            int x1 = src.getX() * SCALE + OFFSET, y1 = src.getY() * SCALE + OFFSET;
            int x2 = dest.getX() * SCALE + OFFSET, y2 = dest.getY() * SCALE + OFFSET;
            g.setColor(edge.isDisponible() ? Color.GRAY : Color.RED);
            g.drawLine(x1, y1, x2, y2);
            if (edge.isDisponible()) {
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("Arial", Font.PLAIN, 11));
                g.drawString(String.valueOf((int) edge.getPoids()), (x1+x2)/2+4, (y1+y2)/2-4);
            }
        }
    }

    private void dessinerCheminSelectionne(Graphics2D g) {
        if (agentSelectionne == null) return;
        List<Node> chemin = agentSelectionne.getChemin();
        if (chemin == null || chemin.size() < 2) return;
        g.setColor(new Color(0, 200, 255, 180));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int debut = chemin.indexOf(agentSelectionne.getPosition());
        if (debut < 0) debut = 0;
        for (int i = debut; i < chemin.size() - 1; i++) {
            Node a = chemin.get(i), b = chemin.get(i+1);
            g.drawLine(a.getX()*SCALE+OFFSET, a.getY()*SCALE+OFFSET,
                       b.getX()*SCALE+OFFSET, b.getY()*SCALE+OFFSET);
        }
        g.setStroke(new BasicStroke(2));
    }

    private void dessinerNoeuds(Graphics2D g, int[] densite) {
        Graph graph = engine.getGraph();
        int maxDensite = 1;
        for (int d : densite) maxDensite = Math.max(maxDensite, d);
        for (int i = 0; i < graph.getNodes().size(); i++) {
            Node node = graph.getNodes().get(i);
            int x = node.getX() * SCALE + OFFSET, y = node.getY() * SCALE + OFFSET;
            Color couleurBase;
            if (node.isBloque())                 couleurBase = Color.BLACK;
            else if (node instanceof ZoneDanger) couleurBase = Color.ORANGE;
            else if (node instanceof Sortie)     couleurBase = ((Sortie)node).isEstOuverte() ? Color.GREEN : Color.LIGHT_GRAY;
            else                                 couleurBase = new Color(100, 149, 237);
            Color couleurFinale = couleurBase;
            if (!node.isBloque() && densite[i] > 0) {
                float ratio = Math.min(1.0f, (float)densite[i] / maxDensite);
                couleurFinale = melanger(couleurBase, new Color(220,30,30), ratio * 0.6f);
            }
            g.setColor(couleurFinale);
            g.fillOval(x-NODE_RADIUS, y-NODE_RADIUS, NODE_RADIUS*2, NODE_RADIUS*2);
            boolean estPosAgent = agentSelectionne != null && agentSelectionne.getPosition().equals(node);
            g.setColor(estPosAgent ? new Color(0,200,255) : Color.BLACK);
            g.setStroke(new BasicStroke(estPosAgent ? 3f : 1.5f));
            g.drawOval(x-NODE_RADIUS, y-NODE_RADIUS, NODE_RADIUS*2, NODE_RADIUS*2);
            g.setColor(node.isBloque() ? Color.WHITE : Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(node.getNom(), x - fm.stringWidth(node.getNom())/2, y+4);
            if (densite[i] > 0) {
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 10));
                g.drawString("x"+densite[i], x+NODE_RADIUS-4, y-NODE_RADIUS+10);
            }
        }
    }

    private Color melanger(Color a, Color b, float ratio) {
        int r  = (int)(a.getRed()*(1-ratio)   + b.getRed()*ratio);
        int v  = (int)(a.getGreen()*(1-ratio) + b.getGreen()*ratio);
        int bl = (int)(a.getBlue()*(1-ratio)  + b.getBlue()*ratio);
        return new Color(Math.min(255,r), Math.min(255,v), Math.min(255,bl));
    }

    private void dessinerAgents(Graphics2D g) {
        List<Agent> agents = engine.getAgents();
        for (Agent agent : agents) {
            if (agent.getEtatDeplacement() == Agent.EtatDeplacement.ARRIVE) continue;
            Node pos = agent.getPosition();
            int x = pos.getX()*SCALE+OFFSET, y = pos.getY()*SCALE+OFFSET;
            int decalage = agents.indexOf(agent) % 4;
            int dx = (decalage%2==0?1:-1)*(decalage/2)*10;
            int dy = (decalage<2)?-10:10;
            Color couleur = switch (agent.getEtatPsychologique()) {
                case PANIQUE -> new Color(255,140,0);
                case FOLIE   -> Color.MAGENTA;
                default      -> switch (agent.getEtatDeplacement()) {
                    case EN_MOUVEMENT -> Color.YELLOW;
                    case BLOQUE       -> Color.RED;
                    default           -> Color.WHITE;
                };
            };
            boolean sel = agent.equals(agentSelectionne);
            int r = sel ? 11 : 8;
            g.setColor(couleur);
            g.fillOval(x+dx-r, y+dy-r, r*2, r*2);
            g.setColor(sel ? new Color(0,200,255) : Color.BLACK);
            g.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g.drawOval(x+dx-r, y+dy-r, r*2, r*2);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString(agent.getId(), x+dx+r+2, y+dy-r+10);
        }
    }

    private void dessinerLegende(Graphics2D g) {
        int x = 10, y = getHeight() - 150;
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.setColor(Color.BLACK);
        g.drawString("Legende :", x, y);
        dessinerEntree(g, x, y+18,  new Color(100,149,237), "Noeud normal");
        dessinerEntree(g, x, y+36,  Color.GREEN,             "Sortie ouverte");
        dessinerEntree(g, x, y+54,  Color.LIGHT_GRAY,        "Sortie fermee");
        dessinerEntree(g, x, y+72,  Color.ORANGE,            "Zone danger / Panique");
        dessinerEntree(g, x, y+90,  Color.BLACK,             "Noeud bloque");
        dessinerEntree(g, x, y+108, Color.YELLOW,            "Agent en mouvement");
        dessinerEntree(g, x, y+126, Color.MAGENTA,           "Agent en folie");
        dessinerEntree(g, x, y+144, new Color(0,200,255),    "Trajet selectionne");
    }

    private void dessinerEntree(Graphics2D g, int x, int y, Color c, String label) {
        g.setColor(c); g.fillRect(x, y-10, 14, 14);
        g.setColor(Color.BLACK); g.drawRect(x, y-10, 14, 14);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString(label, x+20, y);
    }

    private void ajouterGestionSouris() {
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Agent trouve = trouverAgentAuClic(e.getX(), e.getY());
                agentSelectionne = (trouve != null && trouve.equals(agentSelectionne)) ? null : trouve;
                repaint();
                if (onAgentSelectionne != null) onAgentSelectionne.accept(agentSelectionne);
            }
        });
    }

    private Agent trouverAgentAuClic(int mx, int my) {
        for (Agent agent : engine.getAgents()) {
            if (agent.getEtatDeplacement() == Agent.EtatDeplacement.ARRIVE) continue;
            Node pos = agent.getPosition();
            int ax = pos.getX()*SCALE+OFFSET, ay = pos.getY()*SCALE+OFFSET;
            if (Math.hypot(mx-ax, my-ay) <= 15) return agent;
        }
        return null;
    }
}
