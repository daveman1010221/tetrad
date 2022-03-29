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

import edu.cmu.tetrad.cluster.KMeans;
import edu.cmu.tetrad.data.BoxDataSet;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DoubleDataBox;
import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.kernel.Kernel;
import edu.cmu.tetrad.search.kernel.KernelGaussian;
import edu.cmu.tetrad.search.kernel.KernelUtils;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.TetradLogger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Checks the conditional independence X _||_ Y | S, where S is a set of continuous variable, and X and Y are discrete
 * variable not in S, using the Hilbert-Schmidth Independence Criterion (HSIC), a kernel based nonparametric test for
 * conditional independence.
 *
 * @author Robert Tillman
 */

public final class IndTestHsic implements IndependenceTest {

    /**
     * The variables of the covariance matrix, in order. (Unmodifiable list.)
     */
    private List<Node> variables;

    /**
     * The significance level of the independence tests.
     */
    private double alpha;

    /**
     * The cutoff value for 'alpha'
     */
    private double thresh = Double.NaN;

    /**
     * The value of the empirical estimate of HSIC
     */
    private double hsic;

    /**
     * Formats as 0.0000.
     */
    private static final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

    /**
     * Stores a reference to the dataset being analyzed.
     */
    private final DataSet dataSet;

    /**
     * A stored p value, if the deterministic test was used.
     */
    private double pValue = Double.NaN;

    /**
     * The regularizer
     */
    private double regularizer = 0.0001;

    /**
     * Number of permutations to approximate the null distribution
     */
    private int perms = 100;

    /**
     * Use incomplete Choleksy decomposition to calculate Gram matrices
     */
    private double useIncompleteCholesky = 1e-18;
    private boolean verbose;

    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new HSIC Independence test. The given significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public IndTestHsic(final DataSet dataSet, final double alpha) {
        if (!(dataSet.isContinuous())) {
            throw new IllegalArgumentException("Data set must be continuous.");
        }

        final List<Node> nodes = dataSet.getVariables();

        this.variables = Collections.unmodifiableList(nodes);
        setAlpha(alpha);

        this.dataSet = dataSet;
    }

    public IndTestHsic(final Matrix data, final List<Node> variables, final double alpha) {
        final DataSet dataSet = new BoxDataSet(new DoubleDataBox(data.toArray()), variables);

        this.variables = Collections.unmodifiableList(variables);
        setAlpha(alpha);

        this.dataSet = dataSet;
    }


    //==========================PUBLIC METHODS=============================//

    /**
     * Creates a new IndTestHsic instance for a subset of the variables.
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
            indices[i] = this.variables.indexOf(vars.get(i));
        }

        final double alphaNew = getAlpha();
        return new IndTestHsic(this.dataSet.subsetColumns(indices), alphaNew);
    }

    /**
     * Determines whether variable x is independent of variable y given a list of conditioning variables z.
     *
     * @param x the one variable being compared.
     * @param y the second variable being compared.
     * @param z the list of conditioning variables.
     * @return true iff x _||_ y | z.
     */
    public boolean isIndependent(final Node y, final Node x, final List<Node> z) {

        final int m = sampleSize();

        // choose kernels using median distance heuristic
        final Kernel xKernel = new KernelGaussian(1);
        final Kernel yKernel = new KernelGaussian(1);
        final List<Kernel> zKernel = new ArrayList<>();
        yKernel.setDefaultBw(this.dataSet, y);
        xKernel.setDefaultBw(this.dataSet, x);
        if (!z.isEmpty()) {
            for (int i = 0; i < z.size(); i++) {
                final Kernel Zi = new KernelGaussian(1);
                Zi.setDefaultBw(this.dataSet, z.get(i));
                zKernel.add(Zi);
            }
        }

        // consruct Gram matricces
        Matrix Ky = null;
        Matrix Kx = null;
        Matrix Kz = null;
        // use incomplete Cholesky to approximate
        if (this.useIncompleteCholesky > 0) {
            Ky = KernelUtils.incompleteCholeskyGramMatrix(Arrays.asList(yKernel), this.dataSet, Arrays.asList(y), this.useIncompleteCholesky);
            Kx = KernelUtils.incompleteCholeskyGramMatrix(Arrays.asList(xKernel), this.dataSet, Arrays.asList(x), this.useIncompleteCholesky);
            if (!z.isEmpty()) {
                Kz = KernelUtils.incompleteCholeskyGramMatrix(zKernel, this.dataSet, z, this.useIncompleteCholesky);
            }
        }
        // otherwise compute directly
        else {
            Ky = KernelUtils.constructCentralizedGramMatrix(Arrays.asList(yKernel), this.dataSet, Arrays.asList(y));
            Kx = KernelUtils.constructCentralizedGramMatrix(Arrays.asList(xKernel), this.dataSet, Arrays.asList(x));
            if (!z.isEmpty()) {
                Kz = KernelUtils.constructCentralizedGramMatrix(zKernel, this.dataSet, z);
            }
        }

        // get Hilbert-Schmidt dependence measure
        if (z.isEmpty()) {
            if (this.useIncompleteCholesky > 0) {
                this.hsic = empiricalHSICincompleteCholesky(Ky, Kx, m);
            } else {
                this.hsic = empiricalHSIC(Ky, Kx, m);
            }
        } else {
            if (this.useIncompleteCholesky > 0) {
                this.hsic = empiricalHSICincompleteCholesky(Ky, Kx, Kz, m);
            } else {
                this.hsic = empiricalHSIC(Ky, Kx, Kz, m);
            }
        }

        // shuffle data for approximate the null distribution
        final double[] nullapprox = new double[this.perms];
        int[] zind = null;
        final int ycol = this.dataSet.getColumn(y);
        List<List<Integer>> clusterAssign = null;
        if (!z.isEmpty()) {
            // get clusters for z
            final KMeans kmeans = KMeans.randomClusters((m / 3));
            zind = new int[z.size()];
            for (int j = 0; j < z.size(); j++) {
                zind[j] = this.dataSet.getColumn(z.get(j));
            }
            kmeans.cluster(this.dataSet.subsetColumns(z).getDoubleData());
            clusterAssign = kmeans.getClusters();
        }
        for (int i = 0; i < this.perms; i++) {
            final DataSet shuffleData = this.dataSet.copy();
            // shuffle data
            if (z.isEmpty()) {
                final List<Integer> indicesList = new ArrayList<>();
                for (int j = 0; j < m; j++) {
                    indicesList.add(j);
                }
                Collections.shuffle(indicesList);
                for (int j = 0; j < m; j++) {
                    final double shuffleVal = this.dataSet.getDouble(indicesList.get(j), ycol);
                    shuffleData.setDouble(j, ycol, shuffleVal);
                }
            } else {
                // shuffle data within clusters
                for (int j = 0; j < clusterAssign.size(); j++) {
                    final List<Integer> shuffleCluster = new ArrayList<>(clusterAssign.get(j));

                    Collections.shuffle(shuffleCluster);

                    for (int k = 0; k < shuffleCluster.size(); k++) {
                        // first swap y;
                        double swapVal = this.dataSet.getDouble(clusterAssign.get(j).get(k), ycol);
                        shuffleData.setDouble(shuffleCluster.get(k), ycol, swapVal);
                        // now swap z
                        for (int zi = 0; zi < z.size(); zi++) {
                            swapVal = this.dataSet.getDouble(clusterAssign.get(j).get(k), zind[zi]);
                            shuffleData.setDouble(shuffleCluster.get(k), zind[zi], swapVal);
                        }
                    }
                }
            }
            // reset bandwidths
            yKernel.setDefaultBw(shuffleData, y);
            for (int j = 0; j < z.size(); j++) {
                zKernel.get(j).setDefaultBw(shuffleData, z.get(j));
            }
            // Gram matrices
            Matrix Kyn = null;
            if (this.useIncompleteCholesky > 0) {
                Kyn = KernelUtils.incompleteCholeskyGramMatrix(Arrays.asList(yKernel), shuffleData, Arrays.asList(y), this.useIncompleteCholesky);
            } else {
                Kyn = KernelUtils.constructCentralizedGramMatrix(Arrays.asList(yKernel), shuffleData, Arrays.asList(y));

            }
            Matrix Kzn = null;
            if (!z.isEmpty()) {
                if (this.useIncompleteCholesky > 0) {
                    Kzn = KernelUtils.incompleteCholeskyGramMatrix(zKernel, shuffleData, z, this.useIncompleteCholesky);
                } else {
                    Kzn = KernelUtils.constructCentralizedGramMatrix(zKernel, shuffleData, z);
                }
            }
            // HSIC
            if (z.isEmpty()) {
                if (this.useIncompleteCholesky > 0) {
                    nullapprox[i] = empiricalHSICincompleteCholesky(Kyn, Kx, m);
                } else {
                    nullapprox[i] = empiricalHSIC(Kyn, Kx, m);
                }
            } else {
                if (this.useIncompleteCholesky > 0) {
                    nullapprox[i] = empiricalHSICincompleteCholesky(Kyn, Kx, Kz, m);
                } else {
                    nullapprox[i] = empiricalHSIC(Kyn, Kx, Kz, m);
                }
            }
        }

        // permutation test to get p-value
        double evalCdf = 0.0;
        for (int i = 0; i < this.perms; i++) {
            if (nullapprox[i] <= this.hsic) {
                evalCdf += 1.0;
            }
        }

        evalCdf /= this.perms;
        this.pValue = 1.0 - evalCdf;

        // reject if pvalue <= alpha
        if (this.pValue <= this.alpha) {
            TetradLogger.getInstance().log("dependencies", SearchLogUtils
                    .dependenceFactMsg(x, y, z, getPValue()));
            return false;
        }
        if (this.verbose) {
            TetradLogger.getInstance().log("independencies", SearchLogUtils
                    .independenceFactMsg(x, y, z, getPValue()));
        }
        return true;
    }

    /**
     * Empirical unconditional Hilbert-Schmidt Dependence Measure for X and Y
     *
     * @param Ky centralized Gram matrix for Y
     * @param Kx centralized Gram matrix for X
     * @param m  sample size
     */
    public double empiricalHSIC(final Matrix Ky, final Matrix Kx, final int m) {
        final Matrix Kyx = Ky.times(Kx);
        double empHSIC = 0.0;
        for (int i = 0; i < m; i++) {
            empHSIC += Kyx.get(i, i);
        }
        empHSIC /= Math.pow(m - 1, 2);
        return empHSIC;
    }

    /**
     * Empirical unconditional Hilbert-Schmidt Dependence Measure for X and Y using incomplete Cholesky decomposition to
     * approximate Gram matrices
     *
     * @param Gy Choleksy approximate Gram matrix for Y
     * @param Gx Choleksy approximate Gram matrix for X
     * @param m  sample size
     */
    public double empiricalHSICincompleteCholesky(final Matrix Gy, final Matrix Gx, final int m) {
        // centralized Choleksy
        final int ky = Gy.columns();
        final int kx = Gx.columns();
        final Matrix H = KernelUtils.constructH(m);
        final Matrix Gcy = H.times(Gy);
        final Matrix Gcx = H.times(Gx);

        // multiply gram matrices
        final Matrix Gcyt = Gcy.transpose();
        final Matrix A = Gcyt.times(Gcx);
        final Matrix B = Gcy.times(A);
        final Matrix Gcxt = Gcx.transpose();
        double empHSIC = 0.0;
        for (int i = 0; i < m; i++) {
            empHSIC += matrixProductEntry(B, Gcxt, i, i);
        }
        empHSIC /= Math.pow(m - 1, 2);
        return empHSIC;
    }

    /**
     * Empirical conditional Hilbert-Schmidt Dependence Measure Y and X given Z
     *
     * @param Ky centralized Gram matrix for Y
     * @param Kx centralized Gram matrix for X
     * @param Kz centralized Gram matrix for Z
     * @param m  sample size
     */
    private double empiricalHSIC(final Matrix Ky, final Matrix Kx, final Matrix Kz, final int m) {
        final Matrix Kyx = Ky.times(Kx);
        final Matrix Kyz = Ky.times(Kz);
        final Matrix Kzx = Kz.times(Kx);
        Matrix Kzreg = Kz.copy();
        for (int i = 0; i < m; i++) {
            final double ent = (Kzreg.get(i, i) + this.regularizer);
            Kzreg.set(i, i, ent);
        }
        Matrix A = Kzreg.inverse();
        Kzreg = A.times(A);
        Matrix Kyzzregzx = new Matrix(m, m);
        A = Kyz.times(Kzreg);
        Kyzzregzx = A.times(Kzx);
        Matrix Kyzzregzxzzregz = Kyzzregzx.copy();
        Kyzzregzxzzregz = Kyzzregzx.times(Kz);
        A = Kyzzregzxzzregz.times(Kzreg);
        Kyzzregzxzzregz = A.times(Kz);
        // get trace
        double empHSIC = 0.0;
        for (int i = 0; i < m; i++) {
            empHSIC += Kyx.get(i, i);
            empHSIC += (-2 * Kyzzregzx.get(i, i));
            empHSIC += Kyzzregzxzzregz.get(i, i);
        }
        empHSIC /= Math.pow(m - 1, 2);
        double Bz = 0.0;
        for (int i = 0; i < (m - 1); i++) {
            for (int j = (i + 1); j < m; j++) {
                Bz += Math.pow(Kz.get(i, j), 2);
                Bz += Math.pow(Kz.get(j, i), 2);
            }
        }
        Bz = (m * (m - 1)) / Bz;
        empHSIC *= Bz;
        return empHSIC;
    }

    /**
     * Empirical unconditional Hilbert-Schmidt Dependence Measure for X and Y given Z using incomplete Cholesky
     * decomposition to approximate Gram matrices
     *
     * @param Gy Choleksy approximate Gram matrix for Y
     * @param Gx Choleksy approximate Gram matrix for X
     * @param Gz Choleksy approximate Gram matrix for Z
     * @param m  sample size
     */
    public double empiricalHSICincompleteCholesky(final Matrix Gy, final Matrix Gx, final Matrix Gz, final int m) {
        // centralize Choleksy
        final int ky = Gy.columns();
        final int kx = Gx.columns();
        final int kz = Gz.columns();

        final Matrix H = KernelUtils.constructH(m);
        final Matrix Gcy = H.times(Gy);
        final Matrix Gcx = H.times(Gx);
        final Matrix Gcz = H.times(Gz);

        // multiply gram matrices (first block)
        Matrix A = new Matrix(ky, kx);
        final Matrix Gcyt = Gcy.transpose();
        A = Gcyt.times(Gcx);
        final Matrix B = Gcy.times(A);
        Matrix Kyx = new Matrix(m, m);
        Matrix Gcxt = new Matrix(kx, m);
        Gcxt = Gcx.transpose();
        Kyx = B.times(Gcxt);
        double empHSIC = 0.0;
        final double xy = 0.0;
        for (int i = 0; i < m; i++) {
            empHSIC += matrixProductEntry(B, Gcxt, i, i);
        }

        // second block
        final Matrix Gytz = Gcyt.times(Gcz);
        final Matrix Gczt = Gcz.transpose();
        final Matrix Gztx = Gczt.times(Gcx);
        final Matrix Gztz = Gczt.times(Gcz);
        final Matrix Gztzr = Gztz.copy();
        for (int i = 0; i < kz; i++) {
            Gztzr.set(i, i, Gztz.get(i, i) + this.regularizer);
        }
        // invert matrix
        final Matrix ZI = Gztzr.inverse();
        final Matrix ZIzt = ZI.times(Gczt);
        final Matrix Gzr = Gcz.copy();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < kz; j++) {
                Gzr.set(i, j, Gcz.get(i, j) * (-1.0 / this.regularizer));
            }
        }
        final Matrix Zinv = Gzr.times(ZIzt);
        for (int i = 0; i < m; i++) {
            Zinv.set(i, i, Zinv.get(i, i) + (1.0 / this.regularizer));
        }
        final Matrix Gztzinv = Gczt.times(Zinv);
        final Matrix Gzinvz = Zinv.times(Gcz);
        final Matrix Gztinv2z = Gztzinv.times(Gzinvz);
        final Matrix Gytzztzinv2z = Gytz.times(Gztinv2z);
        final Matrix Gytzztzinv2zztx = Gytzztzinv2z.times(Gztx);
        final Matrix Gyytzztzinv2zztx = Gcy.times(Gytzztzinv2zztx);
        double second = 0.0;
        for (int i = 0; i < m; i++) {
            second += matrixProductEntry(Gyytzztzinv2zztx, Gcxt, i, i);
        }
        empHSIC -= 2 * second;

        // third block
        final Matrix Gxtz = Gcxt.times(Gcz);
        final Matrix Gxtzztinv2z = Gxtz.times(Gztinv2z);
        final Matrix Gyytzztzinv2zztxxtzztinv2z = Gyytzztzinv2zztx.times(Gxtzztinv2z);
        for (int i = 0; i < m; i++) {
            empHSIC += matrixProductEntry(Gyytzztzinv2zztxxtzztinv2z, Gczt, i, i);
        }

        // beta z estimate
        double betaz = 0.0;
        for (int i = 0; i < (m - 1); i++) {
            for (int j = (i + 1); j < m; j++) {
                betaz += Math.pow(matrixProductEntry(Gcz, Gczt, i, j), 2);
                betaz += Math.pow(matrixProductEntry(Gcz, Gczt, j, i), 2);
            }
        }

        empHSIC *= (m / (betaz * (m - 1)));

        return empHSIC;
    }

    /**
     * Empirical unconditional Hilbert-Schmidt Dependence Measure for X and Y given Z using incomplete Cholesky
     * decomposition to approximate Gram matrices
     *
     * @param Gy Choleksy approximate Gram matrix for Y
     * @param Gx Choleksy approximate Gram matrix for X
     * @param Gz Choleksy approximate Gram matrix for Z
     * @param m  sample size
     */
    public double empiricalHSICincompleteCholeskyOLD(final Matrix Gy, final Matrix Gx, final Matrix Gz, final int m) {
        // centralize Choleksy
        final int ky = Gy.columns();
        final int kx = Gx.columns();
        final int kz = Gz.columns();

        final Matrix H = KernelUtils.constructH(m);
        final Matrix Gcy = H.times(Gy);
        final Matrix Gcx = H.times(Gx);
        final Matrix Gcz = H.times(Gz);

        // multiply gram matrices (first block)
        final Matrix Gcyt = Gcy.transpose();
        final Matrix A = Gcyt.times(Gcx);
        final Matrix B = Gcy.times(A);
        final Matrix Gcxt = Gcx.transpose();
        final Matrix Kyx = B.times(Gcxt);
        double empHSIC = 0.0;

        final double xy = 0.0;
        for (int i = 0; i < m; i++) {
            empHSIC += matrixProductEntry(B, Gcxt, i, i);
        }

        // second block
        final Matrix Gytz = Gcyt.times(Gcz);
        Matrix Gztx = new Matrix(kz, kx);
        final Matrix Gczt = Gcz.transpose();
        Gztx = Gczt.times(Gcx);
        final Matrix Gztz = Gczt.times(Gcz);
        final Matrix Gztzztx = Gztz.times(Gztx);
        final Matrix Gytzztzztx = Gytz.times(Gztzztx);
        final Matrix Gyytzztzztx = Gcy.times(Gytzztzztx);
        double second = 0.0;
        for (int i = 0; i < m; i++) {
            second += matrixProductEntry(Gyytzztzztx, Gcxt, i, i);
        }
        final Matrix Gztzr = Gztz.copy();
        for (int i = 0; i < kz; i++) {
            Gztzr.set(i, i, Gztz.get(i, i) + this.regularizer);
        }
        final Matrix ZI = Gztzr.inverse();
        final Matrix GzGZI = Gcz.times(ZI);
        final Matrix GzGZIGzt = GzGZI.times(Gczt);
        double inv = 0.0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                GzGZIGzt.set(i, j, GzGZIGzt.get(i, j) * (-1 / this.regularizer));
            }
        }
        for (int i = 0; i < m; i++) {
            GzGZIGzt.set(i, i, GzGZIGzt.get(i, i) + (1 / this.regularizer));
        }
        for (int i = 0; i < m; i++) {
            inv += GzGZIGzt.get(i, i);
        }
        System.out.println("inv " + inv);
        inv = 0.0;
        final Matrix ZI2 = GzGZIGzt.times(GzGZIGzt);
        for (int i = 0; i < m; i++) {
            inv += ZI2.get(i, i);
        }
        System.out.println("inv " + inv);
        final Matrix Gyytz = Gcy.times(Gytz);
        final Matrix Gyytzzt = Gyytz.times(Gczt);
        final Matrix Gzztx = Gcz.times(Gztx);
        final Matrix Gzztxxt = Gzztx.times(Gcxt);
        final Matrix GyzZI = Gyytzzt.times(ZI2);
        final Matrix GyzZIzx = GyzZI.times(Gzztxxt);
        double sec = 0.0;
        for (int i = 0; i < m; i++) {
            sec += GyzZIzx.get(i, i);
        }
        System.out.println("sec " + sec);
        //
        final Matrix Gytzztz = Gytz.times(Gztz);
        final Matrix GytzztzZI = Gytzztz.times(ZI);
        final Matrix GytzztzZIztzztx = GytzztzZI.times(Gztzztx);
        final Matrix GyytzztzZIztzztx = Gcy.times(GytzztzZIztzztx);
        double s1 = 0.0;
        for (int i = 0; i < m; i++) {
            s1 += matrixProductEntry(GyytzztzZIztzztx, Gcxt, i, i);
        }
        second -= 2 * s1;
        final Matrix GZIztzztx = ZI.times(Gztzztx);
        final Matrix GytzztzZIztz = GytzztzZI.times(Gztz);
        final Matrix GytzztzZIztzZIztzztx = GytzztzZIztz.times(GZIztzztx);
        final Matrix GyytzztzZIztzZIztzztx = Gcy.times(GytzztzZIztzZIztzztx);
        for (int i = 0; i < m; i++) {
            second += matrixProductEntry(GyytzztzZIztzZIztzztx, Gcxt, i, i);
        }
        final double reg2 = Math.pow(this.regularizer, 2);
        empHSIC -= (2 / reg2) * second;

        // third block
        final Matrix Gxtz = Gcxt.times(Gcz);
        final Matrix Gxtzztz = Gxtz.times(Gztz);
        final Matrix Gxtzztzzt = Gxtzztz.times(Gczt);
        final Matrix GxtzztzZI = Gxtzztz.times(ZI);
        final Matrix GxtzztzZIztz = GxtzztzZI.times(Gztz);
        final Matrix GxtzztzZIztzzt = GxtzztzZIztz.times(Gczt);
        final Matrix GxtzztzZIztzZI = GxtzztzZIztz.times(ZI);
        final Matrix GxtzztzZIztzZIztz = GxtzztzZIztzZI.times(Gztz);
        final Matrix GxtzztzZIztzZIztzzt = GxtzztzZIztzZIztz.times(Gczt);
        double third = 0.0;
        for (int i = 0; i < m; i++) {
            third += matrixProductEntry(GyytzztzZIztzztx, Gxtzztzzt, i, i);
            third += matrixProductEntry(GyytzztzZIztzztx, GxtzztzZIztzZIztzzt, i, i);
            third += matrixProductEntry(Gyytzztzztx, GxtzztzZIztzzt, i, i);
            third += matrixProductEntry(GyytzztzZIztzZIztzztx, GxtzztzZIztzzt, i, i);
        }
        third *= -2;
        for (int i = 0; i < m; i++) {
            third += matrixProductEntry(Gyytzztzztx, Gxtzztzzt, i, i);
            third += matrixProductEntry(GyytzztzZIztzZIztzztx, Gxtzztzzt, i, i);
            third += matrixProductEntry(Gyytzztzztx, GxtzztzZIztzZIztzzt, i, i);
            third += matrixProductEntry(GyytzztzZIztzZIztzztx, GxtzztzZIztzZIztzzt, i, i);
        }
        double t1 = 0.0;
        for (int i = 0; i < m; i++) {
            t1 += matrixProductEntry(GyytzztzZIztzztx, GxtzztzZIztzzt, i, i);
        }
        third += 4 * t1;
        empHSIC += third / Math.pow(reg2, 2);

        // beta z estimate
        double betaz = 0.0;
        for (int i = 0; i < (m - 1); i++) {
            for (int j = (i + 1); j < m; j++) {
                betaz += Math.pow(matrixProductEntry(Gcz, Gczt, i, j), 2);
                betaz += Math.pow(matrixProductEntry(Gcz, Gczt, j, i), 2);
            }
        }

        empHSIC *= (m / (betaz * (m - 1)));

        return empHSIC;
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

    public double getHsic() {
        return this.hsic;
    }

    public double getThreshold() {
        return this.thresh;
    }

    /**
     * @return the probability associated with the most recently computed independence test.
     */
    public double getPValue() {
        return this.pValue;
    }

    /**
     * Sets the significance level at which independence judgments should be made.
     */
    public void setAlpha(final double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range.");
        }

        this.alpha = alpha;
        this.thresh = Double.NaN;
    }

    /**
     * Sets the precision for the Incomplete Choleksy factorization method for approximating Gram matrices. A value <= 0
     * indicates that the Incomplete Cholesky method should not be used and instead use the exact matrices.
     */
    public void setIncompleteCholesky(final double precision) {
        this.useIncompleteCholesky = precision;
    }

    /**
     * Set the number of bootstrap samples to use
     */
    public void setPerms(final int perms) {
        this.perms = perms;
    }

    /**
     * Sets the regularizer
     */
    public void setRegularizer(final double regularizer) {
        this.regularizer = regularizer;
    }

    /**
     * Gets the getModel significance level.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * Gets the getModel precision for the Incomplete Cholesky
     */
    public double getPrecision() {
        return this.useIncompleteCholesky;
    }

    /**
     * Gets the getModel number of bootstrap samples used
     */
    public int getPerms() {
        return this.perms;
    }

    /**
     * Gets the getModel regularizer
     */
    public double getRegularizer() {
        return this.regularizer;
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
        for (int i = 0; i < getVariables().size(); i++) {
            final Node variable = getVariables().get(i);
            if (variable.getName().equals(name)) {
                return variable;
            }
        }
        return null;
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
        return getPValue();
    }

    public void shuffleVariables() {
        final List<Node> nodes = new ArrayList(this.variables);
        Collections.shuffle(nodes);
        this.variables = Collections.unmodifiableList(nodes);
    }

    /**
     * @return a string representation of this test.
     */
    public String toString() {
        return "HSIC, alpha = " + IndTestHsic.nf.format(getAlpha());
    }

    public boolean determines(final List z, final Node x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Method not implemented");
    }

    //==========================PRIVATE METHODS============================//


    private int sampleSize() {
        return this.dataSet.getNumRows();
    }

    private double matrixProductEntry(final Matrix X, final Matrix Y, final int i, final int j) {
        double entry = 0.0;
        for (int k = 0; k < X.columns(); k++) {
            entry += X.get(i, k) * Y.get(k, j);
        }
        return entry;
    }

    private static double trace(final Matrix A, final int m) {
        double trace = 0.0;
        for (int i = 0; i < m; i++) {
            trace += A.get(i, i);
        }
        return trace;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(final boolean verbose) {
        this.verbose = verbose;
    }
}



