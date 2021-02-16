package methods;

import constant.OPTypeEnum;
import javafx.util.Pair;
import syntax.*;
import syntax.Process;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

public class AbstractMachine {

    public Hashtable<Integer, Operation> pattern;
    public Pattern candidate;
    public Program program;

    int tracker[];
    int indicator[];

    boolean deadlockFound;

    public AbstractMachine(Program program, Pattern pattern){
        this.program = program;
        this.candidate = pattern;
        this.pattern = pattern.pattern;
        deadlockFound = false;
    }

    public boolean execute(Hashtable<Integer, Operation> pattern){
//        initialize the variables which used in the Machine
        tracker = new int[program.getSize()];
        indicator = new int[program.getSize()];
        int[][] sendNums = new int[program.getSize()][program.getSize()+1];
        int[][] recvNums = new int[program.getSize()][program.getSize()+1];
        Hashtable<Integer, LinkedList<Operation>> recvInShape = new Hashtable<Integer, LinkedList<Operation>>();
        Hashtable<Integer, LinkedList<Operation>> sendInShape = new Hashtable<Integer, LinkedList<Operation>>();

        //initialize each indicator of process
        for (int i = 0; i < program.getSize(); i++) {
            if (!pattern.containsKey(i)) {
                indicator[i] = -1;
            } else {
                indicator[i] = program.processArrayList.get(i).ToPoint(pattern.get(i));
            }
            tracker[i] = 0;
        }

        while(true){
            int old_tracker[] = tracker.clone();
            if (reachedControlPoint()){
                return reachedControlPoint();
            }
            for(Process process : program.getAllProcesses()){
                while(true){
//                    tracker reach the end of the process
                    if (tracker[process.rank] == process.Size()) break;

                    Operation operation = process.getOP(tracker[process.rank]);
                    System.out.println("Now THIS STEP EXECUTE TO OP, WHICH IS : "+operation.getStrInfo());
//                  ???
                    if(tracker[process.rank] == indicator[process.rank]){
                        assert operation.isWait() || operation.isBarrier();
                        System.out.println("NOW THIS PROCESS STOP AT : "+operation.getStrInfo());
                        break;//stop at this operation which is recorded by the indicator in this process
                    }

                    if(operation.isSend()){
                        sendNums[operation.dst][operation.src]++;
                        if(!sendInShape.containsKey(operation.getHashCode())) sendInShape.put(operation.getHashCode(), new LinkedList<Operation>());
                        sendInShape.get(operation.getHashCode()).add(operation);
                        sendNums[operation.dst][-1]++;
                        Pair<Integer, Integer> pair = new Pair<>(operation.dst, -1);
                        if(!sendInShape.containsKey(pair.hashCode())) sendInShape.put(pair.hashCode(), new LinkedList<Operation>());
                        sendInShape.get(pair.hashCode()).add(operation);
                        tracker[process.rank]++;

                    }else if (operation.isRecv()){
                        recvNums[operation.dst][operation.src]++;
                        if(!recvInShape.containsKey(operation.getHashCode())) recvInShape.put(operation.getHashCode(), new LinkedList<Operation>());
                        recvInShape.get(operation.getHashCode()).add(operation);
                        tracker[process.rank]++;

                    }else if (operation.isWait()){
                        Operation req = operation.req;
                        assert req.isRecv() || req.isSend();
                        if(req.isRecv()){
                            LinkedList<Operation> sendQueue = sendInShape.get(operation.getHashCode());
                            LinkedList<Operation> recvQueue = recvInShape.get(operation.getHashCode());
                            if(recvQueue.contains(req)){
                                int idx = recvQueue.indexOf(req);
                                if(sendQueue.size()<=idx){
                                    System.out.println("MATCHING ACTION "+req.getStrInfo()+" HAS NOT ISSUED!");
                                    break;
                                }
                                System.out.println("CONSUME THE MATCHES AT :"+req.getStrInfo());
                                consume(sendQueue,recvQueue,idx+1);
                            }

                        }else if (req.isSend()){
                            Pair<Integer, Integer> pair = new Pair<>(req.dst, -1);
                            LinkedList<Operation> sendQueue = sendInShape.get(operation.getHashCode());
                            LinkedList<Operation> wsendQueue = sendInShape.get(pair.hashCode());
                            LinkedList<Operation> recvQueue = recvInShape.get(operation.getHashCode());
                            LinkedList<Operation> wrecvQueue = recvInShape.get(pair.hashCode());

                            if(sendQueue.contains(req) && wsendQueue.contains(req)){
                                int idx = sendQueue.indexOf(req);
                                int widx = wsendQueue.indexOf(req);
                                if(recvQueue.size()<=idx && wrecvQueue.size()<=widx){
                                    System.out.println("MATCHING ACTION "+req.getStrInfo()+" HAS NOT ISSUED!");
                                    break;
                                }else if(recvQueue.size()>idx){
                                    System.out.println("CONSUME THE MATCHES AT :"+req.getStrInfo());
                                    consume(sendQueue,recvQueue,idx+1);
                                }else{
                                    System.out.println("CONSUME THE WILDCARD MATCHES AT :"+req.getStrInfo());
                                    consume(wsendQueue,wrecvQueue,widx+1);
                                }
                            }
                        }
                        tracker[process.rank]++;

                    }else if (operation.isBarrier()){
                        boolean allisBarrier = true;
                        for(Process pro : program.getAllProcesses()){
                            if(!pro.getOP(tracker[pro.rank]).isBarrier()) allisBarrier=false;
                        }
                        if(!allisBarrier){
                            break;//if there is operation which is not barrier, then break; stop;
                        }else{
                            for(int i = 0; i<program.getSize();i++){
                                if(tracker[1]<program.get(i).Size() && tracker[1]<indicator[i])
                                    tracker[i]++;
                            }
                        }
                    }else if (operation.isBot()){
                        tracker[process.rank]++;
                    }
                    boolean hasChange = false;
                    for(int i = 0; i< program.getSize(); i++){
                        if (old_tracker[process.rank]!=tracker[process.rank]){
                            hasChange = true;
                        }
                    }
                    if(!hasChange) return reachedControlPoint();
                }
//                find the deadlock or reach all the control points;
            }
        }
    }

    void consume(LinkedList<Operation> sendQ, LinkedList<Operation> recvQ, int idx){
        for(int i = 0; i<idx; i++){
            sendQ.pop();
            recvQ.pop();
        }
    }
    boolean reachedControlPoint(){
        for(int i = 0; i< program.getSize(); i++){
            if(indicator[i] != -1 && indicator[i] != tracker[i]) return false;
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
