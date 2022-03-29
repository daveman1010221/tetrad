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

import edu.cmu.tetrad.algcomparison.Comparison;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithms;
import edu.cmu.tetrad.algcomparison.algorithm.multi.FaskConcatenated;
import edu.cmu.tetrad.algcomparison.independence.FisherZ;
import edu.cmu.tetrad.algcomparison.score.SemBicScore;
import edu.cmu.tetrad.algcomparison.simulation.Simulations;
import edu.cmu.tetrad.algcomparison.statistic.*;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.Fask;
import edu.cmu.tetrad.search.IndTestFisherZ;
import edu.cmu.tetrad.sem.GeneralizedSemIm;
import edu.cmu.tetrad.sem.GeneralizedSemPm;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.text.ParseException;

/**
 * Pulling this test out for Madelyn.
 *
 * @author jdramsey
 */
public class TestSimulatedFmri {

    public static void main(final String... args) {
        new TestSimulatedFmri().task(false);
    }

    private void task(final boolean testing) {
        final Parameters parameters = new Parameters();
        parameters.set(Params.PENALTY_DISCOUNT, 4);
        parameters.set(Params.DEPTH, -1);
        parameters.set(Params.TWO_CYCLE_ALPHA, 1e-10);
        parameters.set("reverseOrientationsBySignOfCorrelation", false);
        parameters.set("reverseOrientationsBySkewnessOfVariables", false);

        parameters.set(Params.NUM_RUNS, 10);
        parameters.set(Params.RANDOM_SELECTION_SIZE, 10);

        parameters.set("Structure", "Placeholder");

        final Statistics statistics = new Statistics();

        statistics.add(new ParameterColumn("Structure"));
        statistics.add(new AdjacencyPrecision());
        statistics.add(new AdjacencyRecall());
//        statistics.add(new MathewsCorrAdj());
        statistics.add(new ArrowheadPrecision());
        statistics.add(new ArrowheadRecall());
        statistics.add(new TwoCyclePrecision());
        statistics.add(new TwoCycleRecall());
        statistics.add(new TwoCycleFalsePositive());
        statistics.add(new TwoCycleFalseNegative());
        statistics.add(new TwoCycleTruePositive());
        statistics.add(new ElapsedTime());
        statistics.setWeight("AHR", 1.0);
        statistics.setWeight("2CP", 1.0);
        statistics.setWeight("2CR", 1.0);
        statistics.setWeight("2CFP", 1.0);

        final Simulations simulations = new Simulations();

        if (!testing) {
            final String dir = "/Users/user/Downloads/Cycles_Data_fMRI/";
            final String subdir = "data_fslfilter";

            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network1_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network2_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network3_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network4_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_contr", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_contr_p2n6", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_contr_p6n2", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network6_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network6_contr", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network7_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network7_contr", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_amp_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_amp_contr", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_contr_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_amp_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_amp_contr", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_contr_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Diamond", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Markov_Complex_1", subdir));
        } else {

            final String dir = "/Users/user/Downloads/CyclesTestingData/";
            final String subdir = "data_fslfilter";

            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network1_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network2_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network3_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network4_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_cont", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_cont_p3n7", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network5_cont_p7n3", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network6_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network6_cont", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network7_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network7_cont", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_amp_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_amp_cont", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network8_cont_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_amp_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_amp_cont", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Network9_cont_amp", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Diamond", subdir));
            simulations.add(new LoadContinuousDataAndSingleGraph(
                    dir + "Markov_Complex_1", subdir));
        }

        final Algorithms algorithms = new Algorithms();

        algorithms.add(new FaskConcatenated(new SemBicScore(), new FisherZ()));
//        algorithms.add(new FaskGfciConcatenated(new SemBicTest()));

//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.RSkew));

        final Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(false);
        comparison.setShowUtilities(false);
        comparison.setSaveGraphs(false);
        comparison.setTabDelimitedTables(false);
        comparison.setSaveGraphs(true);

        final String directory;

        if (!testing) {
            directory = "comparison_training";
        } else {
            directory = "comparison_testing";
        }

        comparison.compareFromSimulations(directory, simulations, algorithms, statistics, parameters);
    }

    //    @Test
    public void task2() {
        final Parameters parameters = new Parameters();
        parameters.set(Params.PENALTY_DISCOUNT, 1);
        parameters.set(Params.DEPTH, -1);
        parameters.set(Params.TWO_CYCLE_ALPHA, 0);

        parameters.set(Params.NUM_RUNS, 10);
        parameters.set(Params.RANDOM_SELECTION_SIZE, 2);

        parameters.set("Structure", "Placeholder");

        final Statistics statistics = new Statistics();

        statistics.add(new ParameterColumn("Structure"));
        statistics.add(new AdjacencyPrecision());
        statistics.add(new AdjacencyRecall());
//        statistics.add(new MathewsCorrAdj());
        statistics.add(new ArrowheadPrecision());
        statistics.add(new ArrowheadRecall());
        statistics.add(new TwoCyclePrecision());
        statistics.add(new TwoCycleRecall());
        statistics.add(new TwoCycleFalsePositive());
        statistics.add(new TwoCycleFalseNegative());
        statistics.add(new TwoCycleTruePositive());
        statistics.add(new ElapsedTime());
        statistics.setWeight("AHR", 1.0);
        statistics.setWeight("2CP", 1.0);
        statistics.setWeight("2CR", 1.0);
        statistics.setWeight("2CFP", 1.0);

        final Simulations simulations = new Simulations();

        final Algorithms algorithms = new Algorithms();

        for (int i = 1; i <= 28; i++) {
//            if (i == 21) continue;
            simulations.add(new LoadContinuousDataSmithSim("/Users/user/Downloads/smithsim/", i));
//            simulations.add(new LoadContinuousDataPwdd7("/Users/user/Downloads/pwdd7/", i, "50_BOLDdemefilt1"));
//            simulations.add(new LoadContinuousDataPwdd7("/Users/user/Downloads/pwdd7/", i, "50_BOLDnoise"));
        }

//        algorithms.add(new LofsConcatenated(Lofs2.Rule.FASKLR));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.R1));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.R3));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.RSkew));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.RSkewE));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.Skew));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.SkewE));
//        algorithms.add(new LofsConcatenated(Lofs2.Rule.Patel));

        algorithms.add(new FaskConcatenated(
                new SemBicScore(),
                new FisherZ() {
                }));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.R1));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.R3));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.RSkew));
//        algorithms.add(new FasLofsConfcatenated(Lofs2.Rule.RSkewE));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.Skew));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.SkewE));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.Patel));

        final Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(false);
        comparison.setShowUtilities(false);
        comparison.setSaveGraphs(false);
        comparison.setTabDelimitedTables(false);
        comparison.setSaveGraphs(true);

        final String directory = "smithsim";

        comparison.compareFromSimulations(directory, simulations, algorithms, statistics, parameters);
    }

    //    @Test
    public void testTough() {
        final Parameters parameters = new Parameters();

        parameters.set(Params.PENALTY_DISCOUNT, 2);
        parameters.set(Params.DEPTH, 5);
        parameters.set(Params.TWO_CYCLE_ALPHA, .01);

        parameters.set(Params.NUM_RUNS, 1);
        parameters.set(Params.RANDOM_SELECTION_SIZE, 10);

        parameters.set("Structure", "Placeholder");

        final Statistics statistics = new Statistics();

        statistics.add(new ParameterColumn("Structure"));
        statistics.add(new AdjacencyPrecision());
        statistics.add(new AdjacencyRecall());
        statistics.add(new MathewsCorrAdj());
        statistics.add(new ArrowheadPrecision());
        statistics.add(new ArrowheadRecall());
        statistics.add(new TwoCyclePrecision());
        statistics.add(new TwoCycleRecall());
        statistics.add(new TwoCycleFalsePositive());
        statistics.add(new TwoCycleFalseNegative());
        statistics.add(new TwoCycleTruePositive());
        statistics.add(new ElapsedTime());

        statistics.setWeight("AP", 1.0);
        statistics.setWeight("AR", 1.0);
        statistics.setWeight("AHP", 1.0);
        statistics.setWeight("AHR", 1.0);
        statistics.setWeight("2CP", 1.0);
        statistics.setWeight("2CR", 1.0);
        statistics.setWeight("2CFP", 1.0);

        final Simulations simulations = new Simulations();

        final String dir = "/Users/jdramsey/Downloads/";
        final String subdir = "data_fslfilter";

        simulations.add(new LoadContinuousDataAndSingleGraph(
                dir + "Markov_dist_thresh36", subdir));

        final Algorithms algorithms = new Algorithms();

//        algorithms.add(new FasLofs(Lofs2.Rule.R1));
//        algorithms.add(new FasLofs(Lofs2.Rule.R2));
//        algorithms.add(new FasLofs(Lofs2.Rule.R3));
//        algorithms.add(new FasLofs(Lofs2.Rule.Patel));
//        algorithms.add(new FasLofs(Lofs2.Rule.Skew));
//        algorithms.add(new FasLofs(Lofs2.Rule.RSkew));
//
//        algorithms.add(new FgesConcatenated(new edu.cmu.tetrad.algcomparison.score.SemBicScore(), true));
//        algorithms.add(new PcStableMaxConcatenated(new SemBicTest(), true));
        algorithms.add(new FaskConcatenated(
                new SemBicScore(),
                new FisherZ()));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.R1));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.R2));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.R3));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.Patel));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.Skew));
//        algorithms.add(new FasLofsConcatenated(Lofs2.Rule.RSkew));

        final Comparison comparison = new Comparison();

        comparison.setShowAlgorithmIndices(true);
        comparison.setShowSimulationIndices(true);
        comparison.setSortByUtility(false);
        comparison.setShowUtilities(false);
        comparison.setSaveGraphs(false);
        comparison.setTabDelimitedTables(false);

        comparison.compareFromSimulations("comparison", simulations, algorithms, statistics, parameters);
    }

    //    @Test
    public void testClark() {

        final double f = .1;
        final int N = 512;
        final double alpha = 1.0;
        final double penaltyDiscount = 1.0;

        for (int i = 0; i < 100; i++) {
            {
                final Node x = new ContinuousVariable("X");
                final Node y = new ContinuousVariable("Y");
                final Node z = new ContinuousVariable("Z");

                final Graph g = new EdgeListGraph();
                g.addNode(x);
                g.addNode(y);
                g.addNode(z);

                g.addDirectedEdge(x, y);
                g.addDirectedEdge(z, x);
                g.addDirectedEdge(z, y);

                final GeneralizedSemPm pm = new GeneralizedSemPm(g);

                try {
                    pm.setNodeExpression(g.getNode("X"), "0.5 * Z + E_X");
                    pm.setNodeExpression(g.getNode("Y"), "0.5 * X + 0.5 * Z + E_Y");
                    pm.setNodeExpression(g.getNode("Z"), "E_Z");

                    final String error = "pow(Uniform(0, 1), " + f + ")";
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("X")), error);
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("Y")), error);
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("Z")), error);
                } catch (final ParseException e) {
                    System.out.println(e);
                }

                final GeneralizedSemIm im = new GeneralizedSemIm(pm);
                final DataSet data = im.simulateData(N, false);


                final Fask fask = new Fask(data,
                        new edu.cmu.tetrad.search.SemBicScore(data),
                        new IndTestFisherZ(data, 0.001));
                final Graph out = fask.search();

                System.out.println(out);
            }

            {
                final Node x = new ContinuousVariable("X");
                final Node y = new ContinuousVariable("Y");
                final Node z = new ContinuousVariable("Z");

                final Graph g = new EdgeListGraph();
                g.addNode(x);
                g.addNode(y);
                g.addNode(z);

                g.addDirectedEdge(x, y);
                g.addDirectedEdge(x, z);
                g.addDirectedEdge(y, z);

                final GeneralizedSemPm pm = new GeneralizedSemPm(g);

                try {
                    pm.setNodeExpression(g.getNode("X"), "E_X");
                    pm.setNodeExpression(g.getNode("Y"), "0.4 * X + E_Y");
                    pm.setNodeExpression(g.getNode("Z"), "0.4 * X + 0.4 * Y + E_Z");

                    final String error = "pow(Uniform(0, 1), " + f + ")";
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("X")), error);
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("Y")), error);
                    pm.setNodeExpression(pm.getErrorNode(g.getNode("Z")), error);
                } catch (final ParseException e) {
                    System.out.println(e);
                }

                final GeneralizedSemIm im = new GeneralizedSemIm(pm);
                final DataSet data = im.simulateData(N, false);

                final Fask fask = new Fask(data,
                        new edu.cmu.tetrad.search.SemBicScore(data),
                        new IndTestFisherZ(data, 0.001));
                final Graph out = fask.search();

                System.out.println(out);

            }
        }
    }

    //    @Test
    public void testClark2() {

        final Node x = new ContinuousVariable("X");
        final Node y = new ContinuousVariable("Y");
        final Node z = new ContinuousVariable("Z");

        final Graph g = new EdgeListGraph();
        g.addNode(x);
        g.addNode(y);
        g.addNode(z);

        g.addDirectedEdge(x, y);
        g.addDirectedEdge(x, z);
        g.addDirectedEdge(y, z);

        final GeneralizedSemPm pm = new GeneralizedSemPm(g);

        try {
            pm.setNodeExpression(g.getNode("X"), "E_X");
            pm.setNodeExpression(g.getNode("Y"), "0.4 * X + E_Y");
            pm.setNodeExpression(g.getNode("Z"), "0.4 * X + 0.4 * Y + E_Z");

            final String error = "pow(Uniform(0, 1), 1.5)";
            pm.setNodeExpression(pm.getErrorNode(g.getNode("X")), error);
            pm.setNodeExpression(pm.getErrorNode(g.getNode("Y")), error);
            pm.setNodeExpression(pm.getErrorNode(g.getNode("Z")), error);
        } catch (final ParseException e) {
            System.out.println(e);
        }


        final GeneralizedSemIm im = new GeneralizedSemIm(pm);
        final DataSet data = im.simulateData(1000, false);

        final Fask fask = new Fask(data,
                new edu.cmu.tetrad.search.SemBicScore(data),
                new IndTestFisherZ(data, 0.001));
        final Graph out = fask.search();

        System.out.println(out);
    }
}




