package methods;

import constant.OPTypeEnum;
import syntax.*;
import syntax.Process;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

public class AbstractMachine {

    public Hashtable<Integer, Operation> pattern;
    public Program program;

    int tracker[];
    int indicator[];

    boolean deadlockFound;

    public AbstractMachine(Program program, Pattern pattern){
        this.program = program;
        this.pattern = pattern.pattern;
        deadlockFound = false;
    }

    public void execute(Hashtable<Integer, Operation> pattern){
//        initialize the variables which used in the Machine
        tracker = new int[program.getSize()];
        indicator = new int[program.getSize()];
        HashMap<HashMap<Integer,Integer>, Integer> sendNums = new HashMap<HashMap<Integer, Integer>, Integer>();
        HashMap<HashMap<Integer,Integer>, Integer> recvNums = new HashMap<HashMap<Integer, Integer>, Integer>();
        int[] lastrInShape = new int[program.getSize()];
        int[][] lastsInShape = new int[program.getSize()][program.getSize()];

        for (int i = 0; i < program.getSize(); i++) {
            lastrInShape[i] = -1;
            for (int j = 0; j < program.getSize(); j++)
                lastsInShape[i][j] = -1;
        }
        //initialize each indicator of process
        for (int i = 0; i < program.getSize(); i++) {
            if (!pattern.containsKey(i)) {
                indicator[i] = program.processArrayList.get(i).NextBlockPoint();
            } else {
                indicator[i] = program.processArrayList.get(i).ToPoint(pattern.get(i));
            }
            tracker[i] = 0;
        }
        while(true){
            int old_tracker[] = tracker.clone();
            if (isreachControlPoint()) break;
            for(Process process : program.getAllProcesses()){
                while(true){
//                    tracker reach the end of the process
                    if (tracker[process.rank] == process.Size()) break;

                    Operation operation = process.getOP(tracker[process.rank]);
                    System.out.println("Now THIS STEP EXECUTE TO OP, WHICH IS : "+operation.getStrInfo());
//                  ???
                    if(tracker[process.rank] == indicator[process.rank]) break;

                    if(operation.isSend()){

                    }else if (operation.isRecv()){

                    }else if (operation.isWait()){

                    }else if (operation.isBarrier()){

                    }else if (operation.isBot()){

                    }

                }
//                find the deadlock or reach all the control points;
                if (old_tracker.equals(tracker)) break;
            }
        }
        System.out.println("THE RESULT IS : "+deadlockFound);
    }
    boolean isreachControlPoint(){
        for(int i = 0; i< program.getSize(); i++){
            if(tracker[i]<indicator[i]) return false;
        }
        System.out.println("reach all control points");
        return true;
    }




    int totalNUM(HashMap<Integer, HashMap<Integer, Integer>> map, int src, int dest) {
        if (map.containsKey(dest)) {
            if (map.get(dest).containsKey(src)) {
                return map.get(dest).get(src);
            }
        }
        return 0;
    }
}
