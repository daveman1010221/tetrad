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

package edu.cmu.tetrad.test;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.Fges;
import edu.cmu.tetrad.search.LingamCPDAG;
import edu.cmu.tetrad.search.Score;
import edu.cmu.tetrad.search.SemBicScore;
import edu.cmu.tetrad.sem.SemIm;
import edu.cmu.tetrad.sem.SemPm;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.dist.Distribution;
import edu.cmu.tetrad.util.dist.Normal;
import edu.cmu.tetrad.util.dist.Uniform;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Joseph Ramsey
 */
public class TestLingamCPDAG {

    @Test
    public void test1() {
        RandomUtil.getInstance().setSeed(4938492L);

        final int sampleSize = 1000;

        final List<Node> nodes = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            nodes.add(new ContinuousVariable("X" + (i + 1)));
        }

        final Graph graph = new Dag(GraphUtils.randomGraph(nodes, 0, 6,
                4, 4, 4, false));

        final List<Distribution> variableDistributions = new ArrayList<>();

        variableDistributions.add(new Normal(0, 1));
        variableDistributions.add(new Normal(0, 1));
        variableDistributions.add(new Normal(0, 1));
        variableDistributions.add(new Uniform(-1, 1));
        variableDistributions.add(new Normal(0, 1));
        variableDistributions.add(new Normal(0, 1));

        final SemPm semPm = new SemPm(graph);
        final SemIm semIm = new SemIm(semPm);

        final DataSet dataSet = simulateDataNonNormal(semIm, sampleSize, variableDistributions);
        final Score score = new SemBicScore(new CovarianceMatrix(dataSet));
        final Graph estnumCPDAGsToStore = new Fges(score).search();

        final LingamCPDAG lingam = new LingamCPDAG(estnumCPDAGsToStore, dataSet);
        lingam.search();

        final double[] pvals = lingam.getPValues();

        final double[] expectedPVals = {0.18, 0.29, 0.88, 0.00, 0.01, 0.57};

        for (int i = 0; i < pvals.length; i++) {
//            assertEquals(expectedPVals[i], pvals[i], 0.01);
        }
    }

    /**
     * This simulates data by picking random values for the exogenous terms and percolating this information down
     * through the SEM, assuming it is acyclic. Fast for large simulations but hangs for cyclic models.
     *
     * @param sampleSize > 0.
     * @return the simulated data set.
     */
    private DataSet simulateDataNonNormal(final SemIm semIm, final int sampleSize,
                                          final List<Distribution> distributions) {
        final List<Node> variables = new LinkedList<>();
        final List<Node> variableNodes = semIm.getSemPm().getVariableNodes();

        for (final Node node : variableNodes) {
            final ContinuousVariable var = new ContinuousVariable(node.getName());
            variables.add(var);
        }

        final DataSet dataSet = new BoxDataSet(new DoubleDataBox(sampleSize, variables.size()), variables);

        // Create some index arrays to hopefully speed up the simulation.
        final SemGraph graph = semIm.getSemPm().getGraph();
        final List<Node> tierOrdering = graph.getCausalOrdering();

        final int[] tierIndices = new int[variableNodes.size()];

        for (int i = 0; i < tierIndices.length; i++) {
            tierIndices[i] = variableNodes.indexOf(tierOrdering.get(i));
        }

        final int[][] _parents = new int[variables.size()][];

        for (int i = 0; i < variableNodes.size(); i++) {
            final Node node = variableNodes.get(i);
            final List<Node> parents = graph.getParents(node);

            for (final Iterator<Node> j = parents.iterator(); j.hasNext(); ) {
                final Node _node = j.next();

                if (_node.getNodeType() == NodeType.ERROR) {
                    j.remove();
                }
            }

            _parents[i] = new int[parents.size()];

            for (int j = 0; j < parents.size(); j++) {
                final Node _parent = parents.get(j);
                _parents[i][j] = variableNodes.indexOf(_parent);
            }
        }

        // Do the simulation.
        for (int row = 0; row < sampleSize; row++) {
//            System.out.println(row);

            for (int i = 0; i < tierOrdering.size(); i++) {
                final int col = tierIndices[i];
                final Distribution distribution = distributions.get(col);

//                System.out.println(distribution);

                double value = distribution.nextRandom();

                for (int j = 0; j < _parents[col].length; j++) {
                    final int parent = _parents[col][j];
                    value += dataSet.getDouble(row, parent) *
                            semIm.getEdgeCoef().get(parent, col);
                }

                value += semIm.getMeans()[col];
                dataSet.setDouble(row, col, value);
            }
        }

        return dataSet;
    }
}


