package syntax;

import constant.OPTypeEnum;

import java.util.LinkedList;

public class Process {
    public int rank;
    public LinkedList<Operation> ops;
    public LinkedList<Operation> rlist;
    public LinkedList<Operation> slist;

    public Process(int rank) {
        ops = new LinkedList<Operation>();
        rlist = new LinkedList<Operation>();
        slist = new LinkedList<Operation>();
        this.rank = rank;
    }

    public void append(Operation operation) {
        ops.add(operation);
        if (operation.isRecv())
            appendToRList(operation);
        if (operation.isSend())
            appendToSList(operation);
    }

    public void appendToRList(Operation operation) {
        rlist.add(operation);
    }

    public void appendToSList(Operation operation) {
        slist.add(operation);
    }

    public Operation getOP(int index) {
        return ops.get(index);
    }

    public void printProcessInfo() {
        System.out.println("process rank:" + rank + ", has TOTAL " + ops.size() + " calls and has " + rlist.size() + " recv calls and " + slist.size() + " send calls");
    }

    public int Size() {
        return ops.size();
    }

    public int NextBlockPoint() {
        int indicator = 0;
        //LinkedList<Operation> visitedOPs = new LinkedList<Operation>();
        while (indicator < ops.size()) {
            if (ops.get(indicator).type.equals(OPTypeEnum.BARRIER) || ops.get(indicator).type.equals(OPTypeEnum.BOT))
                break;

            if (ops.get(indicator).isRecv()) {
                break;
            }
            //op could be a wait if receives are non-blocking
            if (ops.get(indicator).type.equals(OPTypeEnum.WAIT)) {
                if (ops.get(indicator).req.isRecv())
                    break;
            }
            indicator++;
        }
        return indicator;
    }

    public int ToPoint(Operation op) {
        int indicator = 0;
        while (indicator < ops.size()) {
            if (ops.get(indicator).equals(op))//right now, only receives are considered as block points
                break;
            indicator++;
        }
        return indicator;
    }
}
