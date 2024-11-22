package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.util.PagCache;

import java.io.Serial;
import java.util.HashSet;
import java.util.List;

/**
 * Legal PAG
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class LegalPag implements Statistic {
    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * <p>Constructor for LegalPag.</p>
     */
    public LegalPag() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAbbreviation() {
        return "LegalPAG";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "1 if the estimated graph is Legal PAG, 0 if not";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        List<Node> latent = trueGraph.getNodes().stream()
                .filter(node -> node.getNodeType() == NodeType.LATENT).toList();

        List<Node> measured = trueGraph.getNodes().stream()
                .filter(node -> node.getNodeType() == NodeType.MEASURED).toList();

        List<Node> selection = trueGraph.getNodes().stream()
                .filter(node -> node.getNodeType() == NodeType.SELECTION).toList();

        GraphSearchUtils.LegalPagRet legalPag = GraphSearchUtils.isLegalPag(estGraph, new HashSet<>(selection));

        if (legalPag.isLegalPag()) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNormValue(double value) {
        return value;
    }
}
