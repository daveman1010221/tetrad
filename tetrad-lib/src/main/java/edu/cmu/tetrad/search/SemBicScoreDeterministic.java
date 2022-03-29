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
import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.DepthChoiceGenerator;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.Vector;
import org.apache.commons.math3.linear.SingularMatrixException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.log;

/**
 * Implements the continuous BIC score for FGES.
 *
 * @author Joseph Ramsey
 */
public class SemBicScoreDeterministic implements Score {

    // The covariance matrix.
    private ICovarianceMatrix covariances;

    // The variables of the covariance matrix.
    private List<Node> variables;

    // The sample size of the covariance matrix.
    private final int sampleSize;

    // The penalty penaltyDiscount.
    private double penaltyDiscount = 1.0;

    // True if linear dependencies should return NaN for the score, and hence be
    // ignored by FGES
    private boolean ignoreLinearDependent;

    // The printstream output should be sent to.
    private PrintStream out = System.out;

    // True if verbose output should be sent to out.
    private boolean verbose;

    // Variables that caused computational problems and so are to be avoided.
    private final Set<Integer> forbidden = new HashSet<>();
    private double determinismThreshold = 0.1;

    /**
     * Constructs the score using a covariance matrix.
     */
    public SemBicScoreDeterministic(final ICovarianceMatrix covariances) {
        if (covariances == null) {
            throw new NullPointerException();
        }

        this.setCovariances(covariances);
        this.variables = covariances.getVariables();
        this.sampleSize = covariances.getSampleSize();
    }

    /**
     * Calculates the sample likelihood and BIC score for i given its parents in a simple SEM model
     */
    public double localScore(final int i, final int... parents) {
        for (final int p : parents) if (this.forbidden.contains(p)) return Double.NaN;
        final double small = getDeterminismThreshold();

        double s2 = getCovariances().getValue(i, i);
        final int p = parents.length;

        final Matrix covxx = getSelection(getCovariances(), parents, parents);
        final Vector covxy = getSelection(getCovariances(), parents, new int[]{i}).getColumn(0);

        try {
            s2 -= covxx.inverse().times(covxy).dotProduct(covxy);
        } catch (final SingularMatrixException e) {
            s2 = 0;
        }

//        System.out.println(s2);

        final int n = getSampleSize();
        final int k = 2 * p + 1;

        if (s2 < small) {
            s2 = 0;
        }

        if (s2 == 0) {
            printDeterminism(i, parents);
            return Double.NaN;
        }

        return -(n) * log(s2) - getPenaltyDiscount() * k * log(n);
    }


    @Override
    public double localScoreDiff(final int x, final int y, final int[] z) {


        final double v1 = localScore(y, append(z, x));
        final double v2 = localScore(y, z);
        final double v3 = localScore(y, x);

        if (Double.isNaN(v1) && !Double.isNaN(v2) && !Double.isNaN(v3)) {
            return Double.NaN;
        } else if (Double.isNaN(v1) || Double.isNaN(v2) || Double.isNaN(v3)) {
            return Double.NEGATIVE_INFINITY;
        }

        return v1 - v2;
    }

    @Override
    public double localScoreDiff(final int x, final int y) {
        return localScoreDiff(x, y, new int[0]);

//        return localScore(y, x) - localScore(y);
    }

    private int[] append(final int[] parents, final int extra) {
        final int[] all = new int[parents.length + 1];
        System.arraycopy(parents, 0, all, 1, parents.length);
        all[0] = extra;
        return all;
    }

    /**
     * Specialized scoring method for a single parent. Used to speed up the effect edges search.
     */
    public double localScore(final int i, final int parent) {
        return localScore(i, new int[]{parent});

//        double residualVariance = getCovariances().getValue(i, i);
//        int n = getSampleSize();
//        int p = 1;
//        final double covXX = getCovariances().getValue(parent, parent);
//
//        if (covXX == 0) {
//            if (isVerbose()) {
//                out.println("Dividing by zero");
//            }
//            return Double.NaN;
//        }
//
//        double covxxInv = 1.0 / covXX;
//        double covxy = getCovariances().getValue(i, parent);
//        double b = covxxInv * covxy;
//        residualVariance -= covxy * b;
//
//        if (residualVariance <= 0) {
//            if (isVerbose()) {
//                out.println("Nonpositive residual varianceY: resVar / varianceY = " + (residualVariance / getCovariances().getValue(i, i)));
//            }
//            return Double.NaN;
//        }
//
//        return score(residualVariance, n, p);
    }

    /**
     * Specialized scoring method for no parents. Used to speed up the effect edges search.
     */
    public double localScore(final int i) {
        return localScore(i, new int[0]);
//        double residualVariance = getCovariances().getValue(i, i);
//        int n = getSampleSize();
//        int p = 0;
//
//        if (residualVariance <= 0) {
//            if (isVerbose()) {
//                out.println("Nonpositive residual varianceY: resVar / varianceY = " + (residualVariance / getCovariances().getValue(i, i)));
//            }
//            return Double.NaN;
//        }
//
//        double c = getPenaltyDiscount();
//        return score(residualVariance, n, p);
    }

    /**
     * True iff edges that cause linear dependence are ignored.
     */
    public boolean isIgnoreLinearDependent() {
        return this.ignoreLinearDependent;
    }

    public void setIgnoreLinearDependent(final boolean ignoreLinearDependent) {
        this.ignoreLinearDependent = ignoreLinearDependent;
    }

    public void setOut(final PrintStream out) {
        this.out = out;
    }

    public double getPenaltyDiscount() {
        return this.penaltyDiscount;
    }

    public ICovarianceMatrix getCovariances() {
        return this.covariances;
    }

    public int getSampleSize() {
        return this.sampleSize;
    }

    @Override
    public boolean isEffectEdge(final double bump) {
        return bump > 0;//-0.25 * getPenaltyDiscount() * Math.log(sampleSize);
    }

    public DataSet getDataSet() {
        throw new UnsupportedOperationException();
    }

    public void setPenaltyDiscount(final double penaltyDiscount) {
        this.penaltyDiscount = penaltyDiscount;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public List<Node> getVariables() {
        return this.variables;
    }

    private Matrix getSelection(final ICovarianceMatrix cov, final int[] rows, final int[] cols) {
        return cov.getSelection(rows, cols);
    }

    // Prints a smallest subset of parents that causes a singular matrix exception.
    private boolean printMinimalLinearlyDependentSet(final int[] parents, final ICovarianceMatrix cov) {

        final List<Node> _parents = new ArrayList<>();
        for (final int p : parents) _parents.add(this.variables.get(p));

        final DepthChoiceGenerator gen = new DepthChoiceGenerator(_parents.size(), _parents.size());
        int[] choice;

        while ((choice = gen.next()) != null) {
            final int[] sel = new int[choice.length];
            final List<Node> _sel = new ArrayList<>();
            for (int m = 0; m < choice.length; m++) {
                sel[m] = parents[m];
                _sel.add(this.variables.get(sel[m]));
            }

            final Matrix m = cov.getSelection(sel, sel);


            try {
                m.inverse();
            } catch (final Exception e2) {
                this.forbidden.add(sel[0]);
                this.out.println("### Linear dependence among variables: " + _sel);
                this.out.println("### Removing " + _sel.get(0));
                return true;
            }
        }

        return false;
    }

    private int[] getMinimalLinearlyDependentSet(final int i, final int[] parents, final ICovarianceMatrix cov) {
        final double small = getDeterminismThreshold();

        final List<Node> _parents = new ArrayList<>();
        for (final int p : parents) _parents.add(this.variables.get(p));

        final DepthChoiceGenerator gen = new DepthChoiceGenerator(_parents.size(), _parents.size());
        int[] choice;

        while ((choice = gen.next()) != null) {
            final int[] sel = new int[choice.length];
            final List<Node> _sel = new ArrayList<>();
            for (int m = 0; m < choice.length; m++) {
                sel[m] = parents[m];
                _sel.add(this.variables.get(sel[m]));
            }

            final Matrix m = cov.getSelection(sel, sel);

            double s2 = getCovariances().getValue(i, i);

            final Matrix covxx = getSelection(getCovariances(), parents, parents);
            final Vector covxy = getSelection(getCovariances(), parents, new int[]{i}).getColumn(0);
            s2 -= covxx.inverse().times(covxy).dotProduct(covxy);

            if (s2 <= small) {
                this.out.println("### Linear dependence among variables: " + _sel);
                this.out.println("### Removing " + _sel.get(0));
                return sel;
            }

            try {
                m.inverse();
            } catch (final Exception e2) {
//                forbidden.add(sel[0]);
                this.out.println("### Linear dependence among variables: " + _sel);
                this.out.println("### Removing " + _sel.get(0));
                return sel;
            }
        }

        return new int[0];
    }

    private int[] getMaximalLinearlyDependentSet(final int i, final int[] parents, final ICovarianceMatrix cov) {
        final double small = getDeterminismThreshold();

        final List<Node> _parents = new ArrayList<>();
        for (final int p : parents) _parents.add(this.variables.get(p));

        final DepthChoiceGenerator gen = new DepthChoiceGenerator(_parents.size(), _parents.size());
        int[] choice;

        while ((choice = gen.next()) != null) {
            final int[] sel0 = new int[choice.length];

            final List<Integer> all = new ArrayList<>();
            for (int w = 0; w < parents.length; w++) all.add(parents[w]);
            for (int w = 0; w < sel0.length; w++) all.remove(sel0[w]);
            final int[] sel = new int[all.size()];
            for (int w = 0; w < all.size(); w++) sel[w] = all.get(w);

            final List<Node> _sel = new ArrayList<>();
            for (int m = 0; m < choice.length; m++) {
                sel[m] = parents[m];
                _sel.add(this.variables.get(sel[m]));
            }

            final Matrix m = cov.getSelection(sel, sel);

            double s2 = getCovariances().getValue(i, i);

            final Matrix covxx = getSelection(getCovariances(), parents, parents);
            final Vector covxy = getSelection(getCovariances(), parents, new int[]{i}).getColumn(0);
            s2 -= covxx.inverse().times(covxy).dotProduct(covxy);

            if (s2 <= small) {
                this.out.println("### Linear dependence among variables: " + _sel);
                this.out.println("### Removing " + _sel.get(0));
                return sel;
            }

            try {
                m.inverse();
            } catch (final Exception e2) {
//                forbidden.add(sel[0]);
                this.out.println("### Linear dependence among variables: " + _sel);
                this.out.println("### Removing " + _sel.get(0));
                return sel;
            }
        }

        return new int[0];
    }

    private void printDeterminism(final int i, final int[] parents) {
        final List<Node> _sel = new ArrayList<>();

        for (int m = 0; m < parents.length; m++) {
            _sel.add(this.variables.get(parents[m]));
        }

        final Node x = this.variables.get(i);
//        System.out.println(SearchLogUtils.determinismDetected(_sel, x));
    }

    private void setCovariances(final ICovarianceMatrix covariances) {
        this.covariances = covariances;
    }

    public void setVariables(final List<Node> variables) {
        this.covariances.setVariables(variables);
        this.variables = variables;
    }

    @Override
    public Node getVariable(final String targetName) {
        for (final Node node : this.variables) {
            if (node.getName().equals(targetName)) {
                return node;
            }
        }

        return null;
    }

    @Override
    public int getMaxDegree() {
        return (int) Math.ceil(log(this.sampleSize));
    }

    @Override
    public boolean determines(final List<Node> z, final Node y) {
        final int i = this.variables.indexOf(y);

        final int[] parents = new int[z.size()];

        for (int t = 0; t < z.size(); t++) {
            parents[t] = this.variables.indexOf(z.get(t));
        }

        final double small = getDeterminismThreshold();

        try {
            double s2 = getCovariances().getValue(i, i);

            final Matrix covxx = getSelection(getCovariances(), parents, parents);
            final Vector covxy = getSelection(getCovariances(), parents, new int[]{i}).getColumn(0);
            s2 -= covxx.inverse().times(covxy).dotProduct(covxy);

            if (s2 <= small) {
                printDeterminism(i, parents);
                return true;
            }
        } catch (final Exception e) {
            printDeterminism(i, parents);
        }

        return false;
    }

    public double getDeterminismThreshold() {
        return this.determinismThreshold;
    }

    public void setDeterminismThreshold(final double determinismThreshold) {
        this.determinismThreshold = determinismThreshold;
    }
}



