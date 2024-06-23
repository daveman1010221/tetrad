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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphTransforms;
import edu.cmu.tetradapp.util.GraphUtils;
import edu.cmu.tetradapp.workbench.GraphWorkbench;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This class represents an action to convert a CPDAG (Completed Partially Directed Acyclic Graph) to a random DAG
 * (Directed Acyclic Graph).
 */
public class PickZhangMagInPagAction extends AbstractAction {

    /**
     * The desktop containing the target session editor.
     */
    private final GraphWorkbench workbench;

    /**
     * Picks a MAG in a PAG using Zhang'e algorithm.
     *
     * @param workbench The workbench containing the graph.
     */
    public PickZhangMagInPagAction(GraphWorkbench workbench) {
        super("Pick Zhang MAG in PAG");

        if (workbench == null) {
            throw new NullPointerException("Desktop must not be null.");
        }

        this.workbench = workbench;
    }

    /**
     * This method is called when the user performs an action to convert a CPDAG (Completed Partially Directed Acyclic
     * Graph) to a random DAG (Directed Acyclic Graph).
     *
     * @param e the action event generated by the user's action
     */
    public void actionPerformed(ActionEvent e) {
        Graph graph = workbench.getGraph();

        if (graph == null) {
            JOptionPane.showMessageDialog(GraphUtils.getContainingScrollPane(workbench), "No graph to convert.");
            return;
        }

        // Commenting this out because the PAG algorithms are not always returning legal PAGs
//        if (!graph.paths().isLegalPag()) {
//            JOptionPane.showMessageDialog(GraphUtils.getContainingScrollPane(workbench), "I can only convert PAGs.");
//            return;
//        }

        graph = GraphTransforms.zhangMagFromPag(graph);
        workbench.setGraph(graph);
    }
}



