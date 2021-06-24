package methods;

import syntax.Graph;
import syntax.Operation;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

public class TarjanSCC {
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
        pre = 0;
        count = 0;
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
