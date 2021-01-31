package constant;

public enum OPTypeEnum {
    SEND("s"),//non-blocking send
    B_SEND("bs"),//blocking send

    //non-blocking send
    SYCHRONIZED_SEND("ss"),
    READY_SEND("rs"),
//    BUFFERED_SEND("bfs"), //which is same as SEND
    STANDARD_SEND("sds"),

    RECV("r"),//non-blocking recv
    B_RECV("br"),//blocking recv

    WAIT("w"),
    BARRIER("b"),
    BOT("bot");

    public final String optype;

    OPTypeEnum(String optype){
        this.optype = optype;
    }


    public String getOptype(){
        return optype;
    }
}
