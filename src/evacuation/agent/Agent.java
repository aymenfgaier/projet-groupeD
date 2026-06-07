package evacuation.agent;

import evacuation.graph.Graph;
import evacuation.graph.Node;
import evacuation.routing.DijkstraPathFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Agent {

    public enum EtatDeplacement  { EN_ATTENTE, EN_MOUVEMENT, ARRIVE, BLOQUE }
    public enum EtatPsychologique { CALME, PANIQUE, FOLIE }
    public enum Comportement     { LAISSE_PASSER, PRIORITAIRE, SUIT_AGENT }
    public enum ModeDestination  { FIXE, ALEATOIRE, FUIT_DESTINATION, VERS_DENSE, FUIT_DENSITE }

    private final String id;
    private Node position;
    private Node destination;
    private final Graph graph;
    private int vitesse;
    private double toleranceDensite;
    private Comportement comportement;
    private EtatPsychologique etatPsychologique;
    private ModeDestination modeDestination;
    private EtatDeplacement etatDeplacement;
    private DijkstraPathFinder pathFinder;
    private List<Node> chemin;
    private int indexChemin;
    private int ticksAttente;
    private List<Agent> autresAgents = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public Agent(String id, Node positionInitiale, Node destination, Graph graph,
                 int vitesse, double toleranceDensite,
                 Comportement comportement, EtatPsychologique etatPsychologique,
                 ModeDestination modeDestination) {
        this.id                = id;
        this.position          = positionInitiale;
        this.destination       = destination;
        this.graph             = graph;
        this.vitesse           = Math.max(1, vitesse);
        this.toleranceDensite  = Math.min(1.0, Math.max(0.0, toleranceDensite));
        this.comportement      = comportement;
        this.etatPsychologique = etatPsychologique;
        this.modeDestination   = modeDestination;
        this.etatDeplacement   = EtatDeplacement.EN_ATTENTE;
        this.pathFinder        = new DijkstraPathFinder(graph);
        this.chemin            = new ArrayList<>();
        this.indexChemin       = 0;
        this.ticksAttente      = 0;
    }

    public Agent(String id, Node positionInitiale, Node destination, Graph graph) {
        this(id, positionInitiale, destination, graph,
             1, 1.0, Comportement.LAISSE_PASSER, EtatPsychologique.CALME, ModeDestination.FIXE);
    }

    public void initialiser() {
        if (destination == null || destination.isBloque()) {
            etatDeplacement = EtatDeplacement.BLOQUE; return;
        }
        chemin = pathFinder.calculerChemin(position, destination);
        if (chemin.isEmpty()) {
            etatDeplacement = EtatDeplacement.BLOQUE;
        } else {
            indexChemin = 1;
            etatDeplacement = EtatDeplacement.EN_MOUVEMENT;
            System.out.println("[Agent " + id + "] Chemin : " + cheminToString());
        }
    }

    public void deplacer() {
        if (etatDeplacement == EtatDeplacement.ARRIVE || etatDeplacement == EtatDeplacement.EN_ATTENTE) return;
        if (ticksAttente > 0) { ticksAttente--; return; }
        if (etatDeplacement == EtatDeplacement.BLOQUE) { recalculerChemin(); return; }
        if (etatPsychologique == EtatPsychologique.FOLIE) { deplacerAleatoirement(); return; }
        int pas = vitesseEffective();
        for (int i = 0; i < pas; i++) {
            if (!avancerUnNoeud()) break;
            if (etatDeplacement == EtatDeplacement.ARRIVE) break;
        }
    }

    private boolean avancerUnNoeud() {
        if (indexChemin >= chemin.size()) {
            etatDeplacement = EtatDeplacement.ARRIVE;
            System.out.println("[Agent " + id + "] Arrive a " + destination.getNom() + ".");
            onArrivee(); return false;
        }
        Node prochain = chemin.get(indexChemin);
        if (prochain.isBloque() || !graph.getVoisins(position).contains(prochain)) {
            recalculerChemin(); return false;
        }
        if (comportement == Comportement.LAISSE_PASSER && etatPsychologique != EtatPsychologique.PANIQUE) {
            if (compterAgentsDansNoeud(prochain) > 0 && RANDOM.nextDouble() > toleranceDensite) {
                ticksAttente = 1; return false;
            }
        }
        position = prochain;
        indexChemin++;
        System.out.println("[Agent " + id + "] -> " + position.getNom());
        if (position.equals(destination)) {
            etatDeplacement = EtatDeplacement.ARRIVE;
            System.out.println("[Agent " + id + "] Arrive a " + destination.getNom() + ".");
            onArrivee(); return false;
        }
        return true;
    }

    private void deplacerAleatoirement() {
        List<Node> voisins = graph.getVoisins(position);
        if (voisins.isEmpty()) { etatDeplacement = EtatDeplacement.BLOQUE; return; }
        position = voisins.get(RANDOM.nextInt(voisins.size()));
        System.out.println("[Agent " + id + "] (folie) -> " + position.getNom());
    }

    public void recalculerChemin() {
        if (modeDestination == ModeDestination.ALEATOIRE) destination = choisirDestinationAleatoire();
        pathFinder.recalculer();
        chemin = pathFinder.calculerChemin(position, destination);
        if (chemin.isEmpty()) { etatDeplacement = EtatDeplacement.BLOQUE; }
        else { indexChemin = 1; etatDeplacement = EtatDeplacement.EN_MOUVEMENT; }
    }

    private void onArrivee() {
        if (modeDestination == ModeDestination.ALEATOIRE) {
            destination = choisirDestinationAleatoire();
            if (destination != null) { etatDeplacement = EtatDeplacement.EN_MOUVEMENT; recalculerChemin(); }
        }
    }

    private int vitesseEffective() {
        return etatPsychologique == EtatPsychologique.PANIQUE ? vitesse * 2 : vitesse;
    }

    private int compterAgentsDansNoeud(Node noeud) {
        int count = 0;
        for (Agent a : autresAgents) if (a != this && a.getPosition().equals(noeud)) count++;
        return count;
    }

    private Node choisirDestinationAleatoire() {
        List<Node> acc = new ArrayList<>();
        for (Node n : graph.getNodes()) if (!n.isBloque() && !n.equals(position)) acc.add(n);
        if (acc.isEmpty()) return null;
        return acc.get(RANDOM.nextInt(acc.size()));
    }

    private String cheminToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chemin.size(); i++) {
            sb.append(chemin.get(i).getNom());
            if (i < chemin.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }

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

    public void setVitesse(int v)                         { this.vitesse = Math.max(1, v); }
    public void setToleranceDensite(double t)             { this.toleranceDensite = Math.min(1.0, Math.max(0.0, t)); }
    public void setComportement(Comportement c)           { this.comportement = c; }
    public void setModeDestination(ModeDestination m)     { this.modeDestination = m; }
    public void setDestination(Node d)                    { this.destination = d; }
    public void setAutresAgents(List<Agent> autresAgents) { this.autresAgents = autresAgents; }

    public void setEtatPsychologique(EtatPsychologique etat) {
        this.etatPsychologique = etat;
        if (etat == EtatPsychologique.PANIQUE) this.comportement = Comportement.PRIORITAIRE;
        System.out.println("[Agent " + id + "] Etat psychologique -> " + etat);
    }

    @Override
    public String toString() {
        return "Agent{id='" + id + "', pos='" + position.getNom()
             + "', dest='" + (destination != null ? destination.getNom() : "aucune")
             + "', etat=" + etatDeplacement + ", psycho=" + etatPsychologique + "}";
    }
}
