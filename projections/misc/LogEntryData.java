package projections.misc;

/**
 *  Written by Chee Wai Lee
 *  4/12/2002
 *
 *  LogEntryData encapsulates data that can potentially be read from a
 *  projections log entry.
 *
 */

public class LogEntryData 
{
    // bad - should use accessors, but what the heck 8)
    public int type;
    public int mtype;
    public long time;
    public int entry;
    public int event;
    public int pe;
    public int msglen;

    public LogEntryData() {
    }
}
