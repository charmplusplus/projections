package projections.analysis;


/** The ProjDefs class is the superclass for the projections classes that
 *  store information from the logs; the class only contains a set of
 *  constant definitions for numbers in the logs.
 */
public class ProjDefs extends java.lang.Object
{

/** Message Creation point */
public static final int CREATION                 = 1;

public static final int BEGIN_PROCESSING         = 2;
public static final int END_PROCESSING           = 3;
public static final int ENQUEUE                  = 4;
public static final int DEQUEUE                  = 5;
public static final int BEGIN_COMPUTATION        = 6;
public static final int END_COMPUTATION          = 7;

public static final int BEGIN_INTERRUPT          = 8;
public static final int END_INTERRUPT            = 9;
public static final int MESSAGE_RECV             = 10;
public static final int BEGIN_TRACE              = 11;
public static final int END_TRACE                = 12;
public static final int USER_EVENT               = 13;
public static final int BEGIN_IDLE               = 14;
public static final int END_IDLE                 = 15;
public static final int BEGIN_PACK               = 16;
public static final int END_PACK                 = 17;
public static final int BEGIN_UNPACK             = 18;
public static final int END_UNPACK               = 19;
public static final int CREATION_BCAST           = 20;

public static final int CREATION_MULTICAST       = 21;
public static final int BEGIN_FUNC               = 22;
public static final int END_FUNC                 = 23;

/** A record for a user supplied integer value, likely a timestep */
public static final int USER_SUPPLIED            = 26;

/** A record for the memory usage */
public static final int MEMORY_USAGE            = 27;

/** A record for a user supplied string */
public static final int USER_SUPPLIED_NOTE            = 28;


public static final int USER_EVENT_PAIR          = 100;

/* *** USER category *** */
public static final int NEW_CHARE_MSG            = 0;
public static final int NEW_CHARE_NO_BALANCE_MSG = 1;
public static final int FOR_CHARE_MSG            = 2;
public static final int BOC_INIT_MSG             = 3;
public static final int BOC_MSG                  = 4;
public static final int TERMINATE_TO_ZERO        = 5;  // never used ??
public static final int TERMINATE_SYS            = 6;  // never used ??
public static final int INIT_COUNT_MSG           = 7;
public static final int READ_VAR_MSG             = 8;
public static final int READ_MSG_MSG             = 9;
public static final int BROADCAST_BOC_MSG        = 10;
public static final int DYNAMIC_BOC_INIT_MSG     = 11;

/* *** IMMEDIATE category *** */
public static final int LDB_MSG                  = 12;
public static final int VID_SEND_OVER_MSG        = 13;
public static final int QD_BOC_MSG               = 14;
public static final int QD_BROADCAST_BOC_MSG     = 15;
public static final int IMM_BOC_MSG              = 16;
public static final int IMM_BROADCAST_BOC_MSG    = 17;
public static final int INIT_BARRIER_PHASE_1     = 18;
public static final int INIT_BARRIER_PHASE_2     = 19;

}
