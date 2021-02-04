package prework;

import constant.Constants;
import constant.OPTypeEnum;
import syntax.Operation;

import java.io.*;
import java.util.LinkedList;

/*
 * # syntax (ctp in Python Project)
 * # send - s proc dst idx
 * # recv - r proc src idx
 * # wait - w proc idx
 * # barr - b proc group
 *
 * syntax: (ctp definited by myself)
 * send - s idx proc src dst   (s, is, ss, rs, ds) where is is non-blocking send;
 * recv - r idx proc src dst   (r, ir) where ir is the non-blocking recv;
 * wait - w idx proc req
 * barr - b idx proc group
 * the example of the original file is as following:
 * r 0 0 1 0
 * s 1 0 0 1
 * w 2 0 0
 * b 3 0 1
 * <p>
 * add the wait to the ctp, and save as a new file which is named as *_cpm.txt
 * if receive is blocking ,then we need to add the nearliest wait
 * if send is blocking and is synchronized or ready, then we need to add the nearliest wait
 */
public class CmpFile {

    /**
     * now we just consider the non-blocking operation
     * <p>
     * if there is blocking operation, then we add the wait for it when init the program
     * special for synchronous send which is non-blocing, but in semantic，it is blocking
     *
     * @param filepath
     * @return ctp
     */
    public static LinkedList<String[]> getCTPFromFile(String filepath) {
        LinkedList<String[]> ctp = new LinkedList<>();
        try {
            //read the ctp from the file with filepath
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String actionInfo;//the each line is an action
            boolean lastLine = false;//which record the last line is empty
            //read each line of the file, add the action in ctp
            while ((actionInfo = reader.readLine()) != null) {
                if (actionInfo.isEmpty()) {//if the line is empty, then the next is new process
                    lastLine = true;
                    continue;
                }
                if (lastLine && actionInfo.matches("^[rswb]") && ctp.size() > 0) {
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

    static String BlockToNonBlocking(String type) {
        String result = null;
        switch (type) {
            case "br":
                result = "r";
                break;
            case "bs":
                result = "s";
                break;
            default:
                result = type;
                break;
        }
        return result;
    }


    public static void WriteCTPtoFile(LinkedList<String[]> ctp, String filepath) {
        try {
            if (filepath.contains("fixtures")) filepath = filepath.replaceAll("fixtures", "fixtures/cmp_ctp");
            FileWriter fwriter = new FileWriter(filepath.replaceAll(".txt", "_cmp.txt"));
            //get the completed ctp from above function, then write it to the new file with same format
            BufferedWriter bwriter = new BufferedWriter(fwriter);
            for (String[] action : ctp) {
                String actionStr = "";
                if (action.length > 0) {
                    for (String astr : action) {
                        actionStr = actionStr + astr + " ";
                    }
                }
                bwriter.write(actionStr + "\t\n");
            }
            bwriter.close();
            fwriter.close();
        } catch (IOException error) {
            System.out.println("func:WriteCTPtoFile()  ERROR:" + error.toString());
        }
    }

    // ctp definited by myself
    public static Operation translateFromStrToOP2(String[] aStr) {
        Operation operation = null;
        if (aStr.length <= 0)
            return null;
        if (aStr[0].matches("r|br|s|bs|ss|rs|sds")) {
            operation = new Operation(FromTextGetType(aStr[0]),//type
                    Integer.parseInt(aStr[1]),//idx
                    Integer.parseInt(aStr[2]),//proc
                    Integer.parseInt(aStr[3]),//src
                    Integer.parseInt(aStr[4]),//dst
                    Constants.defultInt,//tag
                    Constants.defultInt,//group
                    Constants.defultInt//reqID
            );
        } else if (aStr[0].matches("w")) {
            operation = new Operation(OPTypeEnum.WAIT,//type
                    Integer.parseInt(aStr[1]),//idx
                    Integer.parseInt(aStr[2]),//proc
                    Constants.defultInt,//src
                    Constants.defultInt,//dst
                    Constants.defultInt,//tag
                    Constants.defultInt,//group
                    Integer.parseInt(aStr[3])//the req action's Idx
            );
        } else if (aStr[0].matches("b")) {
            operation = new Operation(OPTypeEnum.BARRIER,//type
                    Integer.parseInt(aStr[1]),//idx
                    Integer.parseInt(aStr[2]),//proc
                    Constants.defultInt,//src
                    Constants.defultInt,//dst
                    Constants.defultInt,//tag
                    Integer.parseInt(aStr[3]),//group
                    Constants.defultInt//reqID
            );
        } else {
            return null;
        }
        return operation;
    }

    //the ctp from Python project
    public static Operation translateFromStrToOP(String[] aStr) {
        Operation operation = null;
        if (aStr.length <= 0)
            return null;
        if (aStr[0].matches("r|br")) {
            operation = new Operation(FromTextGetType(aStr[0]),//type
                    Integer.parseInt(aStr[3]),//idx
                    Integer.parseInt(aStr[1]),//proc
                    Integer.parseInt(aStr[2]),//src
                    Integer.parseInt(aStr[1]),//dst
                    Constants.defultInt,//tag
                    Constants.defultInt,//group
                    Constants.defultInt//reqID
            );
        } else if (aStr[0].matches("s|bs|ss|rs|sds")) {
            operation = new Operation(FromTextGetType(aStr[0]),//type
                    Integer.parseInt(aStr[3]),//idx
                    Integer.parseInt(aStr[1]),//proc
                    Integer.parseInt(aStr[1]),//src
                    Integer.parseInt(aStr[2]),//dst
                    Constants.defultInt,//tag
                    Constants.defultInt,//group
                    Constants.defultInt//reqID
            );
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
                    Constants.defultInt,//idx
                    Integer.parseInt(aStr[1]),//proc
                    Constants.defultInt,//src
                    Constants.defultInt,//dst
                    Constants.defultInt,//tag
                    Integer.parseInt(aStr[2]),//group
                    Constants.defultInt//reqID
            );
        } else {
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
                return OPTypeEnum.B_SEND;
            case "ss":
                return OPTypeEnum.STANDARD_SEND;
            case "rs":
                return OPTypeEnum.READY_SEND;
            case "sds":
                return OPTypeEnum.STANDARD_SEND;
        }
        return null;
    }

    //TEST
    public static void main(String[] args) {
        String filepath = "./src/test/test_ctp.txt";
        LinkedList<String[]> ctp = getCTPFromFile(filepath);
        System.out.println(ctp.size());
    }
}
