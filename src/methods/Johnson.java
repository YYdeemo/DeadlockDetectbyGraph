package methods;

import constant.OPTypeEnum;
import syntax.*;

import java.util.*;

public class Johnson {
    /**
     * this class is just a function not a object,
     * which achieve finding the cycles in the graph by Johnson
     */
    public Graph graph;
    private int leastVertex;
    public LinkedList<Pattern> patterns;//which save the fake deadlock candidates

    private Stack<Operation> stack;//
    Hashtable<Operation, Boolean> blocked;
    Hashtable<Operation, List<Operation>> blockedNodes;
    Hashtable<Integer, Operation> orphaned_paths;
    Stack<Operation> orphaned;
    Hashtable<Integer, Integer> block_count;

    int count_cut = 0;

    public Johnson(Graph graph) {
        this.graph = graph;
        leastVertex = 0;
        patterns = new LinkedList<Pattern>();
        stack = new Stack<Operation>();
        blocked = new Hashtable<Operation, Boolean>();
        blockedNodes = new Hashtable<Operation, List<Operation>>();
        orphaned = new Stack<Operation>();
        orphaned_paths = new Hashtable<Integer, Operation>();
        block_count = new Hashtable<Integer, Integer>();
//        leastSCC = new HashSet<Operation>();
        find();
    }

    public void find() {
        stack.empty();
        int i = 0;
        while (i < graph.VList.size()-1) {
            if(i==16)
                assert true;
            Set<Operation> leastSCC = getLeastSCC(i);
            System.out.println("i = "+leastVertex+ " first op is "+graph.VList.get(leastVertex).getStrInfo());

            if (leastSCC != null) {
                i = leastVertex;
                blocked.clear();
                blockedNodes.clear();
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

    //if the find function has a Integer parameter, then the function is used to ...
    public void find(int i) {
        stack.empty();
        Set<Operation> leastSCC = getLeastSCC(i);
        if (leastSCC != null) {
            i = leastVertex;
            for (Operation v : leastSCC) {
                blocked.put(v, false);
                blockedNodes.put(v, new LinkedList<Operation>());
            }
            orphaned_paths.clear();
            orphaned.clear();

            circuit(leastSCC, i, i);
        } else {
            System.out.println("this find getLeastSCC is null, which means the subGrash is empty, where i=" + i);
        }
    }

    Set<Operation> getLeastSCC(int i) {
        TarjanSCC tarjanSCC = new TarjanSCC(graph, i);
        leastVertex = tarjanSCC.leastvertex;
        return tarjanSCC.leastSubVectors;
    }

    public boolean circuit(Set<Operation> dg, int v, int s) {
        boolean f = false;
        Operation vertex = graph.VList.get(v);
        Operation startvertex = graph.VList.get(s);

        if (stack.empty() || stack.peek().proc != vertex.proc) {//if the vertex is the first node for the orphaned or if the node from a new process
            orphaned.push(vertex);
        }
        stack.push(vertex);
        //block_count count the number of operations besides the sends which in infinite buffer (<pro.rank, num>)
        if ((vertex.isWait()&&vertex.req.isRecv())
                || (vertex.isWait()&&vertex.req.isSend()&&(!graph.program.isCheckInfiniteBuffer()))
                || vertex.isBarrier()){//TRUE:wait for recv || wait for zero buffer send || barrier
            if (!block_count.containsKey(vertex.proc))
                block_count.put(vertex.proc, 1);
            else block_count.put(vertex.proc, block_count.get(vertex.proc) + 1);
        }

        blocked.put(vertex, true);

        //all the operation in leastSCC that v can connect
        HashSet<Operation> adj_leastSCC = new HashSet<Operation>();

        continuepoint:
        for (Operation w : graph.ETable.get(vertex)) {

            if (!(good_edge(vertex, w, startvertex))) {//check whether the path is good_edge
                System.out.println("no good edge : v="+vertex.getStrInfo()+" w="+w.getStrInfo());
                continue continuepoint;
            }

            if (vertex.proc != w.proc)//???the orphaned_paths save <v.proc,w>
                orphaned_paths.put(vertex.proc, w);

            adj_leastSCC.add(w);
            Stack<Operation> stackclone = (Stack<Operation>) stack.clone();
            System.out.println(stackclone+" "+w.getStrInfo());
            if (w == startvertex) {// find the circle
                if (stack.size() > 2) {

                    count_cut = count_cut + stack.size();
                    Pattern pattern = new Pattern(graph, stackclone);//get the pattern from the circle

                    if (pattern.pattern.size() > 1 && isNewPattern(pattern)) {
                        printCycles(stackclone);
                        pattern.printPattern();
                        patterns.add(pattern);
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
                || (vertex.isWait()&&vertex.req.isSend()&&(!graph.program.isCheckInfiniteBuffer()))
                || vertex.isBarrier()){//TRUE:wait for recv || wait for zero buffer send || barrier
            block_count.put(vertex.proc, block_count.get(vertex.proc) - 1);
        }

        return f;
    }

    public boolean good_edge(Operation v, Operation w, Operation s) {

        if (v.proc == w.proc)
            return true;

        if (v.isWait() && v.proc!=w.proc)
            return false;

        //if infinite buffer, the first action in any process should not be a send
        if (graph.program.isCheckInfiniteBuffer() && w.isSend()) {
            System.out.println("infinite buffer");
            return false;
        }

        //if(w.process.getRank() != s.process.getRank() && proc_stack.contains(w.process.getRank()))
        //return false;

        if (!block_count.containsKey(v.proc)) {
            System.out.println("blocked count");
            return false;
        }

        if (stack.size() == 1) //has to travel down the process of startvertex first instead of jumping to another process
        {
            return false;
        } else if (stack.size() > 1) {
            if (stack.peek().proc != stack.get(stack.size() - 2).proc) {
                System.out.println("stack peek proc rank same");
                return false;
            }
        }

        if (w.isRecv() && w.src == -1)
            return false;

        for (Operation a : orphaned) {
            if (can_match(a, w)) {
                System.out.println("can match");
                return false;
            }
        }

        if (orphaned_paths.containsKey(v.proc) && orphaned_paths.get(v.proc).equals(w)) {
            System.out.println("orphaned_paths");
            return false;
        }

        if (!w.equals(s)) {
            for (Operation a : stack) {
                if (a.proc == w.proc)
                    return false;
            }
            return true;
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

    boolean isNewPattern(Pattern pattern){
        for(Pattern pattern1 : patterns){
            if(pattern.pattern.equals(pattern1.pattern)){
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        Program program = new Program("./src/test/fixtures/diffusion2d4.txt",false);
        NewProgram newProgram = new NewProgram(program);
//        program.checkInfiniteBuffer = false;
//        newProgram.checkInfiniteBuffer = false;
        Graph graph = new Graph(newProgram);
        graph.printGraphETable();
//        graph.printGraphVList();
        Johnson johnson = new Johnson(graph);
        System.out.println("the pattens size is : "+johnson.patterns.size());
        System.out.println(program.checkInfiniteBuffer);
        for(Pattern pattern : johnson.getPatterns()){
            pattern.printPattern();
        }
    }

    public void printleastSCC(Set<Operation> set){
        for (Operation operation : set){
            System.out.print(" "+operation.getStrInfo()+" ");
        }
        System.out.println(" ");
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
