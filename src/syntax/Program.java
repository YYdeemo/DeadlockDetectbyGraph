package syntax;

import constant.OPTypeEnum;
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
    public ArrayList<Process> processArrayList;
    public Hashtable<Operation, LinkedList<Operation>> matchTables;//all matches like: <r.s>
    public Hashtable<Operation, LinkedList<Operation>> matchTablesForS;//all matches like: <s,r>

    public MatchOrder matchOrder;

    public Hashtable<Integer, LinkedList<Operation>> sendqs;
    public Hashtable<Integer, LinkedList<Operation>> recvqs;

    public Hashtable<Integer, LinkedList<Operation>> groups;

    public boolean checkInfiniteBuffer = true;

    public Program(String filepath) {
        //初始化variables！！！
        processArrayList = new ArrayList<>();
        sendqs = new Hashtable<>();
        recvqs = new Hashtable<>();
        groups = new Hashtable<>();
        initializeProgramFromCTP(filepath);
        setMatchTables();
        matchOrder = new MatchOrder(this);
//        matchOrder.printOrderRelation();
        System.out.println("[PROGRAM]:FINISH INIT THE MPI PROGRAM.");


    }

    /*
     *from the file, we create the operations, add them to the process, and add the process to the program
     *param: filepath
     */
    private void initializeProgramFromCTP(String filepath) {
        LinkedList<String[]> ctp = CmpFile.getCTPFromFile(filepath);
        ListIterator<String[]> listIterator = ctp.listIterator(0);
        String[] aStr;
        while (listIterator.hasNext()) {
            aStr = listIterator.next();
            Operation operation = CmpFile.translateFromStrToOP(aStr);
            if (operation != null) {
                if (processArrayList.size() <= operation.proc) {
                    Process process = new Process(operation.proc);
                    operation.rank = process.ops.size();
                    process.append(operation);
                    processArrayList.add(process);
                } else {
                    operation.rank = processArrayList.get(operation.proc).Size();
                    processArrayList.get(operation.proc).append(operation);
                }
                if(operation.isBarrier()){
                    if(!groups.containsKey(operation.group)) groups.put(operation.group,new LinkedList<Operation>());
                    groups.get(operation.group).add(operation);
                }
            }//if(op!=null)
        }//while
        cmpOPsInfo();
    }

    void appendOpToQS(Operation operation){
        if(operation.isSend()){
            if(!sendqs.containsKey(operation.getHashCode())) sendqs.put(operation.getHashCode(), new LinkedList<Operation>());
            sendqs.get(operation.getHashCode()).add(operation);
        }else if(operation.isRecv()){
            if(!recvqs.containsKey(operation.getHashCode())) recvqs.put(operation.getHashCode(), new LinkedList<Operation>());
            recvqs.get(operation.getHashCode()).add(operation);
        }
    }
    /*
     *   after initialize the program, we have added all the actions to the program,
     *   but not finish the work of completing the info of the actions
     *   such as each action's rank ***********
     *   the wait action's req and the blocking recv action's nearliest wait is which one
     */
    private void cmpOPsInfo() {
        for (Process process : processArrayList) {
            for (Operation operation : process.ops) {
                operation.rank = process.ops.indexOf(operation);
                if (operation.type == OPTypeEnum.WAIT) {
                    operation.req = process.ops.get(operation.reqID);
                    process.ops.get(operation.reqID).Nearstwait = operation;
                }
                if (operation.isRecv()) {
                    operation.index = process.rlist.indexOf(operation);
                }
                if (operation.isSend()) {
                    operation.index = process.slist.indexOf(operation);
                }
            }
        }
    }

    public Process get(int i){
        return processArrayList.get(i);
    }

    public int getSize() {
        return processArrayList.size();
    }

    public ArrayList<Process> getAllProcesses() {
        return processArrayList;
    }

    public void setMatchTables() {
        MatchPairs matchPairs = new MatchPairs(this);
        this.matchTables = matchPairs.matchTables;
        if(!matchTables.isEmpty()) setMatchTablesForS();
    }

    public void setMatchTablesForS() {
        matchTablesForS = new Hashtable<Operation, LinkedList<Operation>>();
        for(Operation recv : matchTables.keySet()){
            for(Operation send : matchTables.get(recv)){
                if(!matchTablesForS.containsKey(send)) matchTablesForS.put(send, new LinkedList<Operation>());
                matchTablesForS.get(send).add(recv);
            }
        }
    }

    public void printMatchPairs() {
        System.out.println("[MATCH-PAIRS]: MATCH PAIRS IS SHOWN AS FOLLOWING :");
        for (Operation R : matchTables.keySet()) {
            for (Operation S : matchTables.get(R)) {
                System.out.println("<R"+ R.proc+"_"+ R.index + ", S" + S.proc + "_" + S.index + ">");
            }
        }
    }

    public void printOps(){
        System.out.println("[PROGRAM]: ALL THE OPERATIONS ARE : ");
        for(Process process : processArrayList){
            System.out.println("Process "+process.rank);
            for(Operation operation : process.ops){
                System.out.println("   "+operation.getStrInfo());
            }
        }

    }
    public void printALLOperations(){
        System.out.println("[PROGRAM]: the program:");


        for(Process process : processArrayList){
            System.out.println("TYPE P D I");
            for(Operation operation : process.ops){
                if(operation.isSend()){
                    System.out.println(operation.type+" "+operation.proc+" "+operation.dst+" "
                            +operation.rank);
                }else if(operation.isRecv()) {
                    System.out.println(operation.type+" "+operation.proc+" "+operation.src+" "
                            +operation.rank);
                }else if(operation.isWait()){
                    System.out.println(operation.type+" "+operation.proc+" "+operation.req.rank+" "+operation.rank);
                }else if(operation.isBarrier()){
                    System.out.println(operation.type+" "+operation.proc+" "+operation.rank+" "+operation.group);
                }
            }
            System.out.println(" ");
        }
    }

    public boolean isCheckInfiniteBuffer(){
        return checkInfiniteBuffer;
    }

    public static void main(String[] args) {
        Program program = new Program("./src/test/fixtures/2.txt");
        program.printALLOperations();
        program.printMatchPairs();
    }




}
