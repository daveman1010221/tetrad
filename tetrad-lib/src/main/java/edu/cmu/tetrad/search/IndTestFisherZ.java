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
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.MatrixUtils;
import edu.cmu.tetrad.util.StatUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.SingularMatrixException;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.StrictMath.log;

/**
 * Checks conditional independence of variable in a continuous data set using Fisher's Z test. See Spirtes, Glymour, and
 * Scheines, "Causation, Prediction and Search," 2nd edition, page 94.
 *
 * @author Joseph Ramsey
 * @author Frank Wimberly adapted IndTestCramerT for Fisher's Z
 */
public final class IndTestFisherZ implements IndependenceTest {

    private final Map<Node, Integer> indexMap;
    private final Map<String, Node> nameMap;
    private final NormalDistribution normal = new NormalDistribution();
    private final Map<Node, Integer> nodesHash;
    /**
     * The correlation matrix.
     */
    private final CorrelationMatrix cor;
    /**
     * The variables of the covariance matrix, in order. (Unmodifiable list.)
     */
    private List<Node> variables;
    /**
     * The significance level of the independence tests.
     */
    private double alpha;
    /**
     * Stores a reference to the dataset being analyzed.
     */
    private DataSet dataSet;
    private boolean verbose = true;
    private double p = Double.NaN;
    private boolean sellke;
    private double r = Double.NaN;


    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new Independence test which checks independence facts based on the correlation matrix implied by the
     * given data set (must be continuous). The given significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public IndTestFisherZ(final DataSet dataSet, final double alpha) {
        this.dataSet = dataSet;

        if (!(dataSet.isContinuous())) {
            throw new IllegalArgumentException("Data set must be continuous.");
        }

        if (!dataSet.existsMissingValue()) {
            this.cor = new CorrelationMatrix(dataSet);
            this.variables = this.cor.getVariables();
            this.indexMap = indexMap(this.variables);
            this.nameMap = nameMap(this.variables);
            setAlpha(alpha);

            final Map<Node, Integer> nodesHash = new HashMap<>();

            for (int j = 0; j < this.variables.size(); j++) {
                nodesHash.put(this.variables.get(j), j);
            }

            this.nodesHash = nodesHash;
        } else {
            this.cor = new CorrelationMatrix(dataSet);

            if (!(alpha >= 0 && alpha <= 1)) {
                throw new IllegalArgumentException("Alpha mut be in [0, 1]");
            }

            final List<Node> nodes = dataSet.getVariables();

            this.variables = Collections.unmodifiableList(nodes);
            this.indexMap = indexMap(this.variables);
            this.nameMap = nameMap(this.variables);
            setAlpha(alpha);

            final Map<Node, Integer> nodesHash = new HashMap<>();

            for (int j = 0; j < this.variables.size(); j++) {
                nodesHash.put(this.variables.get(j), j);
            }

            this.nodesHash = nodesHash;
        }
    }

    /**
     * Constructs a new Fisher Z independence test with  the listed arguments.
     *
     * @param data      A 2D continuous data set with no missing values.
     * @param variables A list of variables, a subset of the variables of <code>data</code>.
     * @param alpha     The significance cutoff level. p values less than alpha will be reported as dependent.
     */
    public IndTestFisherZ(final Matrix data, final List<Node> variables, final double alpha) {
        this.dataSet = new BoxDataSet(new VerticalDoubleDataBox(data.transpose().toArray()), variables);
        this.cor = new CorrelationMatrix(this.dataSet);
        this.variables = Collections.unmodifiableList(variables);
        this.indexMap = indexMap(variables);
        this.nameMap = nameMap(variables);
        setAlpha(alpha);

        final Map<Node, Integer> nodesHash = new HashMap<>();

        for (int j = 0; j < variables.size(); j++) {
            nodesHash.put(variables.get(j), j);
        }

        this.nodesHash = nodesHash;
    }

    /**
     * Constructs a new independence test that will determine conditional independence facts using the given correlation
     * matrix and the given significance level.
     */
    public IndTestFisherZ(final ICovarianceMatrix covMatrix, final double alpha) {
        this.cor = new CorrelationMatrix(covMatrix);
        this.variables = covMatrix.getVariables();
        this.indexMap = indexMap(this.variables);
        this.nameMap = nameMap(this.variables);
        setAlpha(alpha);

        final Map<Node, Integer> nodesHash = new HashMap<>();

        for (int j = 0; j < this.variables.size(); j++) {
            nodesHash.put(this.variables.get(j), j);
        }

        this.nodesHash = nodesHash;
    }

    //==========================PUBLIC METHODS=============================//

    /**
     * Creates a new independence test instance for a subset of the variables.
     */
    public IndependenceTest indTestSubset(final List<Node> vars) {
        if (vars.isEmpty()) {
            throw new IllegalArgumentException("Subset may not be empty.");
        }

        for (final Node var : vars) {
            if (!this.variables.contains(var)) {
                throw new IllegalArgumentException(
                        "All vars must be original vars");
            }
        }

        final int[] indices = new int[vars.size()];

        for (int i = 0; i < indices.length; i++) {
            indices[i] = this.indexMap.get(vars.get(i));
        }

        final ICovarianceMatrix newCovMatrix = this.cor.getSubmatrix(indices);

        final double alphaNew = getAlpha();
        return new IndTestFisherZ(newCovMatrix, alphaNew);
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
    public synchronized boolean isIndependent(final Node x, final Node y, final List<Node> z) {
        final double p = getPValue(x, y, z);

        if (Double.isNaN(p)) return true;
        else {
            return p > this.alpha;
        }
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
        return this.p;
    }

    public double getPValue(final Node x, final Node y, final List<Node> z) {
        final double r;
        final int n;

        if (covMatrix() != null) {
            r = partialCorrelation(x, y, z, null);
            n = sampleSize();
        } else {
            final List<Node> allVars = new ArrayList<>(z);
            allVars.add(x);
            allVars.add(y);

            final List<Integer> rows = getRows(allVars, this.nodesHash);
            r = getR(x, y, z, rows);
            n = rows.size();
        }

        this.r = r;
        final double q = .5 * (log(1.0 + abs(r)) - log(1.0 - abs(r)));
        final double fisherZ = sqrt(n - 3. - z.size()) * q;
        final double p = 2 * (1.0 - this.normal.cumulativeProbability(fisherZ));

        this.p = p;
        return p;
    }

    //======================PRIVATE==========================//

    private double partialCorrelation(final Node x, final Node y, final List<Node> z, final List<Integer> rows) throws SingularMatrixException {
        final int[] indices = new int[z.size() + 2];
        indices[0] = this.indexMap.get(x);
        indices[1] = this.indexMap.get(y);
        for (int i = 0; i < z.size(); i++) indices[i + 2] = this.indexMap.get(z.get(i));

        final Matrix cor;

        if (this.cor != null) {
            cor = this.cor.getSelection(indices, indices);
        } else {
            final Matrix cov = getCov(rows, indices);
            cor = MatrixUtils.convertCovToCorr(cov);
        }

        if (z.isEmpty()) return cor.get(0, 1);

        return StatUtils.partialCorrelation(cor);
    }

    private Matrix getCov(final List<Integer> rows, final int[] cols) {
        final Matrix cov = new Matrix(cols.length, cols.length);

        for (int i = 0; i < cols.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                double mui = 0.0;
                double muj = 0.0;

                for (final int k : rows) {
                    mui += this.dataSet.getDouble(k, cols[i]);
                    muj += this.dataSet.getDouble(k, cols[j]);
                }

                mui /= rows.size() - 1;
                muj /= rows.size() - 1;

                double _cov = 0.0;

                for (final int k : rows) {
                    _cov += (this.dataSet.getDouble(k, cols[i]) - mui) * (this.dataSet.getDouble(k, cols[j]) - muj);
                }

                final double mean = _cov / (rows.size());
                cov.set(i, j, mean);
            }
        }

        return cov;
    }

    private double getR(final Node x, final Node y, final List<Node> z, final List<Integer> rows) {
        try {
            return partialCorrelation(x, y, z, rows);
        } catch (final SingularMatrixException e) {
            e.printStackTrace();
            System.out.println(SearchLogUtils.determinismDetected(z, x));
            return Double.NaN;
        }
    }


    public double getBic() {
        return -sampleSize() * Math.log(1.0 - this.r * this.r) - Math.log(sampleSize());
    }

    /**
     * Gets the getModel significance level.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * Sets the significance level at which independence judgments should be made.  Affects the cutoff for partial
     * correlations to be considered statistically equal to zero.
     */
    public void setAlpha(final double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range: " + alpha);
        }

        this.alpha = alpha;
//        double cutoff = StatUtils.getZForAlpha(alpha);
    }

    /**
     * @return the list of variables over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    public List<Node> getVariables() {
        return this.variables;
    }

    public void setVariables(final List<Node> variables) {
        if (variables.size() != this.variables.size()) throw new IllegalArgumentException("Wrong # of variables.");
        this.variables = new ArrayList<>(variables);
        this.cor.setVariables(variables);
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
            parents[j] = this.cor.getVariables().indexOf(z.get(j));
        }

        if (parents.length > 0) {

            // Regress z onto i, yielding regression coefficients b.
            final Matrix Czz = this.cor.getSelection(parents, parents);

            try {
                Czz.inverse();
            } catch (final SingularMatrixException e) {
                System.out.println(SearchLogUtils.determinismDetected(z, x));
                return true;
            }
        }

        return false;
    }

    /**
     * @return the data set being analyzed.
     */
    public DataSet getData() {
        return this.dataSet;
    }

    //==========================PRIVATE METHODS============================//

    /**
     * @return a string representation of this test.
     */
    public String toString() {
        return "Fisher Z, alpha = " + new DecimalFormat("0.0E0").format(getAlpha());
    }

    private int sampleSize() {
        return covMatrix().getSampleSize();
    }

    private ICovarianceMatrix covMatrix() {
        return this.cor;
    }

    private Map<String, Node> nameMap(final List<Node> variables) {
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

    public ICovarianceMatrix getCov() {
        return this.cor;
    }

    @Override
    public List<DataSet> getDataSets() {
        final List<DataSet> dataSets = new ArrayList<>();
        dataSets.add(this.dataSet);
        return dataSets;
    }

    @Override
    public int getSampleSize() {
        return this.cor.getSampleSize();
    }

    @Override
    public List<Matrix> getCovMatrices() {
        return null;
    }

    @Override
    public double getScore() {
        return this.alpha - this.p;//Math.abs(fisherZ) - cutoff;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    public void setSellke(final boolean sellke) {
        this.sellke = sellke;
    }

    private List<Integer> getRows(final List<Node> allVars, final Map<Node, Integer> nodesHash) {
        final List<Integer> rows = new ArrayList<>();

        K:
        for (int k = 0; k < this.dataSet.getNumRows(); k++) {
            for (final Node node : allVars) {
                if (Double.isNaN(this.dataSet.getDouble(k, nodesHash.get(node)))) continue K;
            }

            rows.add(k);
        }

        return rows;
    }
}




