package edu.cmu.tetrad.algcomparison.algorithm.external;

import edu.cmu.tetrad.algcomparison.algorithm.ExternalAlgorithm;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * An API to allow results from external algorithms to be included in a report through the algrorithm comparison tool.
 * This one is for matrix generated by PC in pcalg. See below. This script can generate the files in R.
 * <p>
 * library("MASS"); library("pcalg");
 * <p>
 * path&lt;-"/Users/user/tetrad/comparison-final"; simulation&lt;-1;
 * <p>
 * subdir&lt;-"pc.solve.confl.TRUE"; dir.create(paste(path, "/save/", simulation, "/", subdir, sep=""));
 * <p>
 * for (i in 1:10) { data&lt;-read.table(paste(path, "/save/", simulation, "/data/data.", i, ".txt", sep=""),
 * header=TRUE) n&lt;-nrow(data) C&lt;-cor(data) v&lt;-names(data) suffStat&lt;-list(C = C, n=n)
 * pc.fit&lt;-pc(suffStat=suffStat, indepTest=gaussCItest, alpha=0.001, labels=v, solve.conf=TRUE) A&lt;-as(pc.fit,
 * "amat") name&lt;-paste(path, "/save/", simulation, "/", subdir, "/graph.", i, ".txt", sep="") print(name)
 * write.matrix(A, file=name, sep="\t") }
 *
 * @author josephramsey
 */
public class ExternalAlgorithmBnlearnMmhc extends ExternalAlgorithm {
    private static final long serialVersionUID = 23L;
    private final String extDir;
    private final String shortDescription;

    public ExternalAlgorithmBnlearnMmhc(String extDir) {
        this.extDir = extDir;
        this.shortDescription = new File(extDir).getName().replace("_", " ");
    }

    public ExternalAlgorithmBnlearnMmhc(String extDir, String shortDecription) {
        this.extDir = extDir;
        this.shortDescription = shortDecription;
    }

    /**
     * Reads in the relevant graph from the file (see above) and returns it.
     */
    public Graph search(DataModel dataSet, Parameters parameters) {
        int index = getIndex(dataSet);

        File file = new File(this.path, "/results/" + this.extDir + "/" + (this.simIndex + 1) + "/graph." + index + ".txt");

        System.out.println(file.getAbsolutePath());

        try {
            BufferedReader r = new BufferedReader(new FileReader(file));

            r.readLine(); // Skip the first line.
            String line;

            Graph graph = new EdgeListGraph();

            while ((line = r.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\t");
                String name1 = tokens[0].replace(" ", "").replace("\"", "");
                String name2 = tokens[1].replace(" ", "").replace("\"", "");

                if (graph.getNode(name1) == null) {
                    graph.addNode(new GraphNode(name1));
                }

                if (graph.getNode(name2) == null) {
                    graph.addNode(new GraphNode(name2));
                }

                Node node1 = graph.getNode(name1);
                Node node2 = graph.getNode(name2);

                if (!graph.isAdjacentTo(node1, node2)) {
                    graph.addDirectedEdge(node1, node2);
                } else {
                    graph.removeEdge(node1, node2);
                    graph.addUndirectedEdge(node1, node2);
                }
            }

            LayoutUtil.circleLayout(graph);

            return graph;
        } catch (IOException e) {
            throw new RuntimeException("Couldn't parse graph.");
        }
    }

    /**
     * Returns the CPDAG of the supplied DAG.
     */
    public Graph getComparisonGraph(Graph graph) {
        return new EdgeListGraph(graph);
//        return SearchGraphUtils.cpdagForDag(new EdgeListGraph(graph));
    }

    public String getDescription() {
        if (this.shortDescription == null) {
            return "Load data from " + this.path + "/" + this.extDir;
        } else {
            return this.shortDescription;
        }
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public long getElapsedTime(DataModel dataSet, Parameters parameters) {
        int index = getIndex(dataSet);

        File file = new File(this.path, "/elapsed/" + this.extDir + "/" + (this.simIndex + 1) + "/graph." + index + ".txt");

//        System.out.println(file.getAbsolutePath());

        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String l = r.readLine(); // Skip the first line.
            return Long.parseLong(l);
        } catch (IOException e) {
            return -99;
        }
    }

}
