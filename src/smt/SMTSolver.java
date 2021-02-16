package smt;


import com.microsoft.z3.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import syntax.Operation;
import syntax.Pattern;
import syntax.Process;
import syntax.Program;

import java.util.Hashtable;
import java.util.LinkedList;

/**
 * the SMT solver
 */
public class SMTSolver {

    Program program;
    Pattern candidate;
    LinkedList<Operation> acts; //operations before the control point in each process;

    public Hashtable<Operation, Hashtable<String, Expr>> encodeResult;//<operation ,<t,m,w,c>>
    Context ctx;
    Solver s;


    public final String t = "t";
    public final String m = "m";
    public final String w = "w";
    public final String c = "c";


    public SMTSolver(Program program, Pattern pattern) throws Z3Exception {
        this.program = program;
        this.candidate = pattern;
        initialize();
    }

    public void initialize() throws Z3Exception {
        ctx = new Context();
        encodeResult = new Hashtable<Operation, Hashtable<String, Expr>>();
        initProgram();
    }

    void initProgram() throws Z3Exception {
        initActs();
        for (Operation operation : acts) {
            if (operation.isRecv()) {
                encodeResult.put(operation, mkRecv(operation));
            } else if (operation.isSend()) {
                encodeResult.put(operation, mkSend(operation));
            } else if (operation.isWait()) {
                encodeResult.put(operation, mkWait(operation));
            } else if (operation.isBarrier()) {
                encodeResult.put(operation, mkBarr(operation));
            }
        }
    }

    void initActs() {
        acts = new LinkedList<Operation>();
        for (Process process : program.getAllProcesses()) {
            for (Operation operation : process.ops) {
                if (operation.index >= candidate.tracker[process.rank] && operation.isBot()) break;
                acts.add(operation);
            }
        }
    }

    public Hashtable<String, Expr> mkRecv(Operation recv) {
        if (!recv.isRecv()) return null;
        Hashtable<String, Expr> recvExpr = new Hashtable<String, Expr>();
        recvExpr.put("t", ctx.mkIntConst("time" + recv.toString()));
        recvExpr.put("m", ctx.mkIntConst("match" + recv.toString()));
        recvExpr.put("w", ctx.mkIntConst("wait" + recv.toString()));
        recvExpr.put("c", ctx.mkIntConst("complete" + recv.toString()));
        return recvExpr;
    }

    public Hashtable<String, Expr> mkSend(Operation send) throws Z3Exception {
        if (!send.isSend()) return null;
        Hashtable<String, Expr> sendExpr = new Hashtable<String, Expr>();
        sendExpr.put("t", ctx.mkIntConst("time" + send.toString()));
        sendExpr.put("m", ctx.mkIntConst("match" + send.toString()));
        sendExpr.put("w", ctx.mkIntConst("wait" + send.toString()));
        sendExpr.put("c", ctx.mkIntConst("complete" + send.toString()));
        return sendExpr;
    }

    public Hashtable<String, Expr> mkWait(Operation wait) throws Z3Exception {
        if (!wait.isWait()) return null;
        Hashtable<String, Expr> waitExpr = new Hashtable<String, Expr>();
        waitExpr.put("t", ctx.mkIntConst("time" + wait.toString()));
        waitExpr.put("c", ctx.mkBoolConst("complete" + wait.toString()));
        return waitExpr;
    }

    public Hashtable<String, Expr> mkBarr(Operation barr) throws Z3Exception {
        if (!barr.isBarrier()) return null;
        Hashtable<String, Expr> barrExpr = new Hashtable<String, Expr>();
        barrExpr.put("t", ctx.mkIntConst("time" + barr.toString()));
        barrExpr.put("c", ctx.mkBoolConst("complete" + barr.toString()));
        return barrExpr;
    }

    public Expr time(Operation operation) {
        return encodeResult.get(operation).get(t);
    }

    public Expr match(Operation operation) {
        return encodeResult.get(operation).get(m);
    }

    public Expr wait(Operation operation) {
        return encodeResult.get(operation).get(w);
    }

    public Expr complete(Operation operation) {
        return encodeResult.get(operation).get(c);
    }

    public BoolExpr mkMatch(Operation recv, Operation send) throws Z3Exception {
        return ctx.mkAnd(
                ctx.mkEq(match(recv), time(send)),
                ctx.mkEq(match(send), time(recv)),
                ctx.mkLt(time(send), wait(recv)),
                ctx.mkLt(time(recv), wait(send)),
                complete(recv),
                complete(send)
        );
    }

    Expr mkCompleteBefore(Operation a, Operation b) {
        return ctx.mkAnd(
                ctx.mkImplies(complete(b), complete(a)),
                ctx.mkLt(time(a), time(b))
        );
    }

    Expr mkMustComplete(Operation operation) {
        return ctx.mkEq(complete(operation), ctx.mkBool(true));
    }

    BoolExpr mkRecvMatch(Operation recv) {
        BoolExpr b = null;
        for (Operation send : program.matchTables.get(recv)) {
            if (!candidate.deadlockReqs.contains(send) && send.index < candidate.tracker[send.proc]) {
                BoolExpr a = mkMatch(recv, send);
                b = (b != null) ? ctx.mkOr(b, a) : a;
            }
        }
        return b;
    }

    BoolExpr mkSendMatch(Operation send) {
        BoolExpr b = null;
        for (Operation recv : program.matchTablesForS.get(send)) {
            if (!candidate.deadlockReqs.contains(recv) && recv.index < candidate.tracker[recv.proc]) {
                BoolExpr a = mkMatch(recv, send);
            }
        }
        return b;
    }

    Expr mkMatchIfComplete(Operation operation) {
        if (operation.isSend()) {
            return ctx.mkImplies(complete(operation), mkSendMatch(operation));
        } else if (operation.isRecv()) {
            return ctx.mkImplies(complete(operation), mkRecvMatch(operation));
        } else return null;
    }

    Expr mkNonOvertacking(Operation a, Operation b) {
        return ctx.mkLt(match(a), match(b));
    }

    Expr mkNearstWait(Operation operation, Operation wait) {
        return ctx.mkEq(wait(operation), time(wait));
    }

    Expr mkBarrGroup(Operation barr1, Operation barr2) {
        return ctx.mkEq(time(barr1), time(barr2));
    }

    Expr mkUniqueTimes(){
//        IntExpr[] times = new IntExpr[];
//        for(Operation operation){
//
//        }
//        ctx.mkDistinct()
    }


}

