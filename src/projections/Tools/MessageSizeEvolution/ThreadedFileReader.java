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
    private long timeMinBinSize;
    private int msgNumBins;
    private long msgBinSize;
    private long msgMinBinSize;
    private boolean msgLogScale;
    private boolean msgCreationEvent;

    private double [][][] outputCounts;

    protected ThreadedFileReader(double[][][] outputCounts, int pe, long startTime, long endTime, int timeNumBins, long timeBinSize, long timeMinBinSize, int msgNumBins, long msgBinSize, long msgMinBinSize, boolean msgLogScale, boolean msgCreationEvent) {
        this.pe = pe;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeNumBins = timeNumBins;
        this.timeBinSize = timeBinSize;
        this.timeMinBinSize = timeMinBinSize;
        this.msgNumBins = msgNumBins;
        this.msgBinSize = msgBinSize;
        this.msgMinBinSize = msgMinBinSize;
        this.outputCounts = outputCounts;
        this.msgLogScale = msgLogScale;
        this.msgCreationEvent = msgCreationEvent;
    }

    public void run() {
        double [][][] myCounts = getCounts();
        // in synchronized manner accumulate into global counts:
        synchronized (outputCounts) {
            for(int i = 0; i < outputCounts.length; i++) {
                for(int j = 0; j < outputCounts[i].length; j++) {
                    for(int k = 0; k < outputCounts[i][j].length; k++) {
                        outputCounts[i][j][k] += myCounts[i][j][k];
                    }
                }
            }
        }
    }

    private double[][][] getCounts() {
        long adjustedTime;
        long adjustedSize;

        int numEPs = MainWindow.runObject[myRun].getNumUserEntries()+1;
        double[][][] countData = new double[timeNumBins + 1][msgNumBins + 1][numEPs];

        GenericLogReader reader = new GenericLogReader(pe, MainWindow.runObject[myRun].getVersion());
        try {
            while(true) {
                LogEntryData logData = reader.nextEvent();
                switch (logData.type) {
                    case ProjDefs.CREATION:
                        if(!msgCreationEvent)
                            break;
                    case ProjDefs.BEGIN_PROCESSING:
                        if (logData.time < startTime || logData.time > endTime || (msgLogScale && logData.msglen < msgMinBinSize) || logData.pe == pe)
                            break;
                        adjustedSize = logData.msglen;
                        if(!msgLogScale)
                            adjustedSize -= msgMinBinSize;
                        adjustedTime = logData.time - timeMinBinSize;
                        if (adjustedSize >= 0 && adjustedTime >= 0) {
                            int msgTargetBin;
                            if(msgLogScale)
                                msgTargetBin = (int)Math.floor(Math.log(Math.floor(adjustedSize / msgMinBinSize)) / Math.log(2));
                            else
                                msgTargetBin = (int) (adjustedSize / msgBinSize);
                            int timeTargetBin = (int) (adjustedTime / timeBinSize);
                            if (msgTargetBin >= msgNumBins)
                                msgTargetBin = msgNumBins;
                            if (timeTargetBin >= timeNumBins)
                                timeTargetBin = timeNumBins;
                            countData[timeTargetBin][msgTargetBin][logData.entry] += 1.0;
                        }
                        break;
                }
            }
        } catch(EndOfLogSuccess e) {
            // successfully reached end of log file
        } catch (Exception e) {
            System.err.println("Exception" + e);
            e.printStackTrace();
            System.exit(-1);
        }

        try {
            reader.close();
        } catch (IOException e) {
            System.err.println("Error: could not close log file reader for processor " + pe );
        }

        return countData;
    }
}
