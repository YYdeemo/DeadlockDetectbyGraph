package syntax;

import Enum.*;

public class Operation {
    short index;//index in execution
    short indx;//index in process
    short rank;//the rank of each action in process
    short proc;//the rank of process
    short src;//source
    short dst;//destination
    Object req;//for a wait ;the req is an operation which is witnessed by the wait
    OTypeEnum type;//the type is num : "send", "recv", "wait", "barrier", "bot"
    SendModeEnum sendmode;//the type is num: "ssend", "rsend", "bsend", "tsend"
    Object Nearstwait;//for a recv or a send, this wait is NearestWait

    Operation(short index, short proc, short src, short dst, OTypeEnum type, SendModeEnum sendmode){
        this.index = index;
        this.proc = proc;
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.sendmode = sendmode;
    }
    //log the information of this operation by...
    public void logOPInfo(){

    }


}
