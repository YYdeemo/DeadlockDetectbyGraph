package syntax;

import constant.OPTypeEnum;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

public class Pattern {

    public Hashtable<Integer, Operation> pattern;
    private int programsize;

    public Set<Integer> deadlockPros;
    public Set<Operation> deadlockReqs;

    public int[] tracker;//generate by abstract machine, which record the stop action's rank in each process;
    public boolean DeadlockCandidate = false;

    public Pattern(Graph graph, Stack<Operation> stack){
        this.programsize = graph.program.getSize();
        pattern = new Hashtable<Integer, Operation>();
        deadlockPros = new HashSet<>();
        deadlockReqs = new HashSet<>();
        tracker = new int[graph.program.getSize()];
        for (Operation op : stack) {//barrier/wait/zero send/Irecv/ can be the control point in a pattern
            if (op.isWait() || op.isBarrier() || (op.isSend() && (!graph.program.checkInfiniteBuffer)) || (op.isIRecv())) {
                if(op.isSend() && (!graph.program.checkInfiniteBuffer)) op = op.Nearstwait;
                if (!pattern.containsKey(op.proc)) pattern.put(op.proc, op);
                if (pattern.containsKey(op.proc) && op.rank < pattern.get(op.proc).rank) pattern.put(op.proc, op);
            }
        }

        setDeadlockProcs();
        setDeadlockReqs();
    }

    void setDeadlockProcs() {
        if(pattern.isEmpty()){
            deadlockPros = new HashSet<>();
            for(int i = 0; i<programsize; i++){
                deadlockPros.add(i);
            }
        }else{
            this.deadlockPros = pattern.keySet();
        }
    }

    public Set<Integer> getDeadlockPros() {
        return deadlockPros;
    }

    public void setDeadlockReqs() {
        deadlockReqs = new HashSet<Operation>();
        for(Operation operation : pattern.values()){
            if(operation.isWait()){
                deadlockReqs.add(operation.req);
            }
        }
    }

    public Set<Operation> getDeadlockReqs() {
        return deadlockReqs;
    }

    public Hashtable<Integer, Operation> getPattern() {
        return pattern;
    }

    public int getSize(){
        return pattern.size();
    }

    public void setDeadlockCandidate(boolean deadlockCandidate) {
        DeadlockCandidate = deadlockCandidate;
    }

    public boolean isDeadlockCandidate() {
        return DeadlockCandidate;
    }

    public void printPattern(){
        System.out.print("[PATTERN]: THIS PATTERN IS LIKE THIS:");
        for(Operation operation : pattern.values()){
            System.out.print(" "+operation.getStrInfo()+" ++ ");
        }
        System.out.println(" ");
    }

    public void setTracker(int[] tracker) {
        this.tracker = tracker;
    }
}
