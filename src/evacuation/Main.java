package evacuation;

import evacuation.agent.Agent;
import evacuation.graph.Edge;
import evacuation.graph.Graph;
import evacuation.graph.Node;
import evacuation.graph.Sortie;
import evacuation.graph.ZoneDanger;
import evacuation.simulation.SimulationEngine;
import evacuation.ui.MainFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("cli")) {
            lancerModeCLI();
        } else {
            lancerModeGraphique();
        }
    }

    private static void lancerModeGraphique() {
        SimulationEngine engine = creerSimulationExemple();
        SwingUtilities.invokeLater(() -> new MainFrame(engine));
    }

    private static void lancerModeCLI() {
        System.out.println("=== Mode CLI ===\n");
        creerSimulationExemple().lancerSimulation();
    }

    private static SimulationEngine creerSimulationExemple() {
        Graph graph = new Graph();

        Node a = new Node("A", "Salle A",   1, 1);
        Node b = new Node("B", "Couloir B", 3, 1);
        Node c = new Node("C", "Salle C",   5, 1);
        Node d = new Node("D", "Couloir D", 3, 3);
        Sortie s1     = new Sortie("S1", "Sortie Nord", 1, 5, 10);
        Sortie s2     = new Sortie("S2", "Sortie Est",  7, 2, 10);
        ZoneDanger z1 = new ZoneDanger("Z1", "Incendie", 3, 5, "Feu", 8);

        for (Node n : new Node[]{a, b, c, d, s1, s2, z1}) graph.ajouterNode(n);

        graph.ajouterEdge(new Edge(a, b, 2));
        graph.ajouterEdge(new Edge(b, c, 2));
        graph.ajouterEdge(new Edge(b, d, 2));
        graph.ajouterEdge(new Edge(d, s1, 3));
        graph.ajouterEdge(new Edge(c, s2, 2));
        graph.ajouterEdge(new Edge(a, d, 4));
        graph.ajouterEdge(new Edge(d, z1, 1));

        SimulationEngine engine = new SimulationEngine(graph);
        engine.ajouterAgent(new Agent("AG1", a, s1, graph, 1, 1.0,
                Agent.Comportement.LAISSE_PASSER, Agent.EtatPsychologique.CALME, Agent.ModeDestination.FIXE));
        engine.ajouterAgent(new Agent("AG2", c, s1, graph, 2, 0.3,
                Agent.Comportement.PRIORITAIRE, Agent.EtatPsychologique.PANIQUE, Agent.ModeDestination.FIXE));
        engine.ajouterAgent(new Agent("AG3", b, s2, graph, 1, 0.8,
                Agent.Comportement.LAISSE_PASSER, Agent.EtatPsychologique.CALME, Agent.ModeDestination.FIXE));
        return engine;
    }
}
