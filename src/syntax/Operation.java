package syntax;

import constant.OPTypeEnum;
import constant.Pair;
//import javafx.util.Pair;

/**
 *
 */
public class Operation implements Comparable,Cloneable{
    public int index;//the rank is index of list which actions with same endpoint
    public int indx;//index in program, total number, which contains bot and barrier
    public int rank;//the rank of the process, Line number
    public int proc;//the rank of process
    public int src;//source
    public int dst;//destination
    public int tag;//tag
    public int group;//group
    public int reqID;//the req action's idx, if this op is collective operation, the req is number of element in buffer
    public int root;//collective's root
    public Operation req;//for a wait ;the req is an operation which is witnessed by the wait
    public OPTypeEnum type;//the type is num : "send", "recv", "wait", "barrier", "bot"
    public Operation Nearstwait;//for a recv or a send, this wait is NearestWait
//    SendModeEnum sendmode;//the type is num: "ssend", "rsend", "bsend", "tsend"
    public boolean isCsecOperation = false;

    public Operation(OPTypeEnum type, int index, int proc, int src, int dst, int tag, int group, int reqID) {
        this.type = type;
        this.index = index;
        this.proc = proc;
        this.src = src;
        this.dst = dst;
        this.tag = tag;
        this.group = group;
        this.reqID = reqID;
        Nearstwait = null;
    }

    // 5 features collective except barrier
    public Operation(OPTypeEnum type, int index, int root, int proc, int group) {//collective operation;
        this.type = type;
        this.index = index;
        this.root = root;
        this.proc = proc;
        this.group = group;
    }

    public Operation(OPTypeEnum type, int rank, int indx, int proc) {
        this.type = type;
        this.rank = rank;
        this.indx = indx;
        this.proc = proc;
    }

    public Operation(OPTypeEnum type, Operation req){
        if(type==OPTypeEnum.WAIT){
            this.type = type;
            this.req = req;
        }
    }

    public boolean isSend() {
        return (this.type == OPTypeEnum.SEND
                || this.type == OPTypeEnum.STANDARD_SEND
                || this.type == OPTypeEnum.SYCHRONIZED_SEND
                || this.type == OPTypeEnum.READY_SEND);
    }

    public boolean isRecv() {
        return (this.type == OPTypeEnum.RECV);
    }
    public boolean isIRecv() {
        return (this.type == OPTypeEnum.B_RECV);
    }

    public boolean isWait() { return (this.type == OPTypeEnum.WAIT); }

    public boolean isBarrier() { return (this.type == OPTypeEnum.BARRIER); }
    public boolean isBroadcast() {return (this.type == OPTypeEnum.BROADCAST); }
    public boolean isGather() {return this.type == OPTypeEnum.GATHER; }
    public boolean isReduce() {return this.type == OPTypeEnum.REDUCE; }
    public boolean isScatter() { return this.type == OPTypeEnum.SCATTER; }

    public boolean isCollective() { return isBarrier()||isBroadcast()||isReduce()||isGather()||isScatter(); }

    public boolean isBot() { return (this.type == OPTypeEnum.BOT); }

    public String getStrInfo(){
        return this.type+" "+this.proc+"_"+this.rank;
    }

    public boolean isCsecOperation() {
        return isCsecOperation;
    }

    public Pair<Integer, Integer> getEndpoint(){
        //this hash code depends dst and src;
        Pair<Integer, Integer> pair = new Pair<Integer,Integer>(this.dst, this.src);
        return pair;
    }

    @Override
    public int compareTo(Object op) {
        int compareRank = ((Operation) op).indx;
        return this.indx-compareRank;
//        return compareRank-this.indx;
    }

    @Override
    public String toString() {
        if (this.isRecv())
            return this.type+" "+this.proc+" "+this.src+" "+this.tag+" "+this.rank;
        else if(this.isSend())
            return this.type+" "+this.proc+" "+this.dst+" "+this.tag+" "+this.rank;
        else if(this.isWait())
            return this.type+" "+this.proc+" "+this.req.rank;
        else if(this.isBarrier())
            return this.type+" "+this.proc+" "+this.group+" "+this.rank;
        else
            return this.type+" "+this.proc+"_"+this.root+" "+this.group+" "+this.rank;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public static void main(String[] args) {
        Operation a1 = new Operation(OPTypeEnum.SEND,0,0,0,1,0,0,0);
        Operation a2 = new Operation(OPTypeEnum.SEND,1,0,0,1,0,0,0);
        System.out.println(a1.getEndpoint());
        System.out.println(a2.getEndpoint());
        System.out.println(a1.getEndpoint().equals(a2.getEndpoint()));
    }
}
