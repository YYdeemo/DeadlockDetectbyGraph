package syntax;

import constant.Constants;
import constant.OPTypeEnum;
import constant.Pair;
import constant.Triple;
import sun.awt.image.ImageWatched;

import java.util.*;

public class ExProgram extends Program{

    private HashMap<Operation, LinkedList<Integer>> collectiveGroup;
    private HashMap<Integer, LinkedList<Integer>> barrierGroup;
    public HashMap<Triple<Integer, OPTypeEnum, Integer>, Integer> groupIDToTagID;//<groupID ， OPType, root>-->tag

    public HashSet<Integer> origTags = new HashSet<>();

    public ExProgram(Program program){
        collectiveGroup = new HashMap<>();
        barrierGroup = new HashMap<>();
        groupIDToTagID = new HashMap<>();
        generateALLCOpMap(program.groups);
        if(!initialize(program)) System.out.println("ERROR : Initialize Program Failed");
        cmpOPsInfo();
    }

    private void generateALLCOpMap(Hashtable<Integer,LinkedList<Operation>> groupOps){
        for(Integer g : groupOps.keySet()){
            HashMap<Pair<OPTypeEnum, Integer>, LinkedList<Operation>> otherOps = new HashMap<>();
            LinkedList<Operation> cOps = groupOps.get(g);
            for (Operation op : cOps){
                if(op.isBarrier()){
                    if(!barrierGroup.containsKey(g)) barrierGroup.put(g,new LinkedList<>());
                    barrierGroup.get(g).add(op.proc);
                }else{
                    Pair<OPTypeEnum,Integer> pair = new Pair<>(op.type,op.root);
                    if(!otherOps.containsKey(pair)) otherOps.put(pair,new LinkedList<>());
                    otherOps.get(pair).add(op);
                }
            }
            for(LinkedList<Operation> list : otherOps.values()){
                Operation root = null;
                LinkedList<Integer> integers = new LinkedList<>();
                for (Operation op:list) {
                    if(op.proc==op.root) {
                        root = op;
                    }else{
                        integers.add(op.proc);
                    }
                }
                collectiveGroup.put(root,integers);
            }
        }
    }

    private int getCollTag(Operation operation){
        Random random = new Random();
        Triple<Integer,OPTypeEnum,Integer> triple = new Triple<>(operation.group,operation.type,operation.root);
        if(!groupIDToTagID.containsKey(triple)){
            int tag = random.nextInt(1000)+999;
            groupIDToTagID.put(triple,tag);
            return tag;
        }else{
            return groupIDToTagID.get(triple);
        }
    }

    public boolean initialize(Program program){
        int n = 0;//为不同的tag值进行编号
        for(Process processOld : program.processes){
            Process process = new Process(processOld.rank);
            for(Operation operation : processOld.ops){
                if(operation.isSend() || operation.isRecv() || operation.isWait()){
                    process.append(operation);
                    origTags.add(operation.tag);
                }else{
                    LinkedList<Operation> operations = transfer(operation);
                    for (Operation op:operations) {
                        process.append(op);
                    }
                }
            }
            this.processes.add(process);
        }

        return true;
    }

    public LinkedList<Operation> transfer(Operation operation){
        LinkedList<Operation> opList = new LinkedList<>();
        if(operation.isBarrier()){
            LinkedList<Integer> procIDList = barrierGroup.get(operation.group);
            for(Integer procID : procIDList){
                if(procID!=operation.proc){
                    Operation send = new Operation(OPTypeEnum.SEND,operation.rank, operation.proc,operation.proc,procID,getCollTag(operation),operation.group,operation.reqID);
                    opList.add(send);
                }
            }
            for(Integer procID : procIDList){
                if(procID!=operation.proc){
                    Operation recv = new Operation(OPTypeEnum.RECV,operation.rank, operation.proc,procID,operation.proc,getCollTag(operation),operation.group,operation.reqID);
                    opList.add(recv);
                    Operation wait = new Operation(OPTypeEnum.WAIT, operation.rank, operation.proc, recv);
                    opList.add(wait);
                }
            }
        }else if(operation.isBroadcast()){
            if(operation.proc==operation.root){
                LinkedList<Integer> procIDList = collectiveGroup.get(operation);
                for(Integer procID : procIDList){
                    if (procID != operation.proc) {
                        Operation send = new Operation(OPTypeEnum.SEND, operation.rank, operation.proc, operation.proc, procID, getCollTag(operation), operation.group, operation.reqID);
                        opList.add(send);
                    }
                }
            }else{
                Operation recv = new Operation(OPTypeEnum.RECV,operation.rank, operation.proc, operation.root,operation.proc,getCollTag(operation),operation.group,operation.reqID);
                opList.add(recv);
                Operation wait = new Operation(OPTypeEnum.WAIT, operation.rank, operation.proc, recv);
                opList.add(wait);
            }
        }else if(operation.isGather()){
            if(operation.proc==operation.root){
                LinkedList<Integer> procIDList = collectiveGroup.get(operation);
                for(Integer procID : procIDList){
                    if(procID!=operation.proc){
                        Operation recv = new Operation(OPTypeEnum.RECV,operation.rank, operation.proc,procID,operation.proc,getCollTag(operation),operation.group,operation.reqID);
                        opList.add(recv);
                        Operation wait = new Operation(OPTypeEnum.WAIT, operation.rank, operation.proc, recv);
                        opList.add(wait);
                    }
                }
            }else{
                Operation send = new Operation(OPTypeEnum.SEND,operation.rank, operation.proc,operation.proc, operation.root,getCollTag(operation),operation.group,operation.reqID);
                opList.add(send);
            }
        }else if(operation.isScatter()){
            if(operation.proc==operation.root){
                LinkedList<Integer> procIDList = collectiveGroup.get(operation);
                for(Integer procID : procIDList){
                    if (procID != operation.proc) {
                        Operation send = new Operation(OPTypeEnum.SEND, operation.rank, operation.proc, operation.proc, procID, getCollTag(operation), operation.group, operation.reqID);
                        opList.add(send);
                    }
                }
            }else{
                Operation recv = new Operation(OPTypeEnum.RECV,operation.rank, operation.proc, operation.root,operation.proc,getCollTag(operation),operation.group,operation.reqID);
                opList.add(recv);
                Operation wait = new Operation(OPTypeEnum.WAIT, operation.rank, operation.proc, recv);
                opList.add(wait);
            }
        }else if(operation.isReduce()){
            if(operation.proc==operation.root){
                LinkedList<Integer> procIDList = collectiveGroup.get(operation);
                for(Integer procID : procIDList){
                    if (procID != operation.proc) {
                        Operation send = new Operation(OPTypeEnum.SEND, operation.rank, operation.proc, operation.proc, procID, getCollTag(operation), operation.group, operation.reqID);
                        opList.add(send);
                    }
                }
            }else{
                Operation recv = new Operation(OPTypeEnum.RECV,operation.rank, operation.proc, operation.root,operation.proc,getCollTag(operation),operation.group,operation.reqID);
                opList.add(recv);
                Operation wait = new Operation(OPTypeEnum.WAIT, operation.rank, operation.proc, recv);
                opList.add(wait);
            }
        }

        return opList;
    }

    @Override
    void cmpOPsInfo() {
        int i = 0;
        for(Process process : this.processes){
            for(Operation operation : process.ops){
                operation.indx = i;
                i++;
            }
        }
    }

    private void printCollectiveGroup(){
        for(Operation root : collectiveGroup.keySet()){
            LinkedList<Integer> integers = collectiveGroup.get(root);
            System.out.print("<"+root+"> : ");
            for(Integer integer : integers){
                System.out.print(integer+" ");
            }
            System.out.println(" ");
        }
        for(Integer barrierGroupID : barrierGroup.keySet()){
            LinkedList<Integer> operations = barrierGroup.get(barrierGroupID);
            System.out.print("<"+barrierGroupID+"> : ");
            for(Integer integer : operations){
                System.out.print(integer+", ");
            }
            System.out.println(" ");
        }
    }

    public static void main(String[] args) {
        String filename = "./src/test/fixtures/combine/myTest2.txt";
        Program program = new Program(filename);
        program.printALLOperations();
        ExProgram exProgram = new ExProgram(program);
//        exProgram.printCollectiveGroup();
        exProgram.printALLOperations();
        exProgram.setMatchTables();
        exProgram.printMatchPairs();

    }


}
