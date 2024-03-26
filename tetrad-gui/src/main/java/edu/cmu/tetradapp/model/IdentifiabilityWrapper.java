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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.bayes.*;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.session.SessionModel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.text.NumberFormat;

/**
 * Wraps a Bayes Updater for use in the Tetrad application.
 *
 * @author josephramsey
 * @version $Id: $Id
 */

///////////////////////////////////////
// Identifiability wrapper
// based on RowSummingExactWrapper
///////////////////////////////////////
public class IdentifiabilityWrapper implements SessionModel, UpdaterWrapper, Unmarshallable {
    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * The Bayes updater.
     */
    private ManipulatingBayesUpdater bayesUpdater;

    /**
     * The name of the model.
     */
    private String name;

    /**
     * The params object, so the GUI can remember stuff for logging.
     */
    private Parameters params;

    //=============================CONSTRUCTORS============================//

    /**
     * <p>Constructor for IdentifiabilityWrapper.</p>
     *
     * @param wrapper a {@link edu.cmu.tetradapp.model.BayesImWrapperObs} object
     * @param params  a {@link edu.cmu.tetrad.util.Parameters} object
     */
    public IdentifiabilityWrapper(BayesImWrapperObs wrapper, Parameters params) {
        if (wrapper == null) {
            throw new NullPointerException();
        }

        BayesIm bayesIm = wrapper.getBayesIm();
        setup(bayesIm, params);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return a {@link edu.cmu.tetradapp.model.PcRunner} object
     * @see TetradSerializableUtils
     */
    public static PcRunner serializableInstance() {
        return PcRunner.serializableInstance();
    }

    //==============================PUBLIC METHODS========================//

    /**
     * <p>Getter for the field <code>bayesUpdater</code>.</p>
     *
     * @return a {@link edu.cmu.tetrad.bayes.ManipulatingBayesUpdater} object
     */
    public ManipulatingBayesUpdater getBayesUpdater() {
        return this.bayesUpdater;
    }

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public void setName(String name) {
        this.name = name;
    }

    //===============================PRIVATE METHODS======================//

    private void setup(BayesIm bayesIm, Parameters params) {
        TetradLogger.getInstance().setConfigForClass(this.getClass());
        this.params = params;
        if (params.get("evidence", null) == null || ((Evidence) params.get("evidence", null)).isIncompatibleWith(bayesIm)) {
            this.bayesUpdater = new Identifiability(bayesIm);
        } else {
            this.bayesUpdater = new Identifiability(bayesIm,
                    (Evidence) params.get("evidence", null));
        }


        Node node = (Node) getParams().get("variable", null);

        if (node != null) {
            NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

            TetradLogger.getInstance().forceLogMessage("\nIdentifiability");

            String nodeName = node.getName();
            int nodeIndex = bayesIm.getNodeIndex(bayesIm.getNode(nodeName));
            double[] priors = getBayesUpdater().calculatePriorMarginals(nodeIndex);
            double[] marginals = getBayesUpdater().calculateUpdatedMarginals(nodeIndex);

            TetradLogger.getInstance().forceLogMessage("\nVariable = " + nodeName);
            TetradLogger.getInstance().forceLogMessage("\nEvidence:");
            Evidence evidence = (Evidence) getParams().get("evidence", null);
            Proposition proposition = evidence.getProposition();

            for (int i = 0; i < proposition.getNumVariables(); i++) {
                Node variable = proposition.getVariableSource().getVariables().get(i);
                int category = proposition.getSingleCategory(i);

                if (category != -1) {
                    TetradLogger.getInstance().forceLogMessage("\t" + variable + " = " + category);
                }
            }

            TetradLogger.getInstance().forceLogMessage("\nCat.\tPrior\tMarginal");

            for (int i = 0; i < priors.length; i++) {
                String message = category(evidence, nodeName, i) + "\t"
                                 + nf.format(priors[i]) + "\t" + nf.format(marginals[i]);
                TetradLogger.getInstance().forceLogMessage(message);
            }
        }
        TetradLogger.getInstance().reset();
    }

    private String category(Evidence evidence, String nodeName, int i) {
        DiscreteVariable variable = discreteVariable(evidence, nodeName);
        return variable.getCategory(i);
    }

    private DiscreteVariable discreteVariable(Evidence evidence, String nodeName) {
        return evidence.getVariable(nodeName);
    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     *
     * @param s a {@link java.io.ObjectInputStream} object
     * @throws IOException            If any.
     * @throws ClassNotFoundException If any.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (getBayesUpdater() == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Getter for the field <code>params</code>.</p>
     *
     * @return a {@link edu.cmu.tetrad.util.Parameters} object
     */
    public Parameters getParams() {
        return this.params;
    }
}





