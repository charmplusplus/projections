package projections.gui.Timeline;

import java.text.DecimalFormat;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import javax.swing.*;
import java.awt.FontMetrics;

public class AxisPanel extends JPanel 
{

	/** Desired height of the whole JPanel */
	private int totalHeight() {
		if(data.useMinimalView())
			return axispos()+largeTickHalfLength;
		else
			return 10+axispos()+largeTickHalfLength;
	}
	
	/** Distance from top to the horizontal line **/
	private int axispos() {
		if(data.useMinimalView())
			return 2 + largeTickHalfLength + textpos();
		else
			return 5 + largeTickHalfLength + textpos();
	}
	
	
	/** Baseline for the labels (pixels from top) */
	private int textpos() {
		if(data.useMinimalView())
			return data.axisFont.getSize();
		else
			return 5+data.axisFont.getSize();
	}
	
	/** The distance the small tick marks extend from the horizontal line */
	private int smallTickHalfLength = 2;

	/** The distance the large tick marks extend from the horizontal line */
	private int largeTickHalfLength = 5;

	private static final long serialVersionUID = 1L;

	// Temporary hardcode. This variable will be assigned appropriate
	// meaning in future versions of Projections that support multiple
	// runs.
	int myRun = 0;

	private Data  data;

	private DecimalFormat format_= new DecimalFormat();

	public AxisPanel(Data data)
	{
		this.data = data;
		format_.setGroupingUsed(true);

	}   


	/** Get the preferred size. The Width provided should likely be ignored */
	public Dimension getPreferredSize() {
		int preferredWidth = 200;
		int preferredHeight = totalHeight();
		return new Dimension(preferredWidth, preferredHeight);
	}


	// given a coordinate in coordinates from beginning of 
	// TimelineAxisCanvas, return the time in us
	public double canvasToTime(int x) {
		return (x-data.leftOffset())/(double)(getWidth()-data.leftOffset()-data.rightOffset())*
		(data.endTime()-data.beginTime())+data.beginTime();
	}


	// return the int value of the HSB to make the image start at the 
	// requested time
	public int calcHSBOffset(double startTime) {
		double percentOffset = 
			(startTime-data.beginTime())/(data.totalTime());
		double actualOffset = percentOffset*(getWidth() - data.leftOffset()-data.rightOffset());
		return (int)(actualOffset + data.leftOffset() + 0.5);
	}

	/** Paint the axis in its panel 
	 *
	 */

	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		
		g.setFont(data.axisFont);
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(data.getBackgroundColor());
		Rectangle clipBounds = g.getClipBounds();
		g.fillRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

		long labeloffset = data.timeIncrement(getWidth()) - 
		(data.beginTime() % data.timeIncrement(getWidth()));
		if (labeloffset == data.timeIncrement(getWidth())) {
			labeloffset = 0;
		}

		int maxx = getWidth();

		g.setColor(data.getForegroundColor());
		g.drawLine(data.offset(), axispos(), maxx-data.offset(), axispos());

		int curx;
		String tmp;
		for (int x=0; x<data.numIntervals(getWidth()); x++) {
			curx = data.offset() + (int)(x*data.pixelIncrement(getWidth())) 
			+ (int)(labeloffset * 
					data.pixelIncrement(getWidth())/data.timeIncrement(getWidth()));
			if (curx > maxx) {
				break;
			}
			if (x % data.labelIncrement(getWidth()) == 0) {  
				tmp = format_.format((long)(data.beginTime() + 
						labeloffset + 
						(long)x * data.timeIncrement(getWidth())));
				g.drawLine(curx, axispos()-largeTickHalfLength, curx, axispos() + largeTickHalfLength);
				g.drawString(tmp, curx - fm.stringWidth(tmp)/2, 
						textpos());
			} else {
				g.drawLine(curx, axispos()-smallTickHalfLength, curx, axispos()+smallTickHalfLength);
			}
		}


	}




}
