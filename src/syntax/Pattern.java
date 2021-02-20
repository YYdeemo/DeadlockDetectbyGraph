package syntax;

import constant.OPTypeEnum;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

public class Pattern {

    public Hashtable<Integer, Operation> pattern;
    public Graph graph;

    public Set<Integer> deadlockPros;
    public Set<Operation> deadlockReqs;

    public int[] tracker;//generate by abstract machine, which record the stop action's rank in each process;
    public boolean DeadlockCandidate = false;

    public Pattern(Graph graph, Stack<Operation> stack){
        this.graph = graph;
        pattern = new Hashtable<Integer, Operation>();
        for (Operation op : stack) {
            if (graph.program.isCheckInfiniteBuffer()) {
                if (op.isSend())
                    continue;
            }

            int opEP = op.proc;
            int opOrder = op.type.equals(OPTypeEnum.BARRIER) ? graph.program.processArrayList.get(op.proc).Size() : op.rank;

            if (!pattern.containsKey(opEP)) {
                pattern.put(opEP, op);
                continue;
            }
            //only keep the operation with lowest rank on each process
            int order = pattern.get(opEP).type.equals(OPTypeEnum.BARRIER) ? graph.program.processArrayList.get(pattern.get(opEP).proc).Size() : pattern.get(opEP).rank;

            if (order > opOrder)
                pattern.put(opEP, op);
        }
        setDeadlockProcs();
        setDeadlockReqs();
    }

    void setDeadlockProcs() {
        if(pattern.isEmpty()){
            deadlockPros = new HashSet<>();
            for(int i = 0; i<graph.program.getSize(); i++){
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
        System.out.println("THIS PATTERN IS LIKE THIS:");
        for(Operation operation : pattern.values()){
            System.out.print(" "+operation.getStrInfo()+" ");
        }
    }
}
