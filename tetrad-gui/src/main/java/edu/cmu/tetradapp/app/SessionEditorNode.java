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
package edu.cmu.tetradapp.app;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.session.*;
import edu.cmu.tetrad.util.*;
import edu.cmu.tetradapp.editor.EditorWindow;
import edu.cmu.tetradapp.editor.FinalizingParameterEditor;
import edu.cmu.tetradapp.editor.ParameterEditor;
import edu.cmu.tetradapp.model.SessionNodeWrapper;
import edu.cmu.tetradapp.model.SessionWrapper;
import edu.cmu.tetradapp.model.UnlistedSessionModel;
import edu.cmu.tetradapp.util.DesktopController;
import edu.cmu.tetradapp.util.SessionEditorIndirectRef;
import edu.cmu.tetradapp.util.WatchedProcess;
import edu.cmu.tetradapp.workbench.DisplayNode;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Point;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Wraps a SessionNodeWrapper as a DisplayNode for presentation in a
 * SessionWorkbench. Connecting these nodes using SessionEdges results in
 * parents being added to appropriate SessionNodes underlying. Double clicking
 * these nodes launches corresponding model editors.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 * @see SessionEditorWorkbench
 * @see edu.cmu.tetrad.graph.Edge
 * @see edu.cmu.tetrad.session.SessionNode
 * @see edu.cmu.tetrad.session.Session
 */
public final class SessionEditorNode extends DisplayNode {

    private static final long serialVersionUID = -6145843764762585351L;

    /**
     * If an editor has been opened, this is a reference to that editor. Used to
     * close the editor if necessary.
     */
    private EditorWindow spawnedEditor;

    /**
     * The simulation edu.cmu.tetrad.study (used to edit the repetition values).
     */
    private final SimulationStudy simulationStudy;

    /**
     * A reference to the sessionWrapper, the model associated with this node.
     */
    private SessionWrapper sessionWrapper;

    /**
     * The configuration for this editor node.
     */
    private final SessionNodeConfig config;
    private SessionEditorWorkbench sessionWorkbench;

    //===========================CONSTRUCTORS==============================//

    /**
     * Wraps the given SessionNodeWrapper as a SessionEditorNode.
     *
     * @param modelNode
     * @param simulationStudy
     */
    public SessionEditorNode(final SessionNodeWrapper modelNode, final SimulationStudy simulationStudy) {
        setModelNode(modelNode);
        final TetradApplicationConfig appConfig = TetradApplicationConfig.getInstance();
        this.config = appConfig.getSessionNodeConfig(modelNode.getButtonType());
        if (this.config == null) {
            throw new NullPointerException("There is no configuration for node of type: " + modelNode.getButtonType());
        }
        if (simulationStudy == null) {
            throw new NullPointerException(
                    "Simulation edu.cmu.tetrad.study must not be null.");
        }
        final SessionDisplayComp displayComp = this.config.getSessionDisplayCompInstance();

        this.simulationStudy = simulationStudy;
        displayComp.setName(modelNode.getSessionName());

        if (displayComp instanceof NoteDisplayComp) {
            createParamObjects(this);
            setDisplayComp(displayComp);
            setLayout(new BorderLayout());
            add((JComponent) getSessionDisplayComp(), BorderLayout.CENTER);
            setSelected(false);
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(final MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        final ToolTipManager toolTipManager
                                = ToolTipManager.sharedInstance();
                        toolTipManager.setInitialDelay(750);
                        getNotePopup().show(SessionEditorNode.this, e.getX(), e.getY());
                    }

                    e.consume();
                }
            });
        } else {
            setDisplayComp(displayComp);
            setLayout(new BorderLayout());
            add((JComponent) getSessionDisplayComp(), BorderLayout.CENTER);
            setSelected(false);
            createParamObjects(this);
            addListeners(this, modelNode);
        }
    }

    //===========================PUBLIC METHODS============================//
    public final void adjustToModel() {
        final String acronym = getAcronym();

        // Set the color.
        getSessionDisplayComp().setHasModel(!"No model".equals(acronym));

        // Set the text for the model acronym.
        getSessionDisplayComp().setAcronym(acronym);

        // Make sure the node is deselected.
        setSelected(false);

        final Dimension size = getSize();
        final Point location = getLocation();

        final int centerX = (int) location.getX() + size.width / 2;
        final int centerY = (int) location.getY() + size.height / 2;

        final int newX = centerX - getPreferredSize().width / 2;
        final int newY = centerY - getPreferredSize().height / 2;

        setLocation(newX, newY);
        setSize(getPreferredSize());
        repaint();
    }

    /**
     * @return the acronym for the contained model class.
     */
    private String getAcronym() {
        final SessionNodeWrapper modelNode = (SessionNodeWrapper) getModelNode();
        final SessionNode sessionNode = modelNode.getSessionNode();
        final Object model = sessionNode.getModel();
        if (model == null) {
            return "No model";
        } else {
            final Class<?> modelClass = model.getClass();
            final SessionNodeModelConfig modelConfig = this.config.getModelConfig(modelClass);

            if (modelConfig == null) {
                System.out.println("Tried to load model config for " + modelClass);
                return modelClass.getSimpleName();
            }

            return modelConfig.getAcronym();
        }
    }

    @Override
    public void doDoubleClickAction() {
        doDoubleClickAction(null);
    }

    /**
     * Launches the editor associates with this node.
     *
     * @param sessionWrapper Needed to allow the option of deleting edges
     */
    @Override
    public void doDoubleClickAction(final Graph sessionWrapper) {
        this.sessionWrapper = (SessionWrapper) sessionWrapper;
        final Window owner = (Window) getTopLevelAncestor();

        new WatchedProcess(owner) {
            public void watch() {
                TetradLogger.getInstance().setTetradLoggerConfig(getSessionNode().getLoggerConfig());
                launchEditorVisit();
            }
        };
    }

    private void launchEditorVisit() {
        try {

            // If there is already an editor open, don't launch another one.
            if (spawnedEditor() != null) {
                return;
            }

            final boolean created = createModel(false);

            if (!created) {
                return;
            }

            final SessionNode sessionNode = getSessionNode();
            final boolean cloned = sessionNode.useClonedModel();

            final SessionModel model = sessionNode.getModel();
            final Class<?> modelClass = model.getClass();
            final SessionNodeModelConfig modelConfig = this.config.getModelConfig(modelClass);

            final Object[] arguments = {model};
            final JPanel editor = modelConfig.getEditorInstance(arguments);
            addEditorListener(editor);

            ModificationRegistery.registerEditor(sessionNode, editor);

            final String descrip = modelConfig.getName();
            editor.setName(getName() + " (" + descrip + ")");

            final EditorWindow editorWindow = new EditorWindow(editor, editor.getName(), "Done", cloned, this);

            editorWindow.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameClosing(final InternalFrameEvent e) {
                    if (getChildren().iterator().hasNext()) {
                        finishedEditingDialog();
                    }

                    ModificationRegistery.unregisterSessionNode(
                            sessionNode);
                    setSpawnedEditor(null);

                    final EditorWindow window = (EditorWindow) e.getSource();
                    if (window.isCanceled()) {
                        sessionNode.restoreOriginalModel();
                    }

                    sessionNode.forgetSavedModel();
                }
            });

            DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
            editorWindow.pack();
            editorWindow.setVisible(true);
            this.spawnedEditor = editorWindow;

            if (this.sessionWrapper != null) {
                this.sessionWrapper.setSessionChanged(true);
            }

//            for (SessionNode child : getChildren()) {
//
//                // only break edges to children.
//                if (edge.getNode2() == getModelNode()) {
//                    SessionNodeWrapper otherWrapper =
//                            (SessionNodeWrapper) edge.getNode1();
//                    SessionNode other = otherWrapper.getSessionNode();
//                    if (getChildren().contains(other)) {
//                        sessionWrapper.removeEdge(edge);
//                    }
//                } else {
//                    SessionNodeWrapper otherWrapper =
//                            (SessionNodeWrapper) edge.getNode2();
//                    SessionNode other = otherWrapper.getSessionNode();
//                    if (getChildren().contains(other)) {
//                        sessionWrapper.removeEdge(edge);
//                    }
//                }
//            }
//                Class[] consistentModelClasses = child.getConsistentModelClasses(false);
//                if (consistentModelClasses.length == 0) {
//                    child.removeParent(sessionNode);
//                    SessionEditorWorkbench sessionWorkbench = getSessionWorkbench();
//                    SessionWrapper sessionWrapper = sessionWorkbench.getSessionWrapper();
//                    Node node1 = sessionWrapper.getNode(sessionNode.getDisplayName());
//                    Node node2 = sessionWrapper.getNode(child.getDisplayName());
//                    Edge edge = sessionWrapper.getEdge(node1, node2);
//                    sessionWrapper.removeEdge(edge);
//                }
        } catch (final CouldNotCreateModelException e) {
            SessionUtils.showPermissibleParentsDialog(e.getModelClass(),
                    this, true, true);
            e.printStackTrace();

        } catch (final ClassCastException e) {
            e.printStackTrace();
        } catch (final Exception e) {
            Throwable cause = e;

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            final Component centeringComp = this;
            final String s = cause.getMessage();

            if (!"".equals(s)) {
                JOptionPane.showMessageDialog(centeringComp, s,
                        null, JOptionPane.WARNING_MESSAGE);
            }

            e.printStackTrace();
        }

    }

    /**
     * Sets the selection status of the node.
     *
     * @param selected the selection status of the node (true or false).
     */
    @Override
    public void setSelected(final boolean selected) {
        super.setSelected(selected);
        getSessionDisplayComp().setSelected(selected);
    }

    //===========================PRIVATE METHODS===========================//
    private SessionEditorWorkbench getSessionWorkbench() {
        if (this.sessionWorkbench == null) {
            SessionEditorIndirectRef sessionEditorRef
                    = DesktopController.getInstance().getFrontmostSessionEditor();
            SessionEditor sessionEditor = (SessionEditor) sessionEditorRef;

            if (sessionEditor == null) {
                DesktopController.getInstance().newSessionEditor();
                sessionEditorRef
                        = DesktopController.getInstance().getFrontmostSessionEditor();
                sessionEditor = (SessionEditor) sessionEditorRef;
            }

            this.sessionWorkbench = sessionEditor.getSessionWorkbench();
        }
        return this.sessionWorkbench;
    }

    private void addListeners(final SessionEditorNode sessionEditorNode,
                              final SessionNodeWrapper modelNode) {
        // Add a mouse listener for popups.
        sessionEditorNode.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final ToolTipManager toolTipManager
                            = ToolTipManager.sharedInstance();
                    toolTipManager.setInitialDelay(750);
                    sessionEditorNode.getPopup().show(sessionEditorNode, e.getX(), e.getY());
                }

                e.consume();
            }
        });

//        sessionEditorNode.addMouseMotionListener(new MouseMotionAdapter() {
//            public void mouseMoved(MouseEvent e) {
//                Point p = e.getPoint();
//                if (p.getX() > 40 && p.getY() > 40) {
//                    ToolTipManager toolTipManager =
//                            ToolTipManager.sharedInstance();
//                    toolTipManager.setInitialDelay(750);
//                    JPopupMenu popup = sessionEditorNode.getPopup();
//
//                    if (!popup.isShowing()) {
//                        popup.show(sessionEditorNode, e.getX(), e.getY());
//                    }
//                }
//            }
//        });
        sessionEditorNode.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(final ComponentEvent e) {
                sessionEditorNode.getSimulationStudy().getSession().setSessionChanged(true);
            }
        });

        final SessionNode sessionNode = modelNode.getSessionNode();

        sessionNode.addSessionListener(new SessionAdapter() {

            @Override
            public void modelCreated(final SessionEvent sessionEvent) {
                sessionEditorNode.adjustToModel();

                // This code is here to allow multiple editor windows to be
                // opened at the same time--if a new model is created,
                // the getModel editor window is simply closed.  jdramsey
                // 5/18/02
                if (sessionEditorNode.spawnedEditor() != null) {
                    final EditorWindow editorWindow = sessionEditorNode.spawnedEditor();
                    editorWindow.closeDialog();
                }
            }

            @Override
            public void modelDestroyed(final SessionEvent sessionEvent) {
                sessionEditorNode.adjustToModel();

                // This code is here to allow multiple editor windows to be
                // opened at the same time--if the getModel model is destroyed,
                // the getModel editor window is closed. jdramsey 5/18/02
                if (sessionEditorNode.spawnedEditor() != null) {
                    final EditorWindow editorWindow = sessionEditorNode.spawnedEditor();
                    editorWindow.closeDialog();
                }
            }

            @Override
            public void modelUnclear(final SessionEvent sessionEvent) {
                try {
                    if (SessionEditorNode.this.simulationStudy == null) {
                        final boolean created = sessionEditorNode.createModel(false);

                        if (!created) {
                            return;
                        }

                        sessionEditorNode.adjustToModel();
                    }
                } catch (final Exception e) {
                    String message = e.getMessage();

                    message = "I could not make a model for this box, sorry. Maybe the \n"
                            + "parents aren't right or have not been constructed yet.";

                    e.printStackTrace();

//                    throw new IllegalArgumentException("I could not make a model for this box, sorry. Maybe the \n" +
//                            "parents aren't right or have not been constructed yet.");
                    JOptionPane.showMessageDialog(sessionEditorNode, message);
                }
            }
        });
    }

    /**
     * Adds a property change listener that listends for "changeNodeLabel"
     * events.
     */
    private void addEditorListener(final JPanel editor) {
        editor.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if ("changeNodeLabel".equals(evt.getPropertyName())) {
                    getDisplayComp().setName((String) evt.getNewValue());
                    final SessionNodeWrapper wrapper
                            = (SessionNodeWrapper) getModelNode();
                    wrapper.setSessionName((String) evt.getNewValue());
                    adjustToModel();
                }
            }
        });
    }

    /**
     * Creates a popup for a note node.
     *
     * @return - popup
     */
    private JPopupMenu getNotePopup() {
        final JPopupMenu popup = new JPopupMenu();

        final JMenuItem renameBox = new JMenuItem("Rename note");
        renameBox.setToolTipText("<html>Renames this note.</html>");
        renameBox.addActionListener((e) -> {
            final Component centeringComp = this;
            final String name
                    = JOptionPane.showInputDialog(centeringComp, "New name:");

            if (!NamingProtocol.isLegalName(name)) {
                JOptionPane.showMessageDialog(centeringComp,
                        NamingProtocol.getProtocolDescription());
                return;
            }

            final SessionNodeWrapper wrapper
                    = (SessionNodeWrapper) getModelNode();
            wrapper.setSessionName(name);
            getSessionDisplayComp().setName(name);
            adjustToModel();
        });

        final JMenuItem cloneBox = new JMenuItem("Clone Note");
        cloneBox.setToolTipText("<html>"
                + "Makes a copy of this session note and its contents. To clone<br>"
                + "a whole subgraph, or to paste into a different sessions, select<br>"
                + "the subgraph and use the Copy/Paste gadgets in the Edit menu."
                + "</html>");
        cloneBox.addActionListener((e) -> {
            firePropertyChange("cloneMe", null, this);
        });

        final JMenuItem deleteBox = new JMenuItem("Delete Note");
        deleteBox.setToolTipText(
                "<html>Deletes this note from the workbench</html>");

        deleteBox.addActionListener((e) -> {
            if (getSessionNode().getModel() == null) {
                final Component centeringComp = this;
                final int ret = JOptionPane.showConfirmDialog(centeringComp,
                        "Really delete note?");

                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            } else {
                final Component centeringComp = this;
                final int ret = JOptionPane.showConfirmDialog(centeringComp,
                        "<html>"
                                + "Really delete note? Any information it contains will<br>"
                                + "be destroyed." + "</html>");

                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            firePropertyChange("deleteNode", null, null);
        });

        final JMenuItem help = new JMenuItem("Help");
        deleteBox.setToolTipText("<html>Shows help for this box.</html>");

        help.addActionListener((e) -> {
            final SessionNodeWrapper sessionNodeWrapper
                    = (SessionNodeWrapper) getModelNode();
            final SessionNode sessionNode = sessionNodeWrapper.getSessionNode();
            showInfoBoxForModel(sessionNode, sessionNode.getModelClasses());
        });

        popup.add(renameBox);
        popup.add(cloneBox);
        popup.add(deleteBox);
        popup.addSeparator();
        popup.add(help);

        return popup;
    }

    JPopupMenu popup;

    /**
     * Creates the popup for the node.
     */
    private JPopupMenu getPopup() {
        if (this.popup != null && this.popup.isShowing()) {
            return this.popup;
        }

        this.popup = new JPopupMenu();

        final JMenuItem createModel = new JMenuItem("Create Model");
        createModel.setToolTipText("<html>Creates a new model for this node"
                + "<br>of the type selected.</html>");

        createModel.addActionListener((e) -> {
            try {
                if (getSessionNode().getModel() == null) {
                    createModel(false);
                } else {
                    final Component centeringComp = this;
                    JOptionPane.showMessageDialog(centeringComp,
                            "Please destroy the model model first.");
                }
            } catch (final Exception e1) {
                final Component centeringComp = this;
                JOptionPane.showMessageDialog(centeringComp,
                        "Could not create a model for this box.");
                e1.printStackTrace();
            }
        });

        final JMenuItem editModel = new JMenuItem("Edit Model");
        editModel.setToolTipText("<html>Edits the model in this node.</html>");

        editModel.addActionListener((e) -> {
            try {
                if (getSessionNode().getModel() == null) {
                    final Component centeringComp = this;
                    JOptionPane.showMessageDialog(centeringComp,
                            "Sorry, no model has been created yet; there's nothing to edit.");
                } else {
                    doDoubleClickAction();
                }
            } catch (final Exception e1) {
                final Component centeringComp = this;
                JOptionPane.showMessageDialog(centeringComp,
                        "Double click failed. See console for exception.");
                e1.printStackTrace();
            }
        });

        final JMenuItem destroyModel = new JMenuItem("Destroy Model");
        destroyModel.setToolTipText("<html>Destroys the model for this node, "
                + "<br>if it has one, destroying any "
                + "<br>downstream models as well.</html>");

        destroyModel.addActionListener((e) -> {
            final Component centeringComp = this;

            if (getSessionNode().getModel() == null) {
                JOptionPane.showMessageDialog(centeringComp,
                        "Sorry, this box does not contain a model to destroy.");
                return;
            }

            final Set<SessionNode> children = getSessionNode().getChildren();
            boolean found = false;

            for (final SessionNode child : children) {
                if (child.getModel() != null) {
                    found = true;
                }
            }

            if (found) {
                final int ret = JOptionPane.showConfirmDialog(centeringComp,
                        "Destroying the model in this box will also destroy models in any boxes\n"
                                + "downstream. Is that OK?", null,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            // clear all children parameters
            children.forEach(sessionNode -> {
                Arrays.stream(sessionNode.getModelClasses())
                        .forEach(m -> sessionNode.putParam(m, new Parameters()));
            });

            // clear all parameters
            final SessionNode sessionNode = getSessionNode();
            Arrays.stream(sessionNode.getModelClasses())
                    .forEach(m -> sessionNode.putParam(m, new Parameters()));

            destroyModel();
        });

        final JMenuItem propagateDownstream
                = new JMenuItem("Propagate changes downstream");
        propagateDownstream.setToolTipText("<html>"
                + "Fills in this box and downstream boxes with models,"
                + "<br>overwriting any models that already exist.</html>");

        propagateDownstream.addActionListener((e) -> {
            final Component centeringComp = this;

            if (getSessionNode().getModel() != null && !getSessionNode().getChildren().isEmpty()) {
                final int ret = JOptionPane.showConfirmDialog(centeringComp,
                        "You will be rewriting all downstream models. Is that OK?",
                        "Confirm",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (ret != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                createDescendantModels(true);
            } catch (final RuntimeException e1) {
                JOptionPane.showMessageDialog(centeringComp,
                        "Could not complete the creation of descendant models.");
                e1.printStackTrace();
            }
        });

        final JMenuItem renameBox = new JMenuItem("Rename Box");
        renameBox.setToolTipText("<html>Renames this session box.</html>");
        renameBox.addActionListener((e) -> {
            final Component centeringComp = this;
            final String name
                    = JOptionPane.showInputDialog(centeringComp, "New name:");

            if (!NamingProtocol.isLegalName(name)) {
                JOptionPane.showMessageDialog(centeringComp,
                        NamingProtocol.getProtocolDescription());
                return;
            }

            final SessionNodeWrapper wrapper
                    = (SessionNodeWrapper) getModelNode();
            wrapper.setSessionName(name);
            getSessionDisplayComp().setName(name);
            adjustToModel();
        });

        final JMenuItem cloneBox = new JMenuItem("Clone Box");
        cloneBox.setToolTipText("<html>"
                + "Makes a copy of this session box and its contents. To clone<br>"
                + "a whole subgraph, or to paste into a different sessions, select<br>"
                + "the subgraph and use the Copy/Paste gadgets in the Edit menu."
                + "</html>");
        cloneBox.addActionListener((e) -> {
            firePropertyChange("cloneMe", null, this);
        });

        final JMenuItem deleteBox = new JMenuItem("Delete Box");
        deleteBox.setToolTipText(
                "<html>Deletes this box from the workbench</html>");

        deleteBox.addActionListener((e) -> {
            final Component centeringComp = this;
            final int ret = JOptionPane.showConfirmDialog(centeringComp,
                    "Are you sure you want to delete this box? It contains some work.",
                    null, JOptionPane.YES_NO_OPTION);

            if (ret != JOptionPane.YES_OPTION) {
                return;
            }

            firePropertyChange("deleteNode", null, null);
        });

        this.popup.add(createModel);

        final SessionModel model = getSessionNode().getModel();
        final Class modelClass = (model == null)
                ? determineTheModelClass(getSessionNode())
                : model.getClass();
        if (getSessionNode().existsParameterizedConstructor(modelClass)) {
            final ParameterEditor paramEditor = getParameterEditor(modelClass);

            if (paramEditor != null) {
                final JMenuItem editSimulationParameters = new JMenuItem("Edit Parameters...");
                editSimulationParameters.setToolTipText("<html>");
                editSimulationParameters.addActionListener((e) -> {
                    final Parameters param = getSessionNode().getParam(modelClass);
                    final Object[] arguments = getSessionNode().getModelConstructorArguments(modelClass);

                    if (param != null) {
                        try {
                            editParameters(modelClass, param, arguments);
                            final int ret = JOptionPane.showConfirmDialog(JOptionUtils.centeringComp(),
                                    "Should I overwrite the contents of this box and all delete the contents\n"
                                            + "of all boxes downstream?",
                                    "Double check...", JOptionPane.YES_NO_OPTION);
                            if (ret == JOptionPane.YES_OPTION) {
                                getSessionNode().destroyModel();
                                getSessionNode().createModel(modelClass, true);
                            }
                        } catch (final Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                this.popup.add(editSimulationParameters);
            }
        }

//        final SessionNode thisNode = getSessionNode();
//
//        //        popup.add(getConsistentParentMenuItems(getConsistentParentBoxTypes(thisNode)));
////        popup.add(getConsistentChildBoxMenus(getConsistentChildBoxTypes(thisNode, null)));
//
//        addConsistentParentMenuItems(popup, getConsistentParentBoxTypes(thisNode));
//        addConsistentChildBoxMenus(popup, getConsistentChildBoxTypes(thisNode, null));
//
//        popup.addSeparator();
        this.popup.add(createModel);

        this.popup.add(editModel);
        this.popup.add(destroyModel);

        this.popup.addSeparator();

        this.popup.add(renameBox);
        this.popup.add(cloneBox);
        this.popup.add(deleteBox);

        this.popup.addSeparator();

        addEditLoggerSettings(this.popup);
        this.popup.add(propagateDownstream);

        return this.popup;
    }

    private ParameterEditor getParameterEditor(final Class modelClass) {
        final SessionNodeModelConfig modelConfig = this.config.getModelConfig(modelClass);
        return modelConfig.getParameterEditorInstance();
    }

//    private void addConsistentChildBoxMenus(JPopupMenu menu, List<String> consistentChildBoxes) {
//        for (String _type : consistentChildBoxes) {
//            final JMenuItem menuItem = new JMenuItem(_type);
//
//            menuItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    String text = menuItem.getText();
//                    String[] tokens = text.split(" ");
//                    String type = tokens[1];
//                    new ConstructTemplateAction("Test").addChild(SessionEditorNode.this, type);
//                }
//            });
//
//            menu.add(menuItem);
//        }
//    }
//    private JMenu addConsistentChildBoxMenus(List<String> consistentChildBoxes) {
//        JMenu newChildren = new JMenu("New Child Box");
//
//        for (String _type : consistentChildBoxes) {
//            final JMenuItem menuItem = new JMenuItem(_type);
//
//            menuItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    new ConstructTemplateAction("Test").addChild(SessionEditorNode.this, menuItem.getText());
//                }
//            });
//
//
//
//            newChildren.add(menuItem);
//        }
//        return newChildren;
//    }
//    private JMenu addConsistentParentMenuItems(JPopupMenu menu, List<SessionNode> consistentParentNodes) {
//        final JMenu newParents = new JMenu("New Parent Box");
//
//        for (final SessionNode node : consistentParentNodes) {
//            final JMenuItem menuItem = new JMenuItem("Add Links: " + node.getDisplayName());
//
//            menuItem.addActionListener(new ActionListener() {
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    String displayName1 = node.getDisplayName();
//                    String displayName2 = SessionEditorNode.this.getSessionNode().getDisplayName();
//                    new ConstructTemplateAction("Test").addEdge(displayName1, displayName2);
//                }
//            });
//
//            menu.add(menuItem);
//        }
//
//        return newParents;
//    }
//    private List<String> getConsistentChildBoxTypes(SessionNode thisNode, SessionModel model) {
//        List<String> consistentChildBoxes = new ArrayList<>();
//
//        List<Node> nodes = sessionWorkbench.getSessionWrapper().getNodes();
//        List<SessionNode> sessionNodes = new ArrayList<>();
//        for (Node node : nodes) sessionNodes.add(((SessionNodeWrapper) node).getSessionNode());
//
//        Set<String> strings = TetradApplicationConfig.getInstance().getConfigs().keySet();
//
//        for (String type : strings) {
//            SessionNodeConfig config = TetradApplicationConfig.getInstance().getSessionNodeConfig(type);
//            Class[] modelClasses = config.getModels();
//
//            SessionNode newNode = new SessionNode(modelClasses);
//
//            if (newNode.isConsistentParent(thisNode, sessionNodes)) {
//                consistentChildBoxes.add("Add " + type);
//            }
//        }
//
//        return consistentChildBoxes;
//    }
//    private List<SessionNode> getConsistentParentBoxTypes(SessionNode thisNode) {
//        List<SessionNode> consistentParentBoxes = new ArrayList<>();
//
//        for (Node _node : getSessionWorkbench().getSessionWrapper().getNodes()) {
//            SessionNode node = ((SessionNodeWrapper) _node).getSessionNode();
//
//            if (sessionWorkbench.getSessionWrapper().isAncestorOf(thisNode, node)) {
//                continue;
//            }
//
//            if (!thisNode.getParents().contains(node) && thisNode.isConsistentParent(node)) {
//                consistentParentBoxes.add(node);
//            }
//        }
//
//        return consistentParentBoxes;
//    }

    /**
     * Adds the "Edit logger" option if applicable.
     */
    private void addEditLoggerSettings(final JPopupMenu menu) {
        final SessionNodeWrapper modelNode = (SessionNodeWrapper) getModelNode();
        final SessionNode sessionNode = modelNode.getSessionNode();
        final TetradLoggerConfig config = sessionNode.getLoggerConfig();
        if (config != null) {
            final JMenuItem item = new JMenuItem("Edit Logger Settings ...");
            item.addActionListener((e) -> {
                showLogConfig(config);
            });
            menu.add(item);
        }
    }

    /**
     * Shows a dialog that allows the user to change the settings for the box's
     * model logger.
     */
    private void showLogConfig(final TetradLoggerConfig config) {
        final List<TetradLoggerConfig.Event> events = config.getSupportedEvents();
        final JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, events.size() / 3));
        for (final TetradLoggerConfig.Event event : events) {
            final String id = event.getId();
            final JCheckBox checkBox = new JCheckBox(event.getDescription());
            checkBox.setHorizontalTextPosition(SwingConstants.RIGHT);
            checkBox.setSelected(config.isEventActive(id));
            checkBox.addActionListener((e) -> {
                final JCheckBox box = (JCheckBox) e.getSource();
                config.setEventActive(id, box.isSelected());
            });
            panel.add(checkBox);
        }
        panel.setBorder(new TitledBorder("Select Events to Log"));
        // how show the dialog
        JOptionPane.showMessageDialog(this, panel, "Edit Events to Log", JOptionPane.PLAIN_MESSAGE);
    }

    private void executeSessionNode(final SessionNode sessionNode,
                                    final boolean overwrite) {
        final Window owner = (Window) getTopLevelAncestor();

        new WatchedProcess(owner) {
            @Override
            public void watch() {
                final Class c = SessionEditorWorkbench.class;
                final Container container = SwingUtilities.getAncestorOfClass(c,
                        SessionEditorNode.this);
                final SessionEditorWorkbench workbench
                        = (SessionEditorWorkbench) container;

                System.out.println("Executing " + sessionNode);

                workbench.getSimulationStudy().execute(sessionNode, overwrite);
            }
        };
    }

    private void createDescendantModels(final boolean overwrite) {
        final Window owner = (Window) getTopLevelAncestor();

        new WatchedProcess(owner) {
            @Override
            public void watch() {
                final Class clazz = SessionEditorWorkbench.class;
                final Container container = SwingUtilities.getAncestorOfClass(clazz,
                        SessionEditorNode.this);
                final SessionEditorWorkbench workbench
                        = (SessionEditorWorkbench) container;

                if (workbench != null) {
                    workbench.getSimulationStudy().createDescendantModels(
                            getSessionNode(), overwrite);
                }
            }
        };
    }

    /**
     * After editing a session node, either run changes or break edges.
     */
    private void finishedEditingDialog() {
        if (!ModificationRegistery.modelHasChanged(getSessionNode())) {
            return;
        }

        // If there isn't any model downstream, no point to showing the next
        // dialog.
        for (final SessionNode child : getChildren()) {
            if (child.getModel() != null) {
                continue;
            }

            return;
        }

        final Object[] options = {"Execute", "Break Edges"};
        final Component centeringComp = this;
        final int selection = JOptionPane.showOptionDialog(centeringComp,
                "Changing this node will affect its children.\n"
                        + "Click on \"Execute\" to percolate changes down.\n"
                        + "Click on \"Break Edges\" to leave the children the same.",
                "Warning", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]);

        if (selection == 0) {
            for (final SessionNode child : getChildren()) {
                executeSessionNode(child, true);
            }
        } else if (selection == 1) {
            for (final Edge edge : this.sessionWrapper.getEdges(getModelNode())) {

                // only break edges to children.
                if (edge.getNode2() == getModelNode()) {
                    final SessionNodeWrapper otherWrapper
                            = (SessionNodeWrapper) edge.getNode1();
                    final SessionNode other = otherWrapper.getSessionNode();
                    if (getChildren().contains(other)) {
                        this.sessionWrapper.removeEdge(edge);
                    }
                } else {
                    final SessionNodeWrapper otherWrapper
                            = (SessionNodeWrapper) edge.getNode2();
                    final SessionNode other = otherWrapper.getSessionNode();
                    if (getChildren().contains(other)) {
                        this.sessionWrapper.removeEdge(edge);
                    }
                }
            }
        }
    }

    /**
     * Creates a model in the wrapped SessionNode, given the SessionNode's
     * parent models.
     *
     * @throws IllegalStateException if the model cannot be created. The reason
     *                               why the model cannot be created is in the message of the exception.
     */
    public boolean createModel(final boolean simulation) throws Exception {
        if (getSessionNode().getModel() != null) {
            return true;
        }

        final SessionNode sessionNode = getSessionNode();
        final Class modelClass = determineTheModelClass(sessionNode);

        if (modelClass == null && !simulation) {
            JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                    this.config.getNodeSpecificMessage());
            return false;
        }

        // Must determine whether that model has a parameterizing object
        // associated with it and if so edit that. (This has to be done
        // before creating the model since it will be an argument to the
        // constructor of the model.)
        if (sessionNode.existsParameterizedConstructor(modelClass)) {
            final Parameters params = sessionNode.getParam(modelClass);
            final Object[] arguments = sessionNode.getModelConstructorArguments(modelClass);

            if (params != null) {
                final boolean edited = editParameters(modelClass, params, arguments);

                if (!edited) {
                    return false;
                }
            }
        }

        // Finally, create the model. Don't worry, if anything goes wrong
        // in the process, an exception will be thrown with an
        // appropriate message.
        sessionNode.createModel(modelClass, simulation);
        return true;
    }

    /**
     * @return the model class, or null if no model class was determined.
     */
    public Class determineTheModelClass(final SessionNode sessionNode) {

        // The config file lists which model classes the node can be
        // associated with, based on the node's type.
        SessionEditorNode.loadModelClassesFromConfig(sessionNode);

        final Class[] modelClasses = sessionNode.getConsistentModelClasses(true);

        // If you can't even put a model into the object, throw an
        // exception.
        if ((modelClasses == null) || (modelClasses.length == 0)) {
            return null;
        }

        // Choose a single model class either by asking the user or by just
        // returning the single model in the list.
        return modelClasses.length > 1 ? getModelClassFromUser(modelClasses,
                true) : modelClasses[0];
    }

    /**
     * @return the selected model class, or null if no model class was selected.
     */
    private Class getModelClassFromUser(final Class[] modelClasses,
                                        final boolean cancelable) {

        // Count the number of model classes that can be listed for the user;
        // if there's only one, don't ask the user for input.
        final List<Class> reducedList = new LinkedList<>();

        for (final Class modelClass : modelClasses) {
            if (!(UnlistedSessionModel.class.isAssignableFrom(modelClass))) {
                reducedList.add(modelClass);
            }
        }

        if (reducedList.isEmpty()) {
            throw new RuntimeException("There is no model to choose.");
        }

        final SessionNodeWrapper sessionNodeWrapper = (SessionNodeWrapper) getModelNode();
        final SessionNode sessionNode = sessionNodeWrapper.getSessionNode();
        final ModelChooser chooser = this.config.getModelChooserInstance(sessionNode);
        final Component centeringComp = this;

        if (cancelable) {
            final int selection = JOptionPane.showOptionDialog(centeringComp,
                    chooser, chooser.getTitle(), JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null,
                    new String[]{"OK", "Cancel"}, "OK");

            if (selection == 0) {
                //this.rememberLastClass = modelTypeChooser.getRemembered();
                return chooser.getSelectedModel();
            } else {
                return null;
            }
        } else {
            JOptionPane.showOptionDialog(centeringComp, chooser, chooser.getTitle(),
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, new String[]{"OK"}, "OK");

            //this.rememberLastClass = modelTypeChooser.getRemembered();
            return chooser.getSelectedModel();
        }
    }

    private Class showInfoBoxForModel(final SessionNode sessionNode, final Class[] modelClasses) {

        // Count the number of model classes that can be listed for the user;
        // if there's only one, don't ask the user for input.
        final List<Class> reducedList = new LinkedList<>();

        for (final Class modelClass : modelClasses) {
            if (!(UnlistedSessionModel.class.isAssignableFrom(modelClass))) {
                reducedList.add(modelClass);
            }
        }

        if (reducedList.isEmpty()) {
            throw new RuntimeException("There is no model to choose.");
        }

        final ModelChooser chooser = this.config.getModelChooserInstance(sessionNode);
        final Component centeringComp = this;

        JOptionPane.showMessageDialog(centeringComp, chooser,
                "Choose Model for Help...", JOptionPane.QUESTION_MESSAGE);

        return chooser.getSelectedModel();
    }

    public Set<SessionNode> getChildren() {
        final SessionNodeWrapper _sessionNodeWrapper
                = (SessionNodeWrapper) getModelNode();
        final SessionNode _sessionNode = _sessionNodeWrapper.getSessionNode();
        return _sessionNode.getChildren();
    }

    /**
     * Update the model classes of the given node to the latest available model
     * classes, of such exists. Otherwise, leave the model classes of the node
     * unchanged.
     */
    private static void loadModelClassesFromConfig(final SessionNode sessionNode) {
        final String nodeName = sessionNode.getBoxType();

        if (nodeName != null) {
            final String baseName = SessionEditorNode.extractBase(nodeName);
            final Class[] newModelClasses = SessionEditorNode.modelClasses(baseName);

            if (newModelClasses != null) {
                sessionNode.setModelClasses(newModelClasses);
            } else {
                throw new RuntimeException("Model classes for this session "
                        + "node were not set in the configuration.");
            }
        }
    }

    /**
     * Destroys the model for the wrapped SessionNode.
     */
    private void destroyModel() {
        getSessionNode().destroyModel();
        getSessionNode().forgetOldModel();
    }

    /**
     * Tries to edit the parameters, returns true if successfully otherwise
     * false is returned
     */
    public boolean editParameters(final Class modelClass, final Parameters params,
                                  final Object[] parentModels) {
        if (parentModels == null) {
            throw new NullPointerException("Parent models array is null.");
        }

        if (params == null) {
            throw new NullPointerException("Parameters cannot be null.");
        }

        final ParameterEditor paramEditor = getParameterEditor(modelClass);

        if (paramEditor == null) {
            // if no editor, then consider the params "edited".
            return true;
        } else {
            paramEditor.setParams(params);
            paramEditor.setParentModels(parentModels);
        }
        // If a finalizing editor and a dialog then let it handle things on itself onw.
        if (paramEditor instanceof FinalizingParameterEditor && paramEditor instanceof JDialog) {
            final FinalizingParameterEditor e = (FinalizingParameterEditor) paramEditor;
            e.setup();
            return e.finalizeEdit();
        }
        // wrap editor and deal with response.
        paramEditor.setup();
        final JComponent editor = (JComponent) paramEditor;
        final SessionNodeWrapper nodeWrapper = (SessionNodeWrapper) getModelNode();
        final String buttonType = nodeWrapper.getButtonType();
        editor.setName(buttonType + " Structure Editor");
        final Component centeringComp = this;

        final int ret = JOptionPane.showOptionDialog(centeringComp, editor,
                editor.getName(), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, null,
                null, null);

        // if finalizing editor, then deal with specially.
        return ret == JOptionPane.OK_OPTION && (!(paramEditor instanceof FinalizingParameterEditor)
                || ((FinalizingParameterEditor) paramEditor).finalizeEdit());

    }

    public SessionNode getSessionNode() {
        final SessionNodeWrapper wrapper = (SessionNodeWrapper) this.getModelNode();
        return wrapper.getSessionNode();
    }

    private void setSpawnedEditor(final EditorWindow editorWindow) {
        this.spawnedEditor = editorWindow;
    }

    /**
     * @return the spawned editor, if there is one.
     */
    private EditorWindow spawnedEditor() {
        return this.spawnedEditor;
    }

    /**
     * Sets up default parameter objects for the node.
     */
    private void createParamObjects(final SessionEditorNode sessionEditorNode) {
        final SessionNode sessionNode = sessionEditorNode.getSessionNode();
        final Class[] modelClasses = sessionNode.getModelClasses();
        for (final Class clazz : modelClasses) {
            // A parameter class might exist if this session was read
            // in from a file.
            if (sessionNode.getParam(clazz) == null) {
                final SessionNodeModelConfig modelConfig = this.config.getModelConfig(clazz);
                if (modelConfig == null) {
                    continue;
                }

                sessionNode.putParam(clazz, new Parameters(sessionNode.getParameters()));
            }
        }
    }

    /**
     * @return the substring of <code>name</code> up to but not including a
     * contiguous string of digits at the end. For example, given "Graph123"
     */
    private static String extractBase(final String name) {
        if (name == null) {
            throw new NullPointerException("Name must not be null.");
        }

        for (int i = name.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(name.charAt(i))) {
                return name.substring(0, i + 1);
            }
        }

        return "Node";
    }

    /**
     * @return the model classes associated with the given button type.
     * @throws NullPointerException if no classes are stored for the given type.
     */
    private static Class[] modelClasses(final String boxType) {
        final TetradApplicationConfig config = TetradApplicationConfig.getInstance();
        final SessionNodeConfig nodeConfig = config.getSessionNodeConfig(boxType);
        if (nodeConfig == null) {
            throw new NullPointerException("THere is no configuration for " + boxType);
        }

        return nodeConfig.getModels();
    }

    private SimulationStudy getSimulationStudy() {
        return this.simulationStudy;
    }

    private SessionDisplayComp getSessionDisplayComp() {
        return (SessionDisplayComp) getDisplayComp();
    }
}
