package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.score.SemBicScoreDeterministic;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.annotation.Experimental;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.Score;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.*;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;
import edu.pitt.dbmi.algo.resampling.ResamplingEdgeEnsemble;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.sqrt;

/**
 * FGES (the heuristic version).
 *
 * @author jdramsey
 */
@Bootstrapping
@Experimental
public class GesMe implements Algorithm {

    static final long serialVersionUID = 23L;
    private boolean compareToTrue;
    private final ScoreWrapper score = new SemBicScoreDeterministic();

    public GesMe() {
        setCompareToTrue(false);
    }

    public GesMe(final boolean compareToTrueGraph) {
        setCompareToTrue(compareToTrueGraph);
    }

    @Override
    public Graph search(final DataModel dataSet, final Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
//          dataSet = DataUtils.center((DataSet) dataSet);
            final CovarianceMatrix covarianceMatrix = new CovarianceMatrix((DataSet) dataSet);

            final edu.cmu.tetrad.search.FactorAnalysis analysis = new edu.cmu.tetrad.search.FactorAnalysis(covarianceMatrix);
            analysis.setThreshold(parameters.getDouble("convergenceThreshold"));
            analysis.setNumFactors(parameters.getInt("numFactors"));
//            analysis.setNumFactors(((DataSet) dataSet).getNumColumns());

            final Matrix unrotated = analysis.successiveResidual();
            final Matrix rotated = analysis.successiveFactorVarimax(unrotated);

            if (parameters.getBoolean(Params.VERBOSE)) {
                final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

                String output = "Unrotated Factor Loading Matrix:\n";

                output += tableString(unrotated, nf, Double.POSITIVE_INFINITY);

                if (unrotated.columns() != 1) {
                    output += "\n\nRotated Matrix (using sequential varimax):\n";
                    output += tableString(rotated, nf, parameters.getDouble("fa_threshold"));
                }

                System.out.println(output);
                TetradLogger.getInstance().forceLogMessage(output);
            }

            final Matrix L;

            if (parameters.getBoolean("useVarimax")) {
                L = rotated;
            } else {
                L = unrotated;
            }


            final Matrix residual = analysis.getResidual();

            final ICovarianceMatrix covFa = new CovarianceMatrix(covarianceMatrix.getVariables(), L.times(L.transpose()),
                    covarianceMatrix.getSampleSize());

            System.out.println(covFa);

            final double[] vars = covarianceMatrix.getMatrix().diag().toArray();
            final List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < vars.length; i++) {
                indices.add(i);
            }

            Collections.sort(indices, new Comparator<Integer>() {
                @Override
                public int compare(final Integer o1, final Integer o2) {
                    return -Double.compare(vars[o1], vars[o2]);
                }
            });

            final NumberFormat nf = new DecimalFormat("0.000");

            for (int i = 0; i < indices.size(); i++) {
                System.out.println(nf.format(vars[indices.get(i)]) + " ");
            }

            System.out.println();

            final int n = vars.length;

            final int cutoff = (int) (n * ((sqrt(8 * n + 1) - 1) / (2 * n)));

            final List<Node> nodes = covarianceMatrix.getVariables();

            final List<Node> leaves = new ArrayList<>();

            for (int i = 0; i < cutoff; i++) {
                leaves.add(nodes.get(indices.get(i)));
            }

            final IKnowledge knowledge2 = new Knowledge2();

            for (final Node v : nodes) {
                if (leaves.contains(v)) {
                    knowledge2.addToTier(2, v.getName());
                } else {
                    knowledge2.addToTier(1, v.getName());
                }
            }

            knowledge2.setTierForbiddenWithin(2, true);

            System.out.println("knowledge2 = " + knowledge2);

            final Score score = this.score.getScore(covFa, parameters);

            final edu.cmu.tetrad.search.Fges search = new edu.cmu.tetrad.search.Fges(score);
            search.setFaithfulnessAssumed(parameters.getBoolean(Params.FAITHFULNESS_ASSUMED));

            if (parameters.getBoolean("enforceMinimumLeafNodes")) {
                search.setKnowledge(knowledge2);
            }

            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            search.setMaxDegree(parameters.getInt(Params.MAX_DEGREE));
            search.setSymmetricFirstStep(parameters.getBoolean(Params.SYMMETRIC_FIRST_STEP));

            final Object obj = parameters.get(Params.PRINT_STREAM);
            if (obj instanceof PrintStream) {
                search.setOut((PrintStream) obj);
            }

            if (parameters.getBoolean(Params.VERBOSE)) {
//                NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();
                String output = "Unrotated Factor Loading Matrix:\n";
                final double threshold = parameters.getDouble("fa_threshold");

                output += tableString(L, nf, Double.POSITIVE_INFINITY);

                if (L.columns() != 1) {
                    output += "\n\nL:\n";
                    output += tableString(L, nf, threshold);
                }

                System.out.println(output);
                TetradLogger.getInstance().forceLogMessage(output);
            }

            System.out.println("residual = " + residual);

            return search.search();
        } else {
            final GesMe algorithm = new GesMe(this.compareToTrue);

            final DataSet data = (DataSet) dataSet;
            final GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING));

            search.setPercentResampleSize(parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE));
            search.setResamplingWithReplacement(parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT));

            ResamplingEdgeEnsemble edgeEnsemble = ResamplingEdgeEnsemble.Highest;
            switch (parameters.getInt(Params.RESAMPLING_ENSEMBLE, 1)) {
                case 0:
                    edgeEnsemble = ResamplingEdgeEnsemble.Preserved;
                    break;
                case 1:
                    edgeEnsemble = ResamplingEdgeEnsemble.Highest;
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
        if (this.compareToTrue) {
            return new EdgeListGraph(graph);
        } else {
            return SearchGraphUtils.cpdagForDag(new EdgeListGraph(graph));
        }
    }

    @Override
    public String getDescription() {
        return "FGES (Fast Greedy Equivalence Search) using " + this.score.getDescription();
    }

    @Override
    public DataType getDataType() {
        return this.score.getDataType();
    }

    @Override
    public List<String> getParameters() {
        final List<String> parameters = new ArrayList<>();
        parameters.add(Params.SYMMETRIC_FIRST_STEP);
        parameters.add(Params.FAITHFULNESS_ASSUMED);
        parameters.add(Params.MAX_DEGREE);
        parameters.add(Params.DETERMINISM_THRESHOLD);
        parameters.add("convergenceThreshold");
        parameters.add("fa_threshold");
        parameters.add("numFactors");
        parameters.add("useVarimax");
        parameters.add("enforceMinimumLeafNodes");

        parameters.add(Params.VERBOSE);

        return parameters;
    }

    //    @Override
//    public IKnowledge getKnowledge() {
//        return knowledge;
//    }
//
//    @Override
//    public void setKnowledge(IKnowledge knowledge) {
//        this.knowledge = knowledge;
//    }
    public void setCompareToTrue(final boolean compareToTrue) {
        this.compareToTrue = compareToTrue;
    }

    private String tableString(final Matrix matrix, final NumberFormat nf, final double threshold) {
        final TextTable table = new TextTable(matrix.rows() + 1, matrix.columns() + 1);

        for (int i = 0; i < matrix.rows() + 1; i++) {
            for (int j = 0; j < matrix.columns() + 1; j++) {
                if (i > 0 && j == 0) {
                    table.setToken(i, 0, "X" + i);
                } else if (i == 0 && j > 0) {
                    table.setToken(0, j, "Factor " + j);
                } else if (i > 0 && j > 0) {
                    final double coefficient = matrix.get(i - 1, j - 1);
                    String token = !Double.isNaN(coefficient) ? nf.format(coefficient) : "Undefined";
                    token += Math.abs(coefficient) > threshold ? "*" : " ";
                    table.setToken(i, j, token);
                }
            }
        }

        return "\n" + table;

    }

}
