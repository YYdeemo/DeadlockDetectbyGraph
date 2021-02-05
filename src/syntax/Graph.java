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
    public Hashtable<Operation, Set<Operation>> ETable;

    Operation[] bot;
    Operation[] barr;

    public Graph(Program program) {
        this.program = program;
        VList = new Vector<Operation>();
        ETable = new Hashtable<Operation, Set<Operation>>();
        bot = new Operation[program.getSize()];
        barr = new Operation[program.getSize()];
        initGraph();
    }

    public void initGraph() {

        //generate BOT nodes for all processes
        addBotVertex();

        //generate all edges for the graph
        ETable.putAll(program.matchOrder.MatchOrderTables);

        //Edges generate rules:
        for (Process process : program.processArrayList) {
            //add all vertices_ and partial hb relations from each process
            VList.addAll(process.ops);

            for (Operation op : process.ops)//rule 1: for each op : op-->bot
            {
                if (!ETable.containsKey(op))
                    ETable.put(op, new HashSet<Operation>());
                ETable.get(op).add(bot[process.rank]);
            }
//            the last MPI operation should add the Edge to the nocomm action
            if (process.ops.size() > 0) {
                if (!ETable.containsKey(process.ops.getLast()))
                    ETable.put(process.ops.getLast(), new HashSet<Operation>());
                ETable.get(process.ops.getLast()).add(barr[process.rank]);
            }
//            the nocomm -> the bot
            ETable.put(barr[process.rank], new HashSet<Operation>());
            ETable.get(barr[process.rank]).add(bot[process.rank]);

            for (int i = 0; i < process.ops.size(); i++) {//rule 2: bot --> deterministic recv (follow wildcard recv)
                Operation op1 = process.ops.get(i);
                if (op1.isRecv() && op1.src == -1) {
                    for (int j = i + 1; j < process.ops.size(); j++) {
                        Operation op2 = process.ops.get(j);
                        if (op2.isRecv() && op2.src != -1)
                        {
                            int src = op2.src; //the source for the deterministic receive a
                            if (!ETable.containsKey(bot[src]))
                                ETable.put(bot[src], new HashSet<Operation>());
                            ETable.get(bot[src]).add(op2);
                        }
                    }
                }
            }

            for (Operation op1 : process.ops) {//rule 3: bot --> Send (Send.dst.process has wildcard recv)
                if (op1.isSend()) {
                    int dest = op1.dst;
                    for (Operation op2 : program.processArrayList.get(dest).ops) {
                        if (op2.isRecv() && op2.src == -1) {
                            if (!ETable.containsKey(bot[dest]))
                                ETable.put(bot[dest], new HashSet<Operation>());
                            ETable.get(bot[dest]).add(op1);
                        }
                    }
                }
            }
        }

        //determine if the final barriers can have incoming edges
        HashSet<Operation> barrs = new HashSet<Operation>();//this barrs save the noComm operations
        for (Operation barrier : barr) {
            if (can_reach_outgoing(barrier)) {
                barrs.add(barrier);
            }
        }

//        the nocomm -> other nocomm
        for (Operation srcBarr : barr) {
            if (!ETable.containsKey(srcBarr))
                ETable.put(srcBarr, new HashSet<Operation>());
            for (Operation destBarr : barrs) {
                if (!srcBarr.equals(destBarr)) {
                    ETable.get(srcBarr).add(destBarr);
                }
            }
        }
        // match-pair:<r,s>   r --> s and s --> r
        addEdgesOfMatchPairs();
    }

    void addBotVertex(){
        for(Process process : program.getAllProcesses()){
            barr[process.rank]
                    = new Operation(OPTypeEnum.BARRIER, process.Size(), process.rank, Constants.gourpID);
            VList.add(barr[process.rank]);
        }

        for (Process process : program.processArrayList) {
            bot[process.rank]
                    = new Operation(OPTypeEnum.BOT, process.ops.size()+1, process.rank);
            VList.add(bot[process.rank]);
        }
    }

    void addEdgesOfMatchPairs(){
        Hashtable<Operation, LinkedList<Operation>> match_table = program.matchTables;
        continuepoint:
        for (Operation op : VList) {
            if (!(op.isRecv())) {
                continue continuepoint;
            }

            if (!match_table.containsKey(op)) {
                continue continuepoint;
            }

            if (!ETable.containsKey(op))
                ETable.put(op, new HashSet<Operation>());

            continuepoint1:
            for (Operation s : match_table.get(op)) {
                if (!VList.contains(s)) {
                    continue continuepoint1;
                }
                ETable.get(op).add(s);
                if (!ETable.containsKey(s)) //add reversed match relation
                    ETable.put(s, new HashSet<Operation>());
                ETable.get(s).add(op);
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

    public Set<Operation> adj(Operation v)
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




}
