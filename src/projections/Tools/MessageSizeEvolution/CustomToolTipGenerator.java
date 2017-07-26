package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
import projections.gui.U;

import java.text.DecimalFormat;

public class CustomToolTipGenerator
        extends StandardCategoryToolTipGenerator {
    private double[][] values;
    private long timeNumBins;
    private long timeBinSize;
    private long timeMinBinSize;
    private long msgNumBins;
    private long msgBinSize;
    private long msgMinBinSize;
    private boolean msgLogScale;

    CustomToolTipGenerator(double[][] values, long timeNumBins, long timeBinSize, long timeMinBinSize, long msgNumBins, long msgBinSize, long msgMinBinSize, boolean msgLogScale) {
        super();
        this.values = new double[values.length][values[0].length];
        for(int i = 0; i < values.length; i++) {
            System.arraycopy(values[i], 0, this.values[i], 0, values[i].length);
        }
        this.timeBinSize = timeBinSize;
        this.timeMinBinSize = timeMinBinSize;
        this.timeNumBins = timeNumBins;
        this.msgBinSize = msgBinSize;
        this.msgMinBinSize = msgMinBinSize;
        this.msgNumBins = msgNumBins;
        this.msgLogScale = msgLogScale;
    }

    @Override
    public String generateToolTip(CategoryDataset dataset, int row, int column) {
        DecimalFormat _format = new DecimalFormat();
        String toolTip = "<html>";

        if(row < timeNumBins)
            toolTip += "Time Bin: " + U.humanReadableString(row * timeBinSize + timeMinBinSize) +
                    " to " + U.humanReadableString((row + 1) * timeBinSize + timeMinBinSize);
        else
            toolTip += "Time Bin: > " + U.humanReadableString(timeNumBins * timeBinSize + timeMinBinSize);

        toolTip += "<br>";

        if(column > 0) {
            if(msgLogScale)
                toolTip += "Message Size Bin: " + _format.format(msgMinBinSize * Math.pow(2, msgNumBins - column)) +
                        " bytes to " + _format.format(msgMinBinSize * Math.pow(2, msgNumBins - column + 1)) + " bytes";
            else
                toolTip += "Message Size Bin: " + _format.format(msgMinBinSize + (msgNumBins - column) * msgBinSize) +
                        " bytes to " + _format.format(msgMinBinSize + (msgNumBins - column + 1) * msgBinSize) + " bytes";
        } else {
            if(msgLogScale)
                toolTip += "Message Size Bin: > " + _format.format(msgMinBinSize * Math.pow(2, msgNumBins)) +
                        " bytes";
            else
                toolTip += "Message Size Bin: " + _format.format(msgMinBinSize + msgNumBins * msgBinSize) +
                        " bytes";
        }

        toolTip += "<br>";

        toolTip += "Count: " + values[row][column];

        toolTip += "<html>";

        return toolTip;
    }
}
