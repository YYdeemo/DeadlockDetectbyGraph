package syntax;

import java.util.*;

public class NewProgram extends Program{
    public Hashtable<Operation, CsecOperation> csecOpsTables;

    public Program program;

    public NewProgram(Program program) {
        this.program = program;

        csecOpsTables = new Hashtable<>();

        refactorProgram();
        refactorMatchOrder();
        refactorMatchPair();
        refactorQS();
    }

    /**
     * this method has a problem about wait ***
     */
    void refactorProgram() {
        for (Process process : program.processes) {
            LinkedList<Operation> csecOperationList = new LinkedList<>();
            Process process1 = new Process(process.rank);
            processes.add(process1);
            int rank = process.rank;
            for (Operation operation : process.ops) {
                if (operation.isWait() || operation.isBarrier()) {
                    if (csecOperationList.contains(operation.req)) continue;
                    processes.get(rank).append(operation);
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
                        Operation operation1 = csecOperationList.pop();
                        processes.get(rank).append(operation1);
                        processes.get(rank).append(operation1.Nearstwait);
                    } else {
                        Operation csecOp = creatCsecOperation(csecOperationList);
                        processes.get(rank).append(csecOp);
                        processes.get(rank).append(csecOp.Nearstwait);
                    }
                    csecOperationList.clear();
                    csecOperationList.add(operation);
                }
            }
            if (csecOperationList.size() == 1) {
                //add the non-cescOp to process
                Operation operation1 = csecOperationList.pop();
                processes.get(rank).append(operation1);
                processes.get(rank).append(operation1.Nearstwait);
            } else if (csecOperationList.size() >= 2) {
                Operation csecOp = creatCsecOperation(csecOperationList);
                processes.get(rank).append(csecOp);
                processes.get(rank).append(csecOp.Nearstwait);
            }
            csecOperationList.clear();
        }
    }

    Operation creatCsecOperation(LinkedList<Operation> operations) {
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
        csecOperation.csecOperationList = (LinkedList<Operation>) operations.clone();
        for (Operation operation : operations) {
            csecOpsTables.put(operation, csecOperation);
            if (operation.Nearstwait != null) wait = operation.Nearstwait;
        }
        csecOperation.Nearstwait = wait;
        wait.req = csecOperation;
        csecOperation.rank = firstOperation.rank;
        csecOperation.indx = firstOperation.indx;
        return csecOperation;
    }

    void refactorMatchOrder() {
        for (Operation op1 : program.matchOrderTables.keySet()) {
            Operation nop1;
            if (csecOpsTables.keySet().contains(op1)) nop1 = csecOpsTables.get(op1);
            else nop1 = op1;
            this.matchOrderTables.put(nop1, new HashSet<>());
            for (Operation op2 : program.matchOrderTables.get(op1)) {
                if (csecOpsTables.keySet().contains(op2)) op2 = csecOpsTables.get(op2);
                this.matchOrderTables.get(nop1).add(op2);
            }
        }
    }

    void refactorMatchPair() {
        for (Operation op1 : program.matchTables.keySet()) {
            Operation nop1;
            if (csecOpsTables.keySet().contains(op1)) nop1 = csecOpsTables.get(op1);
            else nop1 = op1;
            matchTables.put(nop1, new LinkedList<>());
            for (Operation op2 : program.matchTables.get(op1)) {
                if (csecOpsTables.keySet().contains(op2)) op2 = csecOpsTables.get(op2);
                if (!matchTables.get(nop1).contains(op2)) matchTables.get(nop1).add(op2);
            }
        }
        setMatchTablesForS();
    }

    void refactorQS(){
        for(Process process : processes){
            for(Operation operation : process.ops){
                appendOpToQS(operation);
            }
        }
    }


    public static void main(String[] args) {
        Program program = new Program("./src/test/fixtures/diffusion2d4.txt");
        program.printMatchPairs();
        NewProgram newProgram = new NewProgram(program);
        newProgram.printMatchPairs();
//        LinkedList<Integer> t1 = new LinkedList<>();
//        t1.add(1);
//        LinkedList<Integer> t2 = new LinkedList<>();
//        t2.add(2);
//        t2.addAll(t1);
//        t2.add(3);
//        t1.add(4);
//
//        System.out.println(t2.size());


    }
}
