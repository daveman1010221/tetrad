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

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.IndTestType;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetradapp.model.MarkovBlanketSearchRunner;
import edu.cmu.tetradapp.util.DoubleTextField;
import edu.cmu.tetradapp.util.WatchedProcess;
import edu.cmu.tetradapp.workbench.GraphWorkbench;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Editor + param editor for markov blanket searches.
 *
 * @author Tyler Gibson
 */
public class MarkovBlanketSearchEditor extends JPanel implements GraphEditable, IndTestTypeSetter {

    /**
     * The algorithm wrapper being viewed.
     */
    private final MarkovBlanketSearchRunner algorithmRunner;


    /**
     * The button one clicks to executeButton the algorithm.
     */
    private final JButton executeButton = new JButton();


    /**
     * The scrollpange for the result workbench.
     */
    private JScrollPane workbenchScroll;


    /**
     * Table used to display data.
     */
    private final TabularDataJTable table;

    /**
     * True if the warning message that previously defined knowledge is being
     * used has already been shown and doesn't need to be shown again.
     */
    private boolean knowledgeMessageShown;


    /**
     * Constructs the eidtor.
     */
    public MarkovBlanketSearchEditor(final MarkovBlanketSearchRunner algorithmRunner) {
        if (algorithmRunner == null) {
            throw new NullPointerException();
        }
        this.algorithmRunner = algorithmRunner;
        final Parameters params = algorithmRunner.getParams();
        final List<String> vars = algorithmRunner.getSource().getVariableNames();
        if (params.getString("targetName", null) == null && !vars.isEmpty()) {
            params.set("targetName", vars.get(0));
        }
        final DataSet data;
        if (algorithmRunner.getDataModelForMarkovBlanket() == null) {
            data = algorithmRunner.getSource();
        } else {
            data = algorithmRunner.getDataModelForMarkovBlanket();
        }
        this.table = new TabularDataJTable(data);
        this.table.setEditable(false);
        this.table.setTableHeader(null);

        setup();
    }


    /**
     * @return the data model being viewed.
     */
    public DataModel getDataModel() {
        if (this.algorithmRunner.getDataModelForMarkovBlanket() != null) {
            return this.algorithmRunner.getDataModelForMarkovBlanket();
        }

        return this.algorithmRunner.getSource();
    }

    public Object getSourceGraph() {
        return getParams().get("sourceGraph", null);
    }

    //===========================PRIVATE METHODS==========================//


    /**
     * Executes the algorithm. The execution takes place inside a thread, so one
     * cannot count on a result graph having been found when the method
     */
    private void execute() {
        final Window owner = (Window) getTopLevelAncestor();

        final WatchedProcess process = new WatchedProcess(owner) {
            public void watch() {
                getExecuteButton().setEnabled(false);
                setErrorMessage(null);

                if (!MarkovBlanketSearchEditor.this.knowledgeMessageShown) {
                    final IKnowledge knowledge = (IKnowledge) getAlgorithmRunner().getParams().get("knowledge", new Knowledge2());
                    if (!knowledge.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                JOptionUtils.centeringComp(),
                                "Using previously set knowledge. (To edit, use " +
                                        "the Knowledge menu.)");
                        MarkovBlanketSearchEditor.this.knowledgeMessageShown = true;
                    }
                }

                try {
                    getAlgorithmRunner().execute();
                } catch (final Exception e) {
                    final CharArrayWriter writer1 = new CharArrayWriter();
                    final PrintWriter writer2 = new PrintWriter(writer1);
                    e.printStackTrace(writer2);
                    final String message = writer1.toString();
                    writer2.close();

                    e.printStackTrace(System.out);

                    TetradLogger.getInstance().error(message);

                    String messageString = e.getMessage();

                    if (e.getCause() != null) {
                        messageString = e.getCause().getMessage();
                    }

                    if (messageString == null) {
                        messageString = message;
                    }
                    setErrorMessage(messageString);

                    getExecuteButton().setEnabled(true);
                    throw new RuntimeException(e);
                }


                setLabel();
                final DataSet modelForMarkovBlanket = MarkovBlanketSearchEditor.this.algorithmRunner.getDataModelForMarkovBlanket();
                if (modelForMarkovBlanket != null) {
                    MarkovBlanketSearchEditor.this.table.setDataSet(modelForMarkovBlanket);
                }
                MarkovBlanketSearchEditor.this.table.repaint();
                getExecuteButton().setEnabled(true);
            }
        };

//        getWorkbenchScroll().setBorder(
//                             new TitledBorder(getResultLabel()));
//                     Graph resultGraph = resultGraph();
//
//                     doDefaultArrangement(resultGraph);
//                     getWorkbench().setBackground(Color.WHITE);
//                     getWorkbench().setGraph(resultGraph);
//                     getGraphHistory().clear();
//                     getGraphHistory().add(resultGraph);
//                     getWorkbench().repaint();


        final Thread watcher = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(300);

                        if (!process.isAlive()) {
                            getExecuteButton().setEnabled(true);
                            return;
                        }
                    } catch (final InterruptedException e) {
                        getExecuteButton().setEnabled(true);
                        return;
                    }
                }
            }
        };

        watcher.start();
    }

    private void setLabel() {
        getWorkbenchScroll().setBorder(new TitledBorder(this.algorithmRunner.getSearchName()));
    }


    private JButton getExecuteButton() {
        return this.executeButton;
    }

    private MarkovBlanketSearchRunner getAlgorithmRunner() {
        return this.algorithmRunner;
    }


    /**
     * Sets up the editor, does the layout, and so on.
     */
    private void setup() {
        setLayout(new BorderLayout());
        add(createToolbar(), BorderLayout.WEST);
        add(workbenchScroll(), BorderLayout.CENTER);
        add(menuBar(), BorderLayout.NORTH);
    }


    /**
     * Creates param editor and tool bar.
     */
    private JPanel createToolbar() {
        final JPanel toolbar = new JPanel();
        getExecuteButton().setText("Execute*");
        getExecuteButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                execute();
            }
        });

        final Box b1 = Box.createVerticalBox();
        b1.add(getParamEditor());
        b1.add(Box.createVerticalStrut(10));
        final Box b2 = Box.createHorizontalBox();
        b2.add(Box.createGlue());
        b2.add(getExecuteButton());
        b1.add(b2);
        b1.add(Box.createVerticalStrut(10));

        final Box b3 = Box.createHorizontalBox();
        final JLabel label = new JLabel("<html>" + "*Please note that some" +
                "<br>searches may take a" + "<br>long time to complete." +
                "</html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBorder(new TitledBorder(""));
        b3.add(label);
        b1.add(b3);

        toolbar.add(b1);
        return toolbar;
    }

    /**
     * Creates the param editor.
     */
    private JComponent getParamEditor() {
        final Box box = Box.createVerticalBox();
        final JComboBox comboBox = new JComboBox(this.algorithmRunner.getSource().getVariableNames().toArray());
        comboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                final String s = (String) e.getItem();
                if (s != null) {
                    MarkovBlanketSearchEditor.this.algorithmRunner.getParams().set("targetName", s);
                }
            }
        });
        final DoubleTextField alphaField = new DoubleTextField(getParams().getDouble("alpha", 0.001), 4,
                NumberFormatUtil.getInstance().getNumberFormat());
        alphaField.setFilter(new DoubleTextField.Filter() {
            public double filter(final double value, final double oldValue) {
                try {
                    getParams().set("alpha", 0.001);
                    Preferences.userRoot().putDouble("alpha",
                            getParams().getDouble("alpha", 0.001));
                    return value;
                } catch (final Exception e) {
                    return oldValue;
                }
            }
        });

        box.add(comboBox);
        box.add(Box.createVerticalStrut(4));
        box.add(createLabeledComponent("Alpha", alphaField));


        box.setBorder(new TitledBorder("Parameters"));
        return box;
    }


    private Box createLabeledComponent(final String label, final JComponent comp) {
        final Box box = Box.createHorizontalBox();
        box.add(new JLabel(label));
        box.add(Box.createHorizontalStrut(5));
        box.add(comp);
        box.add(Box.createHorizontalGlue());

        return box;
    }


    private Parameters getParams() {
        return this.algorithmRunner.getParams();
    }


    /**
     * Creates the workbench
     */
    private JScrollPane workbenchScroll() {
        this.workbenchScroll = new JScrollPane(this.table);
        this.workbenchScroll.setPreferredSize(new Dimension(500, 500));
        this.setLabel();

        return this.workbenchScroll;
    }


    /**
     * Creates the menubar for the search editor.
     */
    private JMenuBar menuBar() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu file = new JMenu("File");
        file.add(new JMenuItem(new SaveDataAction(this)));
        file.add(new GraphFileMenu(this, getWorkbench()));
//        file.add(new SaveGraph(this, "Save Graph..."));

        final JMenu edit = new JMenu("Edit");
        final JMenuItem copyCells = new JMenuItem("Copy Cells");
        copyCells.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        copyCells.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Action copyAction = TransferHandler.getCopyAction();
                final ActionEvent actionEvent = new ActionEvent(MarkovBlanketSearchEditor.this.table,
                        ActionEvent.ACTION_PERFORMED, "copy");
                copyAction.actionPerformed(actionEvent);
            }
        });
        edit.add(copyCells);


        menuBar.add(file);
        menuBar.add(edit);

        final JMenu independence = new JMenu("Independence");
        if (this.algorithmRunner.getSource().isContinuous()) {
            IndTestMenuItems.addContinuousTestMenuItems(independence, this);
            menuBar.add(independence);
        } else if (this.algorithmRunner.getSource().isDiscrete()) {
            IndTestMenuItems.addDiscreteTestMenuItems(independence, this);
            menuBar.add(independence);
        }


        menuBar.add(independence);

        return menuBar;
    }


    /**
     * Builds the ind test menu items for condinuous data and adds them to the given menu.
     */
    private void addContinuousTestMenuItems(final JMenu test) {
        IndTestType testType = (IndTestType) getParams().get("indTestType", IndTestType.FISHER_Z);
        if (testType != IndTestType.FISHER_Z &&
                testType != IndTestType.FISHER_ZD &&
                testType != IndTestType.FISHER_Z_BOOTSTRAP &&
//                testType != IndTestType.CORRELATION_T &&
                testType != IndTestType.LINEAR_REGRESSION) {
            getParams().set("indTestType", IndTestType.FISHER_Z);
        }

        final ButtonGroup group = new ButtonGroup();
        final JCheckBoxMenuItem fishersZ = new JCheckBoxMenuItem("Fisher's Z");
        group.add(fishersZ);
        test.add(fishersZ);

        final JCheckBoxMenuItem fishersZD =
                new JCheckBoxMenuItem("Fisher's Z - Generalized Inverse");
        group.add(fishersZD);
        test.add(fishersZD);

        final JCheckBoxMenuItem fishersZBootstrap =
                new JCheckBoxMenuItem("Fisher's Z - Bootstrap");
        group.add(fishersZBootstrap);
        test.add(fishersZBootstrap);

        final JCheckBoxMenuItem tTest = new JCheckBoxMenuItem("Cramer's T");
        group.add(tTest);
        test.add(tTest);

        final JCheckBoxMenuItem linRegrTest =
                new JCheckBoxMenuItem("Linear Regression Test");
        group.add(linRegrTest);
        test.add(linRegrTest);

        testType = (IndTestType) getParams().get("indTestType", IndTestType.FISHER_Z);
        if (testType == IndTestType.FISHER_Z) {
            fishersZ.setSelected(true);
        } else if (testType == IndTestType.FISHER_ZD) {
            fishersZD.setSelected(true);
        } else if (testType == IndTestType.FISHER_Z_BOOTSTRAP) {
            fishersZBootstrap.setSelected(true);
//        } else if (testType == IndTestType.CORRELATION_T) {
//            tTest.setSelected(true);
        } else if (testType == IndTestType.LINEAR_REGRESSION) {
            linRegrTest.setSelected(true);
        }

        fishersZ.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.FISHER_Z);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using Fisher's Z.");
            }
        });

        fishersZD.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.FISHER_ZD);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using Fisher's Z - Generalized Inverse.");
            }
        });

        fishersZBootstrap.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.FISHER_Z_BOOTSTRAP);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using Fisher's Z - Bootstrap.");
            }
        });

//        tTest.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                getParameters().setIndTestType(IndTestType.CORRELATION_T);
//                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
//                        "Using Cramer's T.");
//            }
//        });

        linRegrTest.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.LINEAR_REGRESSION);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using linear regression test.");
            }
        });
    }


    /**
     * Builds the ind test menu items for discrete data and adds them to the given menu.
     */
    private void addDiscreteTestMenuItems(final JMenu test) {
        final IndTestType testType = (IndTestType) getParams().get("indTestType", IndTestType.FISHER_Z);
        if (testType != IndTestType.CHI_SQUARE &&
                testType != IndTestType.G_SQUARE) {
            getParams().set("indTestType", testType);
        }

        final ButtonGroup group = new ButtonGroup();
        final JCheckBoxMenuItem chiSquare = new JCheckBoxMenuItem("Chi Square");
        group.add(chiSquare);
        test.add(chiSquare);

        final JCheckBoxMenuItem gSquare = new JCheckBoxMenuItem("G Square");
        group.add(gSquare);
        test.add(gSquare);

        if (getParams().get("indTestType", IndTestType.FISHER_Z) == IndTestType.CHI_SQUARE) {
            chiSquare.setSelected(true);
        } else if (getParams().get("indTestType", IndTestType.FISHER_Z) == IndTestType.G_SQUARE) {
            gSquare.setSelected(true);
        }

        chiSquare.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.CHI_SQUARE);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using Chi Square.");
            }
        });

        gSquare.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getParams().set("indTestType", IndTestType.G_SQUARE);
                JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                        "Using G square.");
            }
        });
    }


    private JScrollPane getWorkbenchScroll() {
        return this.workbenchScroll;
    }

    public List getSelectedModelComponents() {
        throw new UnsupportedOperationException("Cannot return selected components.");
    }

    public void pasteSubsession(final List sessionElements, final Point upperLeft) {
        throw new UnsupportedOperationException("Cannot paste subsessions on a search editor.");
    }

    public GraphWorkbench getWorkbench() {
        return getWorkbench();
    }

    /**
     * Not supported.
     */
    public void setGraph(final Graph g) {
        throw new UnsupportedOperationException("Cannot set the graph on a search editor.");
    }


    /**
     * @return the graph.
     */
    public Graph getGraph() {
        if (getWorkbench().getGraph() != null) {
            return getWorkbench().getGraph();
        }
        return new EdgeListGraph();
    }

    public void setTestType(final IndTestType testType) {
        getParams().set("indTestType", testType);
    }

    public IndTestType getTestType() {
        return (IndTestType) getParams().get("indTestType", IndTestType.FISHER_Z);
    }
}




