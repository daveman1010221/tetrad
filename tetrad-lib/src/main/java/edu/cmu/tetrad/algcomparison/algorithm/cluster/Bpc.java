package edu.cmu.tetrad.algcomparison.algorithm.cluster;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.Mimbuild;
import edu.cmu.tetrad.search.utils.BpcTestType;
import edu.cmu.tetrad.search.utils.ClusterSignificance;
import edu.cmu.tetrad.search.utils.ClusterUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetrad.util.TetradLogger;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Build Pure Clusters.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "BPC",
        command = "bpc",
        algoType = AlgType.search_for_structure_over_latents
)
@Bootstrapping
public class Bpc implements Algorithm, ClusterAlgorithm {

    private static final long serialVersionUID = 23L;

    /**
     * Constructs a new BPC algorithm.
     */
    public Bpc() {
    }

    /**
     * {@inheritDoc}
     *
     * Runs the BPC algorithm.
     */
    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            boolean precomputeCovariances = parameters.getBoolean(Params.PRECOMPUTE_COVARIANCES);

            ICovarianceMatrix cov = SimpleDataLoader.getCovarianceMatrix(dataSet, precomputeCovariances);
            double alpha = parameters.getDouble(Params.ALPHA);

            boolean wishart = parameters.getBoolean(Params.USE_WISHART, true);
            BpcTestType testType;

            if (wishart) {
                testType = BpcTestType.TETRAD_WISHART;
            } else {
                testType = BpcTestType.TETRAD_DELTA;
            }

            edu.cmu.tetrad.search.Bpc bpc = new edu.cmu.tetrad.search.Bpc(cov, alpha, testType);

            if (parameters.getInt(Params.CHECK_TYPE) == 1) {
                bpc.setCheckType(ClusterSignificance.CheckType.Significance);
            } else if (parameters.getInt(Params.CHECK_TYPE) == 2) {
                bpc.setCheckType(ClusterSignificance.CheckType.Clique);
            } else if (parameters.getInt(Params.CHECK_TYPE) == 3) {
                bpc.setCheckType(ClusterSignificance.CheckType.None);
            } else {
                throw new IllegalArgumentException("Unexpected check type");
            }

            Graph graph = bpc.search();

            if (!parameters.getBoolean(Params.INCLUDE_STRUCTURE_MODEL)) {
                return graph;
            } else {

                Clusters clusters = ClusterUtils.mimClusters(graph);

                Mimbuild mimbuild = new Mimbuild();
                mimbuild.setPenaltyDiscount(parameters.getDouble(Params.PENALTY_DISCOUNT));
                mimbuild.setKnowledge((Knowledge) parameters.get("knowledge", new Knowledge()));

                if (parameters.getBoolean("includeThreeClusters", true)) {
                    mimbuild.setMinClusterSize(3);
                } else {
                    mimbuild.setMinClusterSize(4);
                }

                List<List<Node>> partition = ClusterUtils.clustersToPartition(clusters, dataSet.getVariables());
                List<String> latentNames = new ArrayList<>();

                for (int i = 0; i < clusters.getNumClusters(); i++) {
                    latentNames.add(clusters.getClusterName(i));
                }

                Graph structureGraph = mimbuild.search(partition, latentNames, cov);
                LayoutUtil.defaultLayout(structureGraph);
                LayoutUtil.fruchtermanReingoldLayout(structureGraph);

                ICovarianceMatrix latentsCov = mimbuild.getLatentsCov();

                TetradLogger.getInstance().log("details", "Latent covs = \n" + latentsCov);

                Graph fullGraph = mimbuild.getFullGraph();
                LayoutUtil.defaultLayout(fullGraph);
                LayoutUtil.fruchtermanReingoldLayout(fullGraph);

                return fullGraph;
            }
        } else {
            Bpc algorithm = new Bpc();

            DataSet data = (DataSet) dataSet;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm,
                    parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE),
                    parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT),
                    parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Returns the true graph if there is one.
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        Graph dag = new EdgeListGraph(graph);
        return GraphTransforms.cpdagForDag(dag);
    }

    /**
     * {@inheritDoc}
     *
     * Returns the description of the algorithm.
     */
    @Override
    public String getDescription() {
        return "BPC (Build Pure Clusters)";
    }

    /**
     * {@inheritDoc}
     *
     * Returns the data type that the algorithm can handle.
     */
    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    /**
     * {@inheritDoc}
     *
     * Returns the parameters for the algorithm.
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.ALPHA);
        parameters.add(Params.PENALTY_DISCOUNT);
        parameters.add(Params.USE_WISHART);
        parameters.add(Params.INCLUDE_STRUCTURE_MODEL);
        parameters.add(Params.CHECK_TYPE);
        parameters.add(Params.PRECOMPUTE_COVARIANCES);
        parameters.add(Params.VERBOSE);

        return parameters;
    }
}
