package syntax;

import constant.OPTypeEnum;
import javafx.util.Pair;
import methods.HBRelations;
import methods.MatchOrder;
import methods.MatchPairs;
import prework.CmpFile;

import java.util.LinkedList;

import java.util.*;

/*
 * a mpi program has n processes and each process has some actions
 * and a program has match-pairs and hb-relations these are both a pair <a,b>
 * so the class should achieve:
 *       1.initialize the MPI program
 *           read the ctp from file, create objects, and add those to the program( add the extra action bot)
 *       2.construct the sufficiently small over-approximated set of match-pairs
 *       3.construct the partial order based on happens-before relation
 */
public class Program {
    public ArrayList<Process> processes;
    public Hashtable<Operation, LinkedList<Operation>> matchTables;//all matches like: <r,list<s>>
    public Hashtable<Operation, LinkedList<Operation>> matchTablesForS;//all matches like: <s,list<r>>

    public Hashtable<Operation, Set<Operation>> matchOrderTables;

    public Hashtable<Pair<Integer, Integer>, LinkedList<Operation>> sendqs;
    public Hashtable<Pair<Integer, Integer>, LinkedList<Operation>> recvqs;

    public Hashtable<Integer, LinkedList<Operation>> groups;

    public boolean checkInfiniteBuffer = true;

    public Program(String filepath) {
        //初始化variables！！！
        processes = new ArrayList<>();
        sendqs = new Hashtable<>();
        recvqs = new Hashtable<>();
        groups = new Hashtable<>();
        matchTables = new Hashtable<>();
        matchTablesForS = new Hashtable<>();
        matchOrderTables = new Hashtable<>();
        initializeProgramFromCTP(filepath);
        setMatchTables();
        setMatchOrderTables();
        System.out.println("[PROGRAM]:FINISH INIT THE MPI PROGRAM.");

    }

    public Program(){
        processes = new ArrayList<>();
        sendqs = new Hashtable<>();
        recvqs = new Hashtable<>();
        groups = new Hashtable<>();
        matchTables = new Hashtable<>();
        matchTablesForS = new Hashtable<>();
        matchOrderTables = new Hashtable<>();
    }

    /**
     * from the file, we create the operations, add them to the process, and add the process to the program
     * after finishing to append all operations, we should complete their info which conclude wait, req, index, rank, indx;
     * we use the appendOpToQS(), cmpOPsInfo() function to achieve these
     *
     * @param: filepath
     */
    private void initializeProgramFromCTP(String filepath) {
        LinkedList<String[]> ctp = CmpFile.getCTPFromFile(filepath);
        ListIterator<String[]> listIterator = ctp.listIterator(0);
        String[] aStr;
        while (listIterator.hasNext()) {
            aStr = listIterator.next();
            Operation operation = CmpFile.translateFromStrToOP(aStr);
            if (operation != null) {
                appendOpToQS(operation);
                if (processes.size() <= operation.proc) {
                    Process process = new Process(operation.proc);
                    operation.rank = process.ops.size();
                    process.append(operation);
                    processes.add(process);
                } else {
                    operation.rank = processes.get(operation.proc).Size();
                    processes.get(operation.proc).append(operation);
                }
                if (operation.isBarrier()) {
                    if (!groups.containsKey(operation.group)) groups.put(operation.group, new LinkedList<Operation>());
                    groups.get(operation.group).add(operation);
                }
            }//if(op!=null)
        }//while
        cmpOPsInfo();
    }

    void appendOpToQS(Operation operation) {
        if (operation.isSend()) {
            if (!sendqs.containsKey(operation.getHashCode()))
                sendqs.put(operation.getHashCode(), new LinkedList<Operation>());
            sendqs.get(operation.getHashCode()).add(operation);
        } else if (operation.isRecv() || operation.isIRecv()) {
            if (!recvqs.containsKey(operation.getHashCode()))
                recvqs.put(operation.getHashCode(), new LinkedList<Operation>());
            recvqs.get(operation.getHashCode()).add(operation);
        }
    }

    void cmpOPsInfo() {
        int indx = 0;
        for (Process process : processes) {
            for (Operation operation : process.ops) {
                operation.indx = indx;
                indx++;
                operation.rank = process.ops.indexOf(operation);
                if (operation.type == OPTypeEnum.WAIT) {
                    operation.req = process.ops.get(operation.reqID);
                    process.ops.get(operation.reqID).Nearstwait = operation;
                }
                if (operation.isRecv()) {
                    operation.index = recvqs.get(operation.getHashCode()).indexOf(operation);
                }
                if (operation.isSend()) {
                    operation.index = sendqs.get(operation.getHashCode()).indexOf(operation);
                }
            }
            indx += 2;
        }
    }

    /**
     * generate the Match Pairs,
     * MatchTables: <Recv, <Send,...>> ,....
     * MatchTablesForS: <Send, <Recv,...>> ,....
     */
    void setMatchTables() {
        MatchPairs matchPairs = new MatchPairs(this);
        this.matchTables = matchPairs.matchTables;
        if (!matchTables.isEmpty()) setMatchTablesForS();
    }

    void setMatchOrderTables(){
        MatchOrder matchOrder = new MatchOrder(this);
        this.matchOrderTables = matchOrder.MatchOrderTables;
    }

    void setMatchTablesForS() {
        matchTablesForS = new Hashtable<Operation, LinkedList<Operation>>();
        for (Operation recv : matchTables.keySet()) {
            for (Operation send : matchTables.get(recv)) {
                if (!matchTablesForS.containsKey(send)) matchTablesForS.put(send, new LinkedList<Operation>());
                matchTablesForS.get(send).add(recv);
            }
        }
    }

    /**
     * public functions:
     */
    public Process get(int i) {
        return processes.get(i);
    }

    public int getSize() {
        return processes.size();
    }

    public ArrayList<Process> getAllProcesses() {
        return processes;
    }

    public void printMatchPairs() {
        System.out.println("[MATCH-PAIRS]: MATCH PAIRS IS SHOWN AS FOLLOWING :");
        for (Operation R : matchTables.keySet()) {
            for (Operation S : matchTables.get(R)) {
                System.out.println("<" + R.getStrInfo() + ", " + S.getStrInfo() + ">");
            }
        }
    }

    public void printOps() {
        System.out.println("[PROGRAM]: ALL THE OPERATIONS ARE : ");
        for (Process process : processes) {
            System.out.println("Process " + process.rank);
            for (Operation operation : process.ops) {
                System.out.println("   " + operation.getStrInfo());
            }
        }

    }

    public void printALLOperations() {
        System.out.println("[PROGRAM]: the program:");

        for (Process process : processes) {
            System.out.println("TYPE P D I");
            for (Operation operation : process.ops) {
                if (operation.isSend()) {
                    System.out.println(operation.type + " " + operation.proc + " " + operation.dst + " "
                            + operation.rank);
                } else if (operation.isRecv()) {
                    System.out.println(operation.type + " " + operation.proc + " " + operation.src + " "
                            + operation.rank);
                } else if (operation.isWait()) {
                    System.out.println(operation.type + " " + operation.proc + " " + operation.req.rank + " " + operation.rank);
                } else if (operation.isBarrier()) {
                    System.out.println(operation.type + " " + operation.proc + " " + operation.rank + " " + operation.group);
                }
            }
            System.out.println(" ");
        }
    }

    public boolean isCheckInfiniteBuffer() {
        return checkInfiniteBuffer;
    }

    public static void main(String[] args) {
        Program program = new Program("./src/test/fixtures/monte4.txt");

//        program.printALLOperations();
        program.printMatchPairs();
    }


}
