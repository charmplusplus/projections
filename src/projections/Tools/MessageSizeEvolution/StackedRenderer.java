package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

import java.awt.Color;
import java.awt.Paint;

class StackedRenderer
        extends StackedBarRenderer {

    private double[][] values;
    private double logMax;
    private GrayPaintScale paintScale;

    StackedRenderer(double[][] values, double maxVal) {
        super();
        setDrawBarOutline(true);
        setBarPainter(new StandardBarPainter());
        setShadowVisible(false);
        setSeriesPaint(0, Color.BLUE);
        this.values = new double[values.length][values[0].length];
        logMax = Math.log(maxVal + 1) / Math.log(2);
        for(int i = 0; i < values.length; i++) {
            System.arraycopy(values[i], 0, this.values[i], 0, values[i].length);
        }
        paintScale = new GrayPaintScale(0, logMax);
    }

    @Override
    public Paint getItemPaint(final int row, final int col) {
        // get opposite value of scale so that white = min, black = max
        return paintScale.getPaint(logMax - (Math.log(values[row][col] + 1) / Math.log(2)));
    }

    @Override
    public Paint getItemOutlinePaint(int row, int column) {
        return new Color(225, 225, 225);
    }
}
