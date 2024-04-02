package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag.Pc;
import edu.cmu.tetrad.algcomparison.graph.*;
import edu.cmu.tetrad.algcomparison.simulation.Simulation;
import edu.cmu.tetrad.algcomparison.simulation.Simulations;
import edu.cmu.tetrad.util.ParamDescription;
import edu.cmu.tetrad.util.ParamDescriptions;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetradapp.app.TetradApplicationConfig;
import edu.cmu.tetradapp.editor.simulation.ParameterTab;
import edu.cmu.tetradapp.model.AlgcomparisonModel;
import edu.cmu.tetradapp.ui.model.IndependenceTestModel;
import edu.cmu.tetradapp.ui.model.IndependenceTestModels;
import edu.cmu.tetradapp.ui.model.ScoreModel;
import edu.cmu.tetradapp.ui.model.ScoreModels;
import edu.cmu.tetradapp.util.ParameterComponents;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.ParameterDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

/**
 * Displays an editor that lets the user interact with the Comparison class in the edu.cmu.tetrad.algcomparison package.
 * The user can select from a list of simulation methods, a list of algorithms, a list of statistics, and a list of
 * parameters. The list of parameters will be populated based on the simulation method, algorithm, and statistics
 * selected. The user can then run the comparison and view the results in a JTextArea, where the string displayed is
 * written to using the PrintStream "localOut" in the Comparison class. The variables 'localOut' needs to be passed in
 * as a parameter to the constructor of the Comparison class.
 * <p>
 * The list of simulation methods, algorithms, statistics, and parameters are populated by the methods in the Comparison
 * class. The lists of simulation methods, algorithms, statistics, and parameters are obtained using the Reflection API.
 * Simulations are classes that implement the edu.cmu.tetrad.algcomparison.simulation.Simulation interface, algorithms
 * are classes that implement the edu.cmu.tetrad.algcomparison.algorithm.Algorithm interface, and statistics are classes
 * that implement the edu.cmu.tetrad.algcomparison.statistic.Statistic interface. The simulation statistics are obtained
 * by a method as yet unknown. The algorithm statistics displayed may be obtained by calling the getStatistics() method
 * in the Algorithm interface. The parameters are obtained by calling the getParameters() method in the Algorithm
 * interface.
 * <p>
 * The simulations, algorithms, and statistics are each presented in a JList in a JScrollPane. The user can select one
 * simulation method, one or more algorithms, and one or more statistics. The parameters are presented in a parameter
 * panel that will be coded in the ParameterPanel class. The user can select the parameters for each algorithm and will
 * dynamically change based on the simulation method and algorithms selected. The user can then run the comparison by
 * clicking the "Run Comparison" button. The results will be displayed in a JTextArea in a JScrollPane.
 *
 * @author josephramsey 2024-3-29
 */
public class AlgcomparisonEditor extends JPanel {

    private final JTextArea simulationChoiceTextArea;
    private final JTextArea algorithChoiceTextArea;
    private final JTextArea statisticsChoiceTextArea;
    private final JTextArea resultsTextArea;
    private final JTextArea helpChoiceTextArea;
    private final JButton addSimulation;
    /**
     * The AlgcomparisonModel class represents a model used in an algorithm comparison application. It contains methods
     * and properties related to the comparison of algorithms.
     */
    AlgcomparisonModel model;
    private JComboBox<Object> independenceTestDropdown;

    /**
     * The constructor for the AlgcomparisonEditor class. The constructor will create a new Comparison object and pass
     * in the PrintStream "localOut" to the constructor. The constructor will then use the Reflection API to populate
     * the lists of simulation methods, algorithms, and statistics, and pass these to the Comparison class through its
     * constructor. The constructor will then create a new JFrame and add the JLists, ParameterPanel, and JTextArea to
     * the JFrame. The constructor will then add an ActionListener to the "Run Comparison" button that will call the
     * runComparison() method in the Comparison class. The constructor will then set the JFrame to be visible.
     */
    public AlgcomparisonEditor(AlgcomparisonModel model) {
        this.model = model;

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // Create a new Comparison object and pass in the PrintStream "localOut" to the constructor
        // Use the Reflection API to populate the lists of simulation methods, algorithms, and statistics
        // Pass these lists
        edu.cmu.tetrad.algcomparison.simulation.Simulation simulation;
        try {
            simulation = getSimulation(RandomForward.class,
                    edu.cmu.tetrad.algcomparison.simulation.BayesNetSimulation.class);
            model.addSimulation(simulation);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        simulationChoiceTextArea = new JTextArea();
        simulationChoiceTextArea.setLineWrap(true);
        simulationChoiceTextArea.setWrapStyleWord(true);

        setSimulationText();

        JPanel simulationChoice = new JPanel();
        simulationChoice.setLayout(new BorderLayout());
        simulationChoice.add(new JScrollPane(simulationChoiceTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        Box simulationSelectionBox = Box.createHorizontalBox();
        simulationSelectionBox.add(Box.createHorizontalGlue());

        addSimulation = new JButton("Add Simulation");
        addAddSimulationListener(model.getParameters(), simulation);

        JButton removeLastSimulation = new JButton("Remove Last Simulation");
        removeLastSimulation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.removeLastSimulation();
                setSimulationText();
            }
        });

        simulationSelectionBox.add(addSimulation);
        simulationSelectionBox.add(removeLastSimulation);
        simulationSelectionBox.add(new JButton("Edit Parameters"));
        simulationSelectionBox.add(Box.createHorizontalGlue());
        simulationChoice.add(simulationSelectionBox, BorderLayout.SOUTH);

        tabbedPane.addTab("Simulations", simulationChoice);

        algorithChoiceTextArea = new JTextArea();
        setAlgorithmText();

        Box algorithSelectionBox = Box.createHorizontalBox();
        algorithSelectionBox.add(Box.createHorizontalGlue());

        JButton addAlgorithm = new JButton("Add Algorithm");
        addAlgorithm.addActionListener(e -> {
            Algorithm algorithm = new Pc();
            model.addAlgorithm(algorithm);
            setAlgorithmText();
        });

        JButton removeLastAlgorithm = new JButton("Remove Last Algorithm");
        removeLastAlgorithm.addActionListener(e -> {
            model.removeLastAlgorithm();
            setAlgorithmText();
        });

        JButton editAlgorithmParameters = new JButton("Edit Parameters");

        algorithSelectionBox.add(addAlgorithm);
        algorithSelectionBox.add(removeLastAlgorithm);
        algorithSelectionBox.add(editAlgorithmParameters);
        algorithSelectionBox.add(Box.createHorizontalGlue());

        JPanel algorithmChoice = new JPanel();
        algorithmChoice.setLayout(new BorderLayout());
        algorithmChoice.add(new JScrollPane(algorithChoiceTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), BorderLayout.CENTER);
        algorithmChoice.add(algorithSelectionBox, BorderLayout.SOUTH);

        tabbedPane.addTab("Algorithms", algorithmChoice);

        statisticsChoiceTextArea = new JTextArea();
        setStatisticsText();

        Box statisticsSelectionBox = Box.createHorizontalBox();
        statisticsSelectionBox.add(Box.createHorizontalGlue());
        statisticsSelectionBox.add(new JButton("Add Statistic(s)"));
        statisticsSelectionBox.add(new JButton("Remove Last Statistic"));
        statisticsSelectionBox.add(Box.createHorizontalGlue());

        JPanel statisticsChoice = new JPanel();
        statisticsChoice.setLayout(new BorderLayout());
        statisticsChoice.add(statisticsChoiceTextArea, BorderLayout.CENTER);
        statisticsChoice.add(statisticsSelectionBox, BorderLayout.SOUTH);

        tabbedPane.addTab("Statistics", statisticsChoice);

        resultsTextArea = new JTextArea();
        setResultsText();
        resultsTextArea.setEditable(false);

        Box resultsSelectionBox = Box.createHorizontalBox();
        resultsSelectionBox.add(Box.createHorizontalGlue());
        resultsSelectionBox.add(new JButton("Run Comparison"));
        resultsSelectionBox.add(Box.createHorizontalGlue());

        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BorderLayout());
        resultsPanel.add(new JScrollPane(resultsTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        resultsPanel.add(resultsSelectionBox, BorderLayout.SOUTH);

        tabbedPane.addTab("Results", resultsPanel);

        JPanel helpChoice = new JPanel();
        helpChoice.setLayout(new BorderLayout());
        helpChoiceTextArea = new JTextArea();
        setHelpText();
        helpChoiceTextArea.setEditable(false);
        helpChoice.add(helpChoiceTextArea, BorderLayout.CENTER);

        Box helpSelectionBox = Box.createHorizontalBox();
        helpSelectionBox.add(Box.createHorizontalGlue());
        helpSelectionBox.add(new JCheckBox("Show Algcomparison XML"));
        helpSelectionBox.add(Box.createHorizontalGlue());

        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BorderLayout());
        helpPanel.add(new JScrollPane(helpChoiceTextArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
        helpPanel.add(helpSelectionBox, BorderLayout.SOUTH);

        tabbedPane.addTab("Help", helpPanel);

        tabbedPane.setPreferredSize(new Dimension(800, 400));

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Create a new JFrame

        // Add the JLists, ParameterPanel, and JTextArea to the JFrame

        // Add an ActionListener to the "Run Comparison" button that will call the runComparison() method in the Comparison class

        // Set the JFrame to be visible


    }

    @NotNull
    private static edu.cmu.tetrad.algcomparison.simulation.Simulation getSimulation(Class<? extends edu.cmu.tetrad.algcomparison.graph.RandomGraph> graphClazz,
                                                                                    Class<? extends edu.cmu.tetrad.algcomparison.simulation.Simulation> simulationClazz)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        RandomGraph randomGraph = graphClazz.getConstructor().newInstance();
        return simulationClazz.getConstructor(RandomGraph.class).newInstance(randomGraph);
    }

    private static void addTestAndScoreDropdowns(Box vert1) {
        IndependenceTestModels independenceTestModels = IndependenceTestModels.getInstance();
        List<IndependenceTestModel> models = independenceTestModels.getModels();

        JComboBox<IndependenceTestModel> indTestComboBox = new JComboBox<>();

        for (IndependenceTestModel model : models) {
            indTestComboBox.addItem(model);
        }

        Box horiz4 = Box.createHorizontalBox();
        horiz4.add(new JLabel("Choose an independence test:"));
        horiz4.add(Box.createHorizontalGlue());
        horiz4.add(indTestComboBox);
        vert1.add(horiz4);

        ScoreModels scoreModels = ScoreModels.getInstance();
        List<ScoreModel> scoreModelsList = scoreModels.getModels();

        JComboBox<ScoreModel> scoreModelComboBox = new JComboBox<>();

        for (ScoreModel model : scoreModelsList) {
            scoreModelComboBox.addItem(model);
        }

        Box horiz5 = Box.createHorizontalBox();
        horiz5.add(new JLabel("Choose a score:"));
        horiz5.add(Box.createHorizontalGlue());
        horiz5.add(scoreModelComboBox);
        vert1.add(horiz5);
    }

    @NotNull
    private static Box getSelectionBox(AlgcomparisonModel model) {
        java.util.List<String> simulations = model.getSimulationName();
        java.util.List<String> algorithms = model.getAlgorithmsName();
        java.util.List<String> statistics = model.getStatisticsNames();

        JList<String> simulationList = new JList<>(simulations.toArray(new String[0]));
        JList<String> algorithmList = new JList<>(algorithms.toArray(new String[0]));
        JList<String> statisticList = new JList<>(statistics.toArray(new String[0]));

        simulationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        algorithmList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        statisticList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        simulationList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { // This condition checks that the user has finished changing the selection.
                    JList source = (JList) e.getSource();
                    String selected = (String) source.getSelectedValue();

                    model.setSelectedSimulation(selected);

                    // Update the algorithm list based on the selected simulation
                    java.util.List<String> algorithms = model.getAlgorithmsName();
                    algorithmList.setListData(algorithms.toArray(new String[0]));
                }
            }
        });

        Box horiz1 = Box.createHorizontalBox();

        Box vert1 = Box.createVerticalBox();
        vert1.add(new JLabel("Simulation:"));
//        vert1.add(new JScrollPane(simulationList));

        Box vert2 = Box.createVerticalBox();
        vert2.add(new JLabel("Algorithms:"));
//        vert2.add(new JScrollPane(algorithmList));

        Box vert3 = Box.createVerticalBox();
        vert3.add(new JLabel("Statistics:"));
        vert3.add(new JScrollPane(statisticList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

        JLabel label = new JLabel("Instructions: Select a simulation, one or more algorithms, and one or more statistics.");
        Box horiz2 = Box.createHorizontalBox();
        horiz2.add(label);
        horiz2.add(Box.createHorizontalGlue());

        Box vert4 = Box.createVerticalBox();
        vert4.add(horiz2);

        horiz1.add(vert1);
        horiz1.add(vert2);
        horiz1.add(vert3);

        vert4.add(horiz1);

        return vert4;
    }


    private void addAddSimulationListener(Parameters parameters, edu.cmu.tetrad.algcomparison.simulation.Simulation simulation) {
        addSimulation.addActionListener(e -> {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());

            Box vert1 = Box.createVerticalBox();
            Box horiz2 = Box.createHorizontalBox();
            horiz2.add(new JLabel("Choose a graph type:"));
            horiz2.add(Box.createHorizontalGlue());
            JComboBox<String> graphsDropdown = new JComboBox<>();

            Arrays.stream(ParameterTab.GRAPH_TYPE_ITEMS).forEach(graphsDropdown::addItem);
            graphsDropdown.setMaximumSize(graphsDropdown.getPreferredSize());
            graphsDropdown.setSelectedItem(parameters.getString("graphsDropdownPreference", ParameterTab.GRAPH_TYPE_ITEMS[0]));
//            graphsDropdown.addActionListener(e -> refreshParameters());

            horiz2.add(graphsDropdown);
            vert1.add(horiz2);
            Box horiz3 = Box.createHorizontalBox();
            horiz3.add(new JLabel("Choose a simulation type:"));
            horiz3.add(Box.createHorizontalGlue());

            JComboBox<String> simulationsDropdown = new JComboBox<>();

//            String[] simulationItems = getSimulationItems();

            Arrays.stream(ParameterTab.MODEL_TYPE_ITEMS).forEach(simulationsDropdown::addItem);
            simulationsDropdown.setMaximumSize(simulationsDropdown.getPreferredSize());
//            simulationsDropdown.setSelectedItem(
//                    simulation.getParams().getString("simulationsDropdownPreference", simulationItems[0]));
//            simulationsDropdown.addActionListener(e -> refreshParameters());

            horiz3.add(simulationsDropdown);
            vert1.add(horiz3);

//            addTestAndScoreDropdowns(vert1);

            panel.add(vert1, BorderLayout.NORTH);

            // Create the JDialog. Use the parent frame to make it modal.
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Add Simulation", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setLayout(new BorderLayout());

            // Your custom JPanel
            // Optionally set the preferred size of yourPanel here
            // yourPanel.setPreferredSize(new Dimension(200, 100));

            // Add your panel to the center of the dialog
            dialog.add(panel, BorderLayout.CENTER);

            // Create a panel for the buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addButton = new JButton("Add");
            JButton cancelButton = new JButton("Cancel");

            // Add action listeners for the buttons
            addButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String graphString = (String) graphsDropdown.getSelectedItem();
                    String simulationString = (String) simulationsDropdown.getSelectedItem();

                    List<String> graphTypeStrings = Arrays.asList(ParameterTab.GRAPH_TYPE_ITEMS);

                    Class<? extends RandomGraph> graphClazz = switch (graphTypeStrings.indexOf(graphString)) {
                        case 0:
                            yield RandomForward.class;
                        case 1:
                            yield ErdosRenyi.class;
                        case 2:
                            yield ScaleFree.class;
                        case 4:
                            yield Cyclic.class;
                        case 5:
                            yield RandomSingleFactorMim.class;
                        case 6:
                            yield RandomTwoFactorMim.class;
                        default:
                            throw new IllegalArgumentException("Unexpected value: " + graphString);
                    };

                    List<String> simulationTypeStrings = Arrays.asList(ParameterTab.MODEL_TYPE_ITEMS);

                    Class<? extends Simulation> simulationClass = switch(simulationTypeStrings.indexOf(simulationString)) {
                        case 0:
                            yield edu.cmu.tetrad.algcomparison.simulation.BayesNetSimulation.class;
                        case 1:
                            yield edu.cmu.tetrad.algcomparison.simulation.SemSimulation.class;
                        case 2:
                            yield edu.cmu.tetrad.algcomparison.simulation.LinearFisherModel.class;
                        case 3:
                            yield edu.cmu.tetrad.algcomparison.simulation.NLSemSimulation.class;
                        case 4:
                            yield edu.cmu.tetrad.algcomparison.simulation.LeeHastieSimulation.class;
                        case 5:
                            yield edu.cmu.tetrad.algcomparison.simulation.ConditionalGaussianSimulation.class;
                        case 6:
                            yield edu.cmu.tetrad.algcomparison.simulation.TimeSeriesSemSimulation.class;
                        default:
                            throw new IllegalArgumentException("Unexpected value: " + simulationString);
                    };

                    try {
                        model.addSimulation(getSimulation(graphClazz, simulationClass));
                        setSimulationText();
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                             IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }

                    dialog.dispose(); // Close the dialog
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Handle the Cancel button click event
                    System.out.println("Cancel button clicked");
                    dialog.dispose(); // Close the dialog
                }
            });

            // Add the buttons to the button panel
            buttonPanel.add(addButton);
            buttonPanel.add(cancelButton);

            // Add the button panel to the bottom of the dialog
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            // Set the dialog size, position, and visibility
            dialog.pack(); // Adjust dialog size to fit its contents
            dialog.setLocationRelativeTo(this); // Center dialog relative to the parent component
            dialog.setVisible(true);
        });
    }

    private void setSimulationText() {
        simulationChoiceTextArea.setText("");

        Simulations selectedSimulations = model.getSelectedSimulations();
        List<Simulation> simulations = selectedSimulations.getSimulations();

        if (simulations.isEmpty()) {
            simulationChoiceTextArea.append("""

                     ** No simulations have been selected. Please select at least one simulation using the Add Simulation button below. **
                    """);
            return;
        } else if (simulations.size() == 1) {
            simulationChoiceTextArea.setText("""
                    The following simulation has been selected. This simulations will be run with the selected algorithms.
                    
                    """);

            Simulation simulation = simulations.get(0);
            Class<? extends RandomGraph> randomGraphClass = simulation.getRandomGraphClass();
            Class<? extends Simulation> simulationClass = simulation.getSimulationClass();
            simulationChoiceTextArea.append("Selected graph type = " + (randomGraphClass == null ? "None" : randomGraphClass.getSimpleName() + "\n"));
            simulationChoiceTextArea.append("Selected simulation type = " + simulationClass.getSimpleName() + "\n");
        } else {
            simulationChoiceTextArea.setText("""
                    The following simulations have been selected. These simulations will be run with the selected algorithms.
                    """);
            for (int i = 0; i < simulations.size(); i++) {
                simulationChoiceTextArea.append("\n");
                simulationChoiceTextArea.append("Simulation #" + (i + 1) + ":\n");
                simulationChoiceTextArea.append("Selected graph type = " + simulations.get(i).getRandomGraphClass().getSimpleName() + "\n");
                simulationChoiceTextArea.append("Selected simulation type = " + simulations.get(i).getSimulationClass().getSimpleName() + "\n");
            }
        }

        simulationChoiceTextArea.append(getSimulationParameterText());
        simulationChoiceTextArea.setCaretPosition(0);
    }

    @NotNull
    private String getSimulationParameterText() {
        Parameters parameters = model.getParameters();

        List<Simulation> simulations = model.getSelectedSimulations().getSimulations();
        Set<String> paramNamesSet = new HashSet<>();

        for (Simulation simulation : simulations) {
            paramNamesSet.addAll(simulation.getParameters());
        }

        List<String> paramNames = new ArrayList<>(paramNamesSet);
        Collections.sort(paramNames);

        StringBuilder paramText;

        if (simulations.size() == 1) {
            paramText = new StringBuilder("\nParameter choices:");
        } else {
            paramText = new StringBuilder("\nParameter choices for all simulations:");
        }

        ParamDescriptions paramDescriptions = ParamDescriptions.getInstance();

        for (String name : paramNames) {
            ParamDescription description = paramDescriptions.get(name);
            paramText.append("\n\n- ").append(name).append(" = ").append(parameters.get(name));
            paramText.append("\n").append(description.getShortDescription());
            paramText.append("--that is, ").append(description.getLongDescription());
        }

        return paramText.toString();
    }

    private void setAlgorithmText() {
        algorithChoiceTextArea.setText("""
                The following algorithms have been selected:
                                
                These algorithms will be run with the selected simulations.
                                
                Algorithm #1 = PC (Peter and Clark)
                                
                Algorithm #2 = FCI (Fast Causal Inference)
                                
                Algorithm #3 = GFCI (Greedy Fast Causal Inference)
                                
                Algorithm #4 = BOSS (Best Order Score Search)
                                
                Parameter choices for all algorithms:
                - Alpha = 0.05
                - Max degree = 3
                - Max path length = 3
                - Depth = 3, 4, 5
                - Seed = 0
                ...
                """);
    }

    private void setStatisticsText() {
        statisticsChoiceTextArea.setText("""
                The following statistics have been selected.
                                
                These statistics will be calculated for the selected simulations and algorithms.
                                
                The comparison table will include these statistics as columns in the table.
                                
                - AP (Average Precision)
                                
                - AR (Average Recall)
                                
                - AHP (Average Hit Precision)
                                
                - AHR (Average Hit Recall)
                """);
    }

    private void setResultsText() {
        resultsTextArea.setText("""
                We have some massive and impressive results for you!
                                
                The results are as follows:
                                
                (Comparison output goes here)
                """);
    }

    private void setHelpText() {
        helpChoiceTextArea.setText("""
                This is some information about how to use the application.
                                
                To run a comparison, select a simulation, one or more algorithms, and one or more statistics.
                Then click the "Run Comparison" button.
                                
                The results will be displayed in the "Results" tab.
                                
                Here is some information on the graph type you have selected:
                                
                RandomForward:
                - This graph type generates a random graph with a forward edge.
                                
                Here is some information on the simulation type you have selected:
                                
                BayesNetSimulation:
                - This simulation type generates a random Bayes net.
                                
                Here is some information on the algorithms you have selected:
                                
                PC:
                - PC is a constraint-based algorithm that searches for the best fitting graph structure.
                                
                FCI
                - FCI is a constraint-based algorithm that searches for the best fitting graph structure.
                              
                """);
    }

    public static class BufferedListeningByteArrayOutputStream extends ByteArrayOutputStream {
        private final StringBuilder buffer = new StringBuilder();

        public static void main(String[] args) {
            BufferedListeningByteArrayOutputStream baos = new BufferedListeningByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);

            // Example usage
            ps.println("Hello, world!");
            ps.printf("Pi is approximately %.2f%n", Math.PI);

            ps.close();
        }

        @Override
        public void write(int b) {
            super.write(b);
            // Convert single byte to character and add to buffer
            char c = (char) b;
            buffer.append(c);
            if (c == '\n') {
                processBuffer();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            super.write(b, off, len);
            // Convert bytes to string and add to buffer
            String s = new String(b, off, len, StandardCharsets.UTF_8);
            buffer.append(s);
            // Process buffer if newline character is found
            if (s.contains("\n")) {
                processBuffer();
            }
        }

        private void processBuffer() {
            // Process the buffered data (print it in this case)
            System.out.print("Buffered data: " + buffer.toString());
            buffer.setLength(0); // Clear the buffer for next data
        }
    }

}
