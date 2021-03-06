package methods;

import com.microsoft.z3.Model;
import constant.Status;
import smt.SMTSolver;
import syntax.Graph;
import syntax.Pattern;
import syntax.Program;

import javax.swing.*;
import java.io.File;
import java.util.LinkedList;
import java.util.logging.FileHandler;


public class Finder {
    public Graph graph;
    public Program program;
    LinkedList<Pattern> patterns;

    public Finder(Program program){
        this.program = program;
        this.graph = new Graph(program);
//        graph.printGraphETable();
        Johnson johnson = new Johnson(graph);
        patterns = johnson.getPatterns();

        System.out.println("[FINDER]: PATTERNS NUMBER IS : "+patterns.size());
        if(patterns.size()==0) System.out.println("[FINDER]: THERE IS NO PATTEREN!");
        AbstractMachine abstractMachine;
        for(Pattern pattern : patterns){
            pattern.printPattern();
            abstractMachine = new AbstractMachine(program, pattern);
            if(abstractMachine.execute()== Status.REACHABLE){
                System.out.println("[ABSTRACT MACHINE] YES! CHECK THIS CYCLE IS DEADLOCK CANDIDATE!");
                SMTSolver solver = new SMTSolver(program, pattern);
                solver.encode();
                Model model = solver.check();
                if(model!=null){
                    System.out.println("[FINDER]: SAT! THE DEADLOCK CANDIDATE IS REAL Deadlock");
                }else{
                    System.out.println("[FINDER]: UNSAT! No deadlock is found for pattern:");
                    pattern.DeadlockCandidate = false;
                }
            }else{
                System.out.println("CANNOT! ABSTRACT MACHINE CANNOT REACH THE CONTROL POINTS!");
            }
        }
        System.out.println("FINISH FIND !");
    }

    public static void main(String[] args){
//        String directoryName = "./src/test/fixtures";
//        File Dire = new File(directoryName);
//        for(File file : Dire.listFiles()){
//            if(!file.isDirectory()){
//                System.out.println("-----------------------"+file.getName()+"----------------------");
//                Program program = new Program(file.getPath());
//                Finder finder = new Finder(program);
//            }
//        }

        Program program = new Program("./src/test/fixtures/2.txt");
        Finder finder = new Finder(program);

    }
}
