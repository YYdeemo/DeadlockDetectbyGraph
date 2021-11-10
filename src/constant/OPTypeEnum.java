package constant;

public enum OPTypeEnum {
    SEND("s"),//non-blocking send, all send is non-blocking send
//    B_SEND("bs"),//blocking send
    //non-blocking send
    SYCHRONIZED_SEND("ss"),
    READY_SEND("rs"),
    BUFFERED_SEND("bs"),
    STANDARD_SEND("as"), //which is same as SEND

    RECV("r"),//non-blocking recv
    B_RECV("br"),//blocking recv

    WAIT("w"),
    BOT("bot"),

    BARRIER("b"),
    BROADCAST("bd"),
    GATHER("ga"),
    SCATTER("sc"),
    REDUCE("rd");

    public final String optype;

    OPTypeEnum(String optype){
        this.optype = optype;
    }


    public String getOptype(){
        return optype;
    }
}
