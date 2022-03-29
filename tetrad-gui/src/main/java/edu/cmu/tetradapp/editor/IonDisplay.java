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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.TetradSerializable;
import edu.cmu.tetradapp.model.IonRunner;
import edu.cmu.tetradapp.workbench.DisplayEdge;
import edu.cmu.tetradapp.workbench.DisplayNode;
import edu.cmu.tetradapp.workbench.GraphWorkbench;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the output DGs of the LiNG algorithm.
 *
 * @author Joseph Ramsey
 */
public class IonDisplay extends JPanel implements GraphEditable {
    private final GraphWorkbench workbench;
    private List<Graph> storedGraphs;
    private final List<Integer> indices;
    private final JSpinner spinner;
    private final JLabel totalLabel;

    public IonDisplay(final List<Graph> storedGraphs, final IonRunner runner) {
        this.storedGraphs = storedGraphs;
        int graphIndex = runner.getParams().getInt("graphIndex", 1);

        if (storedGraphs.size() == 0) {
            this.workbench = new GraphWorkbench();
        } else {
            this.workbench = new GraphWorkbench(storedGraphs.get(0));
        }

        this.indices = getAllIndices(storedGraphs);

        final SpinnerNumberModel model =
                new SpinnerNumberModel(this.indices.size() == 0 ? 0 : 1, this.indices.size() == 0 ? 0 : 1,
                        this.indices.size(), 1);
        model.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final int index = (Integer) model.getValue();
                IonDisplay.this.workbench.setGraph(storedGraphs.get(IonDisplay.this.indices.get(index - 1)));
                firePropertyChange("modelChanged", null, null);
                runner.setResultGraph(IonDisplay.this.workbench.getGraph());
                runner.getParams().set("graphIndex", index - 1);
            }
        });

        if (graphIndex >= this.indices.size()) {
            graphIndex = 0;
            runner.getParams().set("graphIndex", 0);
        }

        if (this.indices.size() > 0) {
            model.setValue(graphIndex + 1);
        }

        this.spinner = new JSpinner();
        this.spinner.setModel(model);
        this.totalLabel = new JLabel(" of " + this.indices.size());


        this.spinner.setPreferredSize(new Dimension(50, 20));
        this.spinner.setMaximumSize(this.spinner.getPreferredSize());
        final Box b = Box.createVerticalBox();
        final Box b1 = Box.createHorizontalBox();
//        b1.add(Box.createHorizontalGlue());
//        b1.add(Box.createHorizontalStrut(10));
        b1.add(Box.createHorizontalGlue());
        b1.add(new JLabel("DAG "));
        b1.add(this.spinner);
        b1.add(this.totalLabel);

        b.add(b1);

        final Box b2 = Box.createHorizontalBox();
        final JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        final JScrollPane jScrollPane = new JScrollPane(this.workbench);
//        jScrollPane.setPreferredSize(new Dimension(400, 400));
        graphPanel.add(jScrollPane);
//        graphPanel.setBorder(new TitledBorder("DAG"));
        b2.add(graphPanel);
        b.add(b2);

        setLayout(new BorderLayout());
//        add(menuBar(), BorderLayout.NORTH);
        add(b, BorderLayout.CENTER);
    }

    private void resetDisplay() {
        final List<Integer> _subsetIndices = getAllIndices(getStoredGraphs());
        this.indices.clear();
        this.indices.addAll(_subsetIndices);

        final int min = this.indices.size() == 0 ? 0 : 1;
        final SpinnerNumberModel model = new SpinnerNumberModel(min, min, this.indices.size(), 1);
        model.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final int index = model.getNumber().intValue();
                IonDisplay.this.workbench.setGraph(IonDisplay.this.storedGraphs.get(IonDisplay.this.indices.get(index - 1)));
            }
        });

        this.spinner.setModel(model);
        this.totalLabel.setText(" of " + _subsetIndices.size());

        if (this.indices.isEmpty()) {
            this.workbench.setGraph(new EdgeListGraph());
        } else {
            this.workbench.setGraph(this.storedGraphs.get(this.indices.get(0)));
        }
    }

    public void resetGraphs(final List<Graph> storedGraphs) {
        this.storedGraphs = storedGraphs;
        resetDisplay();
    }

    private List<Integer> getAllIndices(final List<Graph> storedGraphs) {
        final List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < storedGraphs.size(); i++) {
            indices.add(i);
        }

        return indices;
    }

    public List getSelectedModelComponents() {
        final Component[] components = getWorkbench().getComponents();
        final List<TetradSerializable> selectedModelComponents =
                new ArrayList<>();

        for (final Component comp : components) {
            if (comp instanceof DisplayNode) {
                selectedModelComponents.add(
                        ((DisplayNode) comp).getModelNode());
            } else if (comp instanceof DisplayEdge) {
                selectedModelComponents.add(
                        ((DisplayEdge) comp).getModelEdge());
            }
        }

        return selectedModelComponents;
    }

    public void pasteSubsession(final List sessionElements, final Point upperLeft) {
        getWorkbench().pasteSubgraph(sessionElements, upperLeft);
        getWorkbench().deselectAll();

        for (int i = 0; i < sessionElements.size(); i++) {

            final Object o = sessionElements.get(i);

            if (o instanceof GraphNode) {
                final Node modelNode = (Node) o;
                getWorkbench().selectNode(modelNode);
            }
        }

        getWorkbench().selectConnectingEdges();
    }

    public GraphWorkbench getWorkbench() {
        return this.workbench;
    }

    public Graph getGraph() {
        return this.workbench.getGraph();
    }

    public void setGraph(final Graph graph) {
        this.workbench.setGraph(graph);
    }

    private List<Graph> getStoredGraphs() {
        return this.storedGraphs;
    }
}


