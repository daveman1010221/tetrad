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

import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.GraphScorer;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.session.DoNotAddOldModel;
import edu.cmu.tetrad.session.SessionModel;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradSerializableUtils;
import edu.cmu.tetrad.util.Unmarshallable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Holds a list of graphs.
 *
 * @author Joseph Ramsey
 */
public class ScoredGraphsWrapper implements SessionModel, GraphSource, Unmarshallable, DoNotAddOldModel {
    static final long serialVersionUID = 23L;

    /**
     * @serial Can be null.
     */
    private String name;

    /**
     * deprecated;
     */
    private Graph selectedGraph;

    private final Map<Graph, Double> graphsToScores;

    /**
     * Transient graph scorer, null if non exists (or needs to be refreshed).
     */
    private final transient GraphScorer graphScorer;

    //=============================CONSTRUCTORS==========================//

    private ScoredGraphsWrapper() {
        this.graphsToScores = null;
        this.graphScorer = null;
    }

    public ScoredGraphsWrapper(final Graph graph, final GraphScorer scorer) {
        final List<Graph> dags = SearchGraphUtils.generateCpdagDags(graph, true);
        this.graphsToScores = new HashMap<>();
        this.graphScorer = scorer;

        for (final Graph _graph : dags) {
            double score = Double.NaN;

            if (scorer != null) {
                score = scorer.scoreDag(_graph);
            }

            this.graphsToScores.put(_graph, score);
        }

        if (!this.graphsToScores.keySet().isEmpty()) {
            /*
      The index of the selected graph.
     */
            final int index = 0;
            this.selectedGraph = this.graphsToScores.keySet().iterator().next();
        }

        log();
    }

    public ScoredGraphsWrapper(final FgesRunner runner, final Parameters parameters) {
        this(runner.getTopGraphs().get(runner.getIndex()).getGraph(), runner.getGraphScorer());
    }

    public ScoredGraphsWrapper(final DagWrapper wrapper, final Parameters parameters) {
        this(wrapper.getGraph(), null);
    }

    public ScoredGraphsWrapper(final GraphWrapper wrapper, final Parameters parameters) {
        this(wrapper.getGraph(), null);
    }

    public ScoredGraphsWrapper(final SemGraphWrapper wrapper, final Parameters parameters) {
        this(wrapper.getGraph(), null);
    }

    public ScoredGraphsWrapper(final PcRunner wrapper, final Parameters parameters) {
        this(wrapper.getGraph(), null);
    }

    public ScoredGraphsWrapper(final CpcRunner wrapper, final Parameters parameters) {
        this(wrapper.getGraph(), null);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see TetradSerializableUtils
     */
    public static ScoredGraphsWrapper serializableInstance() {
        return new ScoredGraphsWrapper(PcRunner.serializableInstance(), new Parameters());
    }

    //==============================PUBLIC METHODS======================//

    public Map<Graph, Double> getGraphsToScores() {
        final Map<Graph, Double> _graphsToScores = new LinkedHashMap<>();

        for (final Graph graph : this.graphsToScores.keySet()) {
            _graphsToScores.put(new EdgeListGraph(graph), this.graphsToScores.get(graph));
        }

        return _graphsToScores;
    }


    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    //==========================PRIVATE METHODS===========================//

    private void log() {
        TetradLogger.getInstance().log("info", "DAGs in forbid_latent_common_causes");
        TetradLogger.getInstance().log("selected_graph", "\nSelected Graph\n");
        TetradLogger.getInstance().log("selected_graph", getGraph() + "");

        TetradLogger.getInstance().log("all_graphs", "\nAll Graphs:\n");
        int index = 0;

        for (final Graph graph : this.graphsToScores.keySet()) {
            TetradLogger.getInstance().log("all_graphs", "\nGraph #" + (++index));
            TetradLogger.getInstance().log("all_graphs", graph + "");
        }
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
    }

    /**
     * May be null; a selected graph must be set.
     */
    public Graph getGraph() {
        return this.selectedGraph;
    }

    /**
     * May be null; a selected graph must be set.
     */
    public Graph getSelectedGraph() {
        return this.selectedGraph;
    }

    /**
     * Sets a selected graph. Must be one of the graphs in <code>getGraphToScore().keySet</code>.
     */
    public void setSelectedGraph(final Graph graph) {
        if (!this.graphsToScores.containsKey(graph)) {
            throw new IllegalArgumentException("Not a graph in this set.");
        }

        this.selectedGraph = graph;
    }

    public GraphScorer getGraphScorer() {
        return this.graphScorer;
    }
}


