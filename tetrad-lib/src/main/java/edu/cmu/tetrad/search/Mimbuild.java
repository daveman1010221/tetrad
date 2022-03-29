///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
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

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.Matrix;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.sqrt;

/**
 * An implemetation of Mimbuild based on the Fgsl score. The search will attempt a GES search first and if that
 * throws and exception then a CPC search. The penalty penaltyDiscount parameter is for the GES search; the alpha
 * value is for the CPC search. Or you can just grab the latent covariance matrix and run whatever search you
 * want to. (I don't know why GES sometimes fails, it is a mystery.)
 * </p>
 * Uses a different (better) algorithm from Mimbuild. Preferable.
 *
 * @author Joseph Ramsey
 */
public class Mimbuild {

    /**
     * The clustering from BPC or equivalent. Small clusters are removed.
     */
    private List<List<Node>> clustering;

    /**
     * The graph over the latents.
     */
    private Graph structureGraph;

//    /**
//     * The alpha level used for CPC
//     */
//    private double alpha = 0.001;

    /**
     * Background knowledge for CPC.
     */
    private IKnowledge knowledge = new Knowledge2();

    /**
     * The estimated covariance matrix over the latents.
     */
    private ICovarianceMatrix latentsCov;

    /**
     * The minimum function (Fgsl) value achieved.
     */
    private double minimum;

    /**
     * The p value of the optimization.
     */
    private double pValue;
    private int numParams;
    private List<Node> latents;
    private double epsilon = 1e-4;
    private double penaltyDiscount = 1;
    private int minClusterSize = 3;

    public Mimbuild() {
    }

    //=================================== PUBLIC METHODS =========================================//

    public Graph search(List<List<Node>> clustering, List<String> latentNames, ICovarianceMatrix measuresCov) {
        List<String> _latentNames = new ArrayList<>(latentNames);

        List<String> allVarNames = new ArrayList<>();

        for (List<Node> cluster : clustering) {
            for (Node node : cluster) allVarNames.add(node.getName());
        }

        measuresCov = measuresCov.getSubmatrix(allVarNames);

        List<List<Node>> _clustering = new ArrayList<>();

        for (List<Node> cluster : clustering) {
            List<Node> _cluster = new ArrayList<>();

            for (Node node : cluster) {
                _cluster.add(measuresCov.getVariable(node.getName()));
            }

            _clustering.add(_cluster);
        }

        List<Node> latents = defineLatents(_latentNames);
        this.latents = latents;

        // This removes the small clusters and their names.
        removeSmallClusters(latents, _clustering, getMinClusterSize());
        this.clustering = _clustering;

        Node[][] indicators = new Node[latents.size()][];

        for (int i = 0; i < latents.size(); i++) {
            indicators[i] = new Node[_clustering.get(i).size()];

            for (int j = 0; j < _clustering.get(i).size(); j++) {
                indicators[i][j] = _clustering.get(i).get(j);
            }
        }

        Matrix cov = getCov(measuresCov, latents, indicators);
        CovarianceMatrix latentscov = new CorrelationMatrix(latents, cov, measuresCov.getSampleSize());
        this.latentsCov = latentscov;
        Graph graph;

        SemBicScore score = new SemBicScore(latentscov);
        score.setPenaltyDiscount(penaltyDiscount);
        Grasp search = new Grasp(score);
        search.setKnowledge(knowledge);
        search.bestOrder(latentscov.getVariables());
        graph = search.getGraph(true);

//        try {
//            Ges search = new Ges(latentscov);
//            search.setDepErrorsAlpha(penaltyDiscount);
//            search.setKnowledge(knowledge);
//            graph = search.search();
//        } catch (Exception e) {
////            e.printStackTrace();
//            CPC search = new CPC(new IndTestFisherZ(latentscov, alpha));
//            search.setKnowledge(knowledge);
//            graph = search.search();
//        }

        this.structureGraph = new EdgeListGraph(graph);
        GraphUtils.fruchtermanReingoldLayout(this.structureGraph);

        return this.structureGraph;
    }

    public List<List<Node>> getClustering() {
        return clustering;
    }

//    public double getAlpha() {
//        return alpha;
//    }

//    public void setAlpha(double alpha) {
//        this.alpha = alpha;
//    }

    public IKnowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(IKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public ICovarianceMatrix getLatentsCov() {
        return this.latentsCov;
    }

    public List<String> getLatentNames(List<Node> latents) {
        List<String> latentNames = new ArrayList<>();

        for (Node node : latents) {
            latentNames.add(node.getName());
        }

        return latentNames;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getpValue() {
        return pValue;
    }

    /**
     * @return the allowUnfaithfulness discovered graph, with latents and indicators.
     */
    public Graph getFullGraph() {
        Graph graph = new EdgeListGraph(structureGraph);

        for (int i = 0; i < this.latents.size(); i++) {
            Node latent = this.latents.get(i);
            List<Node> measuredGuys = getClustering().get(i);

            for (Node measured : measuredGuys) {
                if (!graph.containsNode(measured)) {
                    graph.addNode(measured);
                }

                graph.addDirectedEdge(latent, measured);
            }
        }

        return graph;
    }

    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Parameter convergence threshold. Default = 1e-4.
     */
    public void setEpsilon(double epsilon) {
        if (epsilon < 0) throw new IllegalArgumentException("Epsilon mut be >= 0: " + epsilon);
        this.epsilon = epsilon;
    }

    public void setPenaltyDiscount(double penaltyDiscount) {
        this.penaltyDiscount = penaltyDiscount;
    }

    //=================================== PRIVATE METHODS =========================================//

    private List<Node> defineLatents(List<String> names) {
        List<Node> latents = new ArrayList<>();

        for (String name : names) {
            Node node = new GraphNode(name);
            node.setNodeType(NodeType.LATENT);
            latents.add(node);
        }

        return latents;
    }

    private void removeSmallClusters(List<Node> latents, List<List<Node>> clustering, int minimumSize) {
        for (int i = new ArrayList<>(latents).size() - 1; i >= 0; i--) {
            if (clustering.get(i).size() < minimumSize) {
                clustering.remove(clustering.get(i));
                latents.remove(latents.get(i));
            }
        }
    }

    private Matrix getCov(ICovarianceMatrix _measurescov, List<Node> latents, Node[][] indicators) {
        if (latents.size() != indicators.length) {
            throw new IllegalArgumentException();
        }

        Matrix measurescov = _measurescov.getMatrix();
        Matrix latentscov = new Matrix(latents.size(), latents.size());

        for (int i = 0; i < latentscov.rows(); i++) {
            for (int j = i; j < latentscov.columns(); j++) {
                if (i == j) latentscov.set(i, j, 1.0);
                else {
                    double v = .5;
                    latentscov.set(i, j, v);
                    latentscov.set(j, i, v);
                }
            }
        }

        double[][] loadings = new double[indicators.length][];

        for (int i = 0; i < indicators.length; i++) {
            loadings[i] = new double[indicators[i].length];
        }

        for (int i = 0; i < indicators.length; i++) {
            loadings[i] = new double[indicators[i].length];

            for (int j = 0; j < indicators[i].length; j++) {
                loadings[i][j] = .5;
            }
        }

        int[][] indicatorIndices = new int[indicators.length][];
        List<Node> measures = _measurescov.getVariables();

        for (int i = 0; i < indicators.length; i++) {
            indicatorIndices[i] = new int[indicators[i].length];

            for (int j = 0; j < indicators[i].length; j++) {
                indicatorIndices[i][j] = measures.indexOf(indicators[i][j]);
            }
        }

        // Variances of the measures.
        double[] delta = new double[measurescov.rows()];

        Arrays.fill(delta, 1);

        int numNonMeasureVarianceParams = 0;

        for (int i = 0; i < latentscov.rows(); i++) {
            for (int j = i; j < latentscov.columns(); j++) {
                numNonMeasureVarianceParams++;
            }
        }

        for (Node[] indicator : indicators) {
            numNonMeasureVarianceParams += indicator.length;
        }

        double[] allParams1 = getAllParams(indicators, latentscov, loadings, delta);

        optimizeNonMeasureVariancesQuick(indicators, measurescov, latentscov, loadings, indicatorIndices);

//        for (int i = 0; i < 10; i++) {
//            optimizeNonMeasureVariancesConditionally(indicators, measurescov, latentscov, loadings, indicatorIndices, delta);
//            optimizeMeasureVariancesConditionally(measurescov, latentscov, loadings, indicatorIndices, delta);
//
//            double[] allParams2 = getAllParams(indicators, latentscov, loadings, delta);
//            if (distance(allParams1, allParams2) < epsilon) break;
//            allParams1 = allParams2;
//        }

        this.numParams = allParams1.length;

//        // Very slow but could be done alone.
        optimizeAllParamsSimultaneously(indicators, measurescov, latentscov, loadings, indicatorIndices, delta);

        double N = _measurescov.getSampleSize();
        int p = _measurescov.getDimension();

        int df = (p) * (p + 1) / 2 - (numParams);
        double x = (N - 1) * minimum;

        if (df < 1) throw new IllegalStateException(
                "The degrees of freedom for this model was calculated to be less than 1. Perhaps the model is " +
                        "\nnot a multiple indicator model or doesn't have enough pure measurments.");

        this.pValue = 1.0 - new ChiSquaredDistribution(df).cumulativeProbability(x);

        return latentscov;
    }

    private double distance(double[] allParams1, double[] allParams2) {
        double sum = 0;

        for (int i = 0; i < allParams1.length; i++) {
            double diff = allParams1[i] - allParams2[i];
            sum += diff * diff;
        }

        return sqrt(sum);
    }

    private void optimizeNonMeasureVariancesQuick(Node[][] indicators, Matrix measurescov, Matrix latentscov,
                                                  double[][] loadings, int[][] indicatorIndices) {
        int count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                count++;
            }
        }

        for (Node[] indicator : indicators) {
            for (int j = 0; j < indicator.length; j++) {
                count++;
            }
        }

        double[] values = new double[count];
        count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                values[count++] = latentscov.get(i, j);
            }
        }

        for (int i = 0; i < indicators.length; i++) {
            for (int j = 0; j < indicators[i].length; j++) {
                values[count++] = loadings[i][j];
            }
        }

        Function1 function1 = new Function1(indicatorIndices, measurescov, loadings, latentscov, count);
        MultivariateOptimizer search = new PowellOptimizer(1e-7, 1e-7);

        PointValuePair pair = search.optimize(
                new InitialGuess(values),
                new ObjectiveFunction(function1),
                GoalType.MINIMIZE,
                new MaxEval(100000));

        minimum = pair.getValue();
    }

    private void optimizeNonMeasureVariancesConditionally(Node[][] indicators, Matrix measurescov,
                                                          Matrix latentscov, double[][] loadings,
                                                          int[][] indicatorIndices, double[] delta) {
        int count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                count++;
            }
        }

        for (Node[] indicator : indicators) {
            for (int j = 0; j < indicator.length; j++) {
                count++;
            }
        }

        double[] values3 = new double[count];
        count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                values3[count] = latentscov.get(i, j);
                count++;
            }
        }

        for (int i = 0; i < indicators.length; i++) {
            for (int j = 0; j < indicators[i].length; j++) {
                values3[count] = loadings[i][j];
                count++;
            }
        }

        Function2 function2 = new Function2(indicatorIndices, measurescov, loadings, latentscov, delta, count);
        MultivariateOptimizer search = new PowellOptimizer(1e-7, 1e-7);

        PointValuePair pair = search.optimize(
                new InitialGuess(values3),
                new ObjectiveFunction(function2),
                GoalType.MINIMIZE,
                new MaxEval(100000));

        minimum = pair.getValue();
    }

    private void optimizeMeasureVariancesConditionally(Matrix measurescov, Matrix latentscov, double[][] loadings,
                                                       int[][] indicatorIndices, double[] delta) {
        double[] values2 = new double[delta.length];
        int count = 0;

        for (double v : delta) {
            values2[count++] = v;
        }

        Function2 function2 = new Function2(indicatorIndices, measurescov, loadings, latentscov, delta, count);
        MultivariateOptimizer search = new PowellOptimizer(1e-7, 1e-7);

        PointValuePair pair = search.optimize(
                new InitialGuess(values2),
                new ObjectiveFunction(function2),
                GoalType.MINIMIZE,
                new MaxEval(100000));

        minimum = pair.getValue();
    }

    public int getNumParams() {
        return numParams;
    }

    private void optimizeAllParamsSimultaneously(Node[][] indicators, Matrix measurescov,
                                                 Matrix latentscov, double[][] loadings,
                                                 int[][] indicatorIndices, double[] delta) {
        double[] values = getAllParams(indicators, latentscov, loadings, delta);

        Function4 function = new Function4(indicatorIndices, measurescov, loadings, latentscov, delta);
        MultivariateOptimizer search = new PowellOptimizer(1e-7, 1e-7);

        PointValuePair pair = search.optimize(
                new InitialGuess(values),
                new ObjectiveFunction(function),
                GoalType.MINIMIZE,
                new MaxEval(100000));

        minimum = pair.getValue();
    }

    private double[] getAllParams(Node[][] indicators, Matrix latentscov, double[][] loadings, double[] delta) {
        int count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                count++;
            }
        }

        for (Node[] indicator : indicators) {
            for (int j = 0; j < indicator.length; j++) {
                count++;
            }
        }

        for (int i = 0; i < delta.length; i++) {
            count++;
        }

        double[] values = new double[count];
        count = 0;

        for (int i = 0; i < indicators.length; i++) {
            for (int j = i; j < indicators.length; j++) {
                values[count] = latentscov.get(i, j);
                count++;
            }
        }

        for (int i = 0; i < indicators.length; i++) {
            for (int j = 0; j < indicators[i].length; j++) {
                values[count] = loadings[i][j];
                count++;
            }
        }

        for (double v : delta) {
            values[count] = v;
            count++;
        }

        return values;
    }

    /**
     * jf
     * Clusters smaller than this size will be tossed out.
     */
    public int getMinClusterSize() {
        return minClusterSize;
    }

    public void setMinClusterSize(int minClusterSize) {
        if (minClusterSize < 3)
            throw new IllegalArgumentException("Minimum cluster size must be >= 3: " + minClusterSize);
        this.minClusterSize = minClusterSize;
    }

    private class Function1 implements org.apache.commons.math3.analysis.MultivariateFunction {
        private final int[][] indicatorIndices;
        private final Matrix measurescov;
        private final double[][] loadings;
        private final Matrix latentscov;
        private final int numParams;

        public Function1(int[][] indicatorIndices, Matrix measurescov, double[][] loadings,
                         Matrix latentscov, int numParams) {
            this.indicatorIndices = indicatorIndices;
            this.measurescov = measurescov;
            this.loadings = loadings;
            this.latentscov = latentscov;
            this.numParams = numParams;
        }

        @Override
        public double value(double[] values) {
            int count = 0;

            for (int i = 0; i < loadings.length; i++) {
                for (int j = i; j < loadings.length; j++) {
                    latentscov.set(i, j, values[count]);
                    latentscov.set(j, i, values[count]);
                    count++;
                }
            }

            for (int i = 0; i < loadings.length; i++) {
                for (int j = 0; j < loadings[i].length; j++) {
                    loadings[i][j] = values[count];
                    count++;
                }
            }

            return sumOfDifferences(indicatorIndices, measurescov, loadings, latentscov);
        }

//        public int getNumArguments() {
//            return numParams;
//        }
//
//        public double getLowerBound(int i) {
//            return -100;
//        }
//
//        public double getUpperBound(int i) {
//            return 100;
//        }
    }

    private class Function2 implements org.apache.commons.math3.analysis.MultivariateFunction {
        private final int[][] indicatorIndices;
        private final Matrix measurescov;
        private final Matrix measuresCovInverse;
        private final double[][] loadings;
        private final Matrix latentscov;
        private final int numParams;
        private final double[] delta;
        private final List<Integer> aboveZero = new ArrayList<>();

        public Function2(int[][] indicatorIndices, Matrix measurescov, double[][] loadings, Matrix latentscov,
                         double[] delta, int numNonMeasureVarianceParams) {
            this.indicatorIndices = indicatorIndices;
            this.measurescov = measurescov;
            this.loadings = loadings;
            this.latentscov = latentscov;
            this.numParams = numNonMeasureVarianceParams;
            this.delta = delta;
            this.measuresCovInverse = measurescov.inverse();

            int count = 0;

            for (int i = 0; i < loadings.length; i++) {
                for (int j = i; j < loadings.length; j++) {
                    if (i == j) aboveZero.add(count);
                    count++;
                }
            }

            for (int i = 0; i < loadings.length; i++) {
                for (int j = 0; j < loadings[i].length; j++) {
                    count++;
                }
            }
        }

        @Override
        public double value(double[] values) {
            int count = 0;

            for (int i = 0; i < loadings.length; i++) {
                for (int j = i; j < loadings.length; j++) {
                    latentscov.set(i, j, values[count]);
                    latentscov.set(j, i, values[count]);
                    count++;
                }
            }

            for (int i = 0; i < loadings.length; i++) {
                for (int j = 0; j < loadings[i].length; j++) {
                    loadings[i][j] = values[count];
                    count++;
                }
            }

            Matrix implied = impliedCovariance(indicatorIndices, loadings, measurescov, latentscov, delta);

            Matrix I = Matrix.identity(implied.rows());
            Matrix diff = I.minus((implied.times(measuresCovInverse)));

            return 0.5 * (diff.times(diff)).trace();
        }
    }

    private class Function3 implements org.apache.commons.math3.analysis.MultivariateFunction {
        private final int[][] indicatorIndices;
        private final Matrix measurescov;
        private Matrix measuresCovInverse;
        private final double[][] loadings;
        private final Matrix latentscov;
        private final int numParams;
        private final double[] delta;
        private final List<Integer> aboveZero = new ArrayList<>();

        public Function3(int[][] indicatorIndices, Matrix measurescov, double[][] loadings, Matrix latentscov,
                         double[] delta, int numParams) {
            this.indicatorIndices = indicatorIndices;
            this.measurescov = measurescov;
            this.loadings = loadings;
            this.latentscov = latentscov;
            this.numParams = numParams;
            this.delta = delta;
            measuresCovInverse = measurescov.inverse();

            int count = 0;

            for (int i = 0; i < delta.length; i++) {
                aboveZero.add(count);
                count++;
            }
        }

        public double value(double[] values) {
            int count = 0;

            for (int i = 0; i < delta.length; i++) {
                delta[i] = values[count];
                count++;
            }

            Matrix implied = impliedCovariance(indicatorIndices, loadings, measurescov, latentscov, delta);

            Matrix I = Matrix.identity(implied.rows());
            Matrix diff = I.minus((implied.times(measuresCovInverse)));

            return 0.5 * (diff.times(diff)).trace();
        }
    }

    private class Function4 implements org.apache.commons.math3.analysis.MultivariateFunction {
        private final int[][] indicatorIndices;
        private final Matrix measurescov;
        private final Matrix measuresCovInverse;
        private final double[][] loadings;
        private final Matrix latentscov;
        private final int numParams;
        private final double[] delta;
        private final List<Integer> aboveZero = new ArrayList<>();

        public Function4(int[][] indicatorIndices, Matrix measurescov, double[][] loadings, Matrix latentscov,
                         double[] delta) {
            this.indicatorIndices = indicatorIndices;
            this.measurescov = measurescov;
            this.loadings = loadings;
            this.latentscov = latentscov;
            this.delta = delta;
            this.measuresCovInverse = measurescov.inverse();

            int count = 0;

            for (int i = 0; i < loadings.length; i++) {
                for (int j = i; j < loadings.length; j++) {
                    if (i == j) aboveZero.add(count);
                    count++;
                }
            }

            for (double[] loading : loadings) {
                for (int j = 0; j < loading.length; j++) {
                    count++;
                }
            }

            for (int i = 0; i < delta.length; i++) {
                aboveZero.add(count);
                count++;
            }

            numParams = count;
        }

        @Override
        public double value(double[] values) {
            int count = 0;

            for (int i = 0; i < loadings.length; i++) {
                for (int j = i; j < loadings.length; j++) {
                    latentscov.set(i, j, values[count]);
                    latentscov.set(j, i, values[count]);
                    count++;
                }
            }

            for (int i = 0; i < loadings.length; i++) {
                for (int j = 0; j < loadings[i].length; j++) {
                    loadings[i][j] = values[count];
                    count++;
                }
            }

            for (int i = 0; i < delta.length; i++) {
                delta[i] = values[count];
                count++;
            }

            Matrix implied = impliedCovariance(indicatorIndices, loadings, measurescov, latentscov, delta);

            Matrix I = Matrix.identity(implied.rows());
            Matrix diff = I.minus((implied.times(measuresCovInverse)));  // time hog. times().

            return 0.5 * (diff.times(diff)).trace();
        }
    }


    private Matrix impliedCovariance(int[][] indicatorIndices, double[][] loadings, Matrix cov, Matrix loadingscov,
                                     double[] delta) {
        Matrix implied = new Matrix(cov.rows(), cov.columns());

        for (int i = 0; i < loadings.length; i++) {
            for (int j = 0; j < loadings.length; j++) {
                for (int k = 0; k < loadings[i].length; k++) {
                    for (int l = 0; l < loadings[j].length; l++) {
                        double prod = loadings[i][k] * loadings[j][l] * loadingscov.get(i, j);
                        implied.set(indicatorIndices[i][k], indicatorIndices[j][l], prod);
                    }
                }
            }
        }

        for (int i = 0; i < implied.rows(); i++) {
            implied.set(i, i, implied.get(i, i) + delta[i]);
        }

        return implied;
    }

    private double sumOfDifferences(int[][] indicatorIndices, Matrix cov, double[][] loadings, Matrix loadingscov) {
        double sum = 0;

        for (int i = 0; i < loadings.length; i++) {
            for (int k = 0; k < loadings[i].length; k++) {
                for (int l = k + 1; l < loadings[i].length; l++) {
                    double _cov = cov.get(indicatorIndices[i][k], indicatorIndices[i][l]);
                    double prod = loadings[i][k] * loadings[i][l] * loadingscov.get(i, i);
                    double diff = _cov - prod;
                    sum += diff * diff;
                }
            }
        }

        for (int i = 0; i < loadings.length; i++) {
            for (int j = i + 1; j < loadings.length; j++) {
                for (int k = 0; k < loadings[i].length; k++) {
                    for (int l = 0; l < loadings[j].length; l++) {
                        double _cov = cov.get(indicatorIndices[i][k], indicatorIndices[j][l]);
                        double prod = loadings[i][k] * loadings[j][l] * loadingscov.get(i, j);
                        double diff = _cov - prod;
                        sum += 2 * diff * diff;
                    }
                }
            }
        }

        return sum;
    }

}



