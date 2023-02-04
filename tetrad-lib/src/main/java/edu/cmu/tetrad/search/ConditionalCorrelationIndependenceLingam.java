///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
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
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.regression.RegressionDataset;
import edu.cmu.tetrad.regression.RegressionResult;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.*;

import static edu.cmu.tetrad.util.StatUtils.*;
import static org.apache.commons.math3.util.FastMath.pow;
import static org.apache.commons.math3.util.FastMath.*;

/**
 * Checks conditional independence of variable in a continuous data set using Daudin's method. See
 * <p>
 * Ramsey, J. D. (2014). A scalable conditional independence test for nonlinear, non-Gaussian data. arXiv
 * preprint arXiv:1401.5031.
 * <p>
 * This is corrected using Lemma 2, condition 4 of
 * <p>
 * Zhang, K., Peters, J., Janzing, D., and Schölkopf, B. (2012). Kernel-based conditional independence test and
 * application in causal discovery. arXiv preprint arXiv:1202.3775.
 * <p>
 * This all follows the original Daudin paper, which is this:
 * <p>
 * Daudin, J. J. (1980). Partial association measures and a  application to qualitative regression.
 * Biometrika, 67(3), 581-590.
 * <p>
 * We use Nadaraya-Watson kernel regression, though we further restrict the sample size to nearby points.
 *
 * @author Joseph Ramsey
 */
public final class ConditionalCorrelationIndependenceLingam {
    private final RegressionDataset regressionDataset;

    public enum Kernel {Epinechnikov, Gaussian}

    public enum Basis {Polynomial, Cosine}

    /**
     * The dataset supplied in the constructor.
     */
    private final DataSet dataSet;

    /**
     * The variables in datasSet.
     */
    private final List<Node> variables;

    /**
     * Map from nodes to their indices.
     */
    private final HashMap<Node, Integer> nodesHash;

    /**
     * Alpha cutoff for this class.
     */
    private double alpha;

    /**
     * The q value of the most recent test.
     */
    private double score;

    /**
     * Number of functions to use in the (truncated) basis.
     */
    private int numFunctions = 10;

    /**
     * Z cutoff for testing; depends on alpha.
     */
    private double cutoff;

    /**
     * Azzalini kernel widths are multiplied by this.
     */
    private double width = 1.0;

    /**
     * Basis
     */
    private Basis basis = Basis.Polynomial;

    //==================CONSTRUCTORS====================//

    /**
     * Constructs a new Independence test which checks independence facts based on the
     * correlation data implied by the given data set (must be continuous). The given
     * significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public ConditionalCorrelationIndependenceLingam(DataSet dataSet, double alpha) {
        if (dataSet == null) throw new NullPointerException();
        this.alpha = alpha;

        this.variables = dataSet.getVariables();

        this.nodesHash = new HashMap<>();
        for (int i = 0; i < this.variables.size(); i++) {
            this.nodesHash.put(this.variables.get(i), i);
        }

        for (int j = 0; j < dataSet.getNumColumns(); j++) {
            scale(dataSet, j);
        }

        this.dataSet = dataSet;
        regressionDataset = new RegressionDataset(dataSet);
    }

    //=================PUBLIC METHODS====================//

    /**
     * @return true iff x is independent of y conditional on z.
     */
    public double isIndependent(Node x, Node y, List<Node> z) {
        try {
            double[] rx = residuals(x, z);
            double[] ry = residuals(y, z);

            // rx _||_ ry ?
            double score = independent(rx, ry);
            this.score = score;

            return score;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Calculates the residuals of x regressed nonparametrically onto z. Left public
     * so it can be accessed separately.
     *
     * @return a double[2][] array. The first double[] array contains the residuals for x
     * and the second double[] array contains the resituls for y.
     */
    public double[] residuals(Node x, List<Node> z) {
        RegressionResult result = regressionDataset.regress(x, z);
        edu.cmu.tetrad.util.Vector residuals = result.getResiduals();

        int numRows = dataSet.getNumRows();

        double[] _residualsx = new double[numRows];

        for (int i = 0; i < numRows; i++) _residualsx[i] = residuals.get(i);

        return _residualsx;
    }

    /**
     * Number of functions to use in (truncated) basis
     */
    public int getNumFunctions() {
        return this.numFunctions;
    }

    public void setNumFunctions(int numFunctions) {
//        this.numFunctions = numFunctions;
    }

    public void setBasis(Basis basis) {
//        this.basis = basis;
    }

    public double getWidth() {
        return this.width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getPValue() {
        return getPValue(this.score);
    }

    public double getPValue(double score) {
        return 2.0 * (new NormalDistribution(0, 1).cumulativeProbability(-abs(score)));
    }

    /**
     * @return the minimal scores value calculated by the method for the most
     * recent independence check.
     */
    public double getScore() {
        return abs(this.score) - this.cutoff;//  alpha - getPValue();
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
        this.cutoff = getZForAlpha(alpha);
    }

    public double getAlpha() {
        return this.alpha;
    }

    //=====================PRIVATE METHODS====================//

    /**
     * @return true just in the case the x and y vectors are independent,
     * once undefined values have been removed. Left public so it can be
     * accessed separately.
     */
    private double independent(double[] x, double[] y) {
        double[] _x = new double[x.length];
        double[] _y = new double[y.length];

        double maxScore = Double.NEGATIVE_INFINITY;

        for (int m = 1; m <= getNumFunctions(); m++) {
            for (int n = 1; n <= getNumFunctions(); n++) {
                for (int i = 0; i < x.length; i++) {
                    _x[i] = function(m, x[i]);
                    _y[i] = function(n, y[i]);
                }

                double score = abs(nonparametricFisherZ(_x, _y));
                if (Double.isInfinite(score) || Double.isNaN(score)) continue;

                if (score > maxScore) {
                    maxScore = score;
                }
            }
        }

        return maxScore;
    }

    private void scale(DataSet dataSet, int col) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            double d = dataSet.getDouble(i, col);
            if (Double.isNaN(d)) continue;
            if (d > max) max = d;
            if (d < min) min = d;
        }

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            double d = dataSet.getDouble(i, col);
            if (Double.isNaN(d)) continue;
            dataSet.setDouble(i, col, min + (d - min) / (max - min));
        }
    }

    private double nonparametricFisherZ(double[] _x, double[] _y) {

        // Testing the hypothesis that _x and _y are uncorrelated and assuming that 4th moments of _x and _y
        // are finite and that the sample is large.
        double[] __x = standardize(_x);
        double[] __y = standardize(_y);

        double r = covariance(__x, __y); // correlation
        int N = __x.length;

        // Non-parametric Fisher Z test.
        double z = 0.5 * sqrt(N) * (log(1.0 + r) - log(1.0 - r));

        return z / (sqrt((moment22(__x, __y))));
    }

    private double moment22(double[] x, double[] y) {
        int N = x.length;
        double sum = 0.0;

        for (int j = 0; j < x.length; j++) {
            sum += x[j] * x[j] * y[j] * y[j];
        }

        return sum / N;
    }

    private double function(int index, double x) {
        if (this.basis == Basis.Polynomial) {
            double g = 1.0;

            for (int i = 1; i <= index; i++) {
                g *= x;
            }

            if (abs(g) == Double.POSITIVE_INFINITY) g = Double.NaN;

            return g;
        } else if (this.basis == Basis.Cosine) {
            int i = (index + 1) / 2;

            if (index % 2 == 1) {
                return sin(i * x);
            } else {
                return cos(i * x);
            }
        } else {
            throw new IllegalStateException("That basis is not configured: " + this.basis);
        }
    }

    // Optimal bandwidth qsuggested by Bowman and Bowman and Azzalini (1997) q.31,
    // using MAD.
    private double h(double[] xCol) {
        double[] g = new double[xCol.length];
        double median = median(xCol);
        for (int j = 0; j < xCol.length; j++) g[j] = abs(xCol[j] - median);
        double mad = median(g);
        return (1.4826 * mad) * pow((4.0 / 3.0) / xCol.length, 0.2);
    }

    private double kernelEpinechnikov(double z, double h) {
        z /= getWidth() * h;
        if (abs(z) > 1) return 0.0;
        else return (/*0.75 **/ (1.0 - z * z));
    }

    private double kernelGaussian(double z, double h) {
        z /= getWidth() * h;
        return exp(-z * z);
    }

    // Euclidean distance.
    private double distance(double[][] data, int[] z, int i, int j) {
        double sum = 0.0;

        for (int _z : z) {
            double d = (data[_z][i] - data[_z][j]) / 2.0;

            if (!Double.isNaN(d)) {
                sum += d * d;
            }
        }

        return sqrt(sum);
    }

    // Standardizes the given data array. No need to make a copy here.
    private double[] standardize(double[] data) {
        double sum = 0.0;

        for (double d : data) {
            sum += d;
        }

        double mean = sum / data.length;

        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] - mean;
        }

        double var = 0.0;

        for (double d : data) {
            var += d * d;
        }

        var /= (data.length);
        double sd = sqrt(var);

        for (int i = 0; i < data.length; i++) {
            data[i] /= sd;
        }

        return data;
    }

    private Set<Integer> getCloseZs(double[][] _data, int[] _z, int i, int sampleSize,
                                    List<Map<Integer, Integer>> reverseLookup,
                                    List<List<Integer>> sortedIndices) {
        Set<Integer> js = new HashSet<>();

        if (sampleSize > _data[0].length) sampleSize = (int) ceil(0.8 * _data.length);
        if (_z.length == 0) return new HashSet<>();

        int radius = 0;

        while (true) {
            for (int z1 : _z) {
                int q = reverseLookup.get(z1).get(i);

                if (q - radius >= 0 && q - radius < _data[z1 + 1].length) {
                    int r2 = sortedIndices.get(z1).get(q - radius);
                    js.add(r2);
                }

                if (q + radius >= 0 && q + radius < _data[z1 + 1].length) {
                    int r2 = sortedIndices.get(z1).get(q + radius);
                    js.add(r2);
                }
            }

            if (js.size() >= sampleSize) return js;

            radius++;
        }
    }

    private List<Integer> getRows(DataSet dataSet, List<Node> allVars, Map<Node, Integer> nodesHash) {
        List<Integer> rows = new ArrayList<>();

        K:
        for (int k = 0; k < dataSet.getNumRows(); k++) {
            for (Node node : allVars) {
                if (Double.isNaN(dataSet.getDouble(k, nodesHash.get(node)))) continue K;
            }

            rows.add(k);
        }

        return rows;
    }
}



