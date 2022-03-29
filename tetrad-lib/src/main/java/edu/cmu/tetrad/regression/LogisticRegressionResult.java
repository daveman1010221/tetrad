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

package edu.cmu.tetrad.regression;

import edu.cmu.tetrad.util.TetradSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;


/**
 * Stores the various components of a logistic regression result in a single class so
 * that they can be passed together as an argument or return value.
 *
 * @author Frank Wimberly
 */
public class LogisticRegressionResult implements TetradSerializable {
    static final long serialVersionUID = 23L;

    /**
     * String representation of the result
     */
    private final String result;


    /**
     * The variables.
     */
    private final String[] variableNames;


    /**
     * The target.
     */
    private final String target;

    /**
     * The number of data points with target = 0.
     */
    private final int ny0;

    /**
     * The number of data points with target = 1.
     */
    private final int ny1;


    /**
     * The number of regressors.
     */
    private final int numRegressors;

    /**
     * The array of regression coefficients.
     */
    private final double[] coefs;

    /**
     * The array of standard errors for the regression coefficients.
     */
    private final double[] stdErrs;

    /**
     * The array of coefP-values for the regression coefficients.
     */
    private final double[] probs;


    /**
     * THe array of means.
     */
    private final double[] xMeans;


    /**
     * The array of standard devs.
     */
    private final double[] xStdDevs;


    private final double intercept;

    /**
     * The log likelyhood of the regression
     */
    private final double logLikelihood;


    /**
     * Constructs a new LinRegrResult.
     *
     * @param ny0           the number of cases with target = 0.
     * @param ny1           the number of cases with target = 1.
     * @param numRegressors the number of regressors
     * @param coefs         the array of regression coefficients.
     * @param stdErrs       the array of std errors of the coefficients.
     * @param probs         the array of P-values for the regression
     *                      coefficients.
     */
    private LogisticRegressionResult(final String target, final String[] variableNames, final double[] xMeans, final double[] xStdDevs,
                                     final int numRegressors, final int ny0, final int ny1, final double[] coefs,
                                     final double[] stdErrs, final double[] probs, final double intercept, final String result, final double logLikelihood) {


        if (variableNames.length != numRegressors) {
            throw new IllegalArgumentException();
        }

        if (coefs.length != numRegressors + 1) {
            throw new IllegalArgumentException();
        }

        if (stdErrs.length != numRegressors + 1) {
            throw new IllegalArgumentException();
        }

        if (probs.length != numRegressors + 1) {
            throw new IllegalArgumentException();
        }

        if (xMeans.length != numRegressors + 1) {
            throw new IllegalArgumentException();
        }

        if (xStdDevs.length != numRegressors + 1) {
            throw new IllegalArgumentException();
        }
        if (target == null) {
            throw new NullPointerException();
        }

        this.intercept = intercept;
        this.target = target;
        this.xMeans = xMeans;
        this.xStdDevs = xStdDevs;
        this.variableNames = variableNames;
        this.numRegressors = numRegressors;
        this.ny0 = ny0;
        this.ny1 = ny1;
        this.coefs = coefs;
        this.stdErrs = stdErrs;
        this.probs = probs;
        this.result = result;
        this.logLikelihood = logLikelihood;
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static LogisticRegressionResult serializableInstance() {
        final double[] values = {1.0, 2.0};
        return new LogisticRegressionResult("X1", new String[]{"X2"}, values, values, 1, 2, 3,
                values, values, values, 1.5, "", 0.0);
    }

    //================================== Public Methods =======================================//

    public String getTarget() {
        return this.target;
    }

    public double getIntercept() {
        return this.intercept;
    }

    /**
     * @return the number of regressors.
     */
    public int getNumRegressors() {
        return this.numRegressors;
    }

    /**
     * @return the number of cases with target = 0.
     */
    public int getNy0() {
        return this.ny0;
    }

    /**
     * @return the number of cases with target = 1.
     */
    public int getNy1() {
        return this.ny1;
    }

    /**
     * @return the total number of cases.
     */
    public int getnCases() {
        return this.ny0 + this.ny1;
    }

    /**
     * @return the array of strings containing the variable names.
     */
    public String[] getVariableNames() {
        return this.variableNames;
    }

    /**
     * @return the array of regression coeffients.
     */
    public double[] getCoefs() {
        return this.coefs;
    }

    /**
     * @return the array of coefT-statistics for the regression coefficients.
     */
    public double[] getStdErrs() {
        return this.stdErrs;
    }

    /**
     * @return the array of coefP-values for the regression coefficients.
     */
    public double[] getProbs() {
        return this.probs;
    }

    public String[] getVarNames() {
        return this.variableNames;
    }

    public String toString() {
        return this.result;
    }

    public double[] getxMeans() {
        return this.xMeans;
    }

    public double[] getxStdDevs() {
        return this.xStdDevs;
    }

    /**
     * @return -2LogLiklihood
     */
    public double getLogLikelihood() {
        return this.logLikelihood;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     *
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (this.xMeans == null) {
            throw new NullPointerException();
        }
        if (this.xStdDevs == null) {
            throw new NullPointerException();
        }
        if (this.variableNames == null) {
            throw new NullPointerException();
        }
        if (this.coefs == null) {
            throw new NullPointerException();
        }
        if (this.stdErrs == null) {
            throw new NullPointerException();
        }
        if (this.probs == null) {
            throw new NullPointerException();
        }
        if (this.result == null) {
            throw new NullPointerException();
        }
        if (this.target == null) {
            throw new NullPointerException();
        }

    }
}






