package methods;

import syntax.Operation;
import syntax.Program;
import syntax.Process;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;

public class MatchOrder {

    public Hashtable<Operation, Set<Operation>> MatchOrderTables;
    public Hashtable<Operation, Set<Operation>> QueueOrderTables;
    boolean checkInfiniteBuffer;

    public MatchOrder(Program program){
        MatchOrderTables = new Hashtable<Operation, Set<Operation>>();
        QueueOrderTables = new Hashtable<Operation, Set<Operation>>();
        checkInfiniteBuffer = program.checkInfiniteBuffer;
        generateMatchOrder(program);
    }

    public Hashtable<Operation, Set<Operation>> generateMatchOrder(Program program){
        for(Process process : program.getAllProcesses()){
            for(Operation op1 : process.ops){
                for(Operation op2 : process.ops){
                    if(op1 == op2){
                        continue;
                    }
                    if(isQueueOrder(op1,op2)){
                        if(!QueueOrderTables.containsKey(op1)) QueueOrderTables.put(op1, new HashSet<>());
                        QueueOrderTables.get(op1).add(op2);
                    }
                    if(isMatchOrder(op1,op2)){
                        if(!MatchOrderTables.containsKey(op1)) MatchOrderTables.put(op1, new HashSet<>());
                        MatchOrderTables.get(op1).add(op2);
                    }
                }
            }
        }
        return MatchOrderTables;
    }

    boolean isProcessOrder(Operation op1, Operation op2){
        return op1.rank<op2.rank;
    }

    boolean isQueueOrder(Operation op1, Operation op2){
        if(isProcessOrder(op1, op2)) {
            if (op1.isRecv() && op2.isRecv()) {
                if (op1.src == op2.src || op1.src == -1 ) return true;
            }
            if (op1.isSend() && op2.isSend()) {
                if (op1.dst == op2.dst) return true;
            }
        }
        return false;
    }

    boolean isMatchOrder(Operation op1, Operation op2){
        if(isProcessOrder(op1,op2)){
            if(isQueueOrder(op1, op2)) return true;//match order rule1
            if((op1.isWait() && op1.req.isRecv()) || (op1.isWait() && op1.req.isSend() && (!checkInfiniteBuffer))) return true;//rule2
            if(op1.isBarrier()) return true;//rule2
            //rule3:
            if(op1.isRecv() && op2.isWait() && op2.req == op1) return true;//infinite buffer: only recv
            if((!checkInfiniteBuffer) && op1.isSend() && op2.isWait() && op2.req == op1) return true;//zero buffer: send and recv both should wait
        }
        return false;
    }

    public void printOrderRelation(){
        System.out.println("[MATCH_ORDER]: THE ALL MATCH ORDER RELATIONS IS :");
        for(Operation op1 : MatchOrderTables.keySet()){
            for(Operation op2 : MatchOrderTables.get(op1)){
                System.out.println(op1.getStrInfo()+"<mo "+op2.getStrInfo());
            }
        }
        for(Operation op1 : QueueOrderTables.keySet()){
            for(Operation op2 : QueueOrderTables.get(op1)){
                System.out.println(op1.getStrInfo() +"<qo "+op2.getStrInfo());
            }
        }
    }

    public void printMatchOrderOps(){
        System.out.println("[MATCHORDER]: ALL OPERATIONS IN MATCH-ORDER KEYSET ARE :");
        for(Set<Operation> listv : MatchOrderTables.values()){
            for(Operation operation:listv){
                System.out.print(" "+operation.getStrInfo()+" ");

            }
        }
        System.out.println(" ");
    }
}
