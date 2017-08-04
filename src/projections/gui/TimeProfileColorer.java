package projections.gui;

import java.awt.*;

public class TimeProfileColorer implements GenericGraphColorer{
    /** A class that provides the colors for the display */
    int myRun = 0;
    private int outSize, numIntervals, numEPs;
    private double[][] outputData, graphData;
    private boolean[] stateArray;

    public TimeProfileColorer(int outSize, int numIntervals, int numEPs, double[][] outputData, double[][] graphData, boolean[] stateArray){
        this.outSize = outSize;
        this.numIntervals = numIntervals;
        this.numEPs = numEPs;
        this.outputData = outputData;
        this.graphData = graphData;
        this.stateArray = stateArray;
    }

    public Paint[] getColorMap() {
        final int special = 2;
        Paint[] outColors = new Paint[outSize];
        for (int i = 0; i < numIntervals; i++) {
            int count = 0;
            for (int ep = 0; ep < numEPs + special; ep++) {
                if (stateArray[ep]) {
                    outputData[i][count] = graphData[i][ep];
                    if(ep == numEPs){
                        outColors[count++] = MainWindow.runObject[myRun].getOverheadColor();
                    }
                    else if (ep == numEPs+1){
                        outColors[count++] = MainWindow.runObject[myRun].getIdleColor();
                    }
                    else
                        outColors[count++] = MainWindow.runObject[myRun].getEPColorMap()[ep];
                }
            }
        }

        return outColors;
    }
}