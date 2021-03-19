package syntax;

import constant.OPTypeEnum;

import java.util.LinkedList;

public class CsecOperation extends Operation {
    public LinkedList<Operation> csecOperationList;

    public CsecOperation(OPTypeEnum type, int index, int proc, int src, int dst, int tag, int group, int reqID) {
        super(type, index, proc, src, dst, tag, group, reqID);
        isCsecOperation = true;
    }

    public void appendOpToList(Operation operation){
        csecOperationList.add(operation);
    }

    public int getSize(){
        return csecOperationList.size();
    }

    public boolean isCsecRecv(){
        return this.type==OPTypeEnum.RECV;
    }

    @Override
    public String getStrInfo() {
        return "Csec"+super.getStrInfo();
    }

    @Override
    public String toString() {
        return getStrInfo();
    }

    public boolean isCsecSend(){
        return this.type == OPTypeEnum.SEND;
    }

}
