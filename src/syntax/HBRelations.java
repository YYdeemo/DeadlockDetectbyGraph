package syntax;

import constant.OPTypeEnum;

import java.util.Hashtable;
import java.util.LinkedList;

public class HBRelations {

    public static Hashtable<Operation, LinkedList<Operation>> generatHBRelations(Program program) {
        Hashtable<Operation, LinkedList<Operation>> HBTables = new Hashtable<>();
        Operation lastR = null;
        Hashtable<Integer, Operation> lastS = new Hashtable<>();
        Hashtable<Integer, Operation> firstS = new Hashtable<>();

        for (Process process : program.processArrayList) {
            for (Operation operation : process.ops) {
                //if operation is a RECV
                if (operation.isRecv()) {
                    if (lastR != null) {
//                        lastR <HB op(r)
                        if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new LinkedList<Operation>());
                        HBTables.get(lastR).add(operation);
                    }
                    for (Integer dest : lastS.keySet()) {
//                        lastS <HB op(r)
                        if (!HBTables.containsKey(lastS.get(dest)))
                            HBTables.put(lastS.get(dest), new LinkedList<Operation>());
                        HBTables.get(lastS.get(dest)).add(operation);
                    }
                    lastS.clear();
                    firstS.clear();
                    if (operation.type == OPTypeEnum.RECV) lastR = operation;//if r is blocking then lastr is it;
                }
                //if operation is a WAIT
                if (operation.type == OPTypeEnum.WAIT) {
                    //question:there has a wait which waiting for more than one Recv or other operations (????)
                    if (operation.req.isRecv()) {
                        if (lastR != null) {
                            HBTables.get(lastR).add(operation.req);
                            lastR = operation.req;
                        } else {
                            lastR = operation.req;
                            if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new LinkedList<Operation>());
                        }
                    }
                }
                //if operation is a SEND
                if (operation.isSend()) {
                    if (lastR != null) {
                        if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new LinkedList<Operation>());
                        HBTables.get(lastR).add(operation);//lastR <HB s
                    }
                    if(lastS.containsKey(operation.dst)){
                        if(!HBTables.containsKey(lastS.get(operation.dst))) HBTables.put(lastS.get(operation.dst), new LinkedList<Operation>());
                        HBTables.get(lastS.get(operation.dst)).add(operation);//lastS <HB  s
                    }
                    lastS.put(operation.dst,operation);
                }
            }
        }

        return HBTables;
    }

}
