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

import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.SemGraph;
import edu.cmu.tetrad.sem.StandardizedSemIm;
import edu.cmu.tetradapp.model.StandardizedSemImWrapper;
import edu.cmu.tetradapp.util.LayoutEditable;
import edu.cmu.tetradapp.workbench.LayoutMenu;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;

/**
 * Edits a standardized SEM model in which (a) all means are zero, (b) all
 * variances are equal to 1, and (c) the error covariance matrix is always
 * positive definite. Simulations produce standardized data.
 *
 * @author Joseph Ramsey
 */
public final class StandardizedSemImEditor extends JPanel implements LayoutEditable {

    static final long serialVersionUID = 23L;

    /**
     * The SemIm being edited.
     */
    private final StandardizedSemIm semIm;

    /**
     * The graphical editor for the SemIm.
     */
    private StandardizedSemImGraphicalEditor standardizedSemImGraphicalEditor;

    /**
     * The menu item that lets the user either show or hide error terms.
     */
    private final JMenuItem errorTerms;
    private StandardizedSemImImpliedMatricesPanel impliedMatricesPanel;

    //========================CONSTRUCTORS===========================//

    /**
     * Constructs a new SemImEditor from the given OldSemEstimateAdapter.
     */
    public StandardizedSemImEditor(final StandardizedSemImWrapper wrapper) {
        final StandardizedSemIm semIm = wrapper.getStandardizedSemIm();

        if (semIm == null) {
            throw new NullPointerException("The SEM IM has not been specified.");
        }

        this.semIm = semIm;
        setLayout(new BorderLayout());

        final JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Graph", graphicalEditor());
        tabbedPane.add("Implied Matrices", impliedMatricesPanel());

        add(tabbedPane, BorderLayout.CENTER);

        final JMenuBar menuBar = new JMenuBar();
        final JMenu file = new JMenu("File");
        menuBar.add(file);
        file.add(new SaveComponentImage(this.standardizedSemImGraphicalEditor.getWorkbench(),
                "Save Graph Image..."));

        this.errorTerms = new JMenuItem();

        // By default, hide the error terms.
//        getSemGraph().setShowErrorTerms(false);
        final SemGraph graph = (SemGraph) graphicalEditor().getWorkbench().getGraph();
        final boolean shown = wrapper.isShowErrors();
        graph.setShowErrorTerms(shown);

//        errorTerms = new JMenuItem();
//
//        if (shown) {
//            errorTerms.setText("Hide Error Terms");
//        }
//        else {
//            errorTerms.setText("Show Error Terms");
//        }
        this.errorTerms.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final JMenuItem menuItem = (JMenuItem) e.getSource();

                if ("Hide Error Terms".equals(menuItem.getText())) {
                    menuItem.setText("Show Error Terms");
                    final SemGraph graph = (SemGraph) graphicalEditor().getWorkbench().getGraph();
                    graph.setShowErrorTerms(false);
                    wrapper.setShowErrors(false);
                    graphicalEditor().resetLabels();
                } else if ("Show Error Terms".equals(menuItem.getText())) {
                    menuItem.setText("Hide Error Terms");
                    final SemGraph graph = (SemGraph) graphicalEditor().getWorkbench().getGraph();
                    graph.setShowErrorTerms(true);
                    wrapper.setShowErrors(true);
                    graphicalEditor().resetLabels();
                }
            }
        });

//        if (getSemGraph().isShowErrorTerms()) {
//            errorTerms.setText("Hide Error Terms");
//        } else {
//            errorTerms.setText("Show Error Terms");
//        }
//        errorTerms.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                JMenuItem menuItem = (JMenuItem) e.getSource();
//
//                if ("Hide Error Terms".equals(menuItem.getText())) {
//                    menuItem.setText("Show Error Terms");
//                    getSemGraph().setShowErrorTerms(false);
//                    graphicalEditor().resetLabels();
//                } else if ("Show Error Terms".equals(menuItem.getText())) {
//                    menuItem.setText("Hide Error Terms");
//                    getSemGraph().setShowErrorTerms(true);
//                    graphicalEditor().resetLabels();
//                }
//            }
//        });
//        JMenu params = new JMenu("Parameters");
//        params.add(errorTerms);
//        params.addSeparator();
//        params.addSeparator();
//        menuBar.add(params);
        menuBar.add(new LayoutMenu(this));

        add(menuBar, BorderLayout.NORTH);

    }

    /**
     * @return the graph currently in the workbench.
     */
    public Graph getGraph() {
        return this.standardizedSemImGraphicalEditor.getWorkbench().getGraph();
    }

    @Override
    public Map getModelEdgesToDisplay() {
        return this.standardizedSemImGraphicalEditor.getWorkbench().getModelEdgesToDisplay();
    }

    public Map getModelNodesToDisplay() {
        return this.standardizedSemImGraphicalEditor.getWorkbench().getModelNodesToDisplay();
    }

    /**
     * @return the knowledge currently stored in the workbench. Required for and
     * interface.
     */
    public IKnowledge getKnowledge() {
        return this.standardizedSemImGraphicalEditor.getWorkbench().getKnowledge();
    }

    /**
     * @return the source graph currently stored in the workbench. Required for
     * an interface.
     */
    public Graph getSourceGraph() {
        return this.standardizedSemImGraphicalEditor.getWorkbench().getSourceGraph();
    }

    /**
     * Lays out the graph in the workbench.
     *
     * @param graph The graph whose layout is to be mimicked.
     */
    public void layoutByGraph(final Graph graph) {
        final SemGraph _graph = (SemGraph) this.standardizedSemImGraphicalEditor.getWorkbench().getGraph();
        _graph.setShowErrorTerms(false);
        this.standardizedSemImGraphicalEditor.getWorkbench().layoutByGraph(graph);
        _graph.resetErrorPositions();
//        standardizedSemImGraphicalEditor.getWorkbench().setGraph(_graph);
        this.errorTerms.setText("Show Error Terms");
    }

    /**
     * Lays the workbench graph out using knowledge tiers.
     */
    public void layoutByKnowledge() {
        final SemGraph _graph = (SemGraph) this.standardizedSemImGraphicalEditor.getWorkbench().getGraph();
        _graph.setShowErrorTerms(false);
        this.standardizedSemImGraphicalEditor.getWorkbench().layoutByKnowledge();
        _graph.resetErrorPositions();
        this.standardizedSemImGraphicalEditor.getWorkbench().setGraph(_graph);
        this.errorTerms.setText("Show Error Terms");
    }

    //========================PRIVATE METHODS===========================//
    private SemGraph getSemGraph() {
        return this.semIm.getSemPm().getGraph();
    }

    private StandardizedSemIm getSemIm() {
        return this.semIm;
    }

    private StandardizedSemImGraphicalEditor graphicalEditor() {
        if (this.standardizedSemImGraphicalEditor == null) {
            this.standardizedSemImGraphicalEditor = new StandardizedSemImGraphicalEditor(getSemIm(), this);
            this.standardizedSemImGraphicalEditor.addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public void propertyChange(final PropertyChangeEvent evt) {
                            firePropertyChange(evt.getPropertyName(), null,
                                    null);
                        }
                    });
            this.standardizedSemImGraphicalEditor.enableEditing(false);
        }

        return this.standardizedSemImGraphicalEditor;
    }

    private StandardizedSemImImpliedMatricesPanel impliedMatricesPanel() {
        if (this.impliedMatricesPanel == null) {
            final int matrixSelection = 0;
            this.impliedMatricesPanel
                    = new StandardizedSemImImpliedMatricesPanel(getSemIm(), matrixSelection);
        }
        return this.impliedMatricesPanel;
    }
}

/**
 * Dispays the implied covariance and correlation matrices for the given SemIm.
 */
class StandardizedSemImImpliedMatricesPanel extends JPanel {

    private final StandardizedSemIm semIm;
    private JTable impliedJTable;
    private int matrixSelection;
    private JComboBox selector;

    public StandardizedSemImImpliedMatricesPanel(final StandardizedSemIm semIm, final int matrixSelection) {
        this.semIm = semIm;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(selector());
        add(Box.createVerticalStrut(10));
        add(new JScrollPane(impliedJTable()));
        add(Box.createVerticalGlue());
        setBorder(new TitledBorder("Select Implied Matrix to View"));

        setMatrixSelection(matrixSelection);
    }

    /**
     * @return the matrix in tab delimited form.
     */
    public String getMatrixInTabDelimitedForm() {
        final StringBuilder builder = new StringBuilder();
        final TableModel model = impliedJTable().getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int col = 0; col < model.getColumnCount(); col++) {
                final Object o = model.getValueAt(row, col);
                if (o != null) {
                    builder.append(o);
                }
                builder.append('\t');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private JTable impliedJTable() {
        if (this.impliedJTable == null) {
            this.impliedJTable = new JTable();
            this.impliedJTable.setTableHeader(null);
        }
        return this.impliedJTable;
    }

    private JComboBox selector() {
        if (this.selector == null) {
            this.selector = new JComboBox();
            final java.util.List<String> selections = StandardizedSemImImpliedMatricesPanel.getImpliedSelections();

            for (final Object selection : selections) {
                this.selector.addItem(selection);
            }

            this.selector.addItemListener(new ItemListener() {
                public void itemStateChanged(final ItemEvent e) {
                    final String item = (String) e.getItem();
                    setMatrixSelection(StandardizedSemImImpliedMatricesPanel.getImpliedSelections().indexOf(item));
                }
            });
        }
        return this.selector;
    }

    private void setMatrixSelection(final int index) {
        selector().setSelectedIndex(index);
        switchView(index);
    }

    private void switchView(final int index) {
        if (index < 0 || index > 3) {
            throw new IllegalArgumentException(
                    "Matrix selection must be 0, 1, 2, or 3.");
        }

        this.matrixSelection = index;

        switch (index) {
            case 0:
                switchView(false, false);
                break;
            case 1:
                switchView(true, false);
                break;
            case 2:
                switchView(false, true);
                break;
            case 3:
                switchView(true, true);
                break;
        }
    }

    private void switchView(final boolean a, final boolean b) {
        try {
            impliedJTable().setModel(new StandardizedSemImImpliedCovTable(getSemIm(), a, b));
            //     impliedJTable().getTableHeader().setReorderingAllowed(false);
            impliedJTable().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            impliedJTable().setRowSelectionAllowed(false);
            impliedJTable().setCellSelectionEnabled(false);
            impliedJTable().doLayout();
        } catch (final IllegalArgumentException e) {
            return;
        }
    }

    private static java.util.List<String> getImpliedSelections() {
        final java.util.List<String> list = new ArrayList<>();
        list.add("Implied covariance matrix (all variables)");
        list.add("Implied covariance matrix (measured variables only)");
        list.add("Implied correlation matrix (all variables)");
        list.add("Implied correlation matrix (measured variables only)");
        return list;
    }

    private StandardizedSemIm getSemIm() {
        return this.semIm;
    }

    public int getMatrixSelection() {
        return this.matrixSelection;
    }
}
