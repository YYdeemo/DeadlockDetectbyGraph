package constant;

public enum Status {
    UNREACHABLE(-2),
    UNKNOWN(0),
    REACHABLE(1),
    UNSATISFIABLE(-1),
    SATISFIABLE(2);

    public final int intValue;

    Status(int type) {
        this.intValue = type;
    }

}
