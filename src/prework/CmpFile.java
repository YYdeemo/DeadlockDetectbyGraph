package prework;

import constant.Constants;
import constant.OPTypeEnum;
import syntax.Operation;
import syntax.Process;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

/*
 * # syntax (ctp in Python Project)
 * # send - s proc dst id
 * # recv - r proc src id
 * # wait - w proc idx
 * # barr - b proc group id
 *
 *
 * <p>
 * add the wait to the ctp, and save as a new file which is named as *_cpm.txt
 * if receive is blocking ,then we need to add the nearliest wait
 * if send is blocking and is synchronized or ready, then we need to add the nearliest wait
 */
public class CmpFile {

    public static LinkedList<String[]> getCTPFromFile(String filepath) {
        LinkedList<String[]> ctp = new LinkedList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String actionInfo;
            boolean lastLine = false;//which record the last line is empty
            //read each line of the file, add the action in ctp
            while ((actionInfo = reader.readLine()) != null) {
                if (actionInfo.isEmpty()) {//if the line is empty, then the next is new process
                    lastLine = true;
                    continue;
                }
                if (lastLine && actionInfo.matches("^(r|s|w|b|fs|as|g)") && ctp.size() > 0) {
                    ctp.add(new String[0]);
                }
                ctp.addLast(actionInfo.split(" "));
                lastLine = false;
            }
            reader.close();//close the buffered reader
        } catch (IOException error) {
            System.out.println("func:CompleteCTPFromFile()  ERROR:" + error.toString());
        }
        return ctp;
    }


    //the ctp from Python project
    public static Operation translateFromStrToOP(String[] aStr) {
        Operation operation = null;
        if (aStr.length <= 0)
            return null;
        if (aStr[0].matches("r|br")) {
            if(aStr.length==4) {
                operation = new Operation(FromTextGetType(aStr[0]),//type
                        Integer.parseInt(aStr[3]),//idx
                        Integer.parseInt(aStr[1]),//proc
                        Integer.parseInt(aStr[2]),//src
                        Integer.parseInt(aStr[1]),//dst
                        Constants.defultInt,//tag
                        Constants.defultInt,//group
                        Constants.defultInt//reqID
                );
            }else if(aStr.length==5){
                operation = new Operation(FromTextGetType(aStr[0]),//type
                        Integer.parseInt(aStr[4]),//idx
                        Integer.parseInt(aStr[1]),//proc
                        Integer.parseInt(aStr[2]),//src
                        Integer.parseInt(aStr[1]),//dst
                        Integer.parseInt(aStr[3]),//tag
                        Constants.defultInt,//group
                        Constants.defultInt//reqID
                );
            }
        } else if (aStr[0].matches("s|bs|ss|rs|as")) {
            if(aStr.length==4) {
                operation = new Operation(FromTextGetType(aStr[0]),//type
                        Integer.parseInt(aStr[3]),//id
                        Integer.parseInt(aStr[1]),//proc
                        Integer.parseInt(aStr[1]),//src
                        Integer.parseInt(aStr[2]),//dst
                        Constants.defultInt,//tag
                        Constants.defultInt,//group
                        Constants.defultInt//reqID
                );
            }else if(aStr.length==5){
                operation = new Operation(FromTextGetType(aStr[0]),//type
                        Integer.parseInt(aStr[4]),//id
                        Integer.parseInt(aStr[1]),//proc
                        Integer.parseInt(aStr[1]),//src
                        Integer.parseInt(aStr[2]),//dst
                        Integer.parseInt(aStr[3]),//tag
                        Constants.defultInt,//group
                        Constants.defultInt//reqID
                );
            }
        } else if (aStr[0].matches("w")) {
            operation = new Operation(OPTypeEnum.WAIT,//type
                    Constants.defultInt,//idx
                    Integer.parseInt(aStr[1]),//proc
                    Constants.defultInt,//src
                    Constants.defultInt,//dst
                    Constants.defultInt,//tag
                    Constants.defultInt,//group
                    Integer.parseInt(aStr[2])//the req action's Idx
            );
        } else if (aStr[0].matches("b")) {
            operation = new Operation(OPTypeEnum.BARRIER,//type
                    Integer.parseInt(aStr[3]),//id
                    Integer.parseInt(aStr[1]),//proc
                    Constants.defultInt,//src
                    Constants.defultInt,//dst
                    Constants.defultInt,//tag
                    Integer.parseInt(aStr[2]),//group
                    Constants.defultInt//reqID
            );
        }else if(aStr[0].matches("bd|ga|rd|sc")){
            //public Operation(OPTypeEnum type, int rank, int root, int proc, int group) {//collective operation;
            operation = new Operation(FromTextGetType(aStr[0]),
                    Integer.parseInt(aStr[4]),//id
                    Integer.parseInt(aStr[2]),//root
                    Integer.parseInt(aStr[1]),//proc
                    Integer.parseInt(aStr[3])//group
                    );
        }
        else {
            return null;
        }
        return operation;
    }

    public static OPTypeEnum FromTextGetType(String type) {
        switch (type) {
            case "r":
                return OPTypeEnum.RECV;
            case "br":
                return OPTypeEnum.B_RECV;
            case "s":
                return OPTypeEnum.SEND;
            case "bs":
                return OPTypeEnum.BUFFERED_SEND;
            case "ss":
                return OPTypeEnum.STANDARD_SEND;
            case "rs":
                return OPTypeEnum.READY_SEND;
            case "sds":
                return OPTypeEnum.STANDARD_SEND;
            case "bd":
                return OPTypeEnum.BROADCAST;
            case "ga":
                return OPTypeEnum.GATHER;
            case "sc":
                return OPTypeEnum.SCATTER;
            case "rd":
                return OPTypeEnum.REDUCE;
        }
        return null;
    }

    //TEST
    public static void main(String[] args) {
        String filepath = "./src/test/fixtures/test3.txt";
        LinkedList<String[]> ctp = getCTPFromFile(filepath);
        System.out.println(ctp.size());
    }

}
