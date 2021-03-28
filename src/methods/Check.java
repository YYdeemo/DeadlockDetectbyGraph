package methods;

import com.microsoft.z3.Model;
import constant.Status;
import smt.SMTSolver;
import syntax.Operation;
import syntax.Pattern;
import syntax.Program;

import java.util.Arrays;
import java.util.Hashtable;

public class Check {

    public static Status checkPattern(Program program, Pattern pattern) {
        pattern.printPattern();
//        System.out.println("begin the abstract machine :");
        AbstractMachine abstractMachine = new AbstractMachine(program, pattern);
        if (abstractMachine.execute() == Status.REACHABLE) {
            System.out.println("tracker：" + Arrays.toString(pattern.tracker));
//                System.out.println("[ABSTRACT MACHINE] GOOD! CHECK THIS CYCLE IS DEADLOCK CANDIDATE!\n");
            SMTSolver solver = new SMTSolver(program, pattern);
            solver.encode();
            Model model = solver.check();
            if (model != null) {
                System.out.println("[FINDER]: SAT! Deadlock detected for\n");
//                pattern.printPattern();
                pattern.DeadlockCandidate = true;
                return Status.SATISFIABLE;
            } else {
                System.out.println("[FINDER]: UNSAT! No deadlock is found for pattern:");
//                pattern.printPattern();
                pattern.DeadlockCandidate = false;
                return Status.UNSATISFIABLE;
            }
        }else{
//            System.out.println("[ABSTRACT MACHINE]: SORRY! CANNOT REACH THE CONTROL POINT!");
        }
        return Status.UNREACHABLE;
    }
}
