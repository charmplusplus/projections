package projections.analysis;


import java.io.IOException;

import projections.misc.LogEntry;

interface PointCapableReader {
    
    public LogEntry nextEvent()
        throws IOException, EndOfLogSuccess;

    public LogEntry nextEventOnOrAfter(long timestamp)
        throws IOException, EndOfLogSuccess;    

    public LogEntry nextEventOfType(int... eventType)
        throws IOException, EndOfLogSuccess;

}
