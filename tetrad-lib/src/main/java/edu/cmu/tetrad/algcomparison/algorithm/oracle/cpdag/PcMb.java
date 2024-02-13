package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.ReturnsBootstrapGraphs;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.TakesIndependenceWrapper;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * PC.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "PC-MB",
        command = "pc-mb",
        algoType = AlgType.search_for_Markov_blankets
)
@Bootstrapping
public class PcMb implements Algorithm, HasKnowledge, TakesIndependenceWrapper,
        ReturnsBootstrapGraphs {

    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * The independence test to use.
     */
    private IndependenceWrapper test;

    /**
     * The knowledge.
     */
    private Knowledge knowledge = new Knowledge();

    /**
     * The targets.
     */
    private List<Node> targets;

    /**
     * The bootstrap graphs.
     */
    private List<Graph> bootstrapGraphs = new ArrayList<>();

    /**
     * <p>Constructor for PcMb.</p>
     */
    public PcMb() {
    }

    /**
     * <p>Constructor for PcMb.</p>
     *
     * @param type a {@link edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper} object
     */
    public PcMb(IndependenceWrapper type) {
        this.test = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            IndependenceTest test = this.test.getTest(dataSet, parameters);
            edu.cmu.tetrad.search.PcMb search = new edu.cmu.tetrad.search.PcMb(test, parameters.getInt(Params.DEPTH));
            List<Node> targets = targets(test, parameters.getString(Params.TARGETS));
            this.targets = targets;
            search.setDepth(parameters.getInt(Params.DEPTH));
            search.setKnowledge(this.knowledge);
            search.setFindMb(parameters.getBoolean(Params.MB));
            return search.search(targets);
        } else {
            PcMb algorithm = new PcMb(this.test);

            DataSet data = (DataSet) dataSet;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET), parameters.getInt(Params.BOOTSTRAPPING_NUM_THEADS));
            search.setKnowledge(this.knowledge);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            Graph graph = search.search();
            if (parameters.getBoolean(Params.SAVE_BOOTSTRAP_GRAPHS)) this.bootstrapGraphs = search.getGraphs();
            return graph;
        }
    }

    @NotNull
    private List<Node> targets(IndependenceTest test, String targetString) {
        String[] _targets;

        if (targetString.contains(",")) {
            _targets = targetString.split(",");
        } else {
            _targets = targetString.split(" ");
        }

        List<Node> targets = new ArrayList<>();

        for (String _target : _targets) {
            targets.add(test.getVariable(_target));
        }

        return targets;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        return GraphUtils.markovBlanketSubgraph(targets.get(0), new EdgeListGraph(graph));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "PC-MB (Markov blanket search using PC) using " + this.test.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return this.test.getDataType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.TARGETS);
        parameters.add(Params.MB);
        parameters.add(Params.DEPTH);

        parameters.add(Params.VERBOSE);
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge(knowledge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndependenceWrapper getIndependenceWrapper() {
        return this.test;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIndependenceWrapper(IndependenceWrapper test) {
        this.test = test;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Graph> getBootstrapGraphs() {
        return this.bootstrapGraphs;
    }
}
