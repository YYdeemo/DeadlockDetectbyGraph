package methods;

import constant.OPTypeEnum;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import syntax.Operation;
import syntax.Pattern;
import syntax.Process;
import syntax.Program;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

public class AbstractMachine {

    public Hashtable<Integer, Operation> pattern;
    public Program program;

    int tracker[];
    int indicator[];

    boolean deadlockFound;

    HashMap<Integer, HashMap<Integer,Integer>> sendNums;
    HashMap<Integer, HashMap<Integer,Integer>> recvNums;

    public int[] lastrInShape;
    public int[][] lastsInShape;

    public HashMap<Operation, LinkedList<Operation>> witnessedRecv; //used to record the receives that are witnessed

    public AbstractMachine(Program program, Pattern pattern){
        this.program = program;
        this.pattern = pattern.pattern;
        Initialize();
        checkPattern(this.pattern);
    }

    public void checkPattern(Hashtable<Integer, Operation> pattern){
        while(schedulable(pattern)){
            scheduling(pattern);
        }
        System.out.println("THE RESULT IS : "+deadlockFound);
    }

    public void Initialize(){
        tracker = new int[program.size];
        indicator = new int[program.size];
        deadlockFound = false;
        sendNums = new HashMap<Integer, HashMap<Integer, Integer>>();
        recvNums = new HashMap<Integer, HashMap<Integer, Integer>>();
        lastrInShape = new int[program.size];
        lastsInShape = new int[program.size][program.size];
        witnessedRecv = new HashMap<Operation, LinkedList<Operation>>(); //used to record the receives that are witnessed<Wait, List<Recv>>

        for (int i = 0; i < program.size; i++) {
            lastrInShape[i] = -1;
            for (int j = 0; j < program.size; j++)
                lastsInShape[i][j] = -1;
        }
        //initialize each indicator of process
        for (int i = 0; i < program.size; i++) {
            if (!pattern.containsKey(i)) {
                indicator[i] = program.processArrayList.get(i).NextBlockPoint();
            } else {
                indicator[i] = program.processArrayList.get(i).ToPoint(pattern.get(i));
            }
            tracker[i] = 0;
        }
    }

    boolean schedulable(Hashtable<Integer, Operation> pattern)
    {
        boolean reachpatternpoint = true;
        for(int i = 0; i < program.size; i++) {
            int rank = program.get(i).rank;
            //if process does not reach the end of each process
            if (tracker[rank] < indicator[rank] || (indicator[rank] == 0 && tracker[rank] == 0)) {
                Operation op = program.get(rank).getOP(tracker[rank]);

                //process belongs to pattern and the indicator of pattern is not reached
                if (pattern.containsKey(rank) && tracker[rank] < indicator[rank]) {
                    reachpatternpoint = false;
                }
                //if op is not block, then return TRUE
                if (!op.isBlock()) return true;

                if (op.isRecv()) {
                    //if recv is non-blocking, return true
                    if (checkAvailable(op)) {
                        return true;
                    }
                } else if (op.isSend()) {
                    if (!pattern.containsValue(op)) {
                        return true;
                    }
                }
                // add a case when op is a wait
                else if (op.type == OPTypeEnum.WAIT) {
                    if (witnessedRecv.containsKey(op)) {
                        LinkedList<Operation> witnessedR = witnessedRecv.get(op);
                        if (!witnessedR.isEmpty()) {
                            //only needs to check the first recv that the wait witnessed,
                            //if this receive is schedulable, then return true
                            //otherwise, it blocks other receives to be matched
                            Operation firstR = witnessedR.getFirst();
                            //if the first receive for this wait is a receive
                            //of the circular pattern instance, then this process is not schedulable
                            if (!firstR.equals(pattern.get(firstR.dst)))
                                if (checkAvailable(firstR)) {
                                    return true;
                                }
                        }
                    }
                } else {
                    return true;
                }
            }
        }

        if(reachpatternpoint)
        {
            System.out.println("Deadlock is found for pattern " + pattern);
            //use a global variable to mark deadlock is found
            deadlockFound = true;
        }

        System.out.print("No deadlock is found for pattern: "  + pattern + "\n");
        //when false, is it a deadlock for the prefix?
        return false;
    }

    void scheduling(Hashtable<Integer, Operation> pattern){
        for (int p = 0; p < program.size; p++) {
            //tracker = 0 and indicator = 0
            if (tracker[p] == 0 && indicator[p] == 0) {
                Operation op = program.get(p).getOP(tracker[p]);
                //generate entry in recvNums for a indicator receive
                if (op.isRecv() && op.isBlock()) {
                    if (!recvNums.containsKey(op.dst))
                        recvNums.put(op.dst, new HashMap<Integer, Integer>());

                    if (!recvNums.get(op.dst).containsKey(op.src))
                        recvNums.get(op.dst).put(op.src, 0);

                    if (checkAvailable(op)) {
                        recvNums.get(op.dst).put(op.src, recvNums.get(op.dst).get(op.src) + 1);
                        tracker[p]++;
                    } else //when recv can not be matched, scheduling stops for this process
                    {
                        continue;
                    }
                }
            }

            // tracker >= indicator  process has no operation left until indicator
            if (tracker[p] >= indicator[p]) {
                continue;
            }

            //tracker < indicator
            Operation op;
            while (tracker[p] < indicator[p]) {
                op = program.get(p).getOP(tracker[p]);
                if (op.isSend()) SendTransition(op, p);

                if (op.isRecv()) {
                    if (!RecvTransition(op, p)) break;
                }

                if (op.type == OPTypeEnum.WAIT) WaitTransition(op, p);
            }

        }

    }

    boolean RecvTransition(Operation recv, int p) {
        int src = recv.src;
        int dest = recv.dst;

        lastrInShape[dest] = recv.rank;

        if (recv.isBlock()) {
            if (!recvNums.containsKey(dest)) recvNums.put(dest, new HashMap<Integer, Integer>());

            if (!recvNums.get(dest).containsKey(src)) recvNums.get(dest).put(src, 0);


            //increment the size of recv in recvNums
            if (checkAvailable(recv)) {
                recvNums.get(dest).put(src, recvNums.get(dest).get(src) + 1);
                tracker[p]++;
            } else //when recv can not be matched, scheduling stops for this process
            {
                return false;
            }
        } else {
            //if rv is non-blocking, add it to the data structure witnessedRecv
            //suppose the nearest-enclosing wait exists for rv in this case
            Operation nw = recv.Nearstwait;
            if (!witnessedRecv.containsKey(nw))
                witnessedRecv.put(nw, new LinkedList<Operation>());
            witnessedRecv.get(nw).addLast(recv);
            tracker[p]++;
        }
        return true;
    }

    void SendTransition(Operation send, int p) {
        int dest = send.dst;
        int src = send.src;

        lastsInShape[dest][src] = send.rank;

        if (!sendNums.containsKey(dest)) sendNums.put(dest, new HashMap<Integer, Integer>());

        if (!sendNums.get(dest).containsKey(src)) sendNums.get(dest).put(src, 0);

        if (!sendNums.get(dest).containsKey(-1)) sendNums.get(dest).put(-1, 0);

        //increment the size of send in sendNums
        sendNums.get(dest).put(src, sendNums.get(dest).get(src) + 1);
        sendNums.get(dest).put(-1, sendNums.get(dest).get(-1) + 1);
        tracker[p]++;
    }

    void WaitTransition(Operation wait, int p) {
        if (witnessedRecv.containsKey(wait)) {
            //iterately check the availability for each receive for this wait
            //blocking when a receive is unavailable
            //if the process is in the pattern instance, then block
            //when the deterministic receive is witnessed
            LinkedList<Operation> witnessedR = witnessedRecv.get(wait);
            for (Operation r : witnessedR) {
                if (pattern.containsKey(p) && r.equals(pattern.get(p))) {
                    //when r is a receive in the circular pattern instance//stop here and start traversing other processes
                    break;
                }
                int src = r.src;
                int dest = r.dst;
                if (!recvNums.containsKey(dest)) recvNums.put(dest, new HashMap<Integer, Integer>());

                if (!recvNums.get(dest).containsKey(src)) recvNums.get(dest).put(src, 0);

                //increment the size of recv in recvNums
                if (checkAvailable(r)) {
                    recvNums.get(dest).put(src, recvNums.get(dest).get(src) + 1);
                    //remove this receive from the data structure witnessedRecv
                    witnessedR.remove(r);
                } else //when recv can not be matched, scheduling stops for this process
                {
                    break;
                }
            }
            //remove the wait if all the receives are witnessed, and increment the tracker
            if (witnessedR.isEmpty()) {
                witnessedRecv.remove(wait);
                tracker[p]++;
            }
        }
    }

    boolean checkAvailable(Operation operation) {
        int src = operation.src;
        int dest = operation.dst;
        //if the operation is RECV
        if (operation.isRecv()) {
            //more sends than receives with identical src and dest
            if (src != -1)//Deterministic receive
                //two conditions should be satisfied:
                return (totalNUM(sendNums, src, dest) > totalNUM(recvNums, src, dest))//S(c->0) > R(c)
                        && (totalNUM(sendNums, -1, dest) >  //S(c->0) > R(*) + R(c)
                        totalNUM(recvNums, -1, dest) + totalNUM(recvNums, src, dest));
            else {
                //for wildcard receive, the number of send(*->dest) has to be greater than the number
                //of {recv(*->dest), recv(c1->dest), ...}
                if (recvNums.containsKey(dest)) {
                    int totalAvailableRecvs = 0;
                    for (Integer rsrc : recvNums.get(dest).keySet())
                        totalAvailableRecvs += recvNums.get(dest).get(rsrc);
                    //should use ">" other than ">=" because
                    //at least one send is available for the next receive
                    return (totalNUM(sendNums, src, dest) > totalAvailableRecvs);
                }

                return false;
            }
        }
        //if the operation is SEND
        if (operation.isSend()) {
            if (recvNums.containsKey(dest)) {
                int totalAvailableRecvs = 0;
                for (Integer rsrc : recvNums.get(dest).keySet())
                    totalAvailableRecvs += recvNums.get(dest).get(rsrc);
                return (totalNUM(sendNums, src, dest) < totalAvailableRecvs);
            }
            return false;
        }
        return false;
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
