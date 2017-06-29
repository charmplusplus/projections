package projections.Tools.EntryMethodProfile;

import java.io.IOException;
import java.util.Stack;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;

class ThreadedFileReader implements Runnable  {

    private int pe;
    private int myRun;
    private int lastIndex;
    private long startTime;
    private long endTime;

    private double load[];

    private int _SIZE = 500000;

    /** Construct a file reading thread that will determine the load for each entry function
     *
     *  The resulting output data will be assigned into the array specified without synchronization
     *
     *  */
    protected ThreadedFileReader(int pe, int myRun, long startTime, long endTime){
        this.pe = pe;
        this.myRun = myRun;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void run() {
        GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());

        // First take data and put it into intervals.
        load = new double[_SIZE];
        Stack<LogEntryData> stack = new Stack<LogEntryData>();
        lastIndex = -1;

        try {
            while (true) {
//				System.out.println("c before " + (count));
                LogEntryData data = reader.nextEvent();
//				System.out.println("c after " + (count));

                switch(data.type) {
                    case ProjDefs.BEGIN_PROCESSING:
                        stack.push(data);
                        break;
                    case ProjDefs.END_PROCESSING:
                        if(stack.empty()) {
                            break;
                        }
                        LogEntryData beginData = stack.pop();
                        if(beginData.entry != data.entry) {
                            break;
                        } else if((data.time - beginData.time) > 0 && data.time < endTime && beginData.time >= startTime) {
                            if(beginData.entry > _SIZE) {
                                int newSize = _SIZE + beginData.entry;
                                double[] tempEvents = new double[newSize];
                                System.arraycopy(load, 0, tempEvents, 0, _SIZE);
                                load = tempEvents;
                                _SIZE = newSize;
                            }
                            load[beginData.entry] += (data.time - beginData.time);
                        }
                        lastIndex = Math.max(lastIndex, beginData.entry);
                        break;
                    default:
                        break;
                }
            }
        }
        catch (EndOfLogSuccess e) {
            // Done reading file
        } catch (IOException e) {
            // Error reading file
        }

        try {
            reader.close();
        } catch (IOException e1) {
            System.err.println("Error: could not close log file reader for processor " + pe );
        }
    }

    public double[] getData(){
        return load;
    }

    public long getPe() {
        return pe;
    }

    int getLastIndex() {
        return lastIndex;
    }
}





