package syntax;

import constant.OPTypeEnum;

/**
 *
 */
public class Operation {
    short index;//index in execution
    short indx;//index in process
    short rank;//the rank of each action in process
    short proc;//the rank of process
    short src;//source
    short dst;//destination
    short tag;//tag
    short group;//group
    short reqID;//the req action's idx
    Operation req;//for a wait ;the req is an operation which is witnessed by the wait
    OPTypeEnum type;//the type is num : "send", "recv", "wait", "barrier", "bot"
    Operation Nearstwait;//for a recv or a send, this wait is NearestWait
//    SendModeEnum sendmode;//the type is num: "ssend", "rsend", "bsend", "tsend"

    public Operation(OPTypeEnum type, short index, short proc, short src, short dst,short tag, short group, short reqID){
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
    //log the information of this operation by...
    public void logOPInfo(){

    }


}
