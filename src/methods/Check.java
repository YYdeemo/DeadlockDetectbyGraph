package methods;

import com.microsoft.z3.Model;
import constant.Status;
import smt.SMTSolver;
import syntax.*;
import syntax.Process;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;

public class Check {

    public static Status checkPattern(Program program, Pattern pattern) {
        pattern.printPattern();
//        System.out.println("begin the abstract machine :");
        AbstractMachine abstractMachine = new AbstractMachine(program, pattern);
        if (abstractMachine.execute() == Status.REACHABLE) {
            System.out.println("tracker：" + Arrays.toString(pattern.tracker));
            System.out.println("[ABSTRACT MACHINE] GOOD! CHECK THIS CYCLE IS DEADLOCK CANDIDATE!\n");
            SMTSolver solver = new SMTSolver(program, pattern);
            solver.printActs();
            solver.encode();
            Model model = solver.check();
//            solver.displayExprs();
            if (model != null) {
//                System.out.println(" SAT model is :"+model);
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
            System.out.println("[ABSTRACT MACHINE]: SORRY! CANNOT REACH THE CONTROL POINT!");
        }
        return Status.UNREACHABLE;
    }

    public static Status checkPattern(Program program, Pattern pattern, boolean isnewprogram) {
        pattern.printPattern();
//        System.out.println("begin the abstract machine :");
        AbstractMachine abstractMachine = new AbstractMachine(program, pattern);
        if (abstractMachine.execute() == Status.REACHABLE) {
            LinkedList<Operation> acts = getActs((NewProgram) program,pattern.tracker);
            System.out.println(acts);
            System.out.println("tracker：" + Arrays.toString(pattern.tracker));
//                System.out.println("[ABSTRACT MACHINE] GOOD! CHECK THIS CYCLE IS DEADLOCK CANDIDATE!\n");
            SMTSolver solver = new SMTSolver(program, pattern);
            solver.encode();
            Model model = solver.check();
//            solver.displayExprs();
            if (model != null) {
//                System.out.println(" SAT model is :"+model);
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

    static LinkedList<Operation> getActs(NewProgram program, int[] tracker){
//        Hashtable<Operation, CsecOperation> newCsecTable = new Hashtable<>();
        LinkedList<Operation> acts = new LinkedList<>();
        for (int i = 0; i < program.getSize(); i++) {
            LinkedList<Operation> csecOperationList = new LinkedList<>();
            int rank = i;
            for (Operation operation : program.get(i).ops) {
                if (operation.rank >= tracker[i]) break;
                if (operation.rank == tracker[i]-1){
                    acts.add(operation);
                    break;
                }
                if (operation.isWait()) {
                    continue;
                }
                if (operation.isBarrier()){
                    acts.add(operation);
                    continue;
                }

                if (csecOperationList.isEmpty()) {
                    csecOperationList.add(operation);
                    continue;
                }
                Operation lastOperation = csecOperationList.getLast();
                if (operation.type == lastOperation.type
                        && operation.src == lastOperation.src
                        && operation.dst == lastOperation.dst) {
                    csecOperationList.add(operation);
                } else {
                    if (csecOperationList.size() <= 1) {
                        //add the non-cescOp to process
                        acts.add(csecOperationList.getFirst());
                        acts.add(csecOperationList.getFirst().Nearstwait);
                    } else {
                        CsecOperation csecOp = creatCsecOperation(csecOperationList);
                        acts.add(csecOp);
                        acts.add(csecOp.Nearstwait);
                    }
                    csecOperationList.clear();
                    csecOperationList.add(operation);
                }
            }
            if (csecOperationList.size() == 1) {
                //add the non-cescOp to process
                acts.add(csecOperationList.getFirst());
                acts.add(csecOperationList.getFirst().Nearstwait);
            } else if (csecOperationList.size() >= 2) {
                CsecOperation csecOp = creatCsecOperation(csecOperationList);
                acts.add(csecOp);
                acts.add(csecOp.Nearstwait);
            }
            csecOperationList.clear();
        }
        return acts;
    }

    static CsecOperation creatCsecOperation(LinkedList<Operation> operations) {
        Operation wait = null;
        Operation firstOperation = operations.getFirst();
        CsecOperation csecOperation = new CsecOperation(firstOperation.type,
                firstOperation.index,
                firstOperation.proc,
                firstOperation.src,
                firstOperation.dst,
                firstOperation.tag,
                firstOperation.group,
                firstOperation.reqID);
        csecOperation.OperationList = (LinkedList<Operation>) operations.clone();
        Collections.sort(csecOperation.OperationList);
        for (Operation operation : operations) {
//            csecOpsTables.put(operation, csecOperation);
            if (operation.Nearstwait != null) wait = operation.Nearstwait;
        }
        csecOperation.Nearstwait = wait;
        wait.req = csecOperation;
        csecOperation.rank = firstOperation.rank;
        csecOperation.indx = firstOperation.indx;
        return csecOperation;
    }


}
