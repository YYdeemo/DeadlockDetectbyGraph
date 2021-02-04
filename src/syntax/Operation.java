package syntax;

import constant.OPTypeEnum;

/**
 *
 */
public class Operation {
    public int index;//the rank of each action in same type actions process[for example: r.rank = process.rlist.indexof(r)]
    public int indx;//index in process
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

    public Operation(OPTypeEnum type, int index, int proc) {
        this.type = type;
        this.index = index;
        this.proc = proc;
    }

    //log the information of this operation by...
    public void logOPInfo() {

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

    public boolean isWait() { return (this.type == OPTypeEnum.WAIT); }

    public boolean isBarrier() { return (this.type == OPTypeEnum.BARRIER); }

    public boolean isBot() { return (this.type == OPTypeEnum.BOT); }

    public String getStrInfo(){
        return this.type+" "+this.proc+"_"+this.rank+" ";
    }
}
