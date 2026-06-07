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

    /** Statistiques cumulées : nombre total d'agents arrivés à destination. */
    private int totalArrivesHistorique;

    public SimulationEngine(Graph graph) {
        this.graph                  = graph;
        this.agents                 = new ArrayList<>();
        this.tick                   = 0;
        this.enCours                = false;
        this.totalArrivesHistorique = 0;
    }

    // -------------------------------------------------------------------------
    // Gestion des agents
    // -------------------------------------------------------------------------

    /** Ajoute un agent à la simulation. */
    public void ajouterAgent(Agent agent) {
        agents.add(agent);
        agent.setAutresAgents(agents);
    }

    /** Retire un agent de la simulation. */
    public void supprimerAgent(Agent agent) {
        agents.remove(agent);
        System.out.println("[Simulation] Agent " + agent.getId() + " retiré.");
    }

    // -------------------------------------------------------------------------
    // Cycle de simulation
    // -------------------------------------------------------------------------

    /** Initialise tous les agents (calcul des chemins initiaux). */
    public void initialiser() {
        tick                  = 0;
        enCours               = true;
        totalArrivesHistorique = 0;
        System.out.println("=== Simulation initialisée (" + agents.size() + " agent(s)) ===");
        for (Agent agent : agents) {
            agent.setAutresAgents(agents);
            agent.initialiser();
        }
    }

    /**
     * Avance la simulation d'un pas.
     * Chaque agent se déplace d'un nœud selon ses propriétés.
     */
    public void tick() {
        if (!enCours) return;

        tick++;
        System.out.println("\n--- Tick " + tick + " ---");

        for (Agent agent : agents) {
            agent.deplacer();
        }

        totalArrivesHistorique = (int) agents.stream()
                .filter(a -> a.getEtatDeplacement() == Agent.EtatDeplacement.ARRIVE).count();

        afficherEtat();

        if (estTerminee()) {
            enCours = false;
            System.out.println("\n=== Simulation terminée en " + tick + " tick(s) ===");
            afficherResultats();
        }
    }

    /** Lance la simulation complète en boucle (mode ligne de commande). */
    public void lancerSimulation() {
        initialiser();
        while (enCours) {
            tick();
        }
    }

    /** Retourne true si tous les agents ont un état final (ARRIVE ou BLOQUE). */
    public boolean estTerminee() {
        for (Agent agent : agents) {
            Agent.EtatDeplacement etat = agent.getEtatDeplacement();
            if (etat == Agent.EtatDeplacement.EN_MOUVEMENT
                    || etat == Agent.EtatDeplacement.EN_ATTENTE) {
                return false;
            }
        }
        return !agents.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Statistiques
    // -------------------------------------------------------------------------

    /** Retourne le nombre d'agents dans chaque état de déplacement. */
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

    /** Retourne le nombre d'agents dans chaque état psychologique. */
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

    /**
     * Retourne le nombre d'agents présents sur chaque nœud.
     * L'index correspond à la position dans graph.getNodes().
     */
    public int[] getDensiteParNoeud() {
        int[] densite = new int[graph.getNodes().size()];
        for (Agent a : agents) {
            if (a.getEtatDeplacement() == Agent.EtatDeplacement.ARRIVE) continue;
            int idx = graph.getNodes().indexOf(a.getPosition());
            if (idx >= 0) densite[idx]++;
        }
        return densite;
    }

    // -------------------------------------------------------------------------
    // Import / Export binaire
    // -------------------------------------------------------------------------

    /**
     * Sauvegarde l'état complet de la simulation dans un fichier binaire.
     *
     * @param fichier fichier de destination
     * @throws IOException en cas d'erreur d'écriture
     */
    public void sauvegarder(File fichier) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fichier))) {
            oos.writeObject(this);
            System.out.println("[Simulation] Sauvegardée dans : " + fichier.getPath());
        }
    }

    /**
     * Charge une simulation depuis un fichier binaire.
     *
     * @param fichier fichier source
     * @return l'instance SimulationEngine restaurée
     * @throws IOException            en cas d'erreur de lecture
     * @throws ClassNotFoundException si le fichier est incompatible
     */
    public static SimulationEngine charger(File fichier) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichier))) {
            SimulationEngine engine = (SimulationEngine) ois.readObject();
            // Réinjecte la liste d'agents dans chaque agent (référence transiente)
            for (Agent a : engine.agents) {
                a.setAutresAgents(engine.agents);
            }
            System.out.println("[Simulation] Chargée depuis : " + fichier.getPath());
            return engine;
        }
    }

    // -------------------------------------------------------------------------
    // Affichage console
    // -------------------------------------------------------------------------

    private void afficherEtat() {
        int[] d = getStatsDeplacement();
        System.out.println("[Etat] En mouvement: " + d[1]
                + " | Arrivés: " + d[2]
                + " | Bloqués: " + d[3]);
    }

    private void afficherResultats() {
        System.out.println("\n=== Résultats ===");
        for (Agent agent : agents) {
            System.out.println(agent);
        }
        System.out.println("Total arrivés : " + getStatsDeplacement()[2] + "/" + agents.size());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getTick()           { return tick; }
    public boolean isEnCours()     { return enCours; }
    public List<Agent> getAgents() { return agents; }
    public Graph getGraph()        { return graph; }
}

