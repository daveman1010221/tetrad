package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.MarkovCheck;
import edu.cmu.tetrad.search.test.IndTestFisherZ;

import static org.apache.commons.math3.util.FastMath.abs;

/**
 * Estimates whether the p-values under the null are Uniform usign the Markov Checker. This estimates whether the
 * p-value of the Kolmogorov-Smirnov test for distribution of p-values under the null using the Fisher Z test for the
 * local Markov check is uniform, so is only applicable to continuous data and really strictly only for Gaussian data.
 *
 * @author josephramsey
 */
public class PvalueDistanceToAlpha implements Statistic {
    static final long serialVersionUID = 23L;
    private double alpha = 0.01;

    public PvalueDistanceToAlpha(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public String getAbbreviation() {
        return "DistAlpha";
    }

    @Override
    public String getDescription() {
        return "P-value Distance for Alpha";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        MarkovCheck markovCheck = new MarkovCheck(estGraph, new IndTestFisherZ((DataSet) dataModel, alpha), MarkovCheck.ConditioningSetType.PARENTS);
        markovCheck.generateResults();
        return abs(alpha - markovCheck.getKsPValue(true));
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}
