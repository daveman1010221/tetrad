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

package edu.cmu.tetrad.search.score;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.search.test.IndependenceTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Gives a method of interpreting a test as a score. Various independence tests will calculate p-values; they simply
 * report alpha - p as a score, which will be higher for greater dependence. This class wraps such an independence test
 * and returns the score reported by that test.
 *
 * <p>As for all scores in Tetrad, higher scores mean more dependence, and negative
 * scores indicate independence.</p>
 *
 * @author josephramsey
 * @see IndependenceTest
 * @see IndependenceTest#getScore()
 */
public class IndTestScore implements Score {

    private final IndependenceTest test;

    // The variables of the covariance matrix.
    private final List<Node> variables;

    // True if verbose output should be sent to out.
    private boolean verbose;

    /**
     * Constructs the score using a covariance matrix.
     */
    public IndTestScore(IndependenceTest test) {
        this.variables = new ArrayList<>();

        for (Node node : test.getVariables()) {
            if (node.getNodeType() == NodeType.MEASURED) {
                this.variables.add(node);
            }
        }

        this.test = test;
    }

    /**
     * Calculates the sample likelihood and BIC score for i, given its parents in a simple SEM model
     */
    public double localScore(int i, int[] parents) {
        throw new UnsupportedOperationException();
    }

    private List<Node> getVariableList(int[] indices) {
        List<Node> variables = new ArrayList<>();
        for (int i : indices) {
            variables.add(this.variables.get(i));
        }
        return variables;
    }


    @Override
    public double localScoreDiff(int x, int y, int[] z) {
        this.test.checkIndependence(this.variables.get(x), this.variables.get(y),new HashSet<>(getVariableList(z)));
        return this.test.getScore();
    }

    /**
     * Specialized scoring method for a single parent. Used to speed up the effect edges search.
     */
    public double localScore(int i, int parent) {
        throw new UnsupportedOperationException();
    }


    public double localScore(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEffectEdge(double bump) {
        return true;
    }

    public DataSet getDataSet() {
        throw new UnsupportedOperationException();
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public List<Node> getVariables() {
        return this.variables;
    }

    public int getSampleSize() {
        return 0;
    }

    @Override
    public int getMaxDegree() {
        return 1000;
    }

    @Override
    public boolean determines(List<Node> z, Node y) {
        return false;
    }

}



