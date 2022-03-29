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

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Vector;
import edu.cmu.tetrad.util.*;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks conditional independence of variable in a continuous data set using Fisher's Z test. See Spirtes, Glymour, and
 * Scheines, "Causation, Prediction and Search," 2nd edition, page 94.
 *
 * @author Joseph Ramsey
 * @author Frank Wimberly adapted IndTestCramerT for Fisher's Z
 */
public final class IndTestDrton implements IndependenceTest {

    /**
     * The covariance matrix.
     */
    private final ICovarianceMatrix covMatrix;

    /**
     * The matrix out of the cov matrix.
     */
    private final Matrix _covMatrix;

    /**
     * The variables of the covariance matrix, in order. (Unmodifiable list.)
     */
    private List<Node> variables;

    /**
     * The significance level of the independence tests.
     */
    private double alpha;

    /**
     * The value of the Fisher's Z statistic associated with the las calculated partial correlation.
     */
    private double fisherZ;

    /**
     * Formats as 0.0000.
     */
    private static final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

    /**
     * Stores a reference to the dataset being analyzed.
     */
    private DataSet dataSet;

    private PrintStream pValueLogger;
    private final Map<Node, Integer> indexMap;
    private final Map<String, Node> nameMap;
    private boolean verbose;

    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new Independence test which checks independence facts based on the correlation matrix implied by the
     * given data set (must be continuous). The given significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public IndTestDrton(final DataSet dataSet, final double alpha) {
        if (!(dataSet.isContinuous())) {
            throw new IllegalArgumentException("Data set must be continuous.");
        }

        this.covMatrix = DataUtils.covarianceNonparanormalDrton(dataSet);
        this._covMatrix = this.covMatrix.getMatrix();
        final List<Node> nodes = this.covMatrix.getVariables();

        this.variables = Collections.unmodifiableList(nodes);
        this.indexMap = indexMap(this.variables);
        this.nameMap = mapNames(this.variables);
        setAlpha(alpha);

        this.dataSet = DataUtils.center(this.dataSet);
    }

    //==========================PUBLIC METHODS=============================//

    /**
     * Creates a new independence test instance for a subset of the variables.
     */

    public IndependenceTest indTestSubset(final List<Node> vars) {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines whether variable x is independent of variable y given a list of conditioning variables z.
     *
     * @param x the one variable being compared.
     * @param y the second variable being compared.
     * @param z the list of conditioning variables.
     * @return true iff x _||_ y | z.
     * @throws RuntimeException if a matrix singularity is encountered.
     */
    public boolean isIndependent(final Node x, final Node y, final List<Node> z) {
        double r;
        final int n = sampleSize();

        if (z.isEmpty()) {
            Integer xi = this.indexMap.get(x);
            Integer yi = this.indexMap.get(y);

            if (xi == null || yi == null) {
                xi = this.indexMap.get(this.nameMap.get(x.getName()));
                yi = this.indexMap.get(this.nameMap.get(y.getName()));

                if (xi == null || yi == null) {
                    throw new IllegalArgumentException("Node not in map");
                }
            }

            final double a = this._covMatrix.get(xi, xi);
            final double b = this._covMatrix.get(xi, yi);
            final double d = this._covMatrix.get(yi, yi);

            r = -b / Math.sqrt(a * d);
        } else {
            final Matrix submatrix = DataUtils.subMatrix(this._covMatrix, this.indexMap, x, y, z);
            r = StatUtils.partialCorrelation(submatrix);
        }

        // Either dividing by a zero standard deviation (in which case it's dependent) or doing a regression
//        // (effectively) with a multicolliarity.. or missing values in the data!
//        if (Double.isNaN(engine)) {
//
//            // Maybe it's missing values. Try calculating engine using just the rows in the data set
//            // (if it exists) with defined values for all compared variables.
//            if (dataSet != null) {
//                int[] vars = new int[2 + z.size()];
//
//                vars[0] = variables.indexOf(x);
//                vars[1] = variables.indexOf(y);
//
//                for (int k = 0; k < z.size(); k++) {
//                    vars[2 + k] = variables.indexOf(z.get(k));
//                }
//
//                int[] _n = new int[1];
//
//                TetradMatrix submatrix = DataUtils.covMatrixForDefinedRows(dataSet, vars, _n);
//
//                engine = StatUtils.partialCorrelation(submatrix);
//            }
//
//            if (Double.isNaN(engine)) {
//                return false;
//            }
//        }


        if (r > 1.) r = 1.;
        if (r < -1.) r = -1.;

        final double fisherZ = Math.sqrt(n - z.size() - 3.0) *
                0.5 * (Math.log(1.0 + r) - Math.log(1.0 - r));

        this.fisherZ = fisherZ;

        if (Double.isNaN(fisherZ)) {
            return true;
        }

        final double pValue = 2.0 * (1.0 - RandomUtil.getInstance().normalCdf(0, 1, Math.abs(fisherZ)));

        return pValue > this.alpha;
    }

    public boolean isIndependent(final Node x, final Node y, final Node... z) {
        return isIndependent(x, y, Arrays.asList(z));
    }

    public boolean isDependent(final Node x, final Node y, final List<Node> z) {
        return !isIndependent(x, y, z);
    }

    public boolean isDependent(final Node x, final Node y, final Node... z) {
        final List<Node> zList = Arrays.asList(z);
        return isDependent(x, y, zList);
    }

    /**
     * @return the probability associated with the most recently computed independence test.
     */
    public double getPValue() {
        return 2.0 * (1.0 - RandomUtil.getInstance().normalCdf(0, 1, Math.abs(this.fisherZ)));
    }

    /**
     * Sets the significance level at which independence judgments should be made.  Affects the cutoff for partial
     * correlations to be considered statistically equal to zero.
     */
    public void setAlpha(final double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range.");
        }

        this.alpha = alpha;
    }

    /**
     * Gets the getModel significance level.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * @return the list of variables over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    public List<Node> getVariables() {
        return this.variables;
    }

    /**
     * @return the variable with the given name.
     */
    public Node getVariable(final String name) {
        return this.nameMap.get(name);
    }

    /**
     * @return the list of variable varNames.
     */
    public List<String> getVariableNames() {
        final List<Node> variables = getVariables();
        final List<String> variableNames = new ArrayList<>();
        for (final Node variable1 : variables) {
            variableNames.add(variable1.getName());
        }
        return variableNames;
    }

    /**
     * If <code>isDeterminismAllowed()</code>, deters to IndTestFisherZD; otherwise throws
     * UnsupportedOperationException.
     */
    public boolean determines(final List<Node> z, final Node x) throws UnsupportedOperationException {
        final int[] parents = new int[z.size()];

        for (int j = 0; j < parents.length; j++) {
            parents[j] = this.covMatrix.getVariables().indexOf(z.get(j));
        }

        final int i = this.covMatrix.getVariables().indexOf(x);

        final Matrix matrix2D = this.covMatrix.getMatrix();
        double variance = matrix2D.get(i, i);

        if (parents.length > 0) {

            // Regress z onto i, yielding regression coefficients b.
            final Matrix Czz = matrix2D.getSelection(parents, parents);
            final Matrix inverse;

            try {
                inverse = Czz.inverse();
            } catch (final Exception e) {
                return true;
            }

            Vector Cyz = matrix2D.getColumn(i);
            Cyz = Cyz.viewSelection(parents);
            final Vector b = inverse.times(Cyz);

            variance -= Cyz.dotProduct(b);
        }

        return variance < 1e-20;
    }

    /**
     * @return the data set being analyzed.
     */
    public DataSet getData() {
        return this.dataSet;
    }

    @Override
    public ICovarianceMatrix getCov() {
        return null;
    }

    @Override
    public List<DataSet> getDataSets() {
        return null;
    }

    @Override
    public int getSampleSize() {
        return 0;
    }

    @Override
    public List<Matrix> getCovMatrices() {
        return null;
    }

    @Override
    public double getScore() {
        return -(getPValue() - getAlpha());
    }

    public void shuffleVariables() {
        final ArrayList<Node> nodes = new ArrayList<>(this.variables);
        Collections.shuffle(nodes);
        this.variables = Collections.unmodifiableList(nodes);
    }

    /**
     * @return a string representation of this test.
     */
    public String toString() {
        return "Drton, alpha = " + IndTestDrton.nf.format(getAlpha());
    }

    public void setPValueLogger(final PrintStream pValueLogger) {
        this.pValueLogger = pValueLogger;
    }

    //==========================PRIVATE METHODS============================//

    private int sampleSize() {
        return covMatrix().getSampleSize();
    }

    private ICovarianceMatrix covMatrix() {
        return this.covMatrix;
    }

    private Map<String, Node> mapNames(final List<Node> variables) {
        final Map<String, Node> nameMap = new ConcurrentHashMap<>();

        for (final Node node : variables) {
            nameMap.put(node.getName(), node);
        }

        return nameMap;
    }

    private Map<Node, Integer> indexMap(final List<Node> variables) {
        final Map<Node, Integer> indexMap = new ConcurrentHashMap<>();

        for (int i = 0; i < variables.size(); i++) {
            indexMap.put(variables.get(i), i);
        }

        return indexMap;
    }

    @Override
    public boolean isVerbose() {
        return this.verbose;
    }

    @Override
    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }
}



