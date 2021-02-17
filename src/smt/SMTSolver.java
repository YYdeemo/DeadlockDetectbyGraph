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
    Solver solver;


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
        solver = ctx.mkSolver();
        encodeResult = new Hashtable<Operation, Hashtable<String, Expr>>();
        encodeProgram();
    }

    /**
     * encode each operation in acts
     * @throws Z3Exception
     */
    void encodeProgram() throws Z3Exception {
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

    /**
     * the acts contains all the operation which before the control points in each process
     */
    void initActs() {
        acts = new LinkedList<Operation>();
        for (Process process : program.getAllProcesses()) {
            for (Operation operation : process.ops) {
                if (operation.index >= candidate.tracker[process.rank] && operation.isBot()) break;
                acts.add(operation);
            }
        }
    }

    /**
     *
     * @return the number of the barriers in acts
     */
    int getBarrsNum() {
        int count = 0;
        for (Operation operation : acts) {
            if (operation.isBarrier()) count = count + 1;
        }
        return count;
    }

    /**
     *
     * @return the number of the recv and send operation in acts
     */
    int getOpsNum() {
        int count = 0;
        for (Operation operation : acts) {
            if (operation.isSend() || operation.isRecv()) count = count + 1;
        }
        return count;
    }

    public Model check(){
        Hashtable<Integer, Operation> gourps = new Hashtable<Integer, Operation>();
        LinkedList<Expr> times = new LinkedList<Expr>();
        for(Operation operation : acts){
            for(Operation succOp : program.matchOrder.MatchOrderTables.get(operation)){
                if(succOp.index<candidate.tracker[succOp.proc] && !succOp.isBarrier()){
                    solver.add(mkCompleteBefore(operation,succOp));
                }
            }
            if(operation.isSend() || operation.isRecv()){
                times.add(time(operation));
                solver.add(mkMatchIfComplete(operation));
                if(operation.Nearstwait != null
                        && operation.Nearstwait.index<candidate.tracker[operation.proc]){
                    solver.add(mkNearstWait(operation, operation.Nearstwait));
                }
                if(operation.rank>0){
                    if(operation.isSend()){
                        int rank = program.sendqs.get(operation.getHashCode()).indexOf(operation);
                        Operation predOp = program.sendqs.get(operation.getHashCode()).get(rank-1);
                        solver.add(mkNonOvertacking(predOp, operation));
                    }
                }
            }else if(operation.isWait()){
                if(candidate.deadlockPros.contains(operation.proc)){
                    solver.add(mkMustComplete(operation));
                }
            }else if(operation.isBarrier()){
                solver.add(mkMustComplete(operation));
                if(!gourps.containsKey(operation.group)){
                    gourps.put(operation.group,operation);
                    times.add(time(operation));
                }else{
                    solver.add(mkBarrGroup(gourps.get(operation.group), operation));
                }
            }
        }
        for( Operation operation : candidate.pattern.values()){
            if(operation.isWait()){
                if(operation.req.isRecv()){
                    for(Operation matchOp : program.matchTables.get(operation.req)){
                        solver.add(mkMustComplete(matchOp));
                    }
                }
            }
        }
        solver.add(mkUniqueTimes(times));
        solver.add(mkUniqueMatches());
        Status result = solver.check();
        if(result.equals(Status.SATISFIABLE)){
            return solver.getModel();
        }
        return null;
    }

    /**
     * recv has four expr: time(intExpr), match(intExpr), wait(intExpr), complete(boolExpr)
     * @param recv
     * @return <String, Expr> (t-->time)(m-->match)(w-->wait)(c-->complete)
     */
    public Hashtable<String, Expr> mkRecv(Operation recv) {
        if (!recv.isRecv()) return null;
        Hashtable<String, Expr> recvExpr = new Hashtable<String, Expr>();
        recvExpr.put("t", ctx.mkIntConst("time" + recv.toString()));
        recvExpr.put("m", ctx.mkIntConst("match" + recv.toString()));
        recvExpr.put("w", ctx.mkIntConst("wait" + recv.toString()));
        recvExpr.put("c", ctx.mkBoolConst("complete" + recv.toString()));
        return recvExpr;
    }

    /**
     * send has four Expr , same as recv
     * @param send
     * @return
     * @throws Z3Exception
     */
    public Hashtable<String, Expr> mkSend(Operation send) throws Z3Exception {
        if (!send.isSend()) return null;
        Hashtable<String, Expr> sendExpr = new Hashtable<String, Expr>();
        sendExpr.put("t", ctx.mkIntConst("time" + send.toString()));
        sendExpr.put("m", ctx.mkIntConst("match" + send.toString()));
        sendExpr.put("w", ctx.mkIntConst("wait" + send.toString()));
        sendExpr.put("c", ctx.mkBoolConst("complete" + send.toString()));
        return sendExpr;
    }

    /**
     * wait has two expr : time, complete
     * @param wait
     * @return
     * @throws Z3Exception
     */
    public Hashtable<String, Expr> mkWait(Operation wait) throws Z3Exception {
        if (!wait.isWait()) return null;
        Hashtable<String, Expr> waitExpr = new Hashtable<String, Expr>();
        waitExpr.put("t", ctx.mkIntConst("time" + wait.toString()));
        waitExpr.put("c", ctx.mkBoolConst("complete" + wait.toString()));
        return waitExpr;
    }

    /**
     * barrier has two expr : time , complete
     * @param barr
     * @return
     * @throws Z3Exception
     */
    public Hashtable<String, Expr> mkBarr(Operation barr) throws Z3Exception {
        if (!barr.isBarrier()) return null;
        Hashtable<String, Expr> barrExpr = new Hashtable<String, Expr>();
        barrExpr.put("t", ctx.mkIntConst("time" + barr.toString()));
        barrExpr.put("c", ctx.mkBoolConst("complete" + barr.toString()));
        return barrExpr;
    }

    /**
     * time(operation)  this function achieve get the operation's time IntExpr
     * @param operation
     * @return
     */
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

    Expr mkUniqueTimes(LinkedList<Expr> timesList) {
        Expr[] times = new Expr[timesList.size()];//barriers in a common group have a same time
        int i = 0;
        for(Expr t : timesList){
            times[i] = t;
            i += 1;
        }
        return ctx.mkDistinct(times);
    }

    Expr mkUniqueMatches() {
        Expr[] matches = new Expr[getOpsNum()];
        int rank = 0;
        for (Operation operation : acts) {
            if (operation.isRecv() || operation.isSend()) {
                matches[rank] = match(operation);
                rank += 1;
            }
        }
        return ctx.mkDistinct(matches);
    }


}

