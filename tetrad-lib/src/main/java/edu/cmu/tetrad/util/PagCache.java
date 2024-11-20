package edu.cmu.tetrad.util;

import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphTransforms;
import edu.cmu.tetrad.search.utils.DagToPag;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A cache for storing PAGs so that the only need to be calculated once per DAG.
 */
public class PagCache {

    /**
     * A singleton instance of the PagCache class.
     * This ensures that only one instance of the cache exists at any given time.
     */
    private static final PagCache instance = new PagCache();

    /**
     * A map that stores the PAGs corresponding to the DAGs.
     */
    private final Map<Graph, Graph> cache = new WeakHashMap<>();

    /**
     * Returns the singleton instance of the PagCache.
     *
     * @return the singleton instance of PagCache.
     */
    public static PagCache getInstance() {
        return instance;
    }

    /**
     * Returns the PAG (Partial Ancestral Graph) corresponding to the given DAG (Directed Acyclic Graph).
     * If the conversion has already been performed earlier, the cached result will be returned.
     * Otherwise, the DAG will be converted to a PAG, cached, and then returned.
     *
     * @param graph the input DAG to be transformed into a PAG
     * @return the corresponding PAG of the input DAG
     * @throws IllegalArgumentException if the input graph is not a DAG
     */
    public Graph getPag(Graph graph) {
        if (!(graph.paths().isLegalDag() || graph.paths().isLegalMag())) {
            throw new IllegalArgumentException("Graph must be a DAG or a MAG.");
        }

        // If the graph is already in the cache, return it; otherwise, call GraphTransforms.dagToPag(graph)
        // to get the PAG and put it into the cache.
        if (cache.containsKey(graph)) {
            return cache.get(graph);
        } else {
            DagToPag dagToPag = new DagToPag(graph);
            Graph pag = dagToPag.convert();
            cache.put(graph, pag);
            return pag;
        }
    }

    public Graph getPag(Graph graph, Knowledge knowledge, boolean verbose) {
        if (!graph.paths().isLegalDag()) {
            throw new IllegalArgumentException("Graph must be a DAG.");
        }

        // If the graph is already in the cache, return it; otherwise, call GraphTransforms.dagToPag(graph)
        // to get the PAG and put it into the cache.
        if (cache.containsKey(graph)) {
            return cache.get(graph);
        } else {
            DagToPag dagToPag = new DagToPag(graph);
            dagToPag.setKnowledge(knowledge);
            dagToPag.setVerbose(verbose);
            Graph pag = dagToPag.convert();
            cache.put(graph, pag);
            return pag;
        }
    }
}
