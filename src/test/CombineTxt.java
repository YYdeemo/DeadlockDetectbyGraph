package test;

import java.io.*;

public class CombineTxt {

    public static void main(String[] args) throws IOException {
        String direName = "/Users/apple/Desktop/Expriment/diff2d8/diffusion2d4_";
        String newFileName = "./diffusion2d8.txt";

        //get the completed ctp from above function, then write it to the new file with same format
        BufferedWriter bwriter = new BufferedWriter(new FileWriter(newFileName));
//        File director = new File(direName);
//        File[] files = director.listFiles();
        for (int i = 0; i < 8; i++){
            BufferedReader reader = new BufferedReader(new FileReader(direName+i+".txt"));
            String actionInfo;//the each line is an action
            while ((actionInfo = reader.readLine()) != null) {
                System.out.println(actionInfo);
                bwriter.write(actionInfo+"\n");
            }
            bwriter.write("\n");
            reader.close();
        }
        bwriter.close();
    }
}
