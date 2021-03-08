package methods;

import constant.OPTypeEnum;
import constant.Status;
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
    Hashtable<Pair<Integer, Integer>, LinkedList<Operation>> recvInShape;
    Hashtable<Pair<Integer, Integer>, LinkedList<Operation>> sendInShape;

    boolean deadlockFound;

    public AbstractMachine(Program program, Pattern pattern){
        this.program = program;
        this.candidate = pattern;
        this.pattern = pattern.pattern;
        deadlockFound = false;

    }

    public Status execute(){
        //init
        tracker = new int[program.getSize()];
        indicator = new int[program.getSize()];
        recvInShape = new Hashtable<Pair<Integer, Integer>, LinkedList<Operation>>();
        sendInShape = new Hashtable<Pair<Integer, Integer>, LinkedList<Operation>>();
        //initialize each indicator of process
        for (int i = 0; i < program.getSize(); i++) {
            if (!this.pattern.containsKey(i)) {
                indicator[i] = -1;
            } else {
                indicator[i] = this.pattern.get(i).rank;//equal the match
//                indicator[i] = program.processArrayList.get(i).ToPoint(pattern.get(i));
            }
            tracker[i] = 0;
        }
        //start
        while(true){
            int old_tracker[] = tracker.clone();

            if (reachedControlPoint()==Status.REACHABLE){
                candidate.tracker = tracker.clone();
                candidate.DeadlockCandidate = true;
                return Status.REACHABLE;
            }

            for(Process process : program.getAllProcesses()){
                while(true){
//                    tracker reach the end of the process
                    if (tracker[process.rank] == process.Size()) break;

                    Operation operation = process.getOP(tracker[process.rank]);
//                    System.out.println("[ABSTRACT MACHINE]: NOW CHECKING "+operation.getStrInfo()+" OPERATION.");
//
                    if(tracker[process.rank] == indicator[process.rank]){
                        assert operation.isWait() || operation.isBarrier();
//                        System.out.println("[ABSTRACT MACHINE]: * NOW PROCESS "+process.rank+ " STOP AT "+operation.getStrInfo());
                        break;//stop at this operation which is recorded by the indicator in this process
                    }

                    if(operation.isSend()){
                        appendOpInShape(operation);
                        tracker[process.rank]++;

                    }else if (operation.isRecv()){
                        appendOpInShape(operation);
                        tracker[process.rank]++;

                    }else if (operation.isWait()){
                        Operation req = operation.req;
                        assert req.isRecv() || req.isSend();
                        if(req.isRecv()){
                            LinkedList<Operation> sendQueue = sendInShape.get(req.getHashCode());
                            LinkedList<Operation> recvQueue = recvInShape.get(req.getHashCode());
                            if(recvQueue.contains(req)){
                                int idx = recvQueue.indexOf(req);
                                if(sendQueue.size()<=idx){
//                                    System.out.println("[ABSTRACT MACHINE]: MATCHING ACTION "+req.getStrInfo()+" HAS NOT ISSUED!");
                                    break;
                                }
                                consume(req,idx+1);//only the req is recv, we consume the match pair
                            }

                        }else if (req.isSend()) {
                            if(sendInShape.get(req.getHashCode()).contains(req)){
                                if(sendInShape.get(req.getHashCode()).indexOf(req)<sendInShape.get(req.getHashCode()).size()-1)
                                    break;//non over-tacking rule
                            }
                            if(!program.checkInfiniteBuffer){//zero buffer
                                Pair<Integer, Integer> pair = new Pair<>(req.dst, -1);
                                if(sendInShape.get(req.getHashCode()).contains(req)
                                        || sendInShape.get(pair).contains(req)){
                                    break;
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
                        System.out.println("[ABSTRACT MACHINE]: ERROR! THERE IS A BOT!");
                        tracker[process.rank]++;
                    }
                }
                boolean hasChange = false;
                for (int i = 0; i < program.getSize(); i++) {
                    if (old_tracker[process.rank] != tracker[process.rank]) {
                        hasChange = true;
                        break;
                    }
                }
                if (!hasChange) return reachedControlPoint();
//                find the deadlock or reach all the control points;
            }
        }
    }

    private void appendOpInShape(Operation operation) {
        if(!recvInShape.containsKey(operation.getHashCode())) recvInShape.put(operation.getHashCode(), new LinkedList<Operation>());
        if(!sendInShape.containsKey(operation.getHashCode())) sendInShape.put(operation.getHashCode(), new LinkedList<Operation>());
        if(operation.isSend()){
            sendInShape.get(operation.getHashCode()).add(operation);
            Pair<Integer, Integer> pair = new Pair<>(operation.dst, -1);
            if(!sendInShape.containsKey(pair)) sendInShape.put(pair, new LinkedList<Operation>());
            sendInShape.get(pair).add(operation);
            if(!recvInShape.containsKey(pair)) recvInShape.put(pair, new LinkedList<Operation>());
        }
        else if(operation.isRecv())
            recvInShape.get(operation.getHashCode()).add(operation);
    }

    void consume(Operation req, int idx){
        if(req.isRecv()){
            for (int i = 0; i<idx; i++){
                Operation recv = recvInShape.get(req.getHashCode()).pop();
                Operation send = sendInShape.get(req.getHashCode()).pop();
                Pair<Integer, Integer> pair;
//                System.out.println("[CONSUME]:  "+send.getStrInfo()+" <--> "+recv.getStrInfo());
                if(req.src == -1){
                    pair = new Pair<>(send.dst, send.src);
                    sendInShape.get(pair).remove(send);
                }else {
                    pair = new Pair<>(send.dst, -1);
                    sendInShape.get(pair).remove(send);
                }
            }
        }
    }

//    void consume(LinkedList<Operation> sendQ, LinkedList<Operation> recvQ, int idx){
//        for(int i = 0; i<idx; i++){
//            Operation send = sendQ.pop();
//            Operation recv = recvQ.pop();
//            System.out.println("[CONSUME]:  "+send.getStrInfo()+" <--> "+recv.getStrInfo());
//        }
//    }

    Status reachedControlPoint(){
        for(int i = 0; i< program.getSize(); i++){
            if(indicator[i] != -1 && indicator[i] != tracker[i]){
//                System.out.println("[ABSTRACT MACHINE]: SORRY! CANNOT REACH THE CONTROL POINT!");
                return Status.UNREACHABLE;
            }
        }
        System.out.println("[ABSTRACT MACHINE]: REACH ALL CONTROL POINTS!");
        candidate.setTracker(tracker);
        return Status.REACHABLE;
    }

}
