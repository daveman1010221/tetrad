package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.algcomparison.statistic.utils.AdjacencyConfusion;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;

/**
 * The adjacency true positive rate. The true positives are the number of adjacencies in both
 * the true and estimated graphs.
 *
 * @author jdramsey
 */
public class AdjacencyTPR implements Statistic {
    static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "ATPR";
    }

    @Override
    public String getDescription() {
        return "Adjacency True Positive Rate";
    }

    @Override
    public double getValue(final Graph trueGraph, final Graph estGraph, final DataModel dataModel) {
        final AdjacencyConfusion adjConfusion = new AdjacencyConfusion(trueGraph, estGraph);
        final int adjTp = adjConfusion.getAdjTp();
        final int adjFp = adjConfusion.getAdjFp();
        final int adjFn = adjConfusion.getAdjFn();
//        int adjTn = adjConfusion.getAdjTn();
        return adjTp / (double) (adjTp + adjFn);
    }

    @Override
    public double getNormValue(final double value) {
        return value;
    }
}
