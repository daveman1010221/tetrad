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

import edu.cmu.tetrad.bayes.Evidence;
import edu.cmu.tetrad.bayes.Proposition;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

/**
 * Edits evidence for a Bayes net--that is, which categories (perhaps
 * disjunctive) are known for which variables, and which variables are
 * manipulated.
 *
 * @author Joseph Ramsey
 */
class EvidenceEditorObs extends JPanel {
    private final Evidence evidence;
    private final JToggleButton[][] buttons;

    private final HashMap<JToggleButton, Integer> buttonsToVariables =
            new HashMap<>();
    private final HashMap<JToggleButton, Integer> buttonsToCategories =
            new HashMap<>();
    private final HashMap<JCheckBox, Integer> checkBoxesToVariables =
            new HashMap<>();
    private final HashMap<Integer, JCheckBox> variablesToCheckboxes =
            new HashMap<>();

    public EvidenceEditorObs(final Evidence evidence) {
        if (evidence == null) {
            throw new NullPointerException();
        }

        this.evidence = evidence;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final Box d = Box.createHorizontalBox();
        d.add(new JLabel("Variable/Categories"));
        d.add(Box.createHorizontalGlue());
        d.add(new JLabel("Manipulated"));
        add(d);

        this.buttons = new JToggleButton[evidence.getNumNodes()][];

        for (int i = 0; i < evidence.getNumNodes(); i++) {
            // skip latent variables
            if (evidence.getNode(i).getNodeType() == NodeType.LATENT) {
                continue;
            }
            final Box c = Box.createHorizontalBox();
            c.add(new JLabel(evidence.getNode(i).getName() + ":  ") {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            });

            this.buttons[i] = new JToggleButton[evidence.getNumCategories(i)];
            final Node node = evidence.getNode(i);

            for (int j = 0; j < evidence.getNumCategories(i); j++) {
                final String name = evidence.getCategory(node, j);
                final JToggleButton button = new JToggleButton(" " + name + " ") {
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };

                button.setBorder(new CompoundBorder(
                        new LineBorder(Color.DARK_GRAY, 1, true),
                        new EmptyBorder(0, 2, 0, 1)));
                button.setBackground(Color.WHITE);
                button.setUI(new BasicButtonUI());
                button.setFont(new Font("Serif", Font.BOLD, 12));

                this.buttonsToVariables.put(button, i);
                this.buttonsToCategories.put(button, j);

                this.buttons[i][j] = button;

                button.addActionListener(new ActionListener() {
                    public void actionPerformed(final ActionEvent e) {
                        final JToggleButton button = (JToggleButton) e.getSource();
                        final int i = EvidenceEditorObs.this.buttonsToVariables.get(button);
                        final int j = EvidenceEditorObs.this.buttonsToCategories.get(button);

                        final Proposition proposition =
                                getEvidence().getProposition();

                        if (proposition.getNumAllowed(i) ==
                                getEvidence().getNumCategories(i)) {
                            proposition.setCategory(i, j);
                            // for now, all evidence is assumed to be manipulated
                            // (for identifiability)
                            getEvidence().setManipulated(i, true);
                            final JCheckBox checkbox = EvidenceEditorObs.this.variablesToCheckboxes.get(i);
                            checkbox.setSelected(true);
                        } else if (proposition.getNumAllowed(i) == 1) {
                            if (proposition.getSingleCategory(i) == j) {
                                proposition.removeCategory(i, j);
                            } else {
                                // disallow selecting more than one category
                                // in a variable
                                if ((ActionEvent.SHIFT_MASK &
                                        e.getModifiers()) != 1) {
                                    proposition.setVariable(i, false);
                                }
                                //proposition.addCategory(i, j);
                                proposition.setCategory(i, j);
                            }
                        } else { // toggle
                            if (proposition.isAllowed(i, j)) {
                                proposition.removeCategory(i, j);
                            } else {
                                proposition.addCategory(i, j);
                            }
                        }

                        if (proposition.getNumAllowed(i) == 0) {
                            proposition.setVariable(i, true);
                        }

                        resetSelected(i);
                    }
                });

                c.add(button);
                c.add(Box.createHorizontalStrut(2));
            }

            c.add(Box.createHorizontalGlue());
            final JCheckBox checkbox = new JCheckBox() {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            checkbox.setSelected(getEvidence().isManipulated(i));
            this.checkBoxesToVariables.put(checkbox, i);
            this.variablesToCheckboxes.put(i, checkbox);
            checkbox.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final JCheckBox checkbox = (JCheckBox) e.getSource();
                    final boolean selected = checkbox.isSelected();
                    final Object o = EvidenceEditorObs.this.checkBoxesToVariables.get(checkbox);
                    final int variable = (Integer) o;

                    if (getEvidence().getProposition().getSingleCategory(
                            variable) == -1) {
                        JOptionPane.showMessageDialog(checkbox,
                                "Please choose a single category to manipulate on.");
                        checkbox.setSelected(false);
                        getEvidence().setManipulated(variable, false);
                    } else {
                        // for now, always check the manipulated checkbox
                        // (for identifiability) whenever some variable value
                        // is selected
                        getEvidence().setManipulated(variable, true);
                        checkbox.setSelected(true);
                    }
                }
            });
            checkbox.setBackground(Color.WHITE);
            checkbox.setBorder(null);
            c.add(checkbox);
            c.setMaximumSize(new Dimension(1000, 30));
            add(c);

            resetSelected(i);
        }
    }

    private void highlightCorrectly(final Proposition proposition) {
        for (final JToggleButton _button : this.buttonsToVariables.keySet()) {
            final int _i = this.buttonsToVariables.get(_button);
            final int _j = this.buttonsToCategories.get(_button);

            if (!!proposition.isConditioned(_i) && proposition.isAllowed(_i, _j)) {
                _button.setBackground(Color.LIGHT_GRAY);
            } else {
                _button.setBackground(Color.WHITE);
            }
        }
    }

    public Evidence getEvidence() {
        return this.evidence;
    }

    private void resetSelected(final int variable) {
        if (this.evidence.hasNoEvidence(variable)) {
            for (int j = 0; j < this.buttons[variable].length; j++) {
                this.buttons[variable][j].setSelected(false);
            }
        } else {
            for (int j = 0; j < this.buttons[variable].length; j++) {
                this.buttons[variable][j].setSelected(
                        this.evidence.getProposition().isAllowed(variable, j));
            }
        }

        if (this.evidence.getProposition().getSingleCategory(variable) == -1) {
            final JCheckBox checkbox = this.variablesToCheckboxes.get(variable);
            checkbox.setSelected(false);
            getEvidence().setManipulated(variable, false);
        }


        highlightCorrectly(this.evidence.getProposition());
    }
}





