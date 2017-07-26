package projections.Tools.MessageSizeEvolution;

import org.jfree.chart.renderer.GrayPaintScale;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

import java.awt.Color;
import java.awt.Paint;

class StackedRenderer
        extends StackedBarRenderer {

    private static final double DIV_LOG2 = 1.0 / Math.log(2);

    private final int[][] values;
    private final double logMax;
    private final GrayPaintScale paintScale;

    StackedRenderer(int[][] values, double maxVal) {
        super();
        setDrawBarOutline(true);
        setBarPainter(new StandardBarPainter());
        setShadowVisible(false);
        setSeriesPaint(0, Color.BLUE);
        logMax = Math.log(maxVal + 1) * DIV_LOG2;
        this.values = values;
        paintScale = new GrayPaintScale(0, logMax);
    }

    @Override
    public Paint getItemPaint(final int row, final int col) {
        // get opposite value of scale so that white = min, black = max
        return paintScale.getPaint(logMax - (Math.log(values[row][col] + 1) * DIV_LOG2));
    }

    @Override
    public Paint getItemOutlinePaint(int row, int column) {
        return new Color(225, 225, 225);
    }
}
