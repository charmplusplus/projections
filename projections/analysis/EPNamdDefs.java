package projections.analysis;

// special (one-time) interface for static defintions for namd data analysis
public interface EPNamdDefs
{
    static final int NUM_TYPE = 1;
    static final int TIME_DATA = 0;
    
    static final int NUM_CATEGORY = 7;
    static final int COMPUTE_CAT = 0;
    static final int INTEGRATE_CAT = 1;
    static final int PME_CAT = 2;
    static final int MSG_CAT = 3;
    static final int MSG_PROC_CAT = 4;
    static final int OTHER_CAT = 5;
    static final int IDLE_CAT = 6;

    // NAMD hardcoded sts values (for this exercise)
    static final int ENQUEUE_A = 73;
    static final int ENQUEUE_B = 74;
    static final int DUMMY_THR = 0;
    static final int PME_ENQUEUE = 70;
    static final int USER_SEND = 30;
    static final int USER_PUMP = 10;

    // Idle, Pack and Unpack
    static final int NUM_SYS_EPS = 3;
    static final int NUM_USR_EVTS = 10;  // max ... to be un-hardcoded.

}

