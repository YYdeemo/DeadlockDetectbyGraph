package methods;

import constant.OPTypeEnum;
import syntax.Operation;
import syntax.Process;
import syntax.Program;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class HBRelations {
    /**
     * generate the Happens-Before Relations
     * 1. process order
     * 2. queue order
     * 3. match order
     *
     * @param program
     * @return HashTable<Operation, Set<Operation>>
     */

    public Hashtable<Operation, Set<Operation>> HBTables;
    Program program;

    public HBRelations(Program program){
        this.program = program;
        HBTables = new Hashtable<Operation, Set<Operation>>();
        generatHBRelations(this.program);
    }

    public Hashtable<Operation, Set<Operation>> generatHBRelations(Program program) {
        Operation lastR = null;
        Hashtable<Integer, Operation> lastS = new Hashtable<>();

        for (Process process : program.processes) {
            for (Operation operation : process.ops) {
                //if operation is a RECV
                if (operation.isRecv()) {
                    if (lastR != null) {
//                        lastR <HB op(r)
                        if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new HashSet<Operation>());
                        HBTables.get(lastR).add(operation);
                    }
                    for (Integer dest : lastS.keySet()) {
//                        lastS <HB op(r)
                        if (!HBTables.containsKey(lastS.get(dest)))
                            HBTables.put(lastS.get(dest), new HashSet<Operation>());
                        HBTables.get(lastS.get(dest)).add(operation);
                    }
                    lastS.clear();
                    lastR = operation;//if r is blocking then lastr is it;
                }
                //if operation is a WAIT
                if (operation.type == OPTypeEnum.WAIT) {
                    if (operation.req.isRecv()) {
                        if (lastR != null) {
                            HBTables.get(lastR).add(operation.req);
                            lastR = operation.req;
                        } else {
                            lastR = operation.req;
                            if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new HashSet<Operation>());
                        }
                    }
                }
                //if operation is a SEND
                if (operation.isSend()) {
                    if (lastR != null) {
                        if (!HBTables.containsKey(lastR)) HBTables.put(lastR, new HashSet<Operation>());
                        HBTables.get(lastR).add(operation);//lastR <HB s
                    }
                    if (lastS.containsKey(operation.dst)) {
                        if (!HBTables.containsKey(lastS.get(operation.dst)))
                            HBTables.put(lastS.get(operation.dst), new HashSet<Operation>());
                        HBTables.get(lastS.get(operation.dst)).add(operation);//lastS <HB  s
                    }
                    lastS.put(operation.dst, operation);
                }
            }
        }

        return HBTables;
    }

}
