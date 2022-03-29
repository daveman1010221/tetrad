package edu.cmu.tetrad.algcomparison.algorithm.cluster;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.*;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetrad.util.TetradLogger;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;
import edu.pitt.dbmi.algo.resampling.ResamplingEdgeEnsemble;

import java.util.ArrayList;
import java.util.List;

/**
 * Find One Factor Clusters.
 *
 * @author jdramsey
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "FOFC",
        command = "fofc",
        algoType = AlgType.search_for_structure_over_latents,
        dataType = DataType.Continuous
)
@Bootstrapping
public class Fofc implements Algorithm, HasKnowledge, ClusterAlgorithm {

    static final long serialVersionUID = 23L;
    private IKnowledge knowledge = new Knowledge2();

    public Fofc() {
    }

    @Override
    public Graph search(final DataModel dataSet, final Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            final ICovarianceMatrix cov = DataUtils.getCovMatrix(dataSet);
            final double alpha = parameters.getDouble(Params.ALPHA);

            final boolean wishart = parameters.getBoolean(Params.USE_WISHART, true);
            final TestType testType;

            if (wishart) {
                testType = TestType.TETRAD_WISHART;
            } else {
                testType = TestType.TETRAD_DELTA;
            }

            final boolean gap = parameters.getBoolean(Params.USE_GAP, true);
            final FindOneFactorClusters.Algorithm algorithm;

            if (gap) {
                algorithm = FindOneFactorClusters.Algorithm.GAP;
            } else {
                algorithm = FindOneFactorClusters.Algorithm.SAG;
            }

            final edu.cmu.tetrad.search.FindOneFactorClusters search
                    = new edu.cmu.tetrad.search.FindOneFactorClusters(cov, testType, algorithm, alpha);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));

            final Graph graph = search.search();

            if (!parameters.getBoolean(Params.INCLUDE_STRUCTURE_MODEL)) {
                return graph;
            } else {

                final Clusters clusters = ClusterUtils.mimClusters(graph);

                final Mimbuild mimbuild = new Mimbuild();
                mimbuild.setPenaltyDiscount(parameters.getDouble(Params.PENALTY_DISCOUNT));
                mimbuild.setKnowledge((IKnowledge) parameters.get("knowledge", new Knowledge2()));

                if (parameters.getBoolean("includeThreeClusters", true)) {
                    mimbuild.setMinClusterSize(3);
                } else {
                    mimbuild.setMinClusterSize(4);
                }

                final List<List<Node>> partition = ClusterUtils.clustersToPartition(clusters, dataSet.getVariables());
                final List<String> latentNames = new ArrayList<>();

                for (int i = 0; i < clusters.getNumClusters(); i++) {
                    latentNames.add(clusters.getClusterName(i));
                }

                final Graph structureGraph = mimbuild.search(partition, latentNames, cov);
                GraphUtils.circleLayout(structureGraph, 200, 200, 150);
                GraphUtils.fruchtermanReingoldLayout(structureGraph);

                final ICovarianceMatrix latentsCov = mimbuild.getLatentsCov();

                TetradLogger.getInstance().log("details", "Latent covs = \n" + latentsCov);

                final Graph fullGraph = mimbuild.getFullGraph();
                GraphUtils.circleLayout(fullGraph, 200, 200, 150);
                GraphUtils.fruchtermanReingoldLayout(fullGraph);

                return fullGraph;
            }
        } else {
            final Fofc algorithm = new Fofc();

            final DataSet data = (DataSet) dataSet;
            final GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING));
            search.setKnowledge(this.knowledge);

            search.setPercentResampleSize(parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE));
            search.setResamplingWithReplacement(parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT));

            ResamplingEdgeEnsemble edgeEnsemble = ResamplingEdgeEnsemble.Highest;
            switch (parameters.getInt(Params.RESAMPLING_ENSEMBLE, 1)) {
                case 0:
                    edgeEnsemble = ResamplingEdgeEnsemble.Preserved;
                    break;
                case 1:
                    break;
                case 2:
                    edgeEnsemble = ResamplingEdgeEnsemble.Majority;
            }

            search.setEdgeEnsemble(edgeEnsemble);
            search.setAddOriginalDataset(parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    @Override
    public Graph getComparisonGraph(final Graph graph) {
        return SearchGraphUtils.cpdagForDag(graph);
    }

    @Override
    public String getDescription() {
        return "FOFC (Find One Factor Clusters)";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        final List<String> parameters = new ArrayList<>();
        parameters.add(Params.PENALTY_DISCOUNT);
        parameters.add(Params.USE_WISHART);
        parameters.add(Params.USE_GAP);
        parameters.add(Params.INCLUDE_STRUCTURE_MODEL);
        parameters.add(Params.VERBOSE);

        return parameters;
    }

    @Override
    public IKnowledge getKnowledge() {
        return this.knowledge;
    }

    @Override
    public void setKnowledge(final IKnowledge knowledge) {
        this.knowledge = knowledge;
    }
}
