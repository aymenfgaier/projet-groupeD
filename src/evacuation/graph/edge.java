package evacuation.graph;

public class Edge implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Node source;
    private Node destination;
    private double poids;
    private boolean disponible;

    public Edge(Node source, Node destination, double poids) {
        this.source = source; this.destination = destination;
        this.poids = poids; this.disponible = true;
    }

    public double getPoids(){ return disponible ? poids : Double.POSITIVE_INFINITY; }
    public void bloquer(){ this.disponible = false; }
    public void debloquer(){ this.disponible = true; }
    public Node getSource(){ return source; }
    public Node getDestination(){ return destination; }
    public boolean isDisponible(){ return disponible; }

    @Override
    public String toString() {
        return "Edge{" + source.getNom() + " -> " + destination.getNom() + ", poids=" + poids + "}";
    }
}