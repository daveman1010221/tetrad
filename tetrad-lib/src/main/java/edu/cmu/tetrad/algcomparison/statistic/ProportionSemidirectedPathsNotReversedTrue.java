package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;

import java.io.Serial;
import java.util.List;

/**
 * Proportion of semi(X, Y) in true graph for which there is no semi(Y, Z) in estimated graph.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class ProportionSemidirectedPathsNotReversedTrue implements Statistic {
    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAbbreviation() {
        return "semi(X,Y,true)==>!semi(Y,X,est)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Proportion of semi(X, Y) in true graph for which there is no semi(Y, Z) in estimated graph";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        List<Node> nodes = estGraph.getNodes();
        nodes.removeIf(node -> node.getNodeType() == NodeType.LATENT);

        int tp = 0;
        int fn = 0;

        for (Node x : nodes) {
            for (Node y : nodes) {
                if (x == y) continue;

                if (trueGraph.paths().existsSemiDirectedPath(x, y)) {
                    if (!estGraph.paths().existsSemiDirectedPath(y, x)) {
                        tp++;
                    } else {
                        fn++;
                    }
                }
            }
        }

        return tp / (double) (tp + fn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNormValue(double value) {
        return value;
    }
}
