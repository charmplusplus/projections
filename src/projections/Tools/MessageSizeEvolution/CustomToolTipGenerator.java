package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
import projections.gui.U;

import java.text.DecimalFormat;

public class CustomToolTipGenerator
        extends StandardCategoryToolTipGenerator {
    private int[][] values;
    private long timeNumBins;
    private long timeBinSize;
    private long timeMinBinSize;
    private long msgNumBins;
    private long msgBinSize;
    private long msgMinBinSize;
    private boolean msgLogScale;

    CustomToolTipGenerator(int[][] values, long timeNumBins, long timeBinSize, long timeMinBinSize, long msgNumBins, long msgBinSize, long msgMinBinSize, boolean msgLogScale) {
        super();
        this.values = values;
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
        StringBuilder builder = new StringBuilder("<html>");

        if(row < timeNumBins)
            builder.append("Time Bin: ").append(U.humanReadableString(row * timeBinSize + timeMinBinSize)).
                    append(" to ").append(U.humanReadableString((row + 1) * timeBinSize + timeMinBinSize));
        else
            builder.append("Time Bin: > ").append(U.humanReadableString(timeNumBins * timeBinSize + timeMinBinSize));

        builder.append("<br>");

        if(column > 0) {
            if(msgLogScale)
                builder.append("Message Size Bin: ").append(_format.format(msgMinBinSize * Math.pow(2, msgNumBins - column))).
                        append(" bytes to ").append(_format.format(msgMinBinSize * Math.pow(2, msgNumBins - column + 1)) + " bytes");
            else
                builder.append("Message Size Bin: ").append(_format.format(msgMinBinSize + (msgNumBins - column) * msgBinSize)).
                        append(" bytes to ").append(_format.format(msgMinBinSize + (msgNumBins - column + 1) * msgBinSize) + " bytes");
        } else {
            if(msgLogScale)
                builder.append("Message Size Bin: > ").append(_format.format(msgMinBinSize * Math.pow(2, msgNumBins))).
                        append(" bytes");
            else
                builder.append("Message Size Bin: ").append(_format.format(msgMinBinSize + msgNumBins * msgBinSize)).
                        append(" bytes");
        }


        builder.append("<br>Count: ");

        builder.append(values[row][column]);

        builder.append("<html>");

        return builder.toString();
    }
}
