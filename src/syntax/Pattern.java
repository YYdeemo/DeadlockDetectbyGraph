package syntax;

import constant.OPTypeEnum;

import java.util.Hashtable;
import java.util.Stack;

public class Pattern {

    public Hashtable<Integer, Operation> pattern;
    public Graph graph;
    public boolean DeadlockCandidate;

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
}
