package edu.cmu.tetrad.algcomparison.algorithm.external;

import edu.cmu.tetrad.algcomparison.algorithm.ExternalAlgorithm;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.LayoutUtil;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.data.reader.Delimiter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

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
public class ExternalAlgorithmPcalgPc extends ExternalAlgorithm {
    static final long serialVersionUID = 23L;
    private final String extDir;
    private final String shortDescription;

    public ExternalAlgorithmPcalgPc(String extDir) {
        this.extDir = extDir;
        this.shortDescription = new File(extDir).getName().replace("_", " ");
    }

    public ExternalAlgorithmPcalgPc(String extDir, String shortDecription) {
        this.extDir = extDir;
        this.shortDescription = shortDecription;
    }

    public static Graph loadGraphPcAlgMatrix(DataSet dataSet) {
        List<Node> vars = dataSet.getVariables();

        Graph graph = new EdgeListGraph(vars);

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            for (int j = 0; j < dataSet.getNumColumns(); j++) {
                if (i == j) continue;
                int g = dataSet.getInt(i, j);
                int h = dataSet.getInt(j, i);


                if (g == 1 && h == 1 && !graph.isAdjacentTo(vars.get(i), vars.get(j))) {
                    graph.addUndirectedEdge(vars.get(i), vars.get(j)); //
                } else if (g == 1 && h == 0) {
                    graph.addDirectedEdge(vars.get(j), vars.get(i));
                }
            }
        }

        return graph;
    }

    /**
     * Reads in the relevant graph from the file (see above) and returns it.
     */
    public Graph search(DataModel dataSet, Parameters parameters) {
        int index = getIndex(dataSet);

        File file = new File(this.path, "/results/" + this.extDir + "/" + (this.simIndex + 1) + "/graph." + index + ".txt");

        System.out.println(file.getAbsolutePath());

        try {
            DataSet dataSet2 = SimpleDataLoader.loadContinuousData(file, "//", '\"',
                    "*", true, Delimiter.TAB);
            System.out.println("Loading graph from " + file.getAbsolutePath());
            Graph graph = ExternalAlgorithmPcalgPc.loadGraphPcAlgMatrix(dataSet2);

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

        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String l = r.readLine(); // Skip the first line.
            return Long.parseLong(l);
        } catch (IOException e) {
            return -99;
        }
    }
}


