package edu.cmu.tetrad.algcomparison.score;

import edu.cmu.tetrad.algcomparison.utils.HasParameters;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradSerializable;

import java.io.Serial;
import java.util.List;

/**
 * Interface that algorithm must implement.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public interface ScoreWrapper extends HasParameters, TetradSerializable {
    /**
     * Constant <code>serialVersionUID=23L</code>
     */
    @Serial
    long serialVersionUID = 23L;

    /**
     * Returns true, iff x and y are independent, conditional on z for the given data set.
     *
     * @param dataSet    The data set to test independence against.
     * @param parameters The parameters of the test.
     * @return True iff independence holds.
     */
    Score getScore(DataModel dataSet, Parameters parameters);

    /**
     * Returns a short of this independence test.
     *
     * @return This description.
     */
    String getDescription();

    /**
     * Returns the data type that the search requires, whether continuous, discrete, or mixed.
     *
     * @return This type.
     */
    DataType getDataType();

    /**
     * Returns the parameters that this search uses.
     *
     * @return A list for String names of parameters.
     */
    List<String> getParameters();

    /**
     * Returns the variable with the given name.
     *
     * @param name the name.
     * @return the variable.
     */
    Node getVariable(String name);

}
