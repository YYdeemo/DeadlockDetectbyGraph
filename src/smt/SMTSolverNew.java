package smt;


import com.microsoft.z3.*;
import com.sun.org.apache.bcel.internal.generic.LNEG;
import sun.jvm.hotspot.debugger.SymbolLookup;
import syntax.*;
import syntax.Pattern;
import syntax.Process;

import java.util.Hashtable;
import java.util.LinkedList;

/**
 * the SMT solver
 */
public class SMTSolverNew {

    NewProgram program;
    Pattern candidate;
    LinkedList<Operation> acts; //operations before the control point in each process;

    public Hashtable<Operation, Hashtable<String, Expr>> encodeResult;//<operation ,<t,m,w,c>>
    Context ctx;
    Solver solver;

    int[] lastRank;

    public final String t = "t";
    public final String m = "m";
    public final String w = "w";
    public final String c = "c";

    public LinkedList<String> ExprList;

    public boolean isNewProgram;


    public SMTSolverNew(NewProgram program, Pattern pattern) throws Z3Exception {
        this.program = program;
        this.candidate = pattern;
        ExprList = new LinkedList<>();
        isNewProgram = true;
        initialize();
    }

    public void initialize() throws Z3Exception {
        ctx = new Context();
        solver = ctx.mkSolver();
        encodeResult = new Hashtable<Operation, Hashtable<String, Expr>>();
        lastRank = new int[program.getSize()];//the rank of actions in tracker[]
        updateLastRank();
//        printLastRank();
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
                if (operation.isCsecOperation) encodeResult.put(operation, mkCsecRecv(operation));
                else encodeResult.put(operation, mkRecv(operation));
            } else if (operation.isSend()) {
                if (operation.isCsecOperation) encodeResult.put(operation, mkCsecSend(operation));
                else encodeResult.put(operation, mkSend(operation));
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
                if (operation.rank < lastRank[process.rank]){
                    acts.add(operation);
                }
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

    public void encode() {
        Hashtable<Integer, Operation> gourps = new Hashtable<Integer, Operation>();
        LinkedList<Expr> times = new LinkedList<Expr>();
        for(Operation operation : acts){
            if(program.matchOrderTables.containsKey(operation)) {//a <mo b
                for (Operation succOp : program.matchOrderTables.get(operation)) {
                    succOp =  program.getCsecOp(succOp);
                    if (succOp.rank < lastRank[succOp.proc] && program.get(succOp.proc).ops.contains(succOp)) {
                        solver.add(mkCompleteBefore(operation, succOp));
                    }
                }
            }

            if(operation.isSend() || operation.isRecv()){
                if (operation.isCsecOperation){
                    solver.add(mkUniqueTimesandMatches((CsecOperation) operation));
                }
                else times.add(time(operation));
                solver.add(mkMatchIfComplete(operation));
                if(operation.Nearstwait != null
                        && operation.Nearstwait.rank <lastRank[operation.proc]){
                    solver.add(mkNearstWait(operation, operation.Nearstwait));
                }
                if(operation.rank>0){
                    if(operation.isSend()){
                        int rank = program.sendqs.get(operation.getHashCode()).indexOf(operation);
                        if(rank>1){
                            Operation predOp = program.sendqs.get(operation.getHashCode()).get(rank-1);
                            if (isNewProgram) predOp = ((NewProgram) program) .getCsecOp(predOp);
                            if (predOp!=operation)solver.add(mkNonOvertacking(predOp, operation));
                        }
                    }else{
                        int rank = program.recvqs.get(operation.getHashCode()).indexOf(operation);
                        if(rank>1){
                            Operation predOp = program.recvqs.get(operation.getHashCode()).get(rank-1);
                            if (isNewProgram) predOp = ((NewProgram) program) .getCsecOp(predOp);
                            if (predOp!=operation)solver.add(mkNonOvertacking(predOp, operation));
                        }
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
        for( Operation operation : candidate.patternTable.values()){
            if(operation.isWait()){
                if(operation.req.isRecv()){
                    LinkedList<Operation> matchTable = new LinkedList<>();
                    if (operation.req.isCsecOperation){
                        for (Operation op : ((CsecOperation)operation.req).OperationList){
                            matchTable.addAll(program.matchTables.get(op));
                        }
                    }else matchTable = program.matchTables.get(operation.req);

                    for(Operation matchOp : matchTable){
                        if (isNewProgram)matchOp = ((NewProgram)program).getCsecOp(matchOp);
                        if(matchOp!=operation.req && matchOp.rank < lastRank[matchOp.proc])
                            solver.add(mkMustComplete(matchOp));
                    }
                }else if (operation.req.isSend()){
                    LinkedList<Operation> matchTable = new LinkedList<>();
                    if (operation.req.isCsecOperation){
                        for (Operation op : ((CsecOperation)operation.req).OperationList){
                            matchTable.addAll(program.matchTablesForS.get(op));
                        }
                    }else matchTable = program.matchTablesForS.get(operation.req);
                    for(Operation matchOp : matchTable){
                        if (isNewProgram) matchOp = ((NewProgram)program).getCsecOp(matchOp);
                        if (matchOp != operation.req && matchOp.rank < lastRank[matchOp.proc])
                            solver.add(mkMustComplete(matchOp));
                    }
                }
            }
        }
        solver.add(mkUniqueTimes(times));
        solver.add(mkUniqueMatches());
    }

    public Model check() {
        if(!solver.check().equals(Status.SATISFIABLE)){
//            System.out.println("UNSAT");
            return null;
        }
        return solver.getModel();
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

    public Hashtable<String, Expr> mkCsecRecv(Operation recv){
        if(!(recv.isCsecOperation && recv.isRecv())) return null;
        Hashtable<String, Expr> csecRecvExpr = new Hashtable<>();
        csecRecvExpr.put("t", ctx.mkArrayConst("time"+recv.toString(), ctx.getIntSort(), ctx.getIntSort()));
        csecRecvExpr.put("m", ctx.mkArrayConst("match"+recv.toString(), ctx.getIntSort(),ctx.getIntSort()));
        csecRecvExpr.put("w", ctx.mkIntConst("wait"+recv.toString()));
        csecRecvExpr.put("c", ctx.mkBoolConst("complete"+recv.toString()));

        for (int i = 0; i < ((CsecOperation)recv).OperationList.size(); i++) {
            ctx.mkStore(csecRecvExpr.get("t"),ctx.mkInt(i),ctx.mkIntConst("time"+((CsecOperation)recv).OperationList.get(i).toString()));
            ctx.mkStore(csecRecvExpr.get("m"),ctx.mkInt(i),ctx.mkIntConst("match"+((CsecOperation)recv).OperationList.get(i).toString()));
        }
        
        return csecRecvExpr;
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

    public Hashtable<String, Expr> mkCsecSend(Operation send) throws Z3Exception {
        if (!(send.isCsecOperation && send.isSend())) return null;
        Hashtable<String, Expr> csecSendExpr = new Hashtable<>();
        csecSendExpr.put("t", ctx.mkArrayConst("time"+send.toString(), ctx.getIntSort(), ctx.getIntSort()));
        csecSendExpr.put("m", ctx.mkArrayConst("match"+send.toString(), ctx.getIntSort(),ctx.getIntSort()));
        csecSendExpr.put("w", ctx.mkIntConst("wait"+send.toString()));
        csecSendExpr.put("c", ctx.mkBoolConst("complete"+send.toString()));

        for (int i = 0; i < ((CsecOperation)send).OperationList.size(); i++) {
            Expr time = ctx.mkIntConst("time"+((CsecOperation)send).OperationList.get(i).toString());
            Expr match = ctx.mkIntConst("match"+((CsecOperation)send).OperationList.get(i).toString());
            ctx.mkStore(csecSendExpr.get("t"),ctx.mkInt(i),time);
            ctx.mkStore(csecSendExpr.get("m"),ctx.mkInt(i),match);
        }

        return csecSendExpr;
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

    Expr getSingleTime(Operation operation, int index) {
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(time(operation),ctx.mkInt(index));
    }

    Expr getMaxTime(Operation operation){
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(time(operation),ctx.mkInt(((CsecOperation)operation).OperationList.size()-1));
    }

    Expr getMinTime(Operation operation){
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(time(operation),ctx.mkInt(0));
    }

    Expr getSingleMatch(Operation operation, int index) {
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(match(operation),ctx.mkInt(index));
    }

    Expr getMaxMatch(Operation operation){
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(match(operation),ctx.mkInt(((CsecOperation)operation).OperationList.size()-1));
    }

    Expr getMinMatch(Operation operation){
        if (!operation.isCsecOperation) return null;
        return ctx.mkSelect(match(operation),ctx.mkInt(0));
    }

    public BoolExpr mkMatch(Operation recv, Operation send) throws Z3Exception {
        addExprToList("match <"+recv.getStrInfo()+" ,"+send.getStrInfo()+">");
        return ctx.mkAnd(
                ctx.mkEq(match(recv), time(send)),
                ctx.mkEq(match(send), time(recv)),
                ctx.mkLt(time(send), wait(recv)),
                ctx.mkLt(time(recv), wait(send)),
                complete(recv),
                complete(send)
        );
    }

    BoolExpr mkMatchCS(Operation CsecOp, Operation SingleOp, int index) throws Z3Exception {
        return ctx.mkAnd(
                ctx.mkEq(getSingleMatch(CsecOp, index), time(SingleOp)),
                ctx.mkEq(match(SingleOp), getSingleTime(CsecOp,index)),
                ctx.mkLt(time(SingleOp), wait(CsecOp)),
                ctx.mkLt(getSingleTime(CsecOp, index), wait(SingleOp)),
                complete(SingleOp)
        );
    }

    BoolExpr mkMatchCC(Operation CsecOp1, Operation CsecOp2, int index1, int index2) throws Z3Exception {
        BoolExpr b = null;
        int a1 = ((CsecOperation)CsecOp1).OperationList.size()-index1;
        int a2 = ((CsecOperation)CsecOp2).OperationList.size()-index2;
        for (int i = 0; i < ((a1<a2)? a1:a2) ; i++) {
            BoolExpr a = ctx.mkAnd(
                    ctx.mkEq(getSingleMatch(CsecOp1, index1+i),getSingleTime(CsecOp2,index2+i)),
                    ctx.mkEq(getSingleTime(CsecOp1, index1+i),getSingleMatch(CsecOp2,index2+i)),
                    ctx.mkLt(getSingleTime(CsecOp1,index1+i), wait(CsecOp2)),
                    ctx.mkLt(getSingleTime(CsecOp2,index2+i), wait(CsecOp1))
            );
            b = (b != null) ? ctx.mkAnd(b, a) : a;
        }
        return b;
    }

    Expr mkCompleteBefore(Operation a, Operation b) {
        addExprToList(""+a.getStrInfo()+" <c "+b.getStrInfo());
        Expr timeA = null;
        Expr timeB = null;

        if (a.isCsecOperation) timeA = getMaxTime(a);
        else timeA = time(a);

        if (b.isCsecOperation) timeB = getMinTime(b);
        else timeB = time(b);
        return ctx.mkAnd(
                ctx.mkImplies(complete(b), complete(a)),
                ctx.mkLt(timeA, timeB)
        );
    }

    Expr mkMustComplete(Operation operation) {
        addExprToList(""+ operation.getStrInfo() +".complete = TRUE");
        return ctx.mkEq(complete(operation), ctx.mkBool(true));
    }

    BoolExpr mkRecvMatch(Operation recv) {
        addExprToList("mkRecvMatch: "+ recv.getStrInfo()+" OR :{");
        BoolExpr b = null;
        if(!program.matchTables.containsKey(recv)){
//            System.out.println("[ERROR]: THERE IS NO MATCH OPERATION WITH "+recv.getStrInfo());
            return b;
        }
        for (Operation send : program.matchTables.get(recv)) {
//            if (!candidate.deadlockReqs.contains(send) && send.rank < candidate.tracker[send.proc]) {
            if(send.rank < lastRank[send.proc]){
                BoolExpr a = null;
                if (program instanceof NewProgram){
                    Operation send2 = ((NewProgram)program).getCsecOp(send);
                    if (send2.isCsecOperation)
                        a = mkMatchCS(send2, recv, ((CsecOperation)send2).OperationList.indexOf(send));
                    else a = mkMatch(recv, send);
                }
                b = (b != null) ? ctx.mkOr(b, a) : a;
            }
        }
        addExprToList("}");
//        if(b==null) System.out.println("[ERROR] mkRecvMatch: RETURNS NULL!");
        return b;
    }

    Expr mkCsecRecvMatch(Operation recv){
        Expr b = null;
        int index = 0;
        for (Operation sRecv : ((CsecOperation) recv).OperationList){
            BoolExpr a = null;
            for (Operation send : program.matchTables.get(sRecv)){
                if (send.rank < program.get(send.proc).getOP(lastRank[send.proc]).req.rank){
                    BoolExpr c = null;
                    if (program instanceof NewProgram && ((NewProgram)program).csecOpsTables.containsKey(send)){
                        Operation send2 = ((NewProgram)program).getCsecOp(send);
                        c = mkMatchCC(recv, send2, index, ((CsecOperation)send2).OperationList.indexOf(send));
                    }
                    else c = mkMatchCS(recv, send, index);
                    a = (a != null) ? ctx.mkOr(a, c) : c;
                }
            }
            b = (b != null) ? ctx.mkAnd(b, a) : a;
            index++;
        }
        return b;
    }

    Expr mkSendMatch(Operation send) throws Z3Exception{
        addExprToList("mkSendMatch: "+send.getStrInfo() +" OR : {");
        Expr b = null;
        if(!program.matchTablesForS.containsKey(send)){
//            System.out.println("[ERROR]: THERE IS NO MATCH OPERATION WITH "+send.getStrInfo());
            return b;
        }
        for (Operation recv : program.matchTablesForS.get(send)) {
            if(recv.rank < lastRank[recv.proc]){
                BoolExpr a = null;
                if (program instanceof NewProgram){
                    Operation recv2 = ((NewProgram)program).getCsecOp(recv);
                    if (recv2.isCsecOperation)
                        a = mkMatchCS(recv2, send, ((CsecOperation)recv2).OperationList.indexOf(recv));
                    else a = mkMatch(recv, send);
                }
                b = (b != null) ? ctx.mkOr(b, a) : a;
            }
        }
        addExprToList("}");
//        if(b==null) System.out.println("[ERROR] mkSendMatch: RETURNS NULL!");
        return b;
    }

    Expr mkCsecSendMatch(Operation send){
        Expr b = null;
        int index = 0;
        for (Operation sSend : ((CsecOperation) send).OperationList){
            BoolExpr a = null;
            for (Operation recv : program.matchTablesForS.get(sSend)){
                if (recv.rank < program.get(recv.proc).getOP(lastRank[recv.proc]).req.rank){
                    BoolExpr c = null;
                    if (program instanceof NewProgram && ((NewProgram) program).csecOpsTables.containsKey(recv)){
                        Operation recv2 = ((NewProgram)program).getCsecOp(recv);
                        c = mkMatchCC(send, recv2, index, ((CsecOperation)recv2).OperationList.indexOf(recv));
                    }
                    else c = mkMatchCS(send, recv, index);
                    a = (a != null) ? ctx.mkOr(a, c) : c;
                }
            }
            b = (b != null) ? ctx.mkAnd(b, a) : a;
            index++;
        }
        return b;
    }

    Expr mkMatchIfComplete(Operation operation) {
        addExprToList(""+ operation.getStrInfo()+".complete -> ");
        if (operation.isSend()) {
            Expr sendMatch = null;
            if (operation.isCsecOperation) sendMatch = mkCsecSendMatch(operation);
            else sendMatch = mkSendMatch(operation);
            if(sendMatch!=null) return ctx.mkImplies(complete(operation), sendMatch);
        } else if (operation.isRecv()) {
            Expr recvMatch = null;
            if (operation.isCsecOperation) recvMatch = mkCsecRecvMatch(operation);
            else recvMatch = mkRecvMatch(operation);
            if(recvMatch!=null) return ctx.mkImplies(complete(operation), recvMatch);
        }
        return ctx.mkImplies(complete(operation), ctx.mkFalse());//if there is no match operation, so return false ???
    }

    Expr mkNonOvertacking(Operation a, Operation b) {
        addExprToList("(NonOvertacking):" +a.getStrInfo()+" <m "+b.getStrInfo());
        Expr matchA = null;
        Expr matchB = null;
        if (a.isCsecOperation) matchA = getMaxMatch(a);
        else matchA = match(a);
        if (b.isCsecOperation) matchB = getMinMatch(b);
        else matchB = match(b);
        return ctx.mkLt(matchA, matchB);
    }

    Expr mkNearstWait(Operation operation, Operation wait) {
        addExprToList(""+operation.getStrInfo()+".wait = "+wait.getStrInfo()+".time");
        return ctx.mkEq(wait(operation), time(wait));
    }

    Expr mkBarrGroup(Operation barr1, Operation barr2) {
        addExprToList(barr1.getStrInfo()+".time = "+barr2.getStrInfo()+".time");
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
            if (operation.isRecv() || operation.isSend() ) {
                if (operation.isCsecOperation) matches[rank] = getMaxMatch(operation);
                else matches[rank] = match(operation);
                rank += 1;
            }
        }
        return ctx.mkDistinct(matches);
    }

    BoolExpr mkUniqueTimesandMatches(CsecOperation operation){
        BoolExpr b = null;
        for (int i = 1; i < operation.OperationList.size(); i++) {
            BoolExpr a = ctx.mkAnd(ctx.mkLt(ctx.mkSelect(time(operation),ctx.mkInt(i)), ctx.mkSelect(time(operation),ctx.mkInt(i-1))),
            ctx.mkLt(ctx.mkSelect(match(operation),ctx.mkInt(i)), ctx.mkSelect(match(operation),ctx.mkInt(i-1))));
            b = (b != null) ? ctx.mkAnd(b,a) : a;
        }
        return b;
    }

    void addExprToList(String strExpr){
        ExprList.add(strExpr);
    }

    public void printAllExprs(){
        for(String str : ExprList){
            System.out.println(str);
        }
    }

    public void displayExprs(){
        for (BoolExpr expr : solver.getAssertions()){
            System.out.println(expr);
        }
    }

    void updateLastRank(){
        for (int i = 0; i < candidate.tracker.length; i++) {
            lastRank[i] = program.get(i).getOP(candidate.tracker[i]).rank;
        }
    }

    void printLastRank(){
        for (int i = 0; i < candidate.tracker.length; i++) {
            System.out.print(" "+lastRank[i]+" ");
        }
        System.out.println("\n");
    }

    public void printActs(){
        System.out.println(acts);
    }


}

