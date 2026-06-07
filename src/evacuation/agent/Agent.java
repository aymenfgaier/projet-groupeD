package evacuation.agent;

import evacuation.graph.Graph;
import evacuation.graph.Node;
import evacuation.graph.Sortie;
import evacuation.routing.DijkstraPathFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Agent {

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    /** État de déplacement de l'agent dans la simulation. */
    public enum EtatDeplacement {
        EN_ATTENTE, EN_MOUVEMENT, ARRIVE, BLOQUE
    }

    /**
     * État psychologique de l'agent.
     * Influence le comportement et la vitesse effective.
     */
    public enum EtatPsychologique {
        CALME,   // comportement normal
        PANIQUE, // vitesse accrue, ignore les règles de priorité
        FOLIE    // déplacement aléatoire, ignore la destination
    }

    /**
     * Comportement de l'agent face aux autres agents dans une arête.
     */
    public enum Comportement {
        LAISSE_PASSER,  // attend que l'arête soit libre avant d'entrer
        PRIORITAIRE,    // entre sans attendre, peut provoquer des blocages
        SUIT_AGENT      // suit le plus proche agent allant dans la même direction
    }

    /**
     * Mode de choix de la destination.
     */
    public enum ModeDestination {
        FIXE,             // se dirige vers une destination fixe
        ALEATOIRE,        // choisit un nœud aléatoire à chaque arrivée
        FUIT_DESTINATION, // s'éloigne de la destination (chemin le plus long)
        VERS_DENSE,       // se dirige vers les zones les plus denses
        FUIT_DENSITE      // évite les zones denses
    }

    // -------------------------------------------------------------------------
    // Attributs
    // -------------------------------------------------------------------------

    private final String id;
    private Node position;
    private Node destination;
    private final Graph graph;

    /** Vitesse maximale (nœuds par tick à l'état CALME). */
    private int vitesse;

    /** Tolérance à la congestion : 0.0 = fuit toute densité, 1.0 = indifférent. */
    private double toleranceDensite;

    private Comportement comportement;
    private EtatPsychologique etatPsychologique;
    private ModeDestination modeDestination;
    private EtatDeplacement etatDeplacement;

    private DijkstraPathFinder pathFinder;
    private List<Node> chemin;
    private int indexChemin;

    /** Compteur de ticks restants avant de pouvoir avancer (gestion vitesse/congestion). */
    private int ticksAttente;

    private static final Random RANDOM = new Random();

    // -------------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------------

    /**
     * Crée un agent avec toutes ses propriétés.
     *
     * @param id                identifiant unique
     * @param positionInitiale  nœud de départ
     * @param destination       nœud cible
     * @param graph             graphe de l'environnement
     * @param vitesse           vitesse maximale (nœuds/tick)
     * @param toleranceDensite  tolérance à la congestion [0.0 – 1.0]
     * @param comportement      comportement face aux autres agents
     * @param etatPsychologique état psychologique initial
     * @param modeDestination   mode de choix de destination
     */
    public Agent(String id, Node positionInitiale, Node destination, Graph graph,
                 int vitesse, double toleranceDensite,
                 Comportement comportement, EtatPsychologique etatPsychologique,
                 ModeDestination modeDestination) {
        this.id                 = id;
        this.position           = positionInitiale;
        this.destination        = destination;
        this.graph              = graph;
        this.vitesse            = Math.max(1, vitesse);
        this.toleranceDensite   = Math.min(1.0, Math.max(0.0, toleranceDensite));
        this.comportement       = comportement;
        this.etatPsychologique  = etatPsychologique;
        this.modeDestination    = modeDestination;
        this.etatDeplacement    = EtatDeplacement.EN_ATTENTE;
        this.pathFinder         = new DijkstraPathFinder(graph);
        this.chemin             = new ArrayList<>();
        this.indexChemin        = 0;
        this.ticksAttente       = 0;
    }

    /**
     * Constructeur simplifié avec valeurs par défaut (agent calme, vitesse 1,
     * destination fixe, tolérance maximale, comportement LAISSE_PASSER).
     *
     * @param id               identifiant unique
     * @param positionInitiale nœud de départ
     * @param destination      nœud cible
     * @param graph            graphe de l'environnement
     */
    public Agent(String id, Node positionInitiale, Node destination, Graph graph) {
        this(id, positionInitiale, destination, graph,
             1, 1.0, Comportement.LAISSE_PASSER,
             EtatPsychologique.CALME, ModeDestination.FIXE);
    }

    // -------------------------------------------------------------------------
    // Initialisation et déplacement
    // -------------------------------------------------------------------------

    /**
     * Calcule le chemin initial vers la destination.
     * Doit être appelé avant le premier tick.
     */
    public void initialiser() {
        if (destination == null || destination.isBloque()) {
            System.out.println("[Agent " + id + "] Destination indisponible.");
            etatDeplacement = EtatDeplacement.BLOQUE;
            return;
        }
        chemin = pathFinder.calculerChemin(position, destination);
        if (chemin.isEmpty()) {
            System.out.println("[Agent " + id + "] Aucun chemin vers " + destination.getNom() + ".");
            etatDeplacement = EtatDeplacement.BLOQUE;
        } else {
            indexChemin     = 1;
            etatDeplacement = EtatDeplacement.EN_MOUVEMENT;
            System.out.println("[Agent " + id + "] Chemin calculé : " + cheminToString());
        }
    }

    /**
     * Avance l'agent d'un pas de simulation (tick).
     * La vitesse effective dépend de l'état psychologique :
     * PANIQUE double la vitesse, FOLIE force un déplacement aléatoire.
     */
    public void deplacer() {
        if (etatDeplacement == EtatDeplacement.ARRIVE
                || etatDeplacement == EtatDeplacement.EN_ATTENTE) return;

        if (ticksAttente > 0) {
            ticksAttente--;
            System.out.println("[Agent " + id + "] Attend (" + ticksAttente + " tick(s) restant(s)).");
            return;
        }

        if (etatDeplacement == EtatDeplacement.BLOQUE) {
            recalculerChemin();
            return;
        }

        if (etatPsychologique == EtatPsychologique.FOLIE) {
            deplacerAleatoirement();
            return;
        }

        int pas = vitesseEffective();
        for (int i = 0; i < pas; i++) {
            if (!avancerUnNoeud()) break;
            if (etatDeplacement == EtatDeplacement.ARRIVE) break;
        }
    }

    /**
     * Avance d'un seul nœud sur le chemin calculé.
     *
     * @return true si le déplacement a eu lieu
     */
    private boolean avancerUnNoeud() {
        if (indexChemin >= chemin.size()) {
            etatDeplacement = EtatDeplacement.ARRIVE;
            System.out.println("[Agent " + id + "] Arrivé à " + destination.getNom() + ".");
            onArrivee();
            return false;
        }

        Node prochainNoeud = chemin.get(indexChemin);

        if (prochainNoeud.isBloque() || !graph.getVoisins(position).contains(prochainNoeud)) {
            System.out.println("[Agent " + id + "] Chemin bloqué en " + position.getNom() + ". Recalcul...");
            recalculerChemin();
            return false;
        }

        // Gestion congestion selon tolérance
        if (comportement == Comportement.LAISSE_PASSER
                && etatPsychologique != EtatPsychologique.PANIQUE) {
            int agentsDansNoeud = compterAgentsDansNoeud(prochainNoeud);
            if (agentsDansNoeud > 0 && RANDOM.nextDouble() > toleranceDensite) {
                ticksAttente = 1;
                System.out.println("[Agent " + id + "] Attend (congestion en " + prochainNoeud.getNom() + ").");
                return false;
            }
        }

        position = prochainNoeud;
        indexChemin++;
        System.out.println("[Agent " + id + "] -> " + position.getNom());

        if (position.equals(destination)) {
            etatDeplacement = EtatDeplacement.ARRIVE;
            System.out.println("[Agent " + id + "] Arrivé à " + destination.getNom() + ".");
            onArrivee();
            return false;
        }
        return true;
    }

    /**
     * Déplacement aléatoire (état FOLIE) : choisit un voisin au hasard.
     */
    private void deplacerAleatoirement() {
        List<Node> voisins = graph.getVoisins(position);
        if (voisins.isEmpty()) {
            etatDeplacement = EtatDeplacement.BLOQUE;
            return;
        }
        position = voisins.get(RANDOM.nextInt(voisins.size()));
        System.out.println("[Agent " + id + "] (folie) -> " + position.getNom());
    }

    /**
     * Recalcule le chemin depuis la position actuelle.
     * Tient compte du mode destination pour choisir un nouveau nœud cible si besoin.
     */
    public void recalculerChemin() {
        if (modeDestination == ModeDestination.ALEATOIRE) {
            destination = choisirDestinationAleatoire();
        }
        pathFinder.recalculer();
        chemin = pathFinder.calculerChemin(position, destination);
        if (chemin.isEmpty()) {
            etatDeplacement = EtatDeplacement.BLOQUE;
            System.out.println("[Agent " + id + "] Aucun chemin disponible depuis " + position.getNom() + ".");
        } else {
            indexChemin     = 1;
            etatDeplacement = EtatDeplacement.EN_MOUVEMENT;
            System.out.println("[Agent " + id + "] Nouveau chemin : " + cheminToString());
        }
    }

    /**
     * Appelé quand l'agent atteint sa destination.
     * En mode ALEATOIRE, choisit une nouvelle destination automatiquement.
     */
    private void onArrivee() {
        if (modeDestination == ModeDestination.ALEATOIRE) {
            destination = choisirDestinationAleatoire();
            if (destination != null) {
                etatDeplacement = EtatDeplacement.EN_MOUVEMENT;
                recalculerChemin();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------------

    /**
     * Retourne la vitesse effective selon l'état psychologique.
     * PANIQUE double la vitesse, FOLIE et CALME utilisent la vitesse normale.
     */
    private int vitesseEffective() {
        return etatPsychologique == EtatPsychologique.PANIQUE ? vitesse * 2 : vitesse;
    }

    /**
     * Estime le nombre d'agents présents dans un nœud donné.
     * Note : nécessite que SimulationEngine injecte la liste via setAutresAgents().
     */
    private List<Agent> autresAgents = new ArrayList<>();

    private int compterAgentsDansNoeud(Node noeud) {
        int count = 0;
        for (Agent a : autresAgents) {
            if (a != this && a.getPosition().equals(noeud)) count++;
        }
        return count;
    }

    /** Choisit un nœud accessible aléatoire comme nouvelle destination. */
    private Node choisirDestinationAleatoire() {
        List<Node> noeuds = graph.getNodes();
        List<Node> accessibles = new ArrayList<>();
        for (Node n : noeuds) {
            if (!n.isBloque() && !n.equals(position)) accessibles.add(n);
        }
        if (accessibles.isEmpty()) return null;
        return accessibles.get(RANDOM.nextInt(accessibles.size()));
    }

    private String cheminToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chemin.size(); i++) {
            sb.append(chemin.get(i).getNom());
            if (i < chemin.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public String getId()                           { return id; }
    public Node getPosition()                       { return position; }
    public Node getDestination()                    { return destination; }
    public int getVitesse()                         { return vitesse; }
    public double getToleranceDensite()             { return toleranceDensite; }
    public Comportement getComportement()           { return comportement; }
    public EtatPsychologique getEtatPsychologique() { return etatPsychologique; }
    public ModeDestination getModeDestination()     { return modeDestination; }
    public EtatDeplacement getEtatDeplacement()     { return etatDeplacement; }
    public List<Node> getChemin()                   { return chemin; }

    public void setVitesse(int vitesse)                             { this.vitesse = Math.max(1, vitesse); }
    public void setToleranceDensite(double toleranceDensite)        { this.toleranceDensite = Math.min(1.0, Math.max(0.0, toleranceDensite)); }
    public void setComportement(Comportement comportement)          { this.comportement = comportement; }
    public void setModeDestination(ModeDestination modeDestination) { this.modeDestination = modeDestination; }
    public void setDestination(Node destination)                    { this.destination = destination; }
    public void setAutresAgents(List<Agent> autresAgents)           { this.autresAgents = autresAgents; }

    /**
     * Change l'état psychologique et adapte le comportement si nécessaire.
     * Un agent en PANIQUE devient PRIORITAIRE et ignore la congestion.
     */
    public void setEtatPsychologique(EtatPsychologique etat) {
        this.etatPsychologique = etat;
        if (etat == EtatPsychologique.PANIQUE) {
            this.comportement = Comportement.PRIORITAIRE;
        }
        System.out.println("[Agent " + id + "] État psychologique -> " + etat);
    }

    @Override
    public String toString() {
        return "Agent{id='" + id + "', pos='" + position.getNom()
             + "', dest='" + (destination != null ? destination.getNom() : "aucune")
             + "', etat=" + etatDeplacement
             + ", psycho=" + etatPsychologique
             + ", vitesse=" + vitesse + "}";
    }
}
