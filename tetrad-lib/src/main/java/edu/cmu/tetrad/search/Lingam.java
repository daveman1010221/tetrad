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
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.utils.PermutationMatrixPair;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.TetradLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.math3.util.FastMath.*;

/**
 * <p>Implements the ICA LiNGAM algorithm. The reference is here:</p>
 *
 * <p>Shimizu, S., Hoyer, P. O., Hyvärinen, A., Kerminen, A., &amp; Jordan, M. (2006).
 * A linear non-Gaussian acyclic model for causal discovery. Journal of Machine Learning
 * Research, 7(10).</p>
 *
 * <p>The focus for this implementation was making to make a version of LiNGAM that
 * would be compatible with LiNG-D (see). There are two parameters, one to choose
 * whether an acyclic result will be guaranteed, and another to set a threshold on
 * the absolute value of the coefficients in the B Hat matrix. The latter is used
 * to find edges in the final graph.</p>
 *
 * <p>LiNGAM is a method for estimating a causal graph from a dataset. It is based on
 * the assumption that the data are generated by a linear model with non-Gaussian
 * noise. The method is based on the following assumptions:</p>
 *
 * <ol>
 *     <li>The data are generated by a linear model with non-Gaussian noise.</li>
 *     <li>The noise is independent across variables.</li>
 *     <li>The noises for all but possibly one variable are non-Gaussian.</li>
 * </ol>
 *
 * <p>Under these assumptions, the method estimates a matrix W such that WX = e, where
 * X is the data matrix, e is a matrix of noise, and W is a matrix of coefficients.
 * The matrix W is then used to estimate a matrix B Hat, where B Hat is the matrix
 * of coefficients in the linear model that generated the data. The graph is then
 * estimated by finding edges in B Hat.</p>
 *
 * <p>There is an option to guarantee the acyclicity of the output, which will set
 * small coefficients to zero until an acyclic model is achieved. If this option
 * is not selected, coefficients above the threshold will be sorted to high and
 * 5% of the lowest coefficients in B Hat set to zero, which allows for certain
 * cyclic structures to be recovered.</p>
 *
 * <p>There are two methods for estimating W. The first is the default method,
 * which is to use the LiNG-D algorithm to estimate W. The second is to provide
 * a W matrix estimated by some other method. The latter method is useful for
 * comparing the performance of LiNGAM to other methods.</p>
 *
 * <p>There is an option to set a threshold on the coefficients in B Hat. This
 * threshold is used to find edges in the final graph.</p>
 *
 * <p>The method is implemented as follows:</p>
 *
 * <ol>
 *     <li>Estimate W using LiNG-D or using a user-provided W matrix.</li>
 *     <li>Estimate B Hat from W.</li>
 *     <li>Set a threshold on the absolute value of the coefficients in B Hat.</li>
 *     <li>Find edges in B Hat.</li>
 *     <li>Set small coefficients to zero until an acyclic model is achieved, if
 *     acyclicity is guaranteed.</li>
 * </ol>
 *
 * <p>We are using the Hungarian Algorithm to find the best diagonal for W</p>
 *
 * <p>This class is not configured to respect   knowledge of forbidden and required
 * edges.</p>
 *
 * @author josephramsey
 * @see LingD
 * @see edu.cmu.tetrad.search.utils.HungarianAlgorithm
 */
public class Lingam {
    private double bThreshold = 0.1;
    private boolean acyclicityGuaranteed = true;

    /**
     * Constructor..
     */
    public Lingam() {
    }

    /**
     * Fits a LiNGAM model to the given dataset using a default method for estimating W.
     *
     * @param D A continuous dataset.
     * @return The BHat matrix, where B[i][j] gives the coefficient of j->i if nonzero.
     */
    public Matrix fit(DataSet D) {
        Matrix W = LingD.estimateW(D, 5000, 1e-6, 1.2);
        return fitW(W);
    }

    /**
     * Searches given a W matrix is that is provided by the user (where WX = e).
     *
     * @param W A W matrix estimated by the user, possibly by some other method.
     * @return The estimated B Hat matrix.
     */
    public Matrix fitW(Matrix W) {
        PermutationMatrixPair bestPair = LingD.hungarianDiagonal(W);
        Matrix scaledBHat = LingD.getScaledBHat(bestPair, bThreshold);

        class Record {
            double coef;
            int i;
            int j;
        }

        LinkedList<Record> coefs = new LinkedList<>();

        for (int i = 0; i < scaledBHat.getNumRows(); i++) {
            for (int j = 0; j < scaledBHat.getNumColumns(); j++) {
                if (i != j && scaledBHat.get(i, j) > 0.1) {
                    Record record = new Record();
                    record.coef = scaledBHat.get(i, j);
                    record.i = i;
                    record.j = j;

                    coefs.add(record);
                }
            }
        }

        coefs.sort(Comparator.comparingDouble(o -> abs(o.coef)));

        if (acyclicityGuaranteed) {
            List<Node> dummyVars = new ArrayList<>();

            for (int i = 0; i < scaledBHat.getNumRows(); i++) {
                dummyVars.add(new GraphNode("dummy" + i));
            }

            Record coef = coefs.getFirst();

            while (true) {
                if (LingD.isAcyclic(scaledBHat, dummyVars)) {
                    TetradLogger.getInstance().forceLogMessage("Effective threshold = " + coef.coef);
                    return scaledBHat;
                }

                coef = coefs.removeFirst();
                scaledBHat.set(coef.i, coef.j, 0.0);
            }
        } else {
            return scaledBHat;
        }
    }

    /**
     * The threshold to use for set small elemtns to zerp in the B Hat matrices for the
     * LiNGAM algorithm.
     *
     * @param bThreshold Some value >= 0.
     */
    public void setBThreshold(double bThreshold) {
        if (bThreshold < 0) throw new IllegalArgumentException("Expecting a non-negative number: " + bThreshold);
        this.bThreshold = bThreshold;
    }

    /**
     * Whether the LiNGAM algorithm is guaranteed to produce an acyclic graph. This is
     * implemnted by setting small coefficients in B hat to zero until an acyclic model is
     * found.
     *
     * @param acyclicityGuaranteed True if so.
     */
    public void setAcyclicityGuaranteed(boolean acyclicityGuaranteed) {
        this.acyclicityGuaranteed = acyclicityGuaranteed;
    }
}

