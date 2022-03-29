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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.Cefs;
import edu.cmu.tetrad.search.IndTestType;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradSerializableUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Extends AbstractAlgorithmRunner to produce a wrapper for the MB Fan Search
 * algorithm.
 *
 * @author Frank Wimberly after Joe Ramsey's PcRunner
 */

public class CeFanSearchRunner extends AbstractAlgorithmRunner
        implements IndTestProducer {
    static final long serialVersionUID = 23L;

    //=========================CONSTRUCTORS===============================//

    /**
     * Constructs a wrapper for the given DataWrapper. The DataWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public CeFanSearchRunner(final DataWrapper dataWrapper, final Parameters params) {
        super(dataWrapper, params, null);
    }


    /**
     * Constucts a wrapper for the given EdgeListGraph.
     */
    public CeFanSearchRunner(final Graph graph, final Parameters params) {
        super(graph, params);
    }

    /**
     * Constucts a wrapper for the given EdgeListGraph.
     */
    public CeFanSearchRunner(final GraphWrapper graphWrapper, final Parameters params) {
        super(graphWrapper.getGraph(), params);
    }

    /**
     * Constucts a wrapper for the given EdgeListGraph.
     */
    public CeFanSearchRunner(final DagWrapper dagWrapper, final Parameters params) {
        super(dagWrapper.getDag(), params);
    }

    public CeFanSearchRunner(final SemGraphWrapper dagWrapper,
                             final Parameters params) {
        super(dagWrapper.getGraph(), params);
    }

    public CeFanSearchRunner(final IndependenceFactsModel model, final Parameters params) {
        super(model, params, null);
    }

    public CeFanSearchRunner(final IndependenceFactsModel model, final Parameters params, final KnowledgeBoxModel knowledgeBoxModel) {
        super(model, params, knowledgeBoxModel);
    }


    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see TetradSerializableUtils
     */
    public static CeFanSearchRunner serializableInstance() {
        return new CeFanSearchRunner(Dag.serializableInstance(), new Parameters());
    }

    //=================PUBLIC METHODS OVERRIDING ABSTRACT=================//

    /**
     * Executes the algorithm, producing (at least) a result workbench. Must be
     * implemented in the extending class.
     */
    public void execute() {
        final int pcDepth = getParams().getInt("depth", -1);
        final Cefs search =
                new Cefs(getIndependenceTest(), pcDepth);
        final Parameters params = getParams();
        if (params instanceof Parameters) {
            search.setAggressivelyPreventCycles(params.getBoolean("aggressivelyPreventCycles", false));
        }
        final String targetName = getParams().getString("targetName", null);
        final Graph graph = search.search(targetName);
        setResultGraph(graph);

        if (getSourceGraph() != null) {
            GraphUtils.arrangeBySourceGraph(graph, getSourceGraph());
        } else if (((IKnowledge) getParams().get("knowledge", new Knowledge2())).isDefaultToKnowledgeLayout()) {
            SearchGraphUtils.arrangeByKnowledgeTiers(graph, (IKnowledge) getParams().get("knowledge", new Knowledge2()));
        } else {
            GraphUtils.circleLayout(graph, 200, 200, 150);
        }
    }

    public IndependenceTest getIndependenceTest() {
        Object dataModel = getDataModel();

        if (dataModel == null) {
            dataModel = getSourceGraph();
        }

        final Parameters params = getParams();
        final IndTestType testType = (IndTestType) params.get("indTestType", IndTestType.FISHER_Z);
        return new IndTestChooser().getTest(dataModel, params, testType);
    }

    public Graph getGraph() {
        return getResultGraph();
    }

    /**
     * @return the names of the triple classifications. Coordinates with
     */
    public List<String> getTriplesClassificationTypes() {
        return new LinkedList<>();
    }

    /**
     * @return the list of triples corresponding to <code>getTripleClassificationNames</code>.
     */
    public List<List<Triple>> getTriplesLists(final Node node) {
        return new LinkedList<>();
    }

    @Override
    public String getAlgorithmName() {
        return "CE-Fan-Search";
    }
}





