package projections.analysis;
import java.util.Vector;

import projections.gui.MainWindow;

public class AmpiFunctionData {
    // Temporary hardcode. This variable will be assigned appropriate
    // meaning in future versions of Projections that support multiple
    // runs.
    private static int myRun = 0;

    // AMPI function tracing
    public int FunctionID, LineNo;
    public String sourceFileName;

    private long accExecTime;
    private long lastBeginTime;

    private Vector execIntervals;

    public AmpiFunctionData() {
        FunctionID = LineNo = 0;
        sourceFileName = null;
        accExecTime = 0;
	lastBeginTime = 0;
        execIntervals = new Vector();
    }

//    public AmpiFunctionData(int funcId, int line, String srcFileName) {
//        FunctionID = funcId;
//        LineNo = line;
//        sourceFileName = srcFileName;
//        accExecTime = 0;
//        execIntervals = new Vector();
//    }

    protected void incrAccExecTime(long t) { accExecTime += t; }
    public long getAccExecTime() { return accExecTime; }

    protected void incrAccExecTimeNow (long now) { accExecTime += now-lastBeginTime; }
    public void setLastBeginTime(long t) { lastBeginTime = t; }

    public String toString(){
        return sourceFileName+"@"+LineNo+"::"+FunctionID+" : "+accExecTime;
    }


    protected void insertExecInterval(AmpiFuncExecInterval interval){
        execIntervals.add(interval);
    }

    public AmpiFuncExecInterval getIntervalAt(int i){
        return (AmpiFuncExecInterval)execIntervals.get(i);
    }

    public int execIntervalCnt(){
        return execIntervals.size();
    }

    public String getFunctionName(){
        String name = MainWindow.runObject[myRun].getFunctionName(FunctionID);
        return name+"@"+sourceFileName+"("+LineNo+")";
    }

    public static class AmpiFuncExecInterval {
        public long startTimestamp;
        public long endTimestamp;

        protected AmpiFuncExecInterval(long begin, long end){
            startTimestamp = begin;
            endTimestamp = end;
        }
        
        public String toString(){
            return "Start: "+startTimestamp+"; End: "+endTimestamp;
        }
    }
}

