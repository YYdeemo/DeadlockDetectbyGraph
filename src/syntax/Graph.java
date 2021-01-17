package syntax;

import constant.OPTypeEnum;

import java.util.*;

public class Graph {
    /**
     * this class is a graph which has features as follow:
     * program
     * nodes
     * vectors
     * function:
     * initGraph()
     *
     *
     */
    public Program program;
    public Vector<Operation> VList;
    public Hashtable<Operation, Set<Operation>> ETable;

    public Graph(Program program) throws Exception {
        this.program = program;
        VList = new Vector<Operation>();
        ETable = new Hashtable<Operation, Set<Operation>>();
        generateGraph_all_Imp();
    }

    public void generateGraph_all_Imp() throws Exception
    {

        //generate NOCOMM and BOT nodes for all processes
        Operation[] nocomm = new Operation[program.processArrayList.size()];
        for(Process process:program.processArrayList)
        {
            nocomm[program.processArrayList.indexOf(process)]
                    = new Operation(OPTypeEnum.NOCOMM, process.ops.size(), process.rank);
            VList.add(nocomm[program.processArrayList.indexOf(process)]);
        }


        Operation[] bot = new Operation[program.processArrayList.size()];
        for(Process process:program.processArrayList)
        {
            bot[program.processArrayList.indexOf(process)]
                    = new Operation(OPTypeEnum.BOT,process.ops.size()+1, process.rank);
            VList.add(bot[program.processArrayList.indexOf(process)]);
        }

        ETable.putAll(program.HBTables);

        for(Process process: program.processArrayList)
        {
            //add all vertices_ and partial hb relations from each process
            VList.addAll(process.ops);

            for(Operation op : process.ops)//rule 5
            {
                if(!ETable.containsKey(op))
                    ETable.put(op, new HashSet<Operation>());
                ETable.get(op).add(bot[program.processArrayList.indexOf(process)]);
            }

            if(process.ops.size() > 0)
            {
                if(!ETable.containsKey(process.ops.getLast()))
                    ETable.put(process.ops.getLast(), new HashSet<Operation>());
                ETable.get(process.ops.getLast()).add(nocomm[process.rank]);
            }

            ETable.put(nocomm[process.rank], new HashSet<Operation>());
            ETable.get(nocomm[process.rank]).add(bot[process.rank]);

            //rule3
            for(int i = 0; i < process.ops.size(); i++)
            {
                Operation op1 = process.ops.get(i);
                if(op1.isRecv() && op1.src==-1)
                {
                    for(int j = i+1; j < process.ops.size(); j++)
                    {
                        Operation op2 = process.ops.get(j);
                        if( op2.isRecv() && op2.src != -1) //rule 3
                        {
                            int src = op2.src; //the source for the deterministic receive a

                            if(!ETable.containsKey(bot[src]))
                                ETable.put(bot[src], new HashSet<Operation>());
                            ETable.get(bot[src]).add(op2);
                        }
                    }
                }
            }

            //rule 4
            for(Operation op1 : process.ops)
            {
                if(op1.isSend())
                {
                    int dest = op1.dst;
                    for(Operation op2 : program.processArrayList.get(dest).ops)
                    {
                        if(op2.isRecv() && op2.src == -1)
                        {
                            if(!ETable.containsKey(bot[dest]))
                                ETable.put(bot[dest], new HashSet<Operation>());
                            ETable.get(bot[dest]).add(op1);
                        }
                    }
                }
            }
        }

        //determine if the final barriers can have incoming edges
        HashSet<Operation> barrs = new HashSet<Operation>();//this barrs save the noComm operations
        for(Operation barr: nocomm)
        {
            if(can_reach_outgoing(barr))
            {
                //System.out.println("[final barrier]:" + barr);
                barrs.add(barr);
            }
        }


        for(Operation srcBarr : nocomm)
        {
            if(!ETable.containsKey(srcBarr))
                ETable.put(srcBarr, new HashSet<Operation>());
            for(Operation destBarr : barrs)
            {
                if(!srcBarr.equals(destBarr)){
                    ETable.get(srcBarr).add(destBarr);
                }
            }
        }

        //add both match relation and reversed match relation as edges for vertices
//        program.generateMatch();
        //program.displayMatch();
        Hashtable<Operation, LinkedList<Operation>> match_table = program.matchTables;
        continuepoint:
        for(Operation op : VList)
        {
            if(!(op.isRecv()))
            {
                continue continuepoint;
            }

            if(!match_table.containsKey(op))
            {
                continue continuepoint;
            }

            if(!ETable.containsKey(op))
                ETable.put(op, new HashSet<Operation>());

            continuepoint1:
            for(Operation s : match_table.get(op))
            {
                if(!VList.contains(s))
                {
                    continue continuepoint1;
                }
                ETable.get(op).add(s);
                if(!ETable.containsKey(s)) //add reversed match relation
                    ETable.put(s, new HashSet<Operation>());
                ETable.get(s).add(op);
            }
        }
    }

    public boolean can_reach_outgoing(Operation barr)
    {
        if(!ETable.containsKey(barr))
            return false;
        for(Operation destOp : ETable.get(barr))
        {
            if(destOp.proc != barr.proc)
            {
                return true;
            }
        }

        for(Operation destOp : ETable.get(barr))
        {
            if(can_reach_outgoing(destOp))
                return true;
        }

        return false;
    }


}
