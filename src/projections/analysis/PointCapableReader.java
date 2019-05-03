package projections.analysis;


import java.io.IOException;

import projections.misc.LogEntryData;

interface PointCapableReader {
    
    public LogEntryData nextEvent()
        throws IOException, EndOfLogSuccess;

    public LogEntryData nextEventOnOrAfter(long timestamp)
        throws IOException, EndOfLogSuccess;    

    public LogEntryData nextEventOfType(int... eventType)
        throws IOException, EndOfLogSuccess;

}
