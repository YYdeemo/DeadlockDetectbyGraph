package syntax;

import constant.OPTypeEnum;

import java.util.LinkedList;

public class CsecOperation extends Operation implements Cloneable{
    public LinkedList<Operation> OperationList;

    public CsecOperation(OPTypeEnum type, int index, int proc, int src, int dst, int tag, int group, int reqID) {
        super(type, index, proc, src, dst, tag, group, reqID);
        OperationList = new LinkedList<>();
        isCsecOperation = true;
    }

    public void appendOpToList(Operation operation){
        OperationList.add(operation);
    }

    public int getSize(){
        return OperationList.size();
    }


    @Override
    public String getStrInfo() {
        return "Csec"+super.getStrInfo();
    }

    @Override
    public String toString() {
        return getStrInfo();
    }


}
