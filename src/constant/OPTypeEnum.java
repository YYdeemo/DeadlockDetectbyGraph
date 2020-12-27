package constant;

public enum OPTypeEnum {
    SEND("s"),
    ISEND("is"),
    SYCHRONIZED_SEND("ss"),
    READY_SEND("rs"),
    BUFFERED_SEND("bs"),
    STANDARD_SEND("bs"),
    RECV("r"),
    IRECV("ir"),
    WAIT("w"),
    BARRIER("b");

    private final String optype;

    private OPTypeEnum(String optype){
        this.optype = optype;
    }

    public String getOptype(){
        return optype;
    }
}
