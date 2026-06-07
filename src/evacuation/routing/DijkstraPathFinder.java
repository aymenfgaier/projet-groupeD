package evacuation.routing;

import evacuation.graph.Graph;
import evacuation.graph.Node;

import java.util.*;

public class DijkstraPathFinder implements PathFinder {

    private final Graph graph;

    public DijkstraPathFinder(Graph graph) {
        this.graph = graph;
    }

    @Override
    public List<Node> calculerChemin(Node source, Node destination) {
        Map<Node, Double> dist = new HashMap<>();
        Map<Node, Node>   pred = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));

        for (Node n : graph.getNodes()) dist.put(n, Double.POSITIVE_INFINITY);
        dist.put(source, 0.0);
        pq.add(source);

        while (!pq.isEmpty()) {
            Node u = pq.poll();
            if (u.equals(destination)) break;
            for (Node v : graph.getVoisins(u)) {
                double alt = dist.get(u) + graph.getPoids(u, v);
                if (alt < dist.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    dist.put(v, alt);
                    pred.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        List<Node> chemin = new ArrayList<>();
        Node cur = destination;
        while (cur != null) {
            chemin.add(0, cur);
            cur = pred.get(cur);
        }
        if (chemin.isEmpty() || !chemin.get(0).equals(source)) return new ArrayList<>();
        return chemin;
    }

    @Override
    public void recalculer() {}
}
