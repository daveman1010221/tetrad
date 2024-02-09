package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.algcomparison.statistic.utils.OrientationConfusion;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;

/**
 * The orientation precision.
 *
 * @author bryanandrews, osephramsey
 * @version $Id: $Id
 */
public class OrientationPrecision implements Statistic {
    private static final long serialVersionUID = 23L;

    /** {@inheritDoc} */
    @Override
    public String getAbbreviation() {
        return "OP";
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Orientation Precision";
    }

    /** {@inheritDoc} */
    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        OrientationConfusion oriConfusion = new OrientationConfusion(trueGraph, estGraph);
        int oriTp = oriConfusion.getTp();
        int oriFp = oriConfusion.getFp();
        return oriTp / (double) (oriTp + oriFp);
    }

    /** {@inheritDoc} */
    @Override
    public double getNormValue(double value) {
        return value;
    }
}
