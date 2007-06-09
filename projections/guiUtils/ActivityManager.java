package projections.guiUtils;

/**
 *  ActivityManager.java
 *  Chee Wai Lee - 7/7/2004
 *  updated 8/8/2006
 *
 *  Previous approach too ambitious. This class is now a static class
 *  that moves code away from the overloaded Analysis.java class.
 *
 */
public class ActivityManager {

    public static final int NUM_ACTIVITIES = 4;
    public static final int PROJECTIONS = 0;
    public static final int USER_EVENTS = 1;
    public static final int FUNCTIONS = 2;
    public static final int POSE_DOP = 3;
    public static final String NAMES[] = 
    {"PROJECTIONS", "USER_EVENTS",
     "FUNCTIONS", "POSE_DOP"};

}
