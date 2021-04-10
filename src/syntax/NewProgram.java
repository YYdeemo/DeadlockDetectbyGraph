package syntax;

import com.sun.tools.classfile.Opcode;

import java.util.*;

public class NewProgram extends Program{
    public Hashtable<Operation, CsecOperation> csecOpsTables;


    public NewProgram(String filepath, boolean checkInfiniteBuffer) {
        super(filepath, checkInfiniteBuffer);//straight extends program or has a feature program ?
        csecOpsTables = new Hashtable<>();

        refactorProgram();
//        refactorMatchOrder();
//        refactorMatchPair();
    }

    /**
     * this method has a problem about wait ***
     */
    void refactorProgram() {
        for (Process process : processes) {
            LinkedList<Operation> csecOperationList = new LinkedList<>();
            int rank = process.rank;
            for (Operation operation : process.ops) {
                if (operation.isWait() || operation.isBarrier()) {
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

                    } else {
                        CsecOperation csecOp = creatCsecOperation(csecOperationList);

                    }
                    csecOperationList.clear();
                    csecOperationList.add(operation);
                }
            }
            if (csecOperationList.size() == 1) {
                //add the non-cescOp to process
            } else if (csecOperationList.size() >= 2) {
                CsecOperation csecOp = creatCsecOperation(csecOperationList);

            }
            csecOperationList.clear();
        }
        for (CsecOperation csecOperation : csecOpsTables.values()){
            Process process = this.get(csecOperation.proc);
            if (!process.ops.contains(csecOperation)) {
                process.append(csecOperation, csecOperation.rank);
                process.append(csecOperation.Nearstwait, csecOperation.rank + 1);
                for (Operation operation : csecOperation.OperationList){
                    process.remove(operation);
                    process.remove(operation.Nearstwait);
                }
            }
        }


    }

    CsecOperation creatCsecOperation(LinkedList<Operation> operations) {
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
        for (Operation op1 : matchOrderTables.keySet()) {
            if (!op1.isWait()){
                Operation nop1;
                if (csecOpsTables.keySet().contains(op1)) nop1 = csecOpsTables.get(op1);
                else nop1 = op1;
                if (!matchOrderTables.containsKey(nop1)) matchOrderTables.put(nop1, new HashSet<>());
                for (Operation op2 : matchOrderTables.get(op1)) {
                    if (!op2.isWait()){
                        if (csecOpsTables.keySet().contains(op2)) op2 = csecOpsTables.get(op2);
                        if (nop1!=op2 && !matchOrderTables.get(nop1).contains(op2)) matchOrderTables.get(nop1).add(op2);
                    }else{
                        if (csecOpsTables.keySet().contains(op2.req)) op2 = csecOpsTables.get(op2.req).Nearstwait;
                        if (nop1!=op2 && !matchOrderTables.get(nop1).contains(op2)) matchOrderTables.get(nop1).add(op2);
                    }
                }
            }else {
                Operation wait1;
                if(csecOpsTables.keySet().contains(op1.req)) wait1 = csecOpsTables.get(op1.req).Nearstwait;
                else wait1 = op1;
                if (!matchOrderTables.containsKey(wait1)) matchOrderTables.put(wait1, new HashSet<>());
                for(Operation op2 : matchOrderTables.get(op1)){
                    if(!op2.isWait()){
                        if(csecOpsTables.keySet().contains(op2)) op2 = csecOpsTables.get(op2);
                        if (wait1!=op2 && !matchOrderTables.get(wait1).contains(op2)) matchOrderTables.get(wait1).add(op2);
                    }else{
                        if(csecOpsTables.keySet().contains(op2.req)) op2 = csecOpsTables.get(op2.req).Nearstwait;
                        if (wait1!=op2 && !matchOrderTables.get(wait1).contains(op2)) matchOrderTables.get(wait1).add(op2);
                    }
                }
            }
        }
    }

    void refactorMatchPair() {
        for (Operation op1 : matchTables.keySet()) {
            Operation nop1;
            if (csecOpsTables.keySet().contains(op1)) nop1 = csecOpsTables.get(op1);
            else nop1 = op1;
            if (matchTables.containsKey(nop1)) matchTables.put(nop1, new LinkedList<>());
            for (Operation op2 : matchTables.get(op1)) {
                if (csecOpsTables.keySet().contains(op2)) op2 = csecOpsTables.get(op2);
                if (!matchTables.get(nop1).contains(op2)) matchTables.get(nop1).add(op2);
            }
        }
        matchTablesForS.clear();
        setMatchTablesForS();
    }

    public boolean inCsecTables(Operation operation){
        return csecOpsTables.keySet().contains(operation);
    }

    public Operation getCsecOp(Operation operation){
        if (csecOpsTables.keySet().contains(operation))
            operation = csecOpsTables.get(operation);
        return operation;
    }

    public boolean isCsecWait(Operation wait){
        if (!wait.isWait()) return false;
        for (Operation operation : csecOpsTables.values()){
            if (wait==operation.Nearstwait) return true;
        }
        return false;
    }

    public void printCsecOps(){
        for (CsecOperation csecOp : csecOpsTables.values()){
            System.out.println(csecOp.getStrInfo());
            for (Operation operation : csecOp.OperationList){
                System.out.print(" "+operation.getStrInfo()+" ");
            }
        }
    }

    public static void main(String[] args) {
        NewProgram newProgram = new NewProgram("./src/test/fixtures/diffusion2d4.txt", false);
        newProgram.printOps();

    }
}
