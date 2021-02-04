package methods;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import syntax.Operation;
import syntax.Process;
import syntax.Program;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Match Pairs class just provide the static function for generating the over-approximated Match-Pairs
 * in the future, adding the function for generating the match-pairs with TAG
 */


public class MatchPairs {

    /**
     * firstly, generat the list for r[r.src] and s[s.dest][s.src]; when s.dest=r.src
     *
     * @param program
     * @return HashTable<Send, LinkedList < Recv>>;
     */

    public Hashtable<Operation, LinkedList<Operation>> matchTables;
    Program program;

    public MatchPairs(Program program){
        this.program = program;
        matchTables = new Hashtable<Operation, LinkedList<Operation>>();
        overApproximateMatchs(this.program);
    }
    public Hashtable<Operation, LinkedList<Operation>> overApproximateMatchs(Program program) {
        //put each send operation to the specific list
        LinkedList<Operation>[][] sendList = new LinkedList[program.getSize()][program.getSize()];
        for (Process process : program.getAllProcesses()) {
            for (Operation send : process.slist) {
                if (sendList[send.dst][process.rank] == null) sendList[send.dst][process.rank] = new LinkedList<>();
                sendList[send.dst][process.rank].add(send);
            }
        }
        //for each recv we find the set of sends which can match with it
        for (Process process : program.getAllProcesses()) {
            int[] rCount = new int[program.getSize() + 1];
            int sTotalCountForR = 0;
            for (int j = 0; j < sendList[process.rank].length; j++) {
                if (sendList[process.rank][j] != null)
                    sTotalCountForR += sendList[process.rank][j].size();
            }
            for (Operation recv : process.rlist) {
                LinkedList<Operation> sforR = new LinkedList<>();//the list of sends which can match with the recv
                for (int j = 0; j < sendList[process.rank].length; j++) {
                    if (sendList[process.rank][j] == null) continue;
                    Iterator<Operation> iterator_s = sendList[process.rank][j].iterator();
                    while (iterator_s.hasNext()) {
                        Operation send = iterator_s.next();
//                        System.out.println("S"+send.proc+"_"+send.index);
//                        System.out.println("R"+recv.proc+"_"+recv.index);
//                        System.out.println(send.index+" "+rCount[send.dst]+ " "+rCount[program.getSize()]+" "+sTotalCountForR+" "+sendList[send.dst][send.src].size());
                        if ((recv.src == send.src || recv.src == -1)//rule
                                && send.index <= rCount[send.dst] + rCount[program.getSize()] + 1 //rule 1
                                && send.index >= rCount[send.dst] + rCount[program.getSize()] - Integer.max(0, (sTotalCountForR - sendList[send.dst][send.src].size())) //rule2
                        ) sforR.add(send);
                    }
                }
//                System.out.println("recv.src"+recv.src);
                if (recv.src == -1) {
                    rCount[program.getSize()]++;
                } else {
                    rCount[recv.dst]++;
                }
                if (!sforR.isEmpty()) matchTables.put(recv, sforR);
            }
        }
        return matchTables;
    }

}
