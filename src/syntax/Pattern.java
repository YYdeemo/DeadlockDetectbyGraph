package syntax;

import constant.OPTypeEnum;
import constant.Status;
import methods.Check;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

public class Pattern {

    public Hashtable<Integer, Operation> patternTable;
    public Program program;
    private int programsize;

    public Set<Integer> deadlockPros;
    public Set<Operation> deadlockReqs;

    public int[] tracker;//generate by abstract machine, which record the stop action's rank in each process;
    public boolean DeadlockCandidate = false;
    public Status status;

    public Pattern(Program program, Stack<Operation> stack) {
        this.program = program;
        this.programsize = program.getSize();
        patternTable = new Hashtable<Integer, Operation>();
        deadlockPros = new HashSet<>();
        deadlockReqs = new HashSet<>();
        tracker = new int[program.getSize()];
        if (program instanceof NewProgram) {
            try {
                patternTable = generatePatternTableNew(stack, (NewProgram) program);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }else {
            patternTable = generatePatternTable(stack, program.checkInfiniteBuffer);
        }

        setDeadlockProcs();
        setDeadlockReqs();
        status = Check.checkPattern(this.program,this);
    }

    public static Hashtable<Integer, Operation> generatePatternTable(Stack<Operation> stack, boolean checkInfinite){
        Hashtable<Integer, Operation> table = new Hashtable<>();
        for (Operation op : stack) {//barrier/wait/zero send/Irecv/ can be the control point in a pattern
            if (op.isWait() || op.isBarrier() || (op.isSend() && (!checkInfinite)) || (op.isIRecv())) {
                if(op.isSend() && (!checkInfinite)) op = op.Nearstwait;
                if (!table.containsKey(op.proc)) table.put(op.proc, op);
                if (table.containsKey(op.proc) && op.rank < table.get(op.proc).rank) table.put(op.proc, op);
            }
        }
        return table;
    }

    public Hashtable<Integer, Operation> generatePatternTableNew(Stack<Operation> stack, NewProgram program) throws CloneNotSupportedException {
        Hashtable<Integer, Operation> table = new Hashtable<>();
        NewProgram programNew = (NewProgram) program.clone();
        updateProgram(programNew, stack);
        NewProgram newProgram = (NewProgram) programNew;
        for (Operation op : stack){
            if (newProgram.csecOpsTables.containsKey(op)){
                Operation wait = newProgram.csecOpsTables.get(op).Nearstwait;
                if (!table.containsKey(op.proc)) table.put(wait.proc, wait);
                if (table.containsKey(op.proc) && wait.rank < table.get(op.proc).rank) table.put(wait.proc, wait);
            }
            else if (op.isWait() || op.isBarrier() || (op.isSend() && (!program.checkInfiniteBuffer)) || (op.isIRecv())) {
                if(op.isSend() && (!program.checkInfiniteBuffer)) op = op.Nearstwait;
                if (!table.containsKey(op.proc)) table.put(op.proc, op);
                if (table.containsKey(op.proc) && op.rank < table.get(op.proc).rank) table.put(op.proc, op);
            }
        }
        return table;
    }

    void updateProgram(NewProgram program, Stack<Operation> stack) throws CloneNotSupportedException {
        for (Operation operation : stack){
            if (program.csecOpsTables.keySet().contains(operation)){
                CsecOperation csecOperation = program.csecOpsTables.get(operation);
                CsecOperation d1 = null;
                CsecOperation d2 = null;
                d2 = new CsecOperation(operation.type, operation.index, operation.proc, operation.src,
                        operation.dst, operation.tag, operation.group, operation.reqID);
                //update the wait for d2
                d2.Nearstwait = csecOperation.Nearstwait;
                d2.Nearstwait.req = d2;
                //update the single op for d1 and d2
                for (int i = 0; i < csecOperation.OperationList.size(); i++) {
                    Operation operation1 = csecOperation.OperationList.get(i);
                    if (i==0 && i<csecOperation.OperationList.indexOf(operation)){
                        d1 = new CsecOperation(operation1.type,operation1.index, operation.proc, operation1.src, operation1.dst,
                                operation1.tag, operation1.group, operation1.reqID);
                        d1.appendOpToList(operation1);
                    }
                    else if (i<csecOperation.OperationList.indexOf(operation)){
                        d1.appendOpToList(operation1);
                    }
                    else{
                        d2.appendOpToList(operation1);
                    }
                }
                int processRank = csecOperation.proc;
//                int rank = csecOperation.rank;
                int rank = program.get(processRank).ops.indexOf(csecOperation);
                Process process = program.processes.get(processRank);
                if (d1!=null){
                    Operation w1 = d1.OperationList.getLast().Nearstwait;
                    w1.req = d1;
                    d1.Nearstwait = w1;
                    if (d2.OperationList.size()==1){
                        process.append(d2.OperationList.getFirst(),rank);
                        program.csecOpsTables.remove(d2.OperationList.getFirst());
                    }else{
                        process.append(d2,rank);
                        program.updateCsceOpsTables(d2);
                    }
                    if (d1.OperationList.size()==1){
                        process.append(d1.OperationList.getFirst().Nearstwait,rank);
                        process.append(d1.OperationList.getFirst(),rank);
                        program.csecOpsTables.remove(d1.OperationList.getFirst());
                    }else{
                        process.append(w1,rank);
                        process.append(d1,rank);
                        program.updateCsceOpsTables(d1);
                    }
                    program.processes.get(processRank).remove(csecOperation);
                }
            }
        }
        this.program = program;
    }

    void setDeadlockProcs() {
        if(patternTable.isEmpty()){
            deadlockPros = new HashSet<>();
            for(int i = 0; i<programsize; i++){
                deadlockPros.add(i);
            }
        }else{
            this.deadlockPros = patternTable.keySet();
        }
    }


    public void setDeadlockReqs() {
        deadlockReqs = new HashSet<Operation>();
        for(Operation operation : patternTable.values()){
            if(operation.isWait()){
                deadlockReqs.add(operation.req);
            }
        }
    }


    public int getSize(){
        return patternTable.size();
    }


    public void printPattern(){
        System.out.print("[PATTERN]: THIS PATTERN IS LIKE THIS:");
        for(Operation operation : patternTable.values()){
            System.out.print(" "+operation.getStrInfo()+" ++ ");
        }
        System.out.println(" ");
    }

    public void setTracker(int[] tracker) {
        this.tracker = tracker;
    }
}
