package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;

import java.util.List;

/**
 * The proportion of X*->Y in the estimated graph for which there is no path Y~~>X in the true graph.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class TrueDagPrecisionArrow implements Statistic {
    private static final long serialVersionUID = 23L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAbbreviation() {
        return "*->-Prec";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Proportion of X*->Y in the estimated graph for which there is no path Y~~>X in the true graph";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        int tp = 0;
        int fp = 0;

        List<Node> nodes = estGraph.getNodes();

        for (Node x : nodes) {
            for (Node y : nodes) {
                if (estGraph.isAdjacentTo(x, y) && estGraph.getEndpoint(x, y) == Endpoint.ARROW) {
                    if (!trueGraph.paths().isAncestorOf(y, x)) {
                        tp++;
                    } else {
//                        System.out.println("Shouldn't be " + y + "~~>" + x + ": " + estGraph.getEdge(x, y));
                        fp++;
                    }
                }
            }
        }

        return tp / (double) (tp + fp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getNormValue(double value) {
        return value;
    }
}
