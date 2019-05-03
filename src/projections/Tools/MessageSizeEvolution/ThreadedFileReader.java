package projections.Tools.MessageSizeEvolution;

import projections.analysis.EndOfLogSuccess;
import projections.analysis.GenericLogReader;
import projections.analysis.ProjDefs;
import projections.gui.MainWindow;
import projections.misc.LogEntryData;

import java.io.IOException;

class ThreadedFileReader
        implements Runnable {
    private int pe;
    private long startTime;
    private long endTime;
    private int myRun = 0;

    private int timeNumBins;
    private long timeBinSize;
    private int msgNumBins;
    private long msgBinSize;
    private long msgMinBinSize;
    private boolean msgLogScale;
    private boolean msgCreationEvent;

    private int[][]outputCounts;

    protected ThreadedFileReader(int[][] outputCounts, int pe, long startTime, long endTime, int timeNumBins, long timeBinSize, int msgNumBins, long msgBinSize, long msgMinBinSize, boolean msgLogScale, boolean msgCreationEvent) {
        this.pe = pe;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeNumBins = timeNumBins;
        this.timeBinSize = timeBinSize;
        this.msgNumBins = msgNumBins;
        this.msgBinSize = msgBinSize;
        this.msgMinBinSize = msgMinBinSize;
        this.outputCounts = outputCounts;
        this.msgLogScale = msgLogScale;
        this.msgCreationEvent = msgCreationEvent;
    }

    public void run() {
        int[][] myCounts = getCounts();
        // in synchronized manner accumulate into global counts:
        synchronized (outputCounts) {
            for (int i = 0; i < outputCounts.length; i++) {
                for (int j = 0; j < outputCounts[i].length; j++) {
                        outputCounts[i][j] += myCounts[i][j];
                }
            }
        }
    }

    private static int log2(long value) {
        if (value == 0)
            return 0;
        return Long.SIZE - Long.numberOfLeadingZeros(value);
    }

    private int[][] getCounts() {
        long adjustedTime;
        long adjustedSize;

        int[][] countData = new int[timeNumBins + 1][msgNumBins + 1];

        GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
        try {
            while (true) {
                LogEntryData logData = reader.nextEvent();
                if (logData.time > endTime)
                    break;

                switch (logData.type) {
                    case ProjDefs.CREATION:
                        if (!msgCreationEvent)
                            break;
                    case ProjDefs.BEGIN_PROCESSING:
                        if (logData.time < startTime || logData.time > endTime || (msgLogScale && logData.msglen < msgMinBinSize) || logData.pe == pe)
                            break;
                        adjustedSize = logData.msglen;
                        if (!msgLogScale)
                            adjustedSize -= msgMinBinSize;
                        adjustedTime = logData.time - startTime;
                        if (adjustedSize >= 0 && adjustedTime >= 0) {
                            int msgTargetBin;
                            if (msgLogScale)
                                msgTargetBin = log2(adjustedSize / msgMinBinSize);
                            else
                                msgTargetBin = (int) (adjustedSize / msgBinSize);
                            int timeTargetBin = (int) (adjustedTime / timeBinSize);
                            msgTargetBin = Math.min(msgTargetBin, msgNumBins);
                            timeTargetBin = Math.min(timeTargetBin, timeNumBins);
                            countData[timeTargetBin][msgTargetBin] += 1;
                        }
                        break;
                }
            }
        } catch (EndOfLogSuccess e) {
            // successfully reached end of log file
        } catch (Exception e) {
            System.err.println("Exception" + e);
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            reader.close();
        } catch (IOException e) {
            System.err.println("Error: could not close log file reader for processor " + pe);
        }

        return countData;
    }
}
