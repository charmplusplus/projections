package projections.Tools.EntryMethodProfile;

import java.io.IOException;
import java.util.*;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;

class ThreadedFileReader implements Runnable {

    private int pe;
    private int myRun;
    private int lastIndex;
    private long startTime;
    private long endTime;

    private Map<Integer, Long> load;

    /**
     * Construct a file reading thread that will determine the load for each entry function
     * <p>
     * The resulting output data will be assigned into the array specified without synchronization
     */
    protected ThreadedFileReader(int pe, int myRun, long startTime, long endTime) {
        this.pe = pe;
        this.myRun = myRun;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void run() {
        GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

        // First take data and put it into intervals.
        load = new TreeMap<Integer, Long>();
        Deque<LogEntryData> stack = new ArrayDeque<LogEntryData>();
        lastIndex = -1;

        try {
            while (true) {
                LogEntryData data = reader.nextProcessingEvent();

                if (data.time > endTime)
                    break;

                switch (data.type) {
                    case ProjDefs.BEGIN_PROCESSING:
                        stack.push(data);
                        break;
                    case ProjDefs.END_PROCESSING:
                        if (stack.isEmpty()) {
                            break;
                        }
                        LogEntryData beginData = stack.pop();
                        if (beginData.entry != data.entry) {
                            break;
                        } else if ((data.time - beginData.time) > 0 && data.time < endTime && beginData.time >= startTime) {
                            if (!load.containsKey(beginData.entry))
                                load.put(beginData.entry, data.time - beginData.time);
                            else
                                load.put(beginData.entry, load.get(beginData.entry) + (data.time - beginData.time));
                        }
                        lastIndex = Math.max(lastIndex, beginData.entry);
                        break;
                    default:
                        break;
                }
            }
        } catch (EndOfLogSuccess e) {
            // Done reading file
        } catch (IOException e) {
            // Error reading file
        }

        try {
            reader.close();
        } catch (IOException e1) {
            System.err.println("Error: could not close log file reader for processor " + pe);
        }
    }

    public Map<Integer, Long> getData() {
        return load;
    }

    public int getPe() {
        return pe;
    }

    int getLastIndex() {
        return lastIndex;
    }
}
