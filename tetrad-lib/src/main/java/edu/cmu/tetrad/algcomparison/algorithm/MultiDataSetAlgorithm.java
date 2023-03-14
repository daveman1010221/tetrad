package edu.cmu.tetrad.algcomparison.algorithm;

import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;

import java.util.List;

/**
 * Implements an algorithm that takes multiple data sets as input.
 *
 * @author jdramsey
 */
public interface MultiDataSetAlgorithm extends Algorithm {

    /**
     * Runs the search.
     *
     * @param dataSets   The data set to run to the search on.
     * @param parameters The paramters of the search.
     * @return The result graph.
     */
    Graph search(List<DataModel> dataSets, Parameters parameters);

    void setScoreWrapper(ScoreWrapper score);
}
