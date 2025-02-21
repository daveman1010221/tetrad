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

package edu.cmu.tetrad.bayes;

/**
 * Q-factors, for use in Identifiability
 *
 * @author Choh Man Teng
 */
final class QList {

    /**
     * The number of nodes.
     */
    private final int nVariables; // number of nodes

    /**
     * The variables to be summed over in the summation.
     */
    private boolean inNumerator;   // true: in the numerator; false: in the denominator

    /**
     * The variables to be summed over in the summation.
     */
    private int[] sumOverVariables; // the variables to be summed over in the summation

    /**
     * probability term. only one of probTerm and subList should be instantiated
     */
    private int[] probTerm;

    /**
     * recursive list to be summed over sumOverVariables
     */
    private QList subList;

    /**
     * The next term in the list.
     */
    private QList nextTerm;  // next term in the list

    //=============================CONSTRUCTORS=========================//

    /////////////////////////////////////////////////////////////////
    // intialize an empty QList: Q(emptyset) = 1

    /**
     * <p>Constructor for QList.</p>
     *
     * @param nVariables a int
     */
    public QList(int nVariables) {
        this.nVariables = nVariables;
        initialize();
    }

    /////////////////////////////////////////////////////////////////
    // initialize with a probability term

    /**
     * <p>Constructor for QList.</p>
     *
     * @param nVariables a int
     * @param probTerm   an array of  objects
     */
    public QList(int nVariables, int[] probTerm) {
        this.nVariables = nVariables;
        initialize();

        System.arraycopy(probTerm, 0, this.probTerm, 0, nVariables);

        this.inNumerator = true;
    }

    /////////////////////////////////////////////////////////////////
    // copy an existing QList
    //
    private QList(QList q) {
        this.nVariables = q.nVariables;
        initialize();

        for (int i = 0; i < this.nVariables; i++) {
            this.sumOverVariables[i] = q.sumOverVariables[i];
            this.probTerm[i] = q.probTerm[i];
        }

        this.inNumerator = q.inNumerator;

        if (q.subList != null) {
            this.subList = new QList(q.subList);
        }

        if (q.nextTerm != null) {
            this.nextTerm = new QList(q.nextTerm);
        }

    }

    //==========================PUBLIC METHODS==========================//


    /////////////////////////////////////////////////////////////////
    // append a term to the end
    //

    /**
     * <p>add.</p>
     *
     * @param q                a {@link edu.cmu.tetrad.bayes.QList} object
     * @param sumOverVariables an array of  objects
     * @param inNumerator      a boolean
     */
    public void add(QList q, int[] sumOverVariables, boolean inNumerator) {
        QList qAdd = new QList(this.nVariables);

        qAdd.initialize();

        qAdd.inNumerator = inNumerator;
        System.arraycopy(sumOverVariables, 0, qAdd.sumOverVariables, 0, this.nVariables);
        qAdd.subList = new QList(q);

        // append qAdd
        if (this.nextTerm == null) {
            this.nextTerm = new QList(qAdd);
        } else {
            QList curTerm = this.nextTerm;
            while (curTerm.nextTerm != null) {
                curTerm = curTerm.nextTerm;
            }
            curTerm.nextTerm = new QList(qAdd);
        }
    }

    /////////////////////////////////////////////////////////////////
    // print
    // index1: the numbering of terms in the linked list
    // index2: the depth of recursive sublists
    //

    /**
     * <p>printQList.</p>
     *
     * @param index1 a int
     * @param index2 a int
     */
    public void printQList(int index1, int index2) {
        System.out.println("======= " + index1 + "  " + index2 +
                           ": printQList");
        System.out.println("inNumerator: " + this.inNumerator);

        System.out.print("sumOverVariables: ");
        for (int i = 0; i < this.nVariables; i++) {
            System.out.print(this.sumOverVariables[i] + "  ");
        }
        System.out.println();

        if (this.subList == null)    // probTerm is instantiated
        {
            System.out.print("probTerm: ");
            for (int i = 0; i < this.nVariables; i++) {
                System.out.print(this.probTerm[i] + "  ");
            }
            System.out.println();
        } else
        // subList != null
        // increment index1 and reset index2 to 0
        {
            System.out.println("--------------------------------------- "
                               + (index1 + 1) + "  " + 0 +
                               ": subList");
            this.subList.printQList(index1 + 1, 0);
        }

        if (this.nextTerm != null) {
            index2++;
            System.out.println("---------------------------- "
                               + index1 + "  " + index2 +
                               ": nextTerm");
            this.nextTerm.printQList(index1, index2);
        }
    }

    /////////////////////////////////////////////////////////////////
    // compute the numeric value of the expression in qPTS
    // by using the RowSummingExactUpdater
    //

    /**
     * <p>computeValue.</p>
     *
     * @param bayesIm        a {@link edu.cmu.tetrad.bayes.BayesIm} object
     * @param fixedVarValues an array of  objects
     * @return a double
     */
    public double computeValue(BayesIm bayesIm, int[] fixedVarValues) {
        double resultAll = 0.0;
        double resultOneConfig;
        double resultNextTerm = 1.0;

        // updater with no evidence

        int nNodes = bayesIm.getNumNodes();

        int[] loopVarValues = new int[nNodes];

        // starting loop variable configuration
        for (int i = 0; i < nNodes; i++) {
            // inherit values from fixedVarValues
            loopVarValues[i] = fixedVarValues[i];

            // reset the values of the variables to be summed
            if (this.sumOverVariables[i] == 1) {
                // Set node i to its first value
                // It is possible (and legal) to overwrite given values
                // in S and T (and fixedVarValues)
                loopVarValues[i] = 0;
            }
        }

        // next variable to be incremented
        int curVar = nNodes - 1;

        while (curVar >= 0) {

            // one iteration of computing with this variable value
            // configuration

            if (this.subList == null)   // probTerm is instantiated
            {

                // the number of variables represented in probTerm
                int nVarInMarginal = 0;
                for (int i = 0; i < nNodes; i++) {
                    if (this.probTerm[i] == 1) {
                        nVarInMarginal++;
                    }
                }

                int[] pVar = new int[nVarInMarginal];
                int[] pValues = new int[nVarInMarginal];
                int pIndex = 0;
                for (int i = 0; i < nNodes; i++) {
                    if (this.probTerm[i] == 1) {
                        pVar[pIndex] = i;

                        // This variable does not have an instantiated value
                        // and is not one of the ones to be summed.
                        // (It does not matter what value is set as long as
                        // the value is consistent across all cases.
                        // The probabilities with uninstantiated variables
                        // should cancelAll out in the end.)
                        if (loopVarValues[i] == -1) {
                            pValues[pIndex] = 0;
                        } else {
                            pValues[pIndex] = loopVarValues[i];
                        }

                        pIndex++;
                    }
                }

                //resultOneConfig = rowSumUpdater.getJointMarginal(pVar,
                //												 pValues);

                // We do not need this: the "else" condition will never happen

                // skip over combinations where the latent variables
                // do not have values 0
                // This is so as to avoid duplicate summing with only
                // the latent variable values varying
                /*
                boolean flag = true;
				for (int i = 0; i < nVarInMarginal; i++)
				{
					if ((bayesIm.getNode(pVar[i]).getNodeType() == 
														NodeType.LATENT)
						&&
						(pValues[i] != 0))
					{
						flag = false;
						break;
					}
				}
				
				if (flag)
				{
				 */
                Proposition prop = Proposition.tautology(bayesIm);
                for (int i = 0; i < nVarInMarginal; i++) {
                    prop.setCategory(pVar[i], pValues[i]);
                }
                // restrict the proposition to only observed variables
                Proposition propObs =
                        new Proposition(((MlBayesImObs) bayesIm).getBayesImObs(), prop);

                resultOneConfig = ((MlBayesImObs) bayesIm).getJPD().getProb(propObs);
				/*
				}
				else  // skip over extra latent variable combinations
				{
					resultOneConfig = 0.0;
				}
				 */


            } else // process subList
            {

                resultOneConfig = this.subList.computeValue(bayesIm, loopVarValues);
            }

            // sum the result from this iteration
            resultAll = resultAll + resultOneConfig;

            // reset

            // compose next variable value configuration
            if ((this.sumOverVariables[curVar] == 1) &&
                (loopVarValues[curVar] < bayesIm.getNumColumns(curVar) - 1)
            ) {
                loopVarValues[curVar]++;
            } else {
                while ((curVar >= 0)
                       &&
                       ((this.sumOverVariables[curVar] != 1)
                        ||
                        (loopVarValues[curVar] ==
                         bayesIm.getNumColumns(curVar) - 1))
                ) {
                    curVar--;
                }

                if (curVar >= 0) {
                    loopVarValues[curVar]++;

                    for (int j = curVar + 1; j < nNodes; j++) {
                        if (this.sumOverVariables[j] == 1) {
                            loopVarValues[j] = 0;
                        }
                    }
                    curVar = nNodes - 1;
                }

            }
        }

        if (this.nextTerm != null)  // next term
        {

            resultNextTerm = this.nextTerm.computeValue(bayesIm, fixedVarValues);

        }


        if (this.inNumerator) {
            return (resultAll * resultNextTerm);
        } else // in the denominator
        {
            return ((1.0 / resultAll) * resultNextTerm);

        }

    }

    //==============================PRIVATE METHODS=======================//

    /////////////////////////////////////////////////////////////////
    // initialization
    //
    private void initialize() {
        this.sumOverVariables = new int[this.nVariables];
        this.probTerm = new int[this.nVariables];

        for (int i = 0; i < this.nVariables; i++) {
            this.sumOverVariables[i] = 0;
            this.probTerm[i] = 0;   // Q(emptyset) = 1
        }

        this.inNumerator = true;

        this.subList = null;
        this.nextTerm = null;
    }
}




