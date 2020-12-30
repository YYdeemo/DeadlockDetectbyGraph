package syntax;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *  Match Pairs class just provide the static function for generating the over-approximated Match-Pairs
 *  in the future, adding the function for generating the match-pairs with TAG
 */


public class MatchPairs {

    /**
     * firstlt, generat the list for r[r.src] and s[s.dest][s.src]; when s.dest=r.src
     *
     * @param program
     * @return HashTable<Send, LinkedList<Recv>>;
     */
    public static Hashtable<Operation, LinkedList<Operation>> overApproximateMatchs(Program program){
        Hashtable<Operation, LinkedList<Operation>> matchTables = new Hashtable<>();
        //put each send operation to the specific list
        LinkedList<Operation>[][] sendList = new LinkedList[program.size][program.size];
        for(Process process : program.processArrayList){
            for(Operation send : process.slist){
                if(sendList[send.dst][process.rank]==null) sendList[send.dst][process.rank] = new LinkedList<>();
                sendList[send.dst][process.rank].add(send);
            }
        }
        //for each recv we find the set of sends which can match with it
        for(Process process : program.processArrayList){
            int[] rCount = new int[program.size+1];
            int sTotalCountForR = 0;
            for(int j = 0; j < sendList[process.rank].length; j++){
                if(sendList[process.rank][j] != null)
                    sTotalCountForR += sendList[process.rank][j].size();
            }
            for(Operation recv : process.rlist){
                LinkedList<Operation> sforR = new LinkedList<>();//the list of sends which can match with the recv
                for(int j = 0; j<sendList[process.rank].length; j++){
                    if(sendList[process.rank][j]==null)continue;
                    Iterator<Operation> iterator_s = sendList[process.rank][j].iterator();
                    while (iterator_s.hasNext()){
                        Operation send = iterator_s.next();
                        if((recv.src == send.src || recv.src==-1)//rule
                            && send.rank <= rCount[send.src] + rCount[program.size]+1 //rule 1
                            && send.rank >= rCount[send.src] + Integer.max(0,rCount[program.size] -(sTotalCountForR-sendList[send.dst][send.src].size())) //rule2
                        ){
                            sforR.add(send);
                        }
                    }
                    if(recv.src==-1) rCount[program.size]++;
                    else rCount[recv.src]++;

                    if(!sforR.isEmpty()) matchTables.put(recv,sforR);
                }
            }
        }
        return matchTables;
    }

}
