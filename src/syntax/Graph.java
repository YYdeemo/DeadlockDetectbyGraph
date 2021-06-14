package syntax;

import constant.Constants;
import constant.OPTypeEnum;

import java.util.*;

public class Graph {
    /**
     *
     */
    public Program program;
    public Vector<Operation> VList;
    public Hashtable<Operation, LinkedList<Operation>> ETable;

    Operation[] bot;
    Operation[] barr;

//    public int VCount = 0;
    public int ECount = 0;

    public Graph(Program program) {
        this.program = program;
        VList = new Vector<Operation>();
        ETable = new Hashtable<Operation, LinkedList<Operation>>();
        bot = new Operation[program.getSize()];
        barr = new Operation[program.getSize()];
        if (program instanceof NewProgram)
            initGraphNew();
        else
            initGraph();
//        System.out.println("[GRAPH]: FINISH INIT GRAPH FROM A PROGRAM");
        Collections.sort(VList);
        for(Operation op : ETable.keySet()){
            Collections.sort(ETable.get(op));
        }


    }

    public void initGraph() {

        //generate BOT nodes for all processes
        addExtraVertex();

        //generate all edges for the graph
        for(Operation operation : program.matchOrderTables.keySet()){
            for(Operation lop : program.matchOrderTables.get(operation)){
                if(!ETable.containsKey(operation)) ETable.put(operation, new LinkedList<>());
                if(!ETable.get(operation).contains(lop)){
                    ETable.get(operation).add(lop);
                    ECount += 1;
                }
            }
        }


        //Edges generate rules:
        for (Process process : program.processes) {
            //add all vertices_ and partial hb relations from each process
            VList.addAll(process.ops);

            for (Operation op : process.ops)//rule 1: for each op : op-->bot
            {
                if (!ETable.containsKey(op))
                    ETable.put(op, new LinkedList<>());
                if(!ETable.get(op).contains(bot[process.rank])){
                    ETable.get(op).add(bot[process.rank]);
                    ECount += 1;
                }
            }
//            the last MPI operation should add the Edge to the nocomm action
            if (process.ops.size() > 0) {
                if (!ETable.containsKey(process.ops.getLast()))
                    ETable.put(process.ops.getLast(), new LinkedList<>());
                if(!ETable.get(process.ops.getLast()).contains(barr[process.rank])) {
                    ETable.get(process.ops.getLast()).add(barr[process.rank]);
                    ECount += 1;
                }
            }
//            the nocomm -> the bot
            ETable.put(barr[process.rank], new LinkedList<>());
            if (!ETable.get(barr[process.rank]).contains(bot[process.rank])) ETable.get(barr[process.rank]).add(bot[process.rank]);
            ECount += 1;
            if(!ETable.containsKey(bot[process.rank])) ETable.put(bot[process.rank],new LinkedList<>());

            for (int i = 0; i < process.ops.size(); i++) {//rule 2: bot --> deterministic recv (follow wildcard recv)
                Operation op1 = process.ops.get(i);
                if (op1.isRecv() && op1.src == -1) {
                    for (int j = i + 1; j < process.ops.size(); j++) {
                        Operation op2 = process.ops.get(j);
                        if (op2.isRecv() && op2.src != -1)
                        {
                            int src = op2.src; //the source for the deterministic receive a
                            if (!ETable.containsKey(bot[src]))
                                ETable.put(bot[src], new LinkedList<>());
                            if(!ETable.get(bot[src]).contains(op2)) {
                                ETable.get(bot[src]).add(op2);
                                ECount += 1;
                            }
                        }
                    }
                }
            }

            for (Operation op1 : process.ops) {//rule 3: bot --> Send (Send.dst.process has wildcard recv)
                if (op1.isSend()) {
                    int dest = op1.dst;
                    for (Operation op2 : program.processes.get(dest).ops) {
                        if (op2.isRecv() && op2.src == -1) {
                            if (!ETable.containsKey(bot[dest]))
                                ETable.put(bot[dest], new LinkedList<>());
                            if (!ETable.get(bot[dest]).contains(op1)) {
                                ETable.get(bot[dest]).add(op1);
                                ECount += 1;
                            }
                        }
                    }
                }
            }
        }
        // match-pair:<r,s>   r --> s and s --> r
        addEdgesOfMatchPairs();
        //for the barriers in prim MPI program
        HashSet<Operation> barrs = new HashSet<Operation>();//this barrs save the noComm operations
        for(Integer groupID : program.groups.keySet()){
            barrs.clear();
            for(Operation barrier : program.groups.get(groupID)){
                if(true)
                    barrs.add(barrier);
            }
            for(Operation srcBarrier : program.groups.get(groupID)){
                for(Operation snkBarrier : barrs){
                    if(!snkBarrier.equals(srcBarrier)){
                        if(!ETable.containsKey(srcBarrier)) ETable.put(srcBarrier,new LinkedList<>());
                        if (!ETable.get(srcBarrier).contains(snkBarrier)) {
                            ETable.get(srcBarrier).add(snkBarrier);
                            ECount += 1;
                        }
                    }
                }
            }
        }
        //for the extra barriers
        barrs.clear();
        for (Operation barrier : barr) {
            if (can_reach_outgoing(barrier))
                barrs.add(barrier);
        }
        for (Operation srcBarrier : barr) {
            for (Operation snkBarrier : barrs) {
                if (!snkBarrier.equals(srcBarrier)) {
                    if (!ETable.containsKey(srcBarrier)) ETable.put(srcBarrier, new LinkedList<>());
                    if (!ETable.get(srcBarrier).contains(snkBarrier)){
                        ETable.get(srcBarrier).add(snkBarrier);
                        ECount += 1;
                    }
                }
            }
        }
    }

    void initGraphNew(){
        NewProgram newprogram = (NewProgram) program;
        //generate BOT nodes for all processes
        addExtraVertex();

        //generate all edges for the graph
        for(Operation operation : program.matchOrderTables.keySet()){
            if (operation.isWait() && newprogram.inCsecTables(operation.req) && !newprogram.isCsecWait(operation)) continue;
            for(Operation lop : program.matchOrderTables.get(operation)){
                if (lop.isWait() && newprogram.inCsecTables(lop.req) && !newprogram.isCsecWait(lop)) continue;
                operation = newprogram.getCsecOp(operation);
                lop = newprogram.getCsecOp(lop);
                if(!ETable.containsKey(operation)) ETable.put(operation, new LinkedList<>());
                if(!ETable.get(operation).contains(lop) && operation!=lop){
                    ETable.get(operation).add(lop);
                    ECount += 1;
                }
            }
        }


        //Edges generate rules:
        for (Process process : program.processes) {
            //add all vertices_ and partial hb relations from each process and each op to bot
            for (Operation operation: process.ops){
//                if (operation.isWait() && newprogram.inCsecTables(operation.req) && !newprogram.isCsecWait(operation)) continue;
//                operation = newprogram.getCsecOp(operation);
                if (!VList.contains(operation)) VList.add(operation);
                if (!ETable.containsKey(operation)) ETable.put(operation, new LinkedList<>());
                if (!ETable.get(operation).contains(bot[process.rank])){
                    ETable.get(operation).add(bot[process.rank]);
                    ECount += 1;
                }
            }

//            the last MPI operation should add the Edge to the nocomm action
            if (process.ops.size() > 0) {
                Operation lastOp = process.getOP(process.Size()-1);
//                lastOp = newprogram.getCsecOp(lastOp);
                if(!ETable.get(lastOp).contains(barr[process.rank])) {
                    ETable.get(lastOp).add(barr[process.rank]);
                    ECount += 1;
                }
            }

//            the nocomm -> the bot
            ETable.put(barr[process.rank], new LinkedList<>());
            if(!ETable.get(barr[process.rank]).contains(bot[process.rank]))ETable.get(barr[process.rank]).add(bot[process.rank]);
            ECount += 1;

            for (int i = 0; i < process.ops.size(); i++) {//rule 2: bot --> deterministic recv (follow wildcard recv)
                Operation op1 = process.ops.get(i);
                if (op1.isRecv() && op1.src == -1) {
                    for (int j = i + 1; j < process.ops.size(); j++) {
                        Operation op2 = process.ops.get(j);
                        if (op2.isRecv() && op2.src != -1)
                        {
//                            op2 = newprogram.getCsecOp(op2);
                            int src = op2.src; //the source for the deterministic receive a
                            if (!ETable.containsKey(bot[src]))
                                ETable.put(bot[src], new LinkedList<>());
                            if(!ETable.get(bot[src]).contains(op2)) {
                                ETable.get(bot[src]).add(op2);
                                ECount += 1;
                            }
                        }
                    }
                }
            }

            for (Operation op1 : process.ops) {//rule 3: bot --> Send (Send.dst.process has wildcard recv)
                if (op1.isSend()) {
                    int dest = op1.dst;
                    for (Operation op2 : program.processes.get(dest).ops) {
                        if (op2.isRecv() && op2.src == -1) {
//                            op1 = newprogram.getCsecOp(op1);
                            if (!ETable.containsKey(bot[dest]))
                                ETable.put(bot[dest], new LinkedList<>());
                            if (!ETable.get(bot[dest]).contains(op1)) {
                                ETable.get(bot[dest]).add(op1);
                                ECount += 1;
                            }
                        }
                    }
                }
            }
        }
        // match-pair:<r,s>   r --> s and s --> r
        Hashtable<Operation, LinkedList<Operation>> match_table = program.matchTables;
        for (Operation recv : match_table.keySet()){
            for (Operation send : match_table.get(recv)){
                recv = newprogram.getCsecOp(recv);
                send = newprogram.getCsecOp(send);
                if (!ETable.containsKey(recv)) ETable.put(recv, new LinkedList<>());
                if (!ETable.containsKey(send)) ETable.put(send, new LinkedList<>());
                if (!ETable.get(recv).contains(send)){
                    ETable.get(recv).add(send);
                    ECount += 1;
                }
                if (!ETable.get(send).contains(recv)) {
                    ETable.get(send).add(recv);
                    ECount += 1;
                }

            }
        }
        //for the barriers in prim MPI program
        HashSet<Operation> barrs = new HashSet<Operation>();//this barrs save the noComm operations
        for(Integer groupID : program.groups.keySet()){
            barrs.clear();
            for(Operation barrier : program.groups.get(groupID)){
                if(true)
                    barrs.add(barrier);
            }
            for(Operation srcBarrier : program.groups.get(groupID)){
                for(Operation snkBarrier : barrs){
                    if(!snkBarrier.equals(srcBarrier)){
                        if(!ETable.containsKey(srcBarrier)) ETable.put(srcBarrier,new LinkedList<>());
                        if (!ETable.get(srcBarrier).contains(snkBarrier)) {
                            ETable.get(srcBarrier).add(snkBarrier);
                            ECount += 1;
                        }
                    }
                }
            }
        }
        //for the extra barriers
        barrs.clear();
        for (Operation barrier : barr) {
            if (can_reach_outgoing(barrier))
                barrs.add(barrier);
        }
        for (Operation srcBarrier : barr) {
            for (Operation snkBarrier : barrs) {
                if (!snkBarrier.equals(srcBarrier)) {
                    if (!ETable.containsKey(srcBarrier)) ETable.put(srcBarrier, new LinkedList<>());
                    if (!ETable.get(srcBarrier).contains(snkBarrier)){
                        ETable.get(srcBarrier).add(snkBarrier);
                        ECount += 1;
                    }
                }
            }
        }
    }

    void addExtraVertex(){
        for(Process process : program.getAllProcesses()){
            int indx = process.getOP(process.ops.size()-1).indx;
            int rank = process.getOP(process.ops.size()-1).rank;
            barr[process.rank]
                    = new Operation(OPTypeEnum.BARRIER, rank+1, indx+1, process.rank, Constants.gourpID);
            VList.add(barr[process.rank]);
            bot[process.rank]
                    = new Operation(OPTypeEnum.BOT, rank+2, indx+2,  process.rank);
            VList.add(bot[process.rank]);
        }


    }

    void addEdgesOfMatchPairs(){
        Hashtable<Operation, LinkedList<Operation>> match_table = program.matchTables;
        continuepoint:
        for (Operation op : VList) {
            if (!(op.isRecv()|| op.isIRecv())) {
                continue continuepoint;
            }

            if (!match_table.containsKey(op)) {
                continue continuepoint;
            }

            if (!ETable.containsKey(op))
                ETable.put(op, new LinkedList<>());

            continuepoint1:
            for (Operation s : match_table.get(op)) {
                if (!VList.contains(s)) continue continuepoint1;
                if (!ETable.get(op).contains(s)){
                    ETable.get(op).add(s);
                    ECount += 1;
                }
                if (!ETable.containsKey(s)) ETable.put(s, new LinkedList<>());
                if (!ETable.get(s).contains(op)){
                    ETable.get(s).add(op);
                    ECount += 1;
                }
            }
        }
    }

    public boolean can_reach_outgoing(Operation barr) {
        if (!ETable.containsKey(barr))
            return false;
        for (Operation destOp : ETable.get(barr)) {
            if (destOp.proc != barr.proc) {
                return true;
            }
        }

        for (Operation destOp : ETable.get(barr)) {
            if (can_reach_outgoing(destOp))
                return true;
        }
        return false;
    }

    public int getVSize(){
        return VList.size();
    }

    public LinkedList<Operation> adj(Operation v)
    {
        if(!ETable.containsKey(v))
            return null;
        return ETable.get(v);
    }

    public int getEdgeNum(){
        int edgeNum = 0;
        for (Operation op : ETable.keySet()) {
            edgeNum += ETable.get(op).size();
        }
        return edgeNum;
    }

    public void printGraphVList(){
        System.out.println("[GRAPH]: ALL THE VERTEXES ARE AS FOLLOWS:");
        for(Operation v : VList){
            System.out.print(v.getStrInfo()+"_"+v.indx+" ");
        }
        System.out.println(" ");

    }
    public void printGraphETable(){
        System.out.println("[GRAPH]: THE GRAPH IS LIKE THIS:");
        List<Operation> keylist = new ArrayList<>(ETable.keySet());
        Collections.sort(keylist);
        for(Operation v : keylist){
            System.out.println(v.getStrInfo()+"-->");
            for(Operation w : ETable.get(v)){
                System.out.print(" "+w.getStrInfo()+"  ");
            }
            System.out.println(" ");
        }
    }

    public int getECount() {
        return ECount;
    }

    public int getVCount() {
        return VList.size();
    }

    public static void main(String[] args) {
//        Program program = new Program("./src/test/fixtures/diffusion2d4.txt",false);
//        Graph graph1 = new Graph(program);
//        graph1.printGraphETable();
        NewProgram newprogram = new NewProgram("./src/test/fixtures/diffusion2d4.txt",false);
//        newprogram.printCsecOps();
        Graph graph = new Graph(newprogram);
        graph.printGraphVList();
        graph.printGraphETable();
    }






}
