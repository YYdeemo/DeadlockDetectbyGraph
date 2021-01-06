package syntax;

import constant.OPTypeEnum;
import prework.CmpFile;
import sun.awt.image.ImageWatched;

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
    public int size;//the count of processes
    public ArrayList<Process> processArrayList;
    public Hashtable<Operation, LinkedList<Operation>> matchTables;
    public Hashtable<Operation, LinkedList<Operation>> HBTables;

    public Program(String filepath){
        //初始化variables！！！
        processArrayList = new ArrayList<>();
        initializeProgramFromCTP(filepath);
        cmpOPsInfo();
        size = processArrayList.size();
        matchTables = MatchPairs.overApproximateMatchs(this);
        HBTables = HBRelations.generatHBRelations(this);
    }
    /*
    *from the file, we create the operations, add them to the process, and add the process to the program
    *param: filepath
     */
    private void initializeProgramFromCTP(String filepath){
        LinkedList<String[]> ctp = CmpFile.getCTPFromFile(filepath);
        ListIterator<String[]> listIterator = ctp.listIterator(0);
        String[] aStr;
        while(listIterator.hasNext()){
            aStr = listIterator.next();
            Operation operation = CmpFile.translateFromStrToOP(aStr);
            if(operation!=null){
                if(processArrayList.size()<=operation.proc){
                    Process process = new Process(operation.proc);
                    process.append(operation);
                    processArrayList.add(process);
                }else{
                    processArrayList.get(operation.proc).append(operation);
                }
            }//if(op!=null)
        }//while
    }
    /*
    *   after initialize the program, we have added all the actions to the program,
    *   but not finish the work of completing the info of the actions
    *   such as each action's rank ***********
    *   the wait action's req and the blocking recv action's nearliest wait is which one
     */
    private void cmpOPsInfo(){
        for(Process process : processArrayList){
            for(Operation operation : process.ops){
                if(operation.type== OPTypeEnum.WAIT){
                    operation.req = process.ops.get(operation.reqID);
                    process.ops.get(operation.reqID).Nearstwait = operation;
                }
                if(operation.isRecv()){
                    operation.rank = process.rlist.indexOf(operation);
                }
                if(operation.isSend()){
                    operation.rank = process.slist.indexOf(operation);
                }
            }
        }
    }

    public void printMatchPairs(){
        System.out.println("MATCH PAIRS IS SHOWN AS FOLLOWING :");
        for(Operation R : matchTables.keySet()){
            for (Operation S : matchTables.get(R)){
                System.out.println("<R"+R.index+", S"+S.index+">");
            }
        }
    }



    public static void main(String[] args){
        Program program = new Program("./src/test/test_ctp_cmp.txt");
        for(Process process : program.processArrayList){
            process.printProcessInfo();
        }
        program.printMatchPairs();
    }

}
