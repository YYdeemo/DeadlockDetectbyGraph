package methods;

import syntax.*;

import java.util.*;

public class Johnson_p {
    public Graph graph;

    public Stack<Operation> stack;
    Hashtable<Operation, Boolean> blocked;
    Hashtable<Operation, List<Operation>> blists;//blockNodes

    Hashtable<Integer, Integer> block_count;
    Stack<Operation> orphaned;
    Hashtable<Integer, Operation> orphaned_paths;
    Stack<Integer> procStack;
    public LinkedList<Pattern> patterns;//which save the fake deadlock candidates

//    public int leastVertex;
//    Set<Operation> leastSCC;
//    int count_cut = 0;//??????

    public Johnson_p(Graph graph){
        stack = new Stack<>();
        blocked = new Hashtable<>();
        blists = new Hashtable<>();
        block_count = new Hashtable<>();
        orphaned = new Stack<>();
        orphaned_paths = new Hashtable<>();
        procStack = new Stack<>();
        patterns = new LinkedList<>();
        this.graph = graph;
    }

    public void find(){
        int i = 0;
        while (i<graph.VList.size()){
            Vector<Operation> subVlist = new Vector<>();
            subVlist.addAll(graph.VList.subList(i,graph.VList.size()));
            Hashtable<Operation, Set<Operation>> subEtables = induceSubEtables(subVlist);
            TarjanScc_p tarjanScc = new TarjanScc_p(subVlist, subEtables);
            Operation s = tarjanScc.leastOp;
            Hashtable<Operation, LinkedList<Operation>> A = tarjanScc.SccEtables;

            if(s != null){
                i += subVlist.indexOf(s);
                stack.clear();
                blists.clear();
                block_count.clear();
                for (Operation w : A.keySet()){
                    blocked.put(w,false);
                    blists.put(w, new LinkedList<Operation>());
                    block_count.put(w.proc, 0);
                }
                orphaned.clear();
                orphaned_paths.clear();
                procStack.clear();

                circuit(A, s, s);
                i += 1;

            }else{
                i = graph.VList.size();
            }
        }


    }

    boolean circuit(Hashtable<Operation, LinkedList<Operation>> A, Operation s, Operation v) {
        boolean found = false;

        if (stack.empty() || stack.peek().proc != v.proc) {//if the vertex is the first node for the orphaned or if the node from a new process
            procStack.push(v.proc);
            orphaned.push(v);
        }
        stack.push(v);

        blocked.put(v, true);

        //block_count count the number of operations besides the sends which in infinite buffer (<pro.rank, num>)
        if ((v.isWait() && (v.req.isRecv() || (!graph.program.checkInfiniteBuffer && v.req.isSend()))) || v.isBarrier()) {
            if (!block_count.containsKey(v.proc)) block_count.put(v.proc, 0);
            block_count.put(v.proc, block_count.get(v.proc) + 1);
        }

        for (Operation w : A.get(v)) {

            if (!(goodEdges(v, w, s)))//check whether the path is good_edge
                continue;

            if (v.proc != w.proc)//???the orphaned_paths save <v.proc,w>
                orphaned_paths.put(v.proc, w);

            if (w == s) {// find the circle
                if (stack.size() > 2) {

                    boolean isAllBarrier = true;
                    for (Operation op : stack) {
                        if (!op.isBarrier()) {
                            isAllBarrier = false;
                            break;
                        }
                    }
                    if (isAllBarrier) continue;

                    Pattern pattern = new Pattern(graph, stack);//get the pattern from the circle

                    if (pattern.pattern.size() > 1 && isNewPattern(pattern)) {
                        printCycles(stack);
                        patterns.add(pattern);
                        found = true;
                    }

                }

            } else {
                if (blocked.containsKey(w) && !blocked.get(w)) {
                    found = circuit(A, s, w);
                }
            }
        }

        if (found) {
            unblock(v);
        } else {
            for (Operation w : A.get(v)) {
                if (blists.containsKey(w) && !blists.get(w).contains(v)) {
                    blists.get(w).add(v);
                }
            }
        }
        //
        if (orphaned.peek().equals(stack.peek())) {
            orphaned.pop();
            procStack.pop();
            orphaned_paths.remove(stack.peek().proc);
        }
        stack.pop();
        if ((v.isWait() && (v.req.isRecv() || (!graph.program.checkInfiniteBuffer && v.req.isSend()))) || v.isBarrier()) {
            block_count.put(v.proc, block_count.get(v.proc) - 1);
        }

        return found;
    }

    boolean goodEdges(Operation v, Operation w, Operation s) {
        if (v.proc == w.proc)
            return true;

        if (!block_count.containsKey(v.proc))//***
            return false;

        if (v.isBarrier() && block_count.get(v.proc) == 1)
            return false;

        if (w.isRecv() && w.src == -1)
            return false;

        for (Operation a : orphaned) {
            if (can_match(a, w))
                return false;
        }

        if (orphaned_paths.containsKey(v.proc) && orphaned_paths.get(v.proc).equals(w))
            return false;

        if (!w.equals(s)) {
            for (Operation a : stack) {
                if (a.proc == w.proc)
                    return false;
            }
            return true;
        }

        //if infinite buffer, the first action in any process should not be a send
        if (graph.program.isCheckInfiniteBuffer() && w.isSend()) {
            return false;
        }

//        if (stack.size() == 1) //has to travel down the process of startvertex first instead of jumping to another process
//        {
//            return false;
//        } else if (stack.size() > 1) {
//            if (stack.peek().proc != stack.get(stack.size() - 2).proc)
//                return false;
//        }
        return true;
    }

    boolean can_match(Operation op1, Operation op2) {
        if (op1.isSend()) {
            return (op2.isRecv()) && (op1.dst == op2.dst) && (op2.src == -1 || op2.src == op1.src);
        } else if (op1.isRecv()) {
            return (op2.isSend()) && (op2.dst == op1.dst) && (op1.src == -1 || op1.src == op2.src);
        }
        return false;
    }

    void unblock(Operation v) {
        blocked.put(v, false);
        while (blists.get(v).size() > 0) {
            Operation w = blists.get(v).remove(0);
            if (blocked.get(w)) {
                unblock(w);
            }
        }
    }

    Vector<Operation> induceSubVector(Vector<Operation> vlist, int i){
        Vector<Operation> subVlist = new Vector<>();
        subVlist.addAll(vlist.subList(i, vlist.size()));
        int j = 0;
        for(Operation operation : vlist){
            if(j<i){
                j++;
                continue;
            }
            subVlist.add(operation);
        }
        return subVlist;
    }
    Hashtable<Operation, Set<Operation>> induceSubEtables(Vector<Operation> vlist){
        Hashtable<Operation, Set<Operation>> subEtable = new Hashtable<>();
        for (Operation operation : vlist){
            for(Operation op2 : graph.ETable.get(operation)){
                if(!subEtable.containsKey(operation)) subEtable.put(operation, new HashSet<>());
                if(vlist.contains(op2)) subEtable.get(operation).add(op2);
            }
        }
        return subEtable;
    }

    public void printCycles(Stack<Operation> stack){
        System.out.println("[JOHNSON]: THE CYCLE IS AS FOLLOWING :");
        for(Operation operation : stack){
            System.out.print(" "+operation.getStrInfo()+" --> ");
        }
        System.out.println(" ");
    }

    boolean isNewPattern(Pattern pattern){
        for(Pattern pattern1 : patterns){
            if(pattern.pattern.equals(pattern1.pattern)){
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Program program = new Program("./src/test/fixtures/monte4.txt",false);
        Graph graph = new Graph(program);
//        graph.printGraphVList();
//        graph.printGraphETable();
        Johnson_p johnson_p = new Johnson_p(graph);
        johnson_p.find();
    }

}

class TarjanScc_p {
    int i = 0;
    Stack<Operation> stack;
    Hashtable<Operation, Integer> lowLink;
    Hashtable<Operation, Integer> number;

    public Operation leastOp;
    LinkedList<Operation> leastScc;
    public Hashtable<Operation, LinkedList<Operation>> SccEtables;

    Hashtable<Operation, Set<Operation>> Etables;
    Vector<Operation> Vlist;

    public TarjanScc_p(Vector<Operation> Vlist, Hashtable<Operation, Set<Operation>> Etables) {
        this.Etables = Etables;
        this.Vlist = Vlist;
        stack = new Stack<>();
        lowLink = new Hashtable<>();
        number = new Hashtable<>();
        leastScc = new LinkedList<>();

        for (Operation operation : Vlist) {
            if (!number.containsKey(operation)) {
                scc(operation);
            }
        }
        SccEtables = new Hashtable<>();
        for (Operation w : leastScc) {
            if (!SccEtables.containsKey(w)) SccEtables.put(w, new LinkedList<>());
            for (Operation v : Etables.get(w)) {
                if (leastScc.contains(v)) SccEtables.get(w).add(v);
            }
        }
    }

    private void scc(Operation v) {
        i = i + 1;
        number.put(v, i);
        lowLink.put(v, i);
        stack.push(v);

        for (Operation w : Etables.get(v)) {
            if (!number.containsKey(w)) {
                scc(w);
                lowLink.put(v, Math.min(lowLink.get(v), lowLink.get(w)));
            } else if (number.get(w) < number.get(v)) {
                if (stack.contains(w)) lowLink.put(v, Math.min(lowLink.get(v), number.get(w)));
            }
            if (lowLink.get(v) == number.get(v)) {
                Set<Operation> component = new HashSet<>();
                boolean newleast = false;
                while (!stack.empty() && number.get(stack.lastElement()) >= number.get(v)) {
                    w = stack.pop();
                    if (leastOp == null || Vlist.indexOf(w) < Vlist.indexOf(leastOp)) {
                        leastOp = w;
                        newleast = true;
                    }
                    component.add(w);
                    if (newleast) leastScc.addAll(component);
                }
            }

        }

    }


}
