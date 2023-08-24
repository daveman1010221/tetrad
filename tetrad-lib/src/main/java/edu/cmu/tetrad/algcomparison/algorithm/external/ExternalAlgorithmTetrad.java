package edu.cmu.tetrad.algcomparison.algorithm.external;

import edu.cmu.tetrad.algcomparison.algorithm.ExternalAlgorithm;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphSaveLoadUtils;
import edu.cmu.tetrad.graph.LayoutUtil;
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
public class ExternalAlgorithmTetrad extends ExternalAlgorithm {
    static final long serialVersionUID = 23L;
    private final String extDir;
    private final String shortDescription;


    public ExternalAlgorithmTetrad(String extDir) {
        this.extDir = extDir;
        this.shortDescription = new File(extDir).getName().replace("_", " ");
    }

    public ExternalAlgorithmTetrad(String extDir, String shortDecription) {
        this.extDir = extDir;
        this.shortDescription = shortDecription;
    }

    /**
     * Reads in the relevant graph from the file and returns it.
     */
    public Graph search(DataModel dataSet, Parameters parameters) {
        int index = getIndex(dataSet);
        File file = new File(this.path, "/results/" + this.extDir + "/" + (this.simIndex + 1) + "/graph." + index + ".txt");
        System.out.println(file.getAbsolutePath());
        Graph graph = GraphSaveLoadUtils.loadGraphTxt(file);
        LayoutUtil.circleLayout(graph);
        return graph;
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

    public DataType getDataType() {
        return DataType.Continuous;
    }

    public long getElapsedTime(DataModel dataSet, Parameters parameters) {
        int index = getIndex(dataSet);

        File file = new File(this.path, "/elapsed/" + this.extDir + "/" + (this.simIndex + 1) + "/graph." + index + ".txt");

        System.out.println(file.getAbsolutePath());

        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            String l = r.readLine(); // Skip the first line.
            return Long.parseLong(l);
        } catch (IOException e) {
            throw new IllegalArgumentException();
        }
    }
}
