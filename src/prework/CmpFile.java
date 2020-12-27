package prework;

import java.io.*;
import java.util.LinkedList;

/*
*   syntax:
*       send - s idx proc dst is_blocking send_mode
*       recv - r idx proc src is_blocking
*       wait - w idx proc req
*       barr - b idx proc group
*   the example of the original file is as following:
*       recv 0 0 1 true
*       send 1 0 1 true buffer_mode
*       wait 2 0 0
*       barr 3 0 0
*
* add the wait to the ctp, and save as a new file which is named as *_cpm.txt
* if receive is blocking ,then we need to add the nearliest wait
* if send is blocking and is synchronized or ready, then we need to add the nearliest wait
*
 */
public class CmpFile {

    public static LinkedList<String[]> CompleteCTPFromFile(String filepath){
        LinkedList<String[]> ctp = new LinkedList<>();
        try{
            //read the ctp from the file with filepath
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String actionInfo;//the each line is an action
            int count = 0;
            boolean hasBlocking = false;//whether a process has blocking operations such as recv, ssend , rsend
            //read each line of the file, add the action in ctp
            while((actionInfo=reader.readLine())!=null){
                if(actionInfo.isEmpty()){//if the line is empty, then the next is new process
                    ctp.add(new String[0]);
                    count = 0;
                    hasBlocking = false;
                    continue;
                }
                ctp.addLast(actionInfo.split(" "));
                if(hasBlocking){
                    ctp.getLast()[1] = String.valueOf(count);
                }
                count++;
                if(ctp.getLast()[0].matches("r|ss|rs")){
                    //if the last action is bllocking recv or ready/synchronized send, then we add the nearliest wait for them.
                    String[] a_blocking = ctp.getLast();
                    //add the wait is "w count a'processid a.id"
                    String[] new_wait = {"w",String.valueOf(count),a_blocking[2],a_blocking[1]};
                    ctp.addLast(new_wait);
                    count++;
                    hasBlocking = true;
                }
            }
            reader.close();//close the buffered reader
        }catch (IOException error){
            System.out.println(error.toString());
        }

        return ctp;
    }

    public static void WriteCTPtoFile(LinkedList<String[]> ctp, String filepath){
        try {
            FileWriter fwriter = new FileWriter(filepath.replaceAll(".txt", "_cmp.txt"));
            //get the completed ctp from above function, then write it to the new file with same format
            BufferedWriter bwriter = new BufferedWriter(fwriter);
            for (String[] action : ctp) {
                String actionStr = "";
                if(action.length>0) {
                    for (String astr : action){
                        actionStr = actionStr + astr + " ";
                    }
                }
                bwriter.write(actionStr+"\t\n");
            }
            bwriter.close();
            fwriter.close();
        }catch (IOException error){
            System.out.println(error.toString());
        }
    }

    //TEST
    public static void main(String[] args){
        String filepath = "./src/test/test_ctp.txt";
        LinkedList<String[]> ctp = CompleteCTPFromFile(filepath);
        WriteCTPtoFile(ctp,filepath);
        System.out.println(ctp.size());
    }
}
