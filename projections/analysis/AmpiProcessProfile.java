package projections.analysis;

import java.util.*;

/**
 * This class contains information related with ampi usage profile
 * @author Chao Mei
 */

public class AmpiProcessProfile {
    /* The accumlated execution time */ 
    private long accExecTime;
    private long beginTime;

    /*
     * The process id is same with tuple int field in the raw data 
     * from the entry BEGIN_PROCESSING in the log file
     */
    private ObjectId processID;

    /* 
     * Trace the AMPI user functions calls within this process.
     * After finishing processing one process, this variable should
     * not contain any values. Its containing values are all put into
     * the final functions call stack.
     * Every object in the stack is of class AmpiFunctionData
     */
    Stack auxCallFuncStack;  

    /*
     * This stack contains the functions call within this process.
     * The order of calling functions is in reverse order i.e. the
     * higher one encloses the lower one. 
     * Every object in the stack is of class AmpiFunctionData
     */
    Stack callFuncStack;

    public AmpiProcessProfile(long beginTime, ObjectId id) {
	accExecTime = 0;
	this.beginTime = beginTime;
	processID = id;
	auxCallFuncStack = new Stack();
	callFuncStack = new Stack();
    }

    public void incrAccExecTime(long t) { accExecTime += t; }
    public long getAccExecTime() { return accExecTime; }

    public Object toHashKey(){
        return processID.id[0]+":"+processID.id[1]+":"+processID.id[2];
    }

    public Stack getAuxCallFuncStack(){
        return auxCallFuncStack;
    }
    public Stack getFinalCallFuncStack(){
        return callFuncStack;
    }
}
