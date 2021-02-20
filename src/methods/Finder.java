package methods;

import com.microsoft.z3.Model;
import constant.Status;
import smt.SMTSolver;
import syntax.Graph;
import syntax.Pattern;
import syntax.Program;

import javax.swing.*;
import java.util.LinkedList;

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
    LinkedList<Pattern> patterns;

    public Finder(Program program){
        this.program = program;
        this.graph = new Graph(program);
        this.graph.printGraph();

        Johnson johnson = new Johnson(graph);
        patterns = johnson.getPatterns();
        System.out.println("patterns's number:"+patterns.size());
        AbstractMachine abstractMachine;
        for(Pattern pattern : patterns){
            pattern.printPattern();
            abstractMachine = new AbstractMachine(program, pattern);
            if(abstractMachine.execute()== Status.REACHABLE){
                System.out.println("ABSTRACT MACHINE CHECK THIS CYCLE IS DEADLOCK CANDIDATE !");
                SMTSolver solver = new SMTSolver(program, pattern);
                solver.encode();
                Model model = solver.check();
                if(model!=null){
                    System.out.println("[SAT]:Deadlock detected for ");
                }else{
                    System.out.println("[UNSAT]:No deadlock is found for pattern:");
                    pattern.DeadlockCandidate = false;
                }
            }
        }

    }

    public static void main(String[] args){
        Program program = new Program("./src/test/fixtures/1.txt");
        Finder finder = new Finder(program);

    }
}
