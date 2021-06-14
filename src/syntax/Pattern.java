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

        patternTable = generatePatternTable(stack, program.checkInfiniteBuffer);

        setDeadlockProcs();
        setDeadlockReqs();
        //status = Check.checkPattern(this.program,this);
//        check();
//        if (status==Status.SATISFIABLE || status==Status.UNSATISFIABLE)
//
//        System.out.println("cycle is : "+stack);
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

    public void check(){
        this.status = Check.checkPattern(this.program,this);
    }
}
