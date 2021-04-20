package methods;

import constant.OPTypeEnum;
import constant.Status;
import syntax.*;

import java.io.File;
import java.util.*;

public class Johnson {
    /**
     * this class is just a function not a object,
     * which achieve finding the cycles in the graph by Johnson
     */
    public Graph graph;
    public boolean checkInfiniteBuffer;
    private int leastVertex;
    public LinkedList<Pattern> patterns;//which save the fake deadlock candidates

    private Stack<Operation> stack;//
    Hashtable<Operation, Boolean> blocked;
    Hashtable<Operation, List<Operation>> blockedNodes;
    Hashtable<Integer, Operation> orphaned_paths;
    Stack<Operation> orphaned;
    Hashtable<Integer, Integer> block_count;

    Hashtable<Operation,Operation> inCsecOp;
    Hashtable<Operation,Operation> outCsecOp;

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
        orphaned_paths = new Hashtable<Integer, Operation>();
        block_count = new Hashtable<Integer, Integer>();
        inCsecOp = new Hashtable<>();
        outCsecOp = new Hashtable<>();
//        leastSCC = new HashSet<Operation>();
        find();
    }

    public void find() {
        stack.empty();
        int i = 0;
        while (i < graph.VList.size()-1) {
            if (foundDeadlock) return;
            Set<Operation> leastSCC = getLeastSCC(i);
//            System.out.println("i = "+leastVertex+ " first op is "+graph.VList.get(leastVertex).getStrInfo());
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
                i = graph.VList.size()-1;
            }
        }
    }

    Set<Operation> getLeastSCC(int i) {
        TarjanSCC tarjanSCC = new TarjanSCC(graph, i);
        leastVertex = tarjanSCC.leastvertex;
        return tarjanSCC.leastSubVectors;
    }

    public boolean circuit(Set<Operation> dg, int v, int s) {
        boolean f = false;
        if (foundDeadlock) return f;
        Operation vertex = graph.VList.get(v);
        Operation startvertex = graph.VList.get(s);

        if (stack.empty() || stack.peek().proc != vertex.proc) {//if the vertex is the first node for the orphaned or if the node from a new process
            orphaned.push(vertex);
        }
        stack.push(vertex);
        //block_count count the number of operations besides the sends which in infinite buffer (<pro.rank, num>)
        if ((vertex.isWait()&&vertex.req.isRecv())
                || (vertex.isWait()&&vertex.req.isSend()&&(!checkInfiniteBuffer))
                || vertex.isBarrier()
                || (vertex.isCsecOperation &&
                    (vertex.isRecv() || (vertex.isSend() && !checkInfiniteBuffer)))){//TRUE:wait for recv || wait for zero buffer send || barrier
            if (!block_count.containsKey(vertex.proc))
                block_count.put(vertex.proc, 1);
            else block_count.put(vertex.proc, block_count.get(vertex.proc) + 1);
        }

        blocked.put(vertex, true);

        //all the operation in leastSCC that v can connect
        HashSet<Operation> adj_leastSCC = new HashSet<Operation>();

        continuepoint:
        for (Operation w : graph.ETable.get(vertex)) {
            if (vertex.proc==w.proc){
                if (vertex.isCsecOperation) outCsecOp.put(vertex,((CsecOperation) vertex).OperationList.getLast());
                if (w.isCsecOperation) inCsecOp.put(w,((CsecOperation) w).OperationList.getFirst());
            }

            if(w.proc!=vertex.proc && vertex.isCsecOperation){
                updateCsecOutPair((CsecOperation) vertex,w);
            }
            else if(w.proc!=vertex.proc && w.isCsecOperation){
                updateCsecInPair((CsecOperation) w,vertex);
            }


            if (!(good_edge(vertex, w, startvertex))) {//check whether the path is good_edge
//                System.out.println("no good edge : v="+vertex.getStrInfo()+" w="+w.getStrInfo());
                continue continuepoint;
            }

            if (vertex.proc != w.proc)//???the orphaned_paths save <v.proc,w>
            {
                orphaned_paths.put(vertex.proc, w);
            }

            Stack<Operation> stackclone = (Stack<Operation>) stack.clone();
//            System.out.println(stackclone+" "+w.getStrInfo());

            adj_leastSCC.add(w);
            if (w == startvertex) {// find the circle
                if (stack.size() > 2) {

                    count_cut = count_cut + stack.size();
                    if(appendPatternToList(stackclone)) {
                        f = true;
                    }

                    if (count_cut >= 500000000)//time out
                        return f;
                }

            } else {
                if (blocked.containsKey(w) && !blocked.get(w)) {
                    f = circuit(dg, graph.VList.indexOf(w), s);
                }else{
//                    System.out.println("blocked :"+w.getStrInfo()+ " v="+vertex.getStrInfo());
                }
            }
        }

        if (f) {
            unblock(vertex, blocked, blockedNodes);
        } else {
            for (Operation w : adj_leastSCC) {
                if (blockedNodes.containsKey(w) && !blockedNodes.get(w).contains(vertex)) {
                    blockedNodes.get(w).add(vertex);
                }
            }
        }
        //
        if (orphaned.peek().equals(stack.peek())) {
            orphaned.pop();
            orphaned_paths.remove(stack.peek());
        }
        stack.pop();

        if ((vertex.isWait()&&vertex.req.isRecv())
                || (vertex.isWait()&&vertex.req.isSend()&&(!checkInfiniteBuffer))
                || vertex.isBarrier()){//TRUE:wait for recv || wait for zero buffer send || barrier
            block_count.put(vertex.proc, block_count.get(vertex.proc) - 1);
        }

        return f;
    }

    /**
     * append pattern to the list should do :
     * check this pattern is new?
     * @param stack
     */
    private boolean appendPatternToList(Stack<Operation> stack) {
        Stack<Operation> stack1 = updateStack(stack);
        if (isNewPattern(stack)) {
            Pattern pattern = new Pattern(graph.program, stack1);
            patterns.add(pattern);

//            foundDeadlock = true;//Need to be delete!

            if (pattern.status== Status.SATISFIABLE){
                foundDeadlock = true;
            }else if (pattern.status == Status.UNREACHABLE){
                filterNum += 1;
            }else if (pattern.status == Status.UNSATISFIABLE){
                filterSMTNum += 1;
            }
            return true;
        }
        return false;
    }

    public boolean good_edge(Operation v, Operation w, Operation s) {

        if (v.proc == w.proc)
            return true;

        if (v.isWait() && v.proc!=w.proc)
            return false;

        //if infinite buffer, the first action in any process should not be a send
        if (checkInfiniteBuffer && w.isSend()) {
//            System.out.println("infinite buffer");
            return false;
        }

        //if(w.process.getRank() != s.process.getRank() && proc_stack.contains(w.process.getRank()))
        //return false;

        if (!block_count.containsKey(v.proc)) {
//            System.out.println("blocked count");
            return false;
        }

        if (!stack.peek().isCsecOperation) {//if this operation is non-csec-action, then they should satisfy those
            if (stack.size() == 1) //has to travel down the process of startvertex first instead of jumping to another process
            {
                return false;
            } else if (stack.size() > 1) {
                if (stack.peek().proc != stack.get(stack.size() - 2).proc) {
//                System.out.println("stack peek proc rank same");
                    return false;
                }
            }
        }

        if (w.isRecv() && w.src == -1)
            return false;

        for (Operation a : orphaned) {
            if (can_match(a, w)) {
//                System.out.println("can match");
                return false;
            }
        }

        if (orphaned_paths.containsKey(v.proc) && orphaned_paths.get(v.proc).equals(w)) {
//            System.out.println("orphaned_paths");
            return false;
        }

        if (!w.equals(s)) {
            for (Operation a : stack) {
                if (a.proc == w.proc)
                    return false;
            }
            return true;
        }

        if(v.isCsecOperation){
            if(stack.size()==1){
                return true;
            }else if (stack.size()>1){
                if(outCsecOp.get(v).rank<=inCsecOp.get(v).rank) return false;
            }
        }
        if (w.isCsecOperation && w.equals(s)){
            if (outCsecOp.get(w).rank<=inCsecOp.get(w).rank) return false;
        }

        return true;
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

    //recursion
    public void unblock(Operation v, Hashtable<Operation, Boolean> blocked,
                        Hashtable<Operation, List<Operation>> blockedNodes) {
        blocked.put(v, false);
        while (blockedNodes.get(v).size() > 0) {
            Operation w = blockedNodes.get(v).remove(0);
            if (blocked.get(w)) {
                unblock(w, blocked, blockedNodes);
            }
        }
    }

    void updateCsecOutPair(CsecOperation v, Operation w){
        if(w.isCsecOperation){
            for(Operation opw : ((CsecOperation)w).OperationList){
                for(int i = v.OperationList.size()-1;i>=0;i--){
                    Operation operation = (v).OperationList.get(i);
                    LinkedList<Operation> matchList;
                    if(operation.isRecv()){
                        matchList = graph.program.matchTables.get(operation);
                    }else matchList = graph.program.matchTablesForS.get(operation);
                    if (matchList.contains(opw)) {
                        outCsecOp.put(v, operation);
                        inCsecOp.put(w,opw);
                        break;
                    }
                }
            }
        }else{
            for(int i = (v).OperationList.size()-1;i>=0;i--){
                Operation operation = (v).OperationList.get(i);
                LinkedList<Operation> matchList;
                if(operation.isRecv()){
                    matchList = graph.program.matchTables.get(operation);
                }else matchList = graph.program.matchTablesForS.get(operation);
                if (matchList.contains(w)) {
                    outCsecOp.put(v, operation);
                    break;
                }
            }
        }
    }

    void updateCsecInPair(CsecOperation w, Operation v){
        if (!v.isCsecOperation){
            for (Operation operation : w.OperationList){
                if (v.isBot()){
                    inCsecOp.put(w, operation);
                    break;
                }
                LinkedList<Operation> matchList;
                if(operation.isRecv()){
                    matchList = graph.program.matchTables.get(operation);
                }else matchList = graph.program.matchTablesForS.get(operation);
                if(matchList.contains(v)){
                    inCsecOp.put(w,operation);
                    break;
                }
            }
        }
    }

    public void printCycles(Stack<Operation> stack){
        System.out.println("[JOHNSON]: THE CYCLE IS AS FOLLOWING :");
        for(Operation operation : stack){
            System.out.print(" "+operation.getStrInfo()+" --> ");
        }
        System.out.println(" ");
    }
    public LinkedList<Pattern> getPatterns() {
        return patterns;
    }

    boolean isNewPattern(Stack<Operation> stack){
        Hashtable<Integer, Operation> patterntable = Pattern.generatePatternTable(stack,checkInfiniteBuffer);
        for(Pattern pattern1 : patterns){
            if(patterntable.equals(pattern1.patternTable)){
                return false;
            }
        }
        return true;
    }

    Stack<Operation> updateStack(Stack<Operation> stack){
        Stack<Operation> stack1 = new Stack<>();
        for (Operation operation : stack){
            if (!operation.isCsecOperation) stack1.add(operation);
            else{
                stack1.add(inCsecOp.get(operation));
            }
        }
        return stack1;
    }

    public static void main(String[] args) {
//        String directoryName = "./src/test/fixtures";
//        File Dire = new File(directoryName);
//        Program program;
//        for(File file : Dire.listFiles()){
//            if(!file.isDirectory()){
//                System.out.println("-----------------------"+file.getName()+"----------------------");
//                long t1 = System.currentTimeMillis();
//                program = new Program(file.getPath(),false);
//                Graph graph = new Graph(program);
//                Johnson johnson = new Johnson(graph);
//                long t2 = System.currentTimeMillis();
//                System.out.println("Program executes " + ((double)(t2-t1))/(double)1000 + "seconds");
////                program = new Program(file.getPath(),false);
////                NewProgram newProgram = new NewProgram(program);
////                Graph graph1 = new Graph(newProgram);
////                Johnson johnson1 = new Johnson(graph1);
////                long t3 = System.currentTimeMillis();
////                System.out.println("Program executes " + ((double)(t3-t2))/(double)1000 + "seconds");
//                System.out.println(" the patterns number is : "+johnson.patterns.size());
//                System.out.println(" filter pattern has : "+johnson.filterNum);
//                System.out.println(" filter by smt solver has : "+johnson.filterSMTNum);
//                System.out.println("====================================\n");
//            }
//        }
        assert true;
        long t1 = System.currentTimeMillis();
//        Program program = new Program("./src/test/fixtures/is256.txt",true);
        NewProgram program = new NewProgram("./src/test/fixtures/diffusion2d4.txt",false);
//        Program program = new Program("./src/test/fixtures/deep100.txt",true);
        System.out.println("calls number is : "+program.getOpsCount());
        Graph graph = new Graph(program);
//        graph.printGraphETable();
        Johnson johnson = new Johnson(graph);
        System.out.println(" the patterns number is : "+johnson.patterns.size());
        System.out.println(" filter pattern has : "+johnson.filterNum);
        System.out.println(" filter by smt solver has : "+johnson.filterSMTNum);
        long t2 = System.currentTimeMillis();
        System.out.println("Program executes " + ((double)(t2-t1))/(double)1000 + "seconds");

    }
}















class TarjanSCC {
    private boolean[] marked;        // marked[v] = has v been visited?

    private LinkedList<HashSet<Operation>> sccs;
    public HashSet<Operation> leastSubVectors;
    public int leastvertex;
    private int[] low;               // low[v] = low number of v
    private int pre;                 // preorder number counter
    private int count;               // number of strongly-connected components
    private Stack<Operation> stack;
    Graph G;

    //used to filter any vertices lower than lowerbound and any edge connecting those vertices
    private int lowerbound;

    public TarjanSCC(Graph graph, int lb) {
        this.lowerbound = lb;
        this.G = graph;
        int Vsize = G.getVSize();
        marked = new boolean[Vsize];
        stack = new Stack<Operation>();
        sccs = new LinkedList<HashSet<Operation>>();
        leastSubVectors = null;
        leastvertex = lowerbound;
        low = new int[Vsize];
//        pre = 0;
//        count = 0;


        int minrank = Integer.MAX_VALUE;

        //only start from lowerbound
        for (int i = lowerbound; i < Vsize; i++) {
            if (!marked[i]) dfs(G, G.VList.get(i), minrank);
        }
    }


    private void dfs(Graph G, Operation v, int minrank) {
        int vrank = G.VList.indexOf(v);
        marked[vrank] = true;
        low[vrank] = pre++;
        int min = low[vrank];
        stack.push(v);
        if (G.adj(v) != null) {
            for (Operation w : G.adj(v)) {
                //only consider all the edges with vertices >= lowerbound
                if (G.VList.indexOf(w) >= lowerbound) {
                    int wrank = G.VList.indexOf(w);
                    if (!marked[wrank]) dfs(G, w, minrank);
                    if (low[wrank] < min) min = low[wrank];
                }
            }
        }
        if (min < low[vrank]) {
            low[vrank] = min;
            return;
        }
        Operation w;
        int minlocal = Integer.MAX_VALUE;
        do {
            w = stack.pop();
            int wrank = G.VList.indexOf(w);
            if (wrank < minlocal) {
                minlocal = wrank;
            }
            if (sccs.size() <= count)
                sccs.add(new HashSet<Operation>());
            sccs.get(count).add(w);
//            id[wrank] = count;
            low[wrank] = G.getVSize();
        } while (w != v);

        //this scc is assigned leastSCC iff it has the least vertex and the size of this scc > 1
        if (minlocal < minrank && sccs.get(count).size() > 1) {
            minrank = minlocal;
            leastvertex = minrank;
            leastSubVectors = sccs.get(count);
        }
        count++;
    }
}
