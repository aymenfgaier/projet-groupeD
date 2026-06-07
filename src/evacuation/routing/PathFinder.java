package evacuation.routing;

import evacuation.graph.Node;
import java.util.List;

public interface PathFinder {
    List<Node> calculerChemin(Node source, Node destination);
    void recalculer();
}