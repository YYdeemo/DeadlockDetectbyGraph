package methods;

import syntax.Graph;
import syntax.Program;

    /**
     * there is a MPI program, so the steps are as following:
     * 1.init the program
     * 2.init the graph
     * 3.we should use the Johnson to find the cycles, where need the TSC to generate the subGraph
     * 4.if the pattern is found, firstly the pattern should be checked by Abstract Machine
     * 5.if the Abstract Machine returns TRUE, then the program and pattern should be checked by the SMT solver
     * 6.if the SMT solver return TRUE, so it is real DEADLOCK;
     *
     */

public class Finder {
    public Graph graph;
    public Program program;

    public Finder(Program program){
        this.program = program;
        this.graph = new Graph(program);

        Johnson johnson = new Johnson(graph);
    }
}