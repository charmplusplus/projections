package projections.analysis;
import java.util.Vector;

import projections.gui.Analysis;

public class AmpiFunctionData {
    // AMPI function tracing
    public int FunctionID, LineNo;
    public String sourceFileName;

    private long accExecTime;

    private Vector execIntervals;

    public AmpiFunctionData() {
        FunctionID = LineNo = 0;
        sourceFileName = null;
        accExecTime = 0;
        execIntervals = new Vector();
    }

    public AmpiFunctionData(int funcId, int line, String srcFileName) {
        FunctionID = funcId;
        LineNo = line;
        sourceFileName = srcFileName;
        accExecTime = 0;
        execIntervals = new Vector();
    }

    public void incrAccExecTime(long t) { accExecTime += t; }
    public long getAccExecTime() { return accExecTime; }

    public String toString(){
        return sourceFileName+"@"+LineNo+"::"+FunctionID+" : "+accExecTime;
    }

    public Vector getExecIntervals(){
        return execIntervals;
    }

    public void insertExecInterval(AmpiFuncExecInterval interval){
        execIntervals.add(interval);
    }

    public AmpiFuncExecInterval getIntervalAt(int i){
        return (AmpiFuncExecInterval)execIntervals.get(i);
    }

    public int execIntervalCnt(){
        return execIntervals.size();
    }

    public String getFunctionName(){
        String name = Analysis.getFunctionName(FunctionID);
        return name+"@"+sourceFileName+"("+LineNo+")";
    }

    public static class AmpiFuncExecInterval {
        public long startTimestamp;
        public long endTimestamp;

        public AmpiFuncExecInterval(long begin, long end){
            startTimestamp = begin;
            endTimestamp = end;
        }
        
        public String toString(){
            return "Start: "+startTimestamp+"; End: "+endTimestamp;
        }
    }
}

