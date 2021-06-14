package methods;

import constant.OPTypeEnum;
import constant.Status;
import syntax.*;

import java.io.File;
import java.util.*;

public class Johnson {
    public Graph graph;
    public boolean checkInfiniteBuffer;
    private int leastVertex;
    public LinkedList<Pattern> patterns;//which save all patterns

    private Stack<Operation> stack;//
    Hashtable<Operation, Boolean> blocked;
    Hashtable<Operation, List<Operation>> blockedNodes;

    Hashtable<Integer, LinkedList<Operation>> orphaned_paths;
    Stack<Operation> orphaned;
    Hashtable<Integer, Integer> block_count;

    Hashtable<Operation, Integer> inCsecOp;
    Hashtable<Operation, Integer> outCsecOp;

    String nogoodedgeStr = "";


    int count_cut = 0;
    int filterNum = 0;
    int filterSMTNum = 0;

    boolean foundDeadlock = false;

    public Johnson(Graph graph) {
        this.graph = graph;
        checkInfiniteBuffer = graph.program.checkInfiniteBuffer;
        leastVertex = 0;
        patterns = new LinkedList<Pattern>();
        stack = new Stack<Operation>();
        blocked = new Hashtable<Operation, Boolean>();
        blockedNodes = new Hashtable<Operation, List<Operation>>();
        orphaned = new Stack<Operation>();
        orphaned_paths = new Hashtable<Integer, LinkedList<Operation>>();
        block_count = new Hashtable<Integer, Integer>();
        inCsecOp = new Hashtable<>();
        outCsecOp = new Hashtable<>();
//        leastSCC = new HashSet<Operation>();
        find();
    }

    public void find() {
        stack.empty();
        int i = 0;
        while (i < graph.VList.size() - 1) {
            if (foundDeadlock) return;
//            System.out.println("i = "+i +graph.VList.get(i)+" \n");
            Set<Operation> leastSCC = getLeastSCC(i);
            if (leastSCC != null) {
                i = leastVertex;
                blocked.clear();
                blockedNodes.clear();
                inCsecOp.clear();
                outCsecOp.clear();
                for (Operation v : leastSCC) {
                    blocked.put(v, false);
                    blockedNodes.put(v, new LinkedList<Operation>());
                }
                orphaned_paths.clear();
                orphaned.clear();
                circuit(leastSCC, i, i);
                i++;
            } else {
                i = graph.VList.size() - 1;
            }
        }
    }

    Set<Operation> getLeastSCC(int i) {
//        if(graph.VList.get(i).proc==1 && graph.VList.get(i).rank==4)
//            System.out.println(graph.VList.get(i)+"\n");
        TarjanSCC tarjanSCC = new TarjanSCC(graph, i);
        leastVertex = tarjanSCC.leastvertex;
        return tarjanSCC.leastSubVectors;
    }

    public boolean circuit(Set<Operation> dg, int v, int s) {
        boolean f = false;
        if (foundDeadlock) return f;//if found a real deadlock, then stop finding.
        boolean relyGoodEdge = false;
        Operation vertex = graph.VList.get(v);
        Operation startvertex = graph.VList.get(s);

        if(!dg.contains(vertex)) return f;//if the v is not in dg, then return f=false;

        //if the vertex is the first node for the orphaned or if the node from a new process
        if (stack.empty() || stack.peek().proc != vertex.proc) orphaned.push(vertex);


        stack.push(vertex);// push v in stack
        blocked.put(vertex, true);//if v is in stack, then v in blocked is true; otherwise v is not in stack, it is false;
        // record the number of blocked operations in each process
        if ((vertex.isWait() && vertex.req.isRecv())
                || (vertex.isWait() && vertex.req.isSend() && (!checkInfiniteBuffer))
                || vertex.isBarrier()) {//TRUE:wait for recv || wait for zero buffer send || barrier
            if (!block_count.containsKey(vertex.proc)) block_count.put(vertex.proc, 1);
            else block_count.put(vertex.proc, block_count.get(vertex.proc) + 1);
        }

        HashSet<Operation> adj_leastSCC = new HashSet<Operation>();

        continuepoint:
        for (Operation w : graph.ETable.get(vertex)) {
            if(!dg.contains(w)) continue continuepoint;//if w is not in dg, stop dfs;
//            System.out.print("\n"+stack+" now v="+vertex.getStrInfo()+" w="+w.getStrInfo());

            updateCsecPairs(vertex,w);//record each CsecOperation's out operation and in operation.
            int goodEdgeResult = good_edge(vertex, w, startvertex);
            if (goodEdgeResult!=1) {//check whether the path is good_edge
//                System.out.print(" no good edge : "+nogoodedgeStr);
                if (goodEdgeResult==-1) relyGoodEdge = true;
                continue continuepoint;
            }

            Stack<Operation> stackclone = (Stack<Operation>) stack.clone();

            adj_leastSCC.add(w);

            if (w == startvertex) {// find the circle
                if (stack.size() > 2) {
//                    f = true;//there is a new cycle but it may not be a new pattern
                    count_cut = count_cut + stack.size();
                    if (appendPatternToList(stackclone)) {
                        f = true;
                    }

                    if (count_cut >= 500000000) return f;
                }
            } else {
                if (vertex.proc != w.proc){
                    if (!orphaned_paths.containsKey(vertex.proc)) orphaned_paths.put(vertex.proc,new LinkedList<>());
                    orphaned_paths.get(vertex.proc).add(w);
                }
                if (blocked.containsKey(w) && !blocked.get(w)) {
                    if (circuit(dg, graph.VList.indexOf(w), s)) f = true;
                }
//                else System.out.print("  w "+blocked.get(w)+" is false or not in blocked");
            }
        }//for w in adj(v)

        if(f || relyGoodEdge){
            unblock(vertex, blocked, blockedNodes);
        }else {
            for (Operation w : adj_leastSCC) {
                if (blockedNodes.containsKey(w) && !blockedNodes.get(w).contains(vertex)) {
                    blockedNodes.get(w).add(vertex);
                }
            }
        }

        if (!orphaned.empty() && orphaned.peek().equals(stack.peek())) {
//            System.out.println("orphaned peek removed : "+stack.peek());
            orphaned.pop();
//            int peekproc = -1;
//            for (Integer i : orphaned_paths.keySet())
//                if (orphaned_paths.get(i).equals(stack.peek())) peekproc = i;
//            if (orphaned_paths.containsKey(peekproc)) orphaned_paths.remove(peekproc);
            if(orphaned_paths.containsKey(stack.peek().proc)) orphaned_paths.remove(stack.peek().proc);
        }

        Operation pop = stack.pop();

        if ((vertex.isWait() && vertex.req.isRecv())
                || (vertex.isWait() && vertex.req.isSend() && (!checkInfiniteBuffer))
                || vertex.isBarrier()) {//TRUE:wait for recv || wait for zero buffer send || barrier
            block_count.put(vertex.proc, block_count.get(vertex.proc) - 1);
        }

        return f;
    }

    public int good_edge(Operation v, Operation w, Operation s) {
        //true = 1 false = 0; false and rely history = -1
        nogoodedgeStr = "***";
        if (v.proc == w.proc) {

            if ((v.isSend() && w.isSend() && v.Nearstwait != null) || (v.isWait() && w.isWait())) {
                nogoodedgeStr = "two succ sends";
                return 0;
            }

            Operation ww = w;
            for (Operation adj_op : graph.ETable.get(v)) {
                if (adj_op.proc == v.proc && adj_op.rank < ww.rank) ww = adj_op;
            }
            if (!ww.equals(w)) {
                nogoodedgeStr = "w is not follow by v";
                return 0;
            }
            return 1;
        }
        // v.proc != w.proc

        if (v.isWait() && v.proc != w.proc) {
            nogoodedgeStr = "v is wait and v and w proc rank is not same";
            return 0;
        }

        //if infinite buffer, the first action in any process should not be a send
        if (checkInfiniteBuffer && w.isSend()) {
            nogoodedgeStr = "infinite buffer";
            return 0;
        }

        if (!block_count.containsKey(v.proc)) {
            if(v.src==-1){
                return -1;
            }
            nogoodedgeStr = "blocked count";
            return 0;
        }
        //***
        if (stack.size() == 1 ) //has to travel down the process of startvertex first instead of jumping to another process
        {
            nogoodedgeStr = "stack size = 1";
            return 0;
        } else if (stack.size() > 1) {
            if (stack.peek().proc != stack.get(stack.size() - 2).proc) {
                if(v.isCsecOperation){
//                    System.out.println("there is only one csce-operation in a process!"+v);
                }else {
                    nogoodedgeStr = "stack peek proc rank same";
                    if (v.src==-1) return -1;
                    return 0;
                }
            }
        }
//        if (w.isRecv() && w.src==-1){
//            return -1;
//        }
        //***
        for (Operation a : orphaned) {
            if (can_match(a, w)) {
                nogoodedgeStr = "can match";
                return -1;
            }
        }
        //***
        if (orphaned_paths.containsKey(v.proc) && orphaned_paths.get(v.proc).contains(w)) {
            nogoodedgeStr = "orphaned_paths";
            return 0;
        }
        //***
        if (!w.equals(s)) {
            for (Operation a : stack) {
                if (a.proc == w.proc) {
                    nogoodedgeStr = "has same proc with w";
                    return -1;
                }
            }
        }
        //***
        // inCsecOp.rank < outCsecOp.rank
        if (v.isCsecOperation) {
            if (stack.size() > 1) {
                if (outCsecOp.get(v) < inCsecOp.get(v)) {
                    nogoodedgeStr = "the only csec op is invaild";
                    if(v.src==-1) return -1;
                    return 0;
                }
            }
        }
        if (w.isCsecOperation && w.equals(s)) {
            if (outCsecOp.get(w) < inCsecOp.get(w)) {
                nogoodedgeStr = "the w is CsecOperation and it equal start";
                return 0;
            }
        }

        return 1;
    }

    public boolean can_match(Operation op1, Operation op2) {
        if (op1.isSend()) {
            return (op2.isRecv()) && (op1.dst == op2.dst)
                    && (op2.src == -1 || op2.src == op1.src);
        } else if (op1.isRecv()) {
            return (op2.isSend()) && (op2.dst == op1.dst)
                    && (op1.src == -1 || op1.src == op2.src);
        }

        return false;
    }

    public void unblock(Operation v, Hashtable<Operation, Boolean> blocked,
                        Hashtable<Operation, List<Operation>> blockedNodes) {
        blocked.put(v, false);
//        System.out.println(" \nunblock : "+v);

        while (blockedNodes.get(v).size() > 0) {
            Operation w = blockedNodes.get(v).remove(0);
            if (blocked.get(w)) {
                unblock(w, blocked, blockedNodes);
            }
        }
    }

    void updateCsecPairs(Operation v, Operation w){
        if (v.proc == w.proc) {
            if (v.isCsecOperation) outCsecOp.put(v, ((CsecOperation) v).OperationList.size() - 1);
            if (w.isCsecOperation) inCsecOp.put(w, 0);
        }

        if (w.proc != v.proc && v.isCsecOperation) {
            updateCsecOutPair((CsecOperation) v, w);
        } else if (w.proc != v.proc && w.isCsecOperation) {
            updateCsecInPair((CsecOperation) w, v);
        }
    }

    void updateCsecOutPair(CsecOperation v, Operation w) {
        if (w.isCsecOperation) {
            for (Operation opw : ((CsecOperation) w).OperationList) {
                for (int i = v.OperationList.size() - 1; i >= 0; i--) {
                    Operation operation = (v).OperationList.get(i);
                    LinkedList<Operation> matchList;
                    if (operation.isRecv()) {
                        matchList = graph.program.matchTables.get(operation);
                    } else matchList = graph.program.matchTablesForS.get(operation);
                    if (matchList.contains(opw)) {
                        outCsecOp.put(v, i);
                        inCsecOp.put(w, ((CsecOperation) w).OperationList.indexOf(opw));
                        break;
                    }
                }
            }
        } else {
            for (int i = (v).OperationList.size() - 1; i >= 0; i--) {
                Operation operation = (v).OperationList.get(i);
                LinkedList<Operation> matchList;
                if (operation.isRecv()) {
                    matchList = graph.program.matchTables.get(operation);
                } else matchList = graph.program.matchTablesForS.get(operation);
                if (matchList.contains(w)) {
                    outCsecOp.put(v, i);
                    break;
                }
            }
        }
    }

    void updateCsecInPair(CsecOperation w, Operation v) {
        if (!v.isCsecOperation) {
//            for (Operation operation : w.OperationList){
            for (int i = 0; i < w.OperationList.size(); i++) {
                Operation operation = w.OperationList.get(i);
                if (v.isBot()) {
                    inCsecOp.put(w, i);
                    break;
                }
                LinkedList<Operation> matchList;
                if (operation.isRecv()) {
                    matchList = graph.program.matchTables.get(operation);
                } else matchList = graph.program.matchTablesForS.get(operation);
                if (matchList.contains(v)) {
                    inCsecOp.put(w, i);
                    break;
                }
            }
        }
    }

    /**
     * append pattern to the list should do :
     * check this pattern is new?
     *
     * @param stack
     */
    private boolean appendPatternToList(Stack<Operation> stack) {
        if (isNewPattern(stack)) {
            Pattern pattern = new Pattern(graph.program, stack);
            patterns.add(pattern);
            pattern.check();
//            foundDeadlock = true;//Need to be delete!
            boolean checkAll = false;
            if (pattern.status == Status.SATISFIABLE) {
                if (!checkAll)foundDeadlock = true;
//                foundDeadlock = false;
            } else if (pattern.status == Status.UNREACHABLE) {
                filterNum += 1;
            } else if (pattern.status == Status.UNSATISFIABLE) {
                filterSMTNum += 1;
            }
            return true;
        }
        return false;
    }

    boolean isNewPattern(Stack<Operation> stack) {
        Hashtable<Integer, Operation> patterntable = Pattern.generatePatternTable(stack, checkInfiniteBuffer);
        for (Pattern pattern1 : patterns) {
            if (patterntable.equals(pattern1.patternTable)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        String directoryName = "./src/test/fixtures";
        File Dire = new File(directoryName);
        Program program;
        NewProgram newProgram;
        for (File file : Dire.listFiles()) {
            program = null;
            newProgram = null;
            if (!file.isDirectory()) {
//                String regex = "((diffusion2d(4|8|16|32|64|128))|(monte(8|16|32|64))|(heat(8|16|32|64))|(floyd(8|16|32|64|128))|(ge(8|16|32|64|128))|(integrate(8|10|16|32|64|128))|(is(256|64|128))).txt";
                String regex = "((diffusion2d(4|8|16|32))|(monte(8|16|32|64))|(heat(8|16|32|64))|(floyd(8|16|32|64|128))|(is(256|64|128))).txt";
//                String regex = "((diffusion2d(4|8|16|32|64))|(heat(8|16|32|64))|(monte(8|16|32|64))).txt";
//                String regex = "((diffusion2d(4|8|16))|(heat(8|16|32|64))|(monte(8|16))).txt";
//                String regex = "diffusion2d8.txt";
                if (!file.getName().matches(regex)) continue;
                System.out.println("-----------------------" + file.getName() + "----------------------");
                long t1 = System.currentTimeMillis();
                program = new Program(file.getPath(), false);
                Graph graph = new Graph(program);
                Johnson johnson = new Johnson(graph);
//                for(Pattern pattern : johnson.patterns){
//                    pattern.check();
//                    if(pattern.status == Status.SATISFIABLE){
////                        System.out.println("[SAT]: SAT! Deadlock detected for "+pattern.patternTable.values());
////                        break;
//                    }else if (pattern.status == Status.UNSATISFIABLE){
//                        johnson.filterSMTNum++;
////                        System.out.println("[UNSAT]: UNSAT! No deadlock is found for pattern:"+pattern.patternTable.values());
//                    }else{
//                        johnson.filterNum++;
////                        System.out.println("[filtered]: filter : "+pattern.patternTable.values());
//                    }
//                }
//                newProgram = new NewProgram(file.getPath(), false);
//                Graph graph = new Graph(newProgram);
////                System.out.println("in Graph has " + graph.getVCount() + " Vectors and "+ graph.getECount()+" Edges");
//                JohnsonNew johnson = new JohnsonNew(graph);
                long t2 = System.currentTimeMillis();
                System.out.println("Program executes " + ((double) (t2 - t1)) / (double) 1000 + "seconds");
                System.out.println(" the patterns number is : " + johnson.patterns.size());
                System.out.println(" filter pattern has : " + johnson.filterNum);
                System.out.println(" filter by smt solver has : " + johnson.filterSMTNum);
                System.out.println("====================================\n");
            }
        }
    }
}
