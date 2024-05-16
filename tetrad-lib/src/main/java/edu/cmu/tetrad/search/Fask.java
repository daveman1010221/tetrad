///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (c) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataTransforms;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.test.ScoreIndTest;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.SublistGenerator;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.cmu.tetrad.util.StatUtils.*;
import static java.lang.Math.abs;
import static org.apache.commons.math3.util.FastMath.*;

/**
 * Fast adjacency search followed by robust skew orientation. Checks are done for adding two cycles. The two-cycle
 * checks do not require non-Gaussianity. The robust skew orientation of edges left or right does.
 *
 * @author Joseph Ramsey
 */
public final class Fask {
    /**
     * The score to be used for the FAS adjacency search.
     */
    private final Score score;
    /**
     * Data as a double[][].
     */
    private final double[][] data;
    /**
     * The data sets being analyzed. They must all have the same variables and the same number of records.
     */
    private final DataSet dataSet;
    /**
     * An initial graph to orient, skipping the adjacency step.
     */
    private Graph initialGraph = null;
    /**
     * For the Fast Adjacency Search.
     */
    private int depth = -1;
    /**
     * Alpha for orienting 2-cycles. Usually needs to be low.
     */
    private double alpha = 1e-5;
    /**
     * Knowledge the search will obey, of forbidden and required edges.
     */
    private Knowledge knowledge = new Knowledge();
    /**
     * Cutoff for T tests for 2-cycle tests.
     */
    private double cutoff;
    /**
     * True if empirical corrections should be used.
     */
    private boolean empirical = false;
    /**
     * A threshold for including extra adjacencies due to skewness.
     */
    private double extraEdgeThreshold = 0.3;
    /**
     * True if FAS adjacencies should be included in the output.
     */
    private boolean useFasAdjacencies = true;
    /**
     * True if skew adjacencies should be included in the output.
     */
    private boolean useSkewAdjacencies = true;
    /**
     * Threshold for reversing casual judgments for negative coefficients.
     */
    private double delta = -0.1;
    /**
     * The left right rule to use, default FASK1.
     */
    private Fask.LeftRight leftRight = Fask.LeftRight.FASK1;

    /**
     * Constructs a new instance of the FaskOrig class with the given DataSet and Score objects.
     *
     * @param dataSet The DataSet object containing the data.
     * @param score   The Score object representing the scoring algorithm.
     */
    public Fask(DataSet dataSet, Score score) {
        this.dataSet = dataSet;
        this.score = score;
        data = dataSet.getDoubleData().transpose().toArray();
    }

    /**
     * Calculates the expected correlation between two arrays of double values where the condition is greater than 0.
     *
     * @param x         The data for the first variable.
     * @param y         The data for the second variable.
     * @param condition The condition array indicating whether the correlation should be calculated or not.
     * @return The expected correlation between the two arrays of double values.
     */
    private static double cu(double[] x, double[] y, double[] condition) {
        double exy = 0.0;

        int n = 0;

        for (int k = 0; k < x.length; k++) {
            if (condition[k] > 0) {
                exy += x[k] * y[k];
                n++;
            }
        }

        return exy / n;
    }

    /**
     * Calculates a left-right judgment using the robust skewness between two arrays of double values.
     *
     * @param x         The data for the first variable.
     * @param y         The data for the second variable.
     * @param empirical Whether to use an empirical correction to the skewness.
     * @return The robust skewness between the two arrays.
     */
    private static boolean robustSkew(double[] x, double[] y, boolean empirical) {

        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = g(x[i]) * y[i] - x[i] * g(y[i]);
        }

        return correlation(x, y) * mean(lr) > 0;
    }

    /**
     * Calculates a left-right judgment using the skewness of two arrays for double values.
     *
     * @param x         the first array of double values
     * @param y         the second array of double values
     * @param empirical flag to indicate whether to apply empirical correction for skewness
     * @return the skewness of the two arrays
     */
    private static boolean skew(double[] x, double[] y, boolean empirical) {
        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = x[i] * x[i] * y[i] - x[i] * y[i] * y[i];
        }

        return correlation(x, y) * mean(lr) > 0;
    }

    /**
     * Calculates the logarithm of the hyperbolic cosine of the maximum for x and 0.
     *
     * @param x The input value.
     * @return The result of the calculation.
     */
    private static double g(double x) {
        return log(cosh(FastMath.max(x, 0)));
    }

    /**
     * Calculates the expected correlation between two arrays of double values where z is positive.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @param z The data for the third variable used in the correlation calculation.
     * @return The correlation exponent between the two arrays of double values.
     */
    private static double corrExp(double[] x, double[] y, double[] z) {
        return E(x, y, z) / sqrt(E(x, x, z) * E(y, y, z));
    }

    /**
     * Calculates E(xy) for positive values of z.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @param z The data for the third variable used in the correlation calculation.
     * @return The correlation exponent between the two arrays of double values.
     */
    private static double E(double[] x, double[] y, double[] z) {
        double exy = 0.0;
        int n = 0;

        for (int k = 0; k < x.length; k++) {
            if (z[k] > 0) {
                exy += x[k] * y[k];
                n++;
            }
        }

        return exy / n;
    }

    /**
     * Corrects the skewness of the given data using the provided skewness value.
     *
     * @param data The array of data to be corrected.
     * @param sk   The skewness value to be used for correction.
     * @return The corrected data array.
     */
    private static double[] correctSkewness(double[] data, double sk) {
        double[] data2 = new double[data.length];
        for (int i = 0; i < data.length; i++) data2[i] = data[i] * signum(sk);
        return data2;
    }

    /**
     * Runs the search on the concatenated data, returning a graph, possibly cyclic, possibly with two-cycles. Runs the
     * fast adjacency search (FAS, Spirtes et al., 2000) followed by a modification of the robust skew rule (Pairwise
     * Likelihood Ratios for Estimation of Non-Gaussian Structural Equation Models, Smith and Hyvarinen), together with
     * some heuristics for orienting two-cycles.
     *
     * @return the graph. Some edges may be undirected; some adjacencies may be two-cycles.
     */
    public Graph search() {
        setCutoff(alpha);

        DataSet dataSet = DataTransforms.standardizeData(this.dataSet);

        List<Node> variables = dataSet.getVariables();
        double[][] colData = dataSet.getDoubleData().transpose().toArray();
        Graph G0;

        if (initialGraph != null) {
            Graph g1 = new EdgeListGraph(initialGraph.getNodes());

            for (Edge edge : initialGraph.getEdges()) {
                Node x = edge.getNode1();
                Node y = edge.getNode2();

                if (!g1.isAdjacentTo(x, y)) g1.addUndirectedEdge(x, y);
            }

            g1 = GraphUtils.replaceNodes(g1, dataSet.getVariables());
            G0 = g1;
        } else {
            IndependenceTest test = new ScoreIndTest(score, dataSet);
            Fas fas = new Fas(test);
            fas.setStable(true);
            fas.setDepth(depth);
            fas.setVerbose(false);
            fas.setKnowledge(knowledge);
            G0 = fas.search();
        }

        GraphSearchUtils.pcOrientbk(knowledge, G0, G0.getNodes(), false);

        Graph graph = new EdgeListGraph(variables);

        for (int i = 0; i < variables.size(); i++) {
            for (int j = i + 1; j < variables.size(); j++) {
                Node X = variables.get(i);
                Node Y = variables.get(j);

                // Centered
                final double[] x = colData[i];
                final double[] y = colData[j];

                double c1 = StatUtils.cov(x, y, x, 0, +1)[1];
                double c2 = StatUtils.cov(x, y, y, 0, +1)[1];

                if ((useFasAdjacencies && G0.isAdjacentTo(X, Y)) || (useSkewAdjacencies && Math.abs(c1 - c2) > extraEdgeThreshold)) {
                    if (knowledgeOrients(X, Y)) {
                        graph.addDirectedEdge(X, Y);
                    } else if (knowledgeOrients(Y, X)) {
                        graph.addDirectedEdge(Y, X);
                    } else if (bidirected(x, y, G0, X, Y)) {
                        Edge edge1 = Edges.directedEdge(X, Y);
                        Edge edge2 = Edges.directedEdge(Y, X);
                        graph.addEdge(edge1);
                        graph.addEdge(edge2);
                    } else {
                        if (leftRight(x, y)) {
                            graph.addDirectedEdge(X, Y);
                        } else {
                            graph.addDirectedEdge(Y, X);
                        }
                    }
                }
            }
        }

        return graph;
    }

    /**
     * Sets the left-right rule used.
     *
     * @param leftRight The rule.
     * @see Fask.LeftRight
     */
    public void setLeftRight(Fask.LeftRight leftRight) {
        this.leftRight = leftRight;
    }

    /**
     * Sets the significance level at which independence judgments should be made.  Affects the cutoff for partial
     * correlations to be considered statistically equal to zero.
     */
    public void setCutoff(double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range: " + alpha);
        }

        this.cutoff = StatUtils.getZForAlpha(alpha);
    }

    /**
     * Sets the depth of the search for the Fast Adjacency Search.
     *
     * @param depth The depth of the search. A depth of -1 indicates unlimited depth.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Sets the significance level for making independence judgments.
     *
     * @param alpha The significance level value.
     */
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Sets the knowledge object for the current instance.
     *
     * @param knowledge The Knowledge object containing the information to be set.
     */
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    /**
     * Sets the initial graph for the FaskOrig class.
     *
     * @param initialGraph The initial graph to be set.
     */
    public void setInitialGraph(Graph initialGraph) {
        this.initialGraph = initialGraph;
    }

    /**
     * ` Sets the extra-edge threshold for the FaskOrig class.
     *
     * @param extraEdgeThreshold The value to set for the extra-edge threshold.
     */
    public void setExtraEdgeThreshold(double extraEdgeThreshold) {
        this.extraEdgeThreshold = extraEdgeThreshold;
    }

    /**
     * Sets the flag indicating whether to use Fast Adjacencies (FAS) for the search algorithm.
     *
     * @param useFasAdjacencies The flag indicating whether to use FAS.
     */
    public void setUseFasAdjacencies(boolean useFasAdjacencies) {
        this.useFasAdjacencies = useFasAdjacencies;
    }

    /**
     * Sets the flag indicating whether to use skew adjacencies in the FaskOrig class.
     *
     * @param useSkewAdjacencies The flag indicating whether to use skew adjacencies.
     */
    public void setUseSkewAdjacencies(boolean useSkewAdjacencies) {
        this.useSkewAdjacencies = useSkewAdjacencies;
    }

    /**
     * Sets the delta value for the current instance of the FaskOrig class. The delta value affects the skewness
     * correction of the data during the search algorithm.
     *
     * @param delta The delta value to be set.
     */
    public void setDelta(double delta) {
        this.delta = delta;
    }

    /**
     * Sets the empirical flag for the current instance of the FaskOrig class.
     *
     * @param empirical The value indicating whether to use an empirical correction to the skewness.
     */
    public void setEmpirical(boolean empirical) {
        this.empirical = empirical;
    }

    /**
     * Determines if there is a bidirectional edge between two nodes in the graph, considering the given data and a
     * depth level.
     *
     * @param x  The x-values of the data.
     * @param y  The y-values of the data.
     * @param G0 The graph to check for bidirectional edges.
     * @param X  The first node.
     * @param Y  The second node.
     * @return {@code true} if there is a bidirectional edge between {@code X} and {@code Y}, {@code false} otherwise.
     */
    private boolean bidirected(double[] x, double[] y, Graph G0, Node X, Node Y) {

        Set<Node> adjSet = new HashSet<>(G0.getAdjacentNodes(X));
        adjSet.addAll(G0.getAdjacentNodes(Y));
        List<Node> adj = new ArrayList<>(adjSet);
        adj.remove(X);
        adj.remove(Y);

        SublistGenerator gen = new SublistGenerator(adj.size(), Math.min(depth, adj.size()));
        int[] choice;

        while ((choice = gen.next()) != null) {
            List<Node> _adj = GraphUtils.asList(choice, adj);
            double[][] _Z = new double[_adj.size()][];

            for (int f = 0; f < _adj.size(); f++) {
                Node _z = _adj.get(f);
                int column = dataSet.getColumn(_z);
                _Z[f] = data[column];
            }

            double pc = partialCorrelation(x, y, _Z, x, Double.NEGATIVE_INFINITY, +1);
            double pc1 = partialCorrelation(x, y, _Z, x, 0, +1);
            double pc2 = partialCorrelation(x, y, _Z, y, 0, +1);

            int nc = StatUtils.getRows(x, x, Double.NEGATIVE_INFINITY, +1).size();
            int nc1 = StatUtils.getRows(x, x, 0, +1).size();
            int nc2 = StatUtils.getRows(y, y, 0, +1).size();

            double z = 0.5 * (log(1.0 + pc) - log(1.0 - pc));
            double z1 = 0.5 * (log(1.0 + pc1) - log(1.0 - pc1));
            double z2 = 0.5 * (log(1.0 + pc2) - log(1.0 - pc2));

            double zv1 = (z - z1) / sqrt((1.0 / ((double) nc - 3) + 1.0 / ((double) nc1 - 3)));
            double zv2 = (z - z2) / sqrt((1.0 / ((double) nc - 3) + 1.0 / ((double) nc2 - 3)));

            boolean rejected1 = abs(zv1) > cutoff;
            boolean rejected2 = abs(zv2) > cutoff;

            boolean possibleTwoCycle = false;

            if (zv1 < 0 && zv2 > 0 && rejected1) {
                possibleTwoCycle = true;
            } else if (zv1 > 0 && zv2 < 0 && rejected2) {
                possibleTwoCycle = true;
            } else if (rejected1 && rejected2) {
                possibleTwoCycle = true;
            }

            if (!possibleTwoCycle) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates a left-right judgment using the given arrays of double values.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @return True if the left-right judgment is positive, false if right-left.
     */
    private boolean leftRight(double[] x, double[] y) {
        if (leftRight == Fask.LeftRight.FASK1) {
            return leftRightV1(x, y);
        } else if (leftRight == Fask.LeftRight.FASK2) {
            return leftRightV2(x, y);
        } else if (leftRight == Fask.LeftRight.SKEW) {
            return skew(x, y, empirical);
        } else if (leftRight == Fask.LeftRight.RSKEW) {
            return robustSkew(x, y, empirical);
        } else if (leftRight == Fask.LeftRight.TANH) {
            return tanh(x, y, empirical);
        } else {
            throw new IllegalArgumentException("Unknown left-right rule: " + leftRight);
        }
    }

    /**
     * Calculates a left-right judgment using the given arrays of double values.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @return True if the left-right judgment is positive, false otherwise.
     */
    private boolean leftRightV1(double[] x, double[] y) {
        double left = cu(x, y, x) / (sqrt(cu(x, x, x) * cu(y, y, x)));
        double right = cu(x, y, y) / (sqrt(cu(x, x, y) * cu(y, y, y)));
        double lr = left - right;

        double r = StatUtils.correlation(x, y);
        double sx = StatUtils.skewness(x);
        double sy = StatUtils.skewness(y);

        r *= signum(sx) * signum(sy);
        lr *= signum(r);
        if (r < delta) lr *= -1;

        return lr > 0;
    }

    /**
     * Calculates a left-right judgment using the difference of corrExp values between two arrays of double values.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @return True if the corrExp value of the first variable is greater than the corrExp value of the second variable,
     * false otherwise.
     */
    private boolean leftRightV2(double[] x, double[] y) {
        return corrExp(x, y, x) - corrExp(x, y, y) > 0;
    }

    /**
     * Calculates a left-right judgment using the hyperbolic tangent of each element in the given arrays and performs a
     * computation combining these results.
     *
     * @param x         an array of doubles
     * @param y         an array of doubles
     * @param empirical flag indicating whether empirical correction should be applied to the input arrays
     * @return the final result of the computation
     */
    private boolean tanh(double[] x, double[] y, boolean empirical) {

        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = x[i] * FastMath.tanh(y[i]) - FastMath.tanh(x[i]) * y[i];
        }

        return correlation(x, y) * mean(lr) > 0;
    }

    /**
     * Computes the partial correlation coefficient between two variables, given a set of control variables. The partial
     * correlation coefficient measures the linear relationship between two variables after removing the effect of
     * control variables.
     *
     * @param x         the values of the first variable
     * @param y         the values of the second variable
     * @param z         a matrix containing the values of the control variables. Each row represents an observation, and
     *                  each column represents a control variable.
     * @param condition an array containing the conditions for each observation. This is used to determine which
     *                  observations should be included in the computation.
     * @param threshold the threshold value for inclusion of observations. Only observations with a condition value
     *                  greater than or equal to the threshold will be included.
     * @param direction the direction of the relationship to consider for the partial correlation. A positive value
     *                  indicates a positive relationship, a negative value indicates a negative relationship, and zero
     *                  indicates no preference.
     * @return the partial correlation coefficient between variables x and y, after removing the effect of control
     * variables z
     * @throws SingularMatrixException if the covariance matrix of the variables is singular, indicating a perfect
     *                                 linear dependence between the variables
     */
    private double partialCorrelation(double[] x, double[] y, double[][] z, double[] condition, double threshold,
                                      double direction) throws SingularMatrixException {
        double[][] cv = StatUtils.covMatrix(x, y, z, condition, threshold, direction);
        Matrix m = new Matrix(cv).transpose();
        return StatUtils.partialCorrelation(m);
    }

    /**
     * Determines if the knowledge orients from the left node to the right node.
     *
     * @param left  the left node
     * @param right the right node
     * @return true if the knowledge orients from the left node to the right node, otherwise false
     */
    private boolean knowledgeOrients(Node left, Node right) {
        return knowledge.isForbidden(right.getName(), left.getName()) || knowledge.isRequired(left.getName(), right.getName());
    }

    /**
     * Enumerates the options left-right rules to use for FASK. Options include the FASK left-right rule and three
     * left-right rules from the Hyvarinen and Smith pairwise orientation paper: Robust Skew, Skew, and Tanh. In that
     * paper, "empirical" versions were given in which the variables are multiplied through by the signs of the
     * skewnesses; we follow this advice here (with good results). These others are provided for those who prefer them.
     */
    public enum LeftRight {
        /**
         * The original FASK left-right rule.
         */
        FASK1,

        /**
         * The modified FASK left-right rule.
         */
        FASK2,

        /**
         * The robust skew rule from the Hyvarinen and Smith paper.
         */
        RSKEW,

        /**
         * The skew rule from the Hyvarinen and Smith paper.
         */
        SKEW,

        /**
         * The tanh rule from the Hyvarinen and Smith paper.
         */
        TANH
    }

    /**
     * Enumerates the alternatives to use for finding the initial adjacencies for FASK.
     */
    public enum AdjacencyMethod {

        /**
         * Fast Adjacency Search (FAS) with the stable option.
         */
        FAS_STABLE,

        /**
         * FGES with the BIC score.
         */
        FGES,

        /**
         * A permutation search with the BOSS algorithm.
         */
        BOSS,

        /**
         * A permutation search with the GRASP algorithm.
         */
        GRASP,

        /**
         * Use an external graph.
         */
        EXTERNAL_GRAPH,

        /**
         * No initial adjacencies.
         */
        NONE
    }
}





