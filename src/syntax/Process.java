package syntax;

import constant.OPTypeEnum;

import java.util.LinkedList;

public class Process {
    public int rank;
    public LinkedList<Operation> ops;
    public LinkedList<Operation> rlist;
    public LinkedList<Operation> slist;

    public Process(int rank){
        ops = new LinkedList<>();
        rlist = new LinkedList<>();
        slist = new LinkedList<>();
        this.rank = rank;
    }

    public void append(Operation operation){
        ops.add(operation);
        if(operation.isRecv())
            appendToRList(operation);
        if(operation.isSend())
            appendToSList(operation);
    }

    public void appendToRList(Operation operation){
        rlist.add(operation);
    }

    public void appendToSList(Operation operation){
        slist.add(operation);
    }

    public Operation getOP(int index){
        return ops.get(index);
    }

    public void printProcessInfo(){
        System.out.println("process rank:"+rank+", has TOTAL "+ops.size()+" calls and has "+rlist.size()+" recv calls and "+slist.size()+" send calls");
    }
}
