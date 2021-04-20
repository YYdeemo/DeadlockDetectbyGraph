package syntax;

import constant.OPTypeEnum;
import javafx.util.Pair;

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
    public int reqID;//the req action's idx
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

    public Operation(OPTypeEnum type, int rank, int indx, int proc, int group) {
        this.type = type;
        this.rank = rank;
        this.indx = indx;
        this.proc = proc;
        this.group = group;
    }

    public Operation(OPTypeEnum type, int rank, int indx, int proc) {
        this.type = type;
        this.rank = rank;
        this.indx = indx;
        this.proc = proc;
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

    public boolean isBot() { return (this.type == OPTypeEnum.BOT); }

    public String getStrInfo(){
        return this.type+" "+this.proc+"_"+this.rank;
    }

    public boolean isCsecOperation() {
        return isCsecOperation;
    }

    public Pair<Integer, Integer> getHashCode(){
        //this hash code depends dst and src;
        Pair<Integer, Integer> pair = new Pair<>(this.dst, this.src);
        return pair;
    }

    @Override
    public int compareTo(Object op) {
        int compareRank = ((Operation) op).indx;
        return this.indx-compareRank;
    }

    @Override
    public String toString() {
        return this.type+" "+this.proc+"_"+this.rank;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
