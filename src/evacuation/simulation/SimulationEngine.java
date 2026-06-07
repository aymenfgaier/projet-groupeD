package evacuation.simulation;

import evacuation.agent.Agent;
import evacuation.graph.Graph;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SimulationEngine implements Serializable {

    private static final long serialVersionUID = 1L;

    private Graph graph;
    private List<Agent> agents;
    private int tick;
    private boolean enCours;

    public SimulationEngine(Graph graph) {
        this.graph   = graph;
        this.agents  = new ArrayList<>();
        this.tick    = 0;
        this.enCours = false;
    }

    public void ajouterAgent(Agent agent) {
        agents.add(agent);
        agent.setAutresAgents(agents);
    }

    public void supprimerAgent(Agent agent) {
        agents.remove(agent);
        System.out.println("[Simulation] Agent " + agent.getId() + " retire.");
    }

    public void initialiser() {
        tick    = 0;
        enCours = true;
        System.out.println("=== Simulation initialisee (" + agents.size() + " agent(s)) ===");
        for (Agent agent : agents) {
            agent.setAutresAgents(agents);
            agent.initialiser();
        }
    }

    public void tick() {
        if (!enCours) return;
        tick++;
        System.out.println("\n--- Tick " + tick + " ---");
        for (Agent agent : agents) agent.deplacer();
        afficherEtat();
        if (estTerminee()) {
            enCours = false;
            System.out.println("\n=== Simulation terminee en " + tick + " tick(s) ===");
            afficherResultats();
        }
    }

    public void lancerSimulation() {
        initialiser();
        while (enCours) tick();
    }

    public boolean estTerminee() {
        for (Agent agent : agents) {
            Agent.EtatDeplacement e = agent.getEtatDeplacement();
            if (e == Agent.EtatDeplacement.EN_MOUVEMENT || e == Agent.EtatDeplacement.EN_ATTENTE) return false;
        }
        return !agents.isEmpty();
    }

    public int[] getStatsDeplacement() {
        int enAttente = 0, enMouvement = 0, arrive = 0, bloque = 0;
        for (Agent a : agents) {
            switch (a.getEtatDeplacement()) {
                case EN_ATTENTE   -> enAttente++;
                case EN_MOUVEMENT -> enMouvement++;
                case ARRIVE       -> arrive++;
                case BLOQUE       -> bloque++;
            }
        }
        return new int[]{enAttente, enMouvement, arrive, bloque};
    }

    public int[] getStatsPsychologiques() {
        int calme = 0, panique = 0, folie = 0;
        for (Agent a : agents) {
            switch (a.getEtatPsychologique()) {
                case CALME   -> calme++;
                case PANIQUE -> panique++;
                case FOLIE   -> folie++;
            }
        }
        return new int[]{calme, panique, folie};
    }

    public int[] getDensiteParNoeud() {
        int[] densite = new int[graph.getNodes().size()];
        for (Agent a : agents) {
            if (a.getEtatDeplacement() == Agent.EtatDeplacement.ARRIVE) continue;
            int idx = graph.getNodes().indexOf(a.getPosition());
            if (idx >= 0) densite[idx]++;
        }
        return densite;
    }

    public void sauvegarder(File fichier) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fichier))) {
            oos.writeObject(this);
        }
    }

    public static SimulationEngine charger(File fichier) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichier))) {
            SimulationEngine engine = (SimulationEngine) ois.readObject();
            for (Agent a : engine.agents) a.setAutresAgents(engine.agents);
            return engine;
        }
    }

    private void afficherEtat() {
        int[] d = getStatsDeplacement();
        System.out.println("[Etat] En mouvement: " + d[1] + " | Arrives: " + d[2] + " | Bloques: " + d[3]);
    }

    private void afficherResultats() {
        System.out.println("\n=== Resultats ===");
        for (Agent agent : agents) System.out.println(agent);
        System.out.println("Total arrives : " + getStatsDeplacement()[2] + "/" + agents.size());
    }

    public int getTick()           { return tick; }
    public boolean isEnCours()     { return enCours; }
    public List<Agent> getAgents() { return agents; }
    public Graph getGraph()        { return graph; }
}
